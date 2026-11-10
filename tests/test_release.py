"""Regression tests for selecting a production release and the read-only CF preflight."""
import hashlib
from contextlib import redirect_stderr, redirect_stdout
from email import policy
from email.parser import BytesParser
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('prepare_release', ROOT / 'tools' / 'prepare_release.py')
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)

PROPERTIES = '''mod_name=Singulo-Arsenal
mod_version=0.2.0
minecraft_version=1.21.1
minecraft_version_range=[1.21.1]
mod_loader_suffix=N
neo_version=21.1.256
singulo_version_range=[0.16.0,0.17.0)
'''
METADATA = '''[[mods]]
modId="singulo_arsenal"
version="0.2.0"
[[dependencies.singulo_arsenal]]
modId="minecraft"
type="required"
side="BOTH"
versionRange="[1.21.1]"
[[dependencies.singulo_arsenal]]
modId="neoforge"
type="required"
side="BOTH"
versionRange="[21.1.256,)"
[[dependencies.singulo_arsenal]]
modId="singulo"
type="required"
side="BOTH"
versionRange="[0.16.0,0.17.0)"
'''
ENTRY_POINT = 'io/github/genichimaruo/singuloarsenal/SinguloArsenal.class'
VERSION_TYPES = [{'id': i, 'slug': slug} for i, slug in [(1, 'minecraft-1-21'), (2, 'modloader'), (3, 'java'), (4, 'bukkit'), (5, 'environment')]]
VERSIONS = [{'id': i, 'name': name, 'gameVersionTypeID': group} for i, name, group in
            [(100, '1.21.1', 1), (200, 'NeoForge', 2), (300, 'Java 21', 3), (400, '1.21.1', 4), (500, 'Client', 5), (501, 'Server', 5)]]


class ArtifactTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        (self.root / 'gradle.properties').write_text(PROPERTIES, encoding='utf-8')
        self.changelog = self.root / 'docs' / 'curseforge' / 'changelog-0.2.0.md'
        self.changelog.parent.mkdir(parents=True)
        self.changelog.write_text('Release notes', encoding='utf-8')
        self.jars = self.root / 'build' / 'libs'
        self.jars.mkdir(parents=True)
        self.jar = self.jars / 'Singulo-Arsenal-0.2.0-1.21.1N.jar'
        self.write_jar()

    def write_jar(self, metadata=METADATA, extra=None, entry_point=True):
        with zipfile.ZipFile(self.jar, 'w') as archive:
            archive.writestr('META-INF/neoforge.mods.toml', metadata)
            if entry_point:
                archive.writestr(ENTRY_POINT, b'class fixture')
            if extra:
                archive.writestr(extra, b'unwanted fixture')

    def prepare(self):
        return release.prepare(self.root, self.jars, 'beta')

    def test_selects_exact_current_jar_and_hash(self):
        # A stale JAR and a sources JAR must never enter the published file list.
        (self.jars / 'Singulo-Arsenal-0.1.0-1.21.1N.jar').write_bytes(b'stale')
        (self.jars / 'Singulo-Arsenal-0.2.0-1.21.1N-sources.jar').write_bytes(b'sources')
        plan = self.prepare()
        self.assertEqual(Path(plan['jar']), self.jar)
        self.assertEqual(Path(plan['changelog']), self.changelog)
        self.assertEqual(plan['artifact_name'], self.jar.stem)
        self.assertEqual(plan['sha256'], hashlib.sha256(self.jar.read_bytes()).hexdigest())

    def test_old_jar_cannot_replace_missing_current_jar(self):
        self.jar.rename(self.jars / 'Singulo-Arsenal-0.1.0-1.21.1N.jar')
        with self.assertRaises(FileNotFoundError):
            self.prepare()

    def test_rejects_renamed_old_release_and_wrong_mod(self):
        for old, new in [('version="0.2.0"', 'version="0.1.0"'), ('modId="singulo_arsenal"', 'modId="singulo"')]:
            with self.subTest(new=new):
                self.write_jar(METADATA.replace(old, new, 1))
                with self.assertRaisesRegex(ValueError, 'mod ID/version'):
                    self.prepare()

    def test_rejects_incorrect_dependency_requirements(self):
        for old, new in [('type="required"', 'type="optional"'), ('side="BOTH"', 'side="CLIENT"'),
                         ('[1.21.1]', '[1.20.1]'), ('[21.1.256,)', '[21.1,)'), ('[0.16.0,0.17.0)', '[0.15.0,)')]:
            with self.subTest(new=new):
                self.write_jar(METADATA.replace(old, new))
                with self.assertRaisesRegex(ValueError, 'dependency metadata'):
                    self.prepare()

    def test_rejects_missing_singulo_dependency(self):
        self.write_jar(METADATA.rsplit('[[dependencies.singulo_arsenal]]', 1)[0])
        with self.assertRaisesRegex(ValueError, 'dependency metadata: singulo'):
            self.prepare()

    def test_rejects_review_test_and_bundled_base_classes(self):
        for entry in ['io/github/genichimaruo/singuloarsenal/gametest/Tests.class',
                      'io/github/genichimaruo/singuloarsenal/review/Review.class',
                      'data/singulo_arsenal/structure/empty.nbt',
                      'io/github/genichimaruo/singulo/Singulo.class']:
            with self.subTest(entry=entry):
                self.write_jar(extra=entry)
                with self.assertRaisesRegex(ValueError, 'test/review assets or bundled Singulo'):
                    self.prepare()

    def test_rejects_missing_entry_point(self):
        self.write_jar(entry_point=False)
        with self.assertRaisesRegex(ValueError, 'entry point'):
            self.prepare()

    def test_requires_nonempty_version_specific_changelog(self):
        self.changelog.write_text(' \n', encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'changelog'):
            self.prepare()
        self.changelog.unlink()
        with self.assertRaisesRegex(ValueError, 'changelog'):
            self.prepare()

    def test_rejects_unsupported_platform_and_version_paths(self):
        for old, new in [('1.21.1', '1.20.1'), ('mod_loader_suffix=N', 'mod_loader_suffix=F'),
                         ('mod_version=0.2.0', 'mod_version=../../0.2.0')]:
            with self.subTest(new=new):
                (self.root / 'gradle.properties').write_text(PROPERTIES.replace(old, new), encoding='utf-8')
                with self.assertRaises(ValueError):
                    self.prepare()


class ApiTests(unittest.TestCase):
    def test_preflight_only_uses_get_and_token_header(self):
        with patch.object(release.urllib.request, 'build_opener') as opener:
            opener.return_value.open.side_effect = [io.BytesIO(json.dumps(x).encode()) for x in [VERSIONS, VERSION_TYPES]]
            self.assertEqual(release.check_api('1793647', 'test-token'), [100, 200, 300, 500, 501])
            self.assertEqual(opener.return_value.open.call_count, 2)
            request = opener.return_value.open.call_args_list[0].args[0]
            self.assertEqual(request.get_method(), 'GET')
            self.assertIsNone(request.data)
            self.assertEqual(request.full_url, release.CF_VERSIONS)
            self.assertEqual(request.get_header('X-api-token'), 'test-token')
            self.assertEqual(opener.return_value.open.call_args.kwargs['timeout'], 30)
            self.assertIsInstance(opener.call_args.args[0], release.NoRedirect)

    def test_missing_credentials_and_wrong_project_fail_before_network(self):
        with patch.object(release.urllib.request, 'build_opener') as opener:
            for project, token in [('', 'test-token'), ('wrong', 'test-token'), ('123', 'test-token'), ('1793647', '')]:
                with self.subTest(project=project, token_present=bool(token)):
                    with self.assertRaises(ValueError):
                        release.check_api(project, token)
            opener.assert_not_called()

    def test_missing_versions_and_malformed_responses_fail(self):
        for response in [b'not json', b'{}', b'[{"name":"1.21.1"}]', b'[{"name":[]}]']:
            with self.subTest(response=response), patch.object(release.urllib.request, 'build_opener') as opener:
                opener.return_value.open.return_value = io.BytesIO(response)
                with self.assertRaises(ValueError):
                    release.check_api('1793647', 'test-token')

    def test_http_errors_do_not_reveal_request_or_response_secrets(self):
        error = urllib.error.HTTPError(release.CF_VERSIONS, 401, 'test-secret', {}, io.BytesIO(b'test-secret'))
        with patch.object(release.urllib.request, 'build_opener') as opener:
            opener.return_value.open.side_effect = error
            with self.assertRaises(ValueError) as result:
                release.check_api('1793647', 'test-secret')
            self.assertEqual(str(result.exception), 'CurseForge version lookup returned HTTP 401')

    def test_redirects_cannot_forward_the_token(self):
        request = urllib.request.Request(release.CF_VERSIONS, headers={'X-Api-Token': 'test-token'})
        self.assertIsNone(release.NoRedirect().redirect_request(request, None, 302, '', {}, 'https://example.com'))

    def test_ambiguous_minecraft_versions_are_rejected(self):
        duplicate = {'id': 101, 'name': '1.21.1', 'gameVersionTypeID': 1}
        with patch.object(release, 'api_json', side_effect=[VERSIONS + [duplicate], VERSION_TYPES]):
            with self.assertRaisesRegex(ValueError, 'no unique minecraft version'):
                release.check_api('1793647', 'test-token')


class UploadTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        self.jar = root / 'Singulo-Arsenal-0.2.0-1.21.1N.jar'
        self.jar.write_bytes(b'jar fixture\x00\xff')
        self.changelog = root / 'changelog-0.2.0.md'
        self.changelog.write_text('# 0.2.0\n日本語の変更履歴', encoding='utf-8')
        self.plan = {'jar': str(self.jar), 'changelog': str(self.changelog), 'artifact_name': self.jar.stem,
                     'version': '0.2.0', 'release_type': 'beta',
                     'sha256': hashlib.sha256(self.jar.read_bytes()).hexdigest()}

    def test_multipart_publishes_exact_bytes_and_required_dependency(self):
        with patch.object(release.urllib.request, 'build_opener') as opener:
            opener.return_value.open.side_effect = [io.BytesIO(json.dumps(x).encode()) for x in [VERSIONS, VERSION_TYPES, {'id': 1234}]]
            url = release.upload(self.plan, '1793647', 'test-token')
            requests = [call.args[0] for call in opener.return_value.open.call_args_list]
            self.assertEqual([r.get_method() for r in requests], ['GET', 'GET', 'POST'])
            request = requests[2]
            self.assertEqual(request.full_url, 'https://minecraft.curseforge.com/api/projects/1793647/upload-file')
            self.assertEqual(request.get_header('X-api-token'), 'test-token')
            message = BytesParser(policy=policy.default).parsebytes(
                ('Content-Type: ' + request.get_header('Content-type') + '\r\nMIME-Version: 1.0\r\n\r\n').encode() + request.data)
            parts = {part.get_param('name', header='content-disposition'): part for part in message.iter_parts()}
            self.assertEqual(set(parts), {'metadata', 'file'})
            self.assertEqual(parts['file'].get_filename(), self.jar.name)
            self.assertEqual(parts['file'].get_payload(decode=True), self.jar.read_bytes())
            metadata = json.loads(parts['metadata'].get_payload(decode=True))
            self.assertEqual(metadata, {
                'displayName': self.jar.stem, 'releaseType': 'beta', 'changelogType': 'markdown',
                'changelog': '# 0.2.0\n日本語の変更履歴',
                'gameVersions': [100, 200, 300, 500, 501],
                'relations': {'projects': [{'slug': 'singulo', 'type': 'requiredDependency'}]},
            })
            self.assertEqual(url, 'https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal/files/1234')

    def test_preflight_failure_never_posts(self):
        with patch.object(release, 'check_api', side_effect=ValueError('Rejected')), \
             patch.object(release.urllib.request, 'build_opener') as opener:
            with self.assertRaisesRegex(ValueError, 'Rejected'):
                release.upload(self.plan, '1793647', 'test-token')
            opener.assert_not_called()

    def test_changed_jar_never_posts(self):
        self.jar.write_bytes(b'changed')
        with patch.object(release, 'check_api', return_value=[100, 200, 300, 500, 501]), patch.object(release.urllib.request, 'build_opener') as opener:
            with self.assertRaisesRegex(ValueError, 'changed after validation'):
                release.upload(self.plan, '1793647', 'test-token')
            opener.assert_not_called()

    def test_upload_errors_do_not_retry_or_remove_dependencies(self):
        errors = [TimeoutError('test-secret'),
                  urllib.error.HTTPError('https://example.com', 400, 'invalid dependency', {}, io.BytesIO(b'test-secret'))]
        for error in errors:
            with self.subTest(error=type(error).__name__), patch.object(release, 'check_api', return_value=[100, 200, 300, 500, 501]), \
                 patch.object(release.urllib.request, 'build_opener') as opener:
                opener.return_value.open.side_effect = error
                with self.assertRaisesRegex(ValueError, 'check the Files page before retrying') as result:
                    release.upload(self.plan, '1793647', 'test-secret')
                self.assertNotIn('test-secret', str(result.exception))
                opener.return_value.open.assert_called_once()

    def test_invalid_upload_response_is_not_retried(self):
        for response in [b'not json', b'{}', b'{"id":true}', b'{"id":-1}']:
            with self.subTest(response=response), patch.object(release, 'check_api', return_value=[100, 200, 300, 500, 501]), \
                 patch.object(release.urllib.request, 'build_opener') as opener:
                opener.return_value.open.return_value = io.BytesIO(response)
                with self.assertRaisesRegex(ValueError, 'check the Files page before retrying'):
                    release.upload(self.plan, '1793647', 'test-token')
                opener.return_value.open.assert_called_once()

    def test_upload_error_reports_api_code_with_redacted_message(self):
        body = json.dumps({'errorCode': 1009, 'errorMessage': 'Invalid version test-secret 1793647'}).encode()
        error = urllib.error.HTTPError('https://example.com', 400, 'rejected', {}, io.BytesIO(body))
        with patch.object(release, 'check_api', return_value=[100, 200, 300, 500, 501]), \
             patch.object(release.urllib.request, 'build_opener') as opener:
            opener.return_value.open.side_effect = error
            with self.assertRaises(ValueError) as result:
                release.upload(self.plan, '1793647', 'test-secret')
            self.assertIn('API code 1009', str(result.exception))
            self.assertNotIn('test-secret', str(result.exception))
            self.assertNotIn('1793647', str(result.exception))

    def test_cli_default_and_dry_run_never_upload(self):
        for arguments in [[], ['--check-api']]:
            with self.subTest(arguments=arguments), patch.object(release.sys, 'argv', ['prepare_release.py', *arguments]), \
                 patch.object(release, 'prepare', return_value=self.plan), patch.object(release, 'check_api', return_value=[100, 200, 300, 500, 501]), \
                 patch.object(release, 'upload') as upload, patch.dict(release.os.environ, {}, clear=True), \
                 redirect_stdout(io.StringIO()):
                self.assertEqual(release.main(), 0)
                upload.assert_not_called()

    def test_cli_blocks_uploads_from_other_branches_and_tags(self):
        for ref in ['', 'refs/heads/feature/0.2.0', 'refs/tags/v0.2.0']:
            with self.subTest(ref=ref), patch.object(release.sys, 'argv', ['prepare_release.py', '--upload']), \
                 patch.object(release, 'prepare', return_value=self.plan), patch.object(release, 'upload') as upload, \
                 patch.dict(release.os.environ, {'GITHUB_REF': ref}, clear=True), redirect_stderr(io.StringIO()):
                self.assertEqual(release.main(), 1)
                upload.assert_not_called()


if __name__ == '__main__':
    unittest.main()
