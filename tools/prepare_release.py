#!/usr/bin/env python3
"""Validate a production release; explicitly opt into CF preflight or a single upload."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import tomllib
import urllib.error
import urllib.request
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CF_VERSIONS = 'https://minecraft.curseforge.com/api/game/versions'
CF_PROJECT_ID = '1793647'
CF_VERSION_NAMES = ['1.21.1', 'NeoForge', 'Java 21']


def prepare(root, jar_dir, release_type):
    if release_type not in ('alpha', 'beta', 'release'):
        raise ValueError('Release type must be alpha, beta or release')
    properties = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text(encoding='utf-8').splitlines()
                      if line and not line.startswith('#') and '=' in line)
    version = properties['mod_version']
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?', version):
        raise ValueError('mod_version must be a version such as 0.2.0')
    if (properties['mod_name'], properties['minecraft_version'], properties['mod_loader_suffix']) != ('Singulo-Arsenal', '1.21.1', 'N'):
        raise ValueError('This publisher supports Singulo Arsenal for Minecraft 1.21.1 / NeoForge only')
    name = f'Singulo-Arsenal-{version}-1.21.1N'
    jar = jar_dir / f'{name}.jar'
    changelog = root / 'docs' / 'curseforge' / f'changelog-{version}.md'
    if not changelog.is_file() or not changelog.read_text(encoding='utf-8').strip():
        raise ValueError(f'Missing or empty changelog: {changelog.name}')
    with zipfile.ZipFile(jar) as archive:
        entries = archive.namelist()
        metadata = tomllib.loads(archive.read('META-INF/neoforge.mods.toml').decode('utf-8'))
        if [(m['modId'], m['version']) for m in metadata['mods']] != [('singulo_arsenal', version)]:
            raise ValueError('The JAR mod ID/version does not match gradle.properties')
        dependencies = {d['modId']: d for d in metadata['dependencies']['singulo_arsenal']}
        expected = {'singulo': properties['singulo_version_range'], 'minecraft': properties['minecraft_version_range'],
                    'neoforge': f"[{properties['neo_version']},)"}
        for mod, required_range in expected.items():
            dep = dependencies.get(mod, {})
            if dep.get('type') != 'required' or dep.get('side') != 'BOTH' or dep.get('versionRange') != required_range:
                raise ValueError(f'The JAR has incorrect dependency metadata: {mod}')
        if 'io/github/genichimaruo/singuloarsenal/SinguloArsenal.class' not in entries:
            raise ValueError('The JAR is missing the addon entry point')
        if any('/gametest/' in p or '/review/' in p or p.endswith('/structure/empty.nbt')
               or p.startswith('io/github/genichimaruo/singulo/') for p in entries):
            raise ValueError('The JAR contains test/review assets or bundled Singulo classes')
    return {'artifact_name': name, 'version': version, 'jar': str(jar), 'changelog': str(changelog),
            'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'release_type': release_type}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # The token belongs to this API endpoint only.
        return None


def api_json(url, token):
    request = urllib.request.Request(url, headers={'X-Api-Token': token, 'User-Agent': 'Singulo-Arsenal-release-check'})
    try:
        with urllib.request.build_opener(NoRedirect()).open(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise ValueError(f'CurseForge version lookup returned HTTP {error.code}') from None
    except (urllib.error.URLError, TimeoutError, OSError, ValueError):
        raise ValueError('CurseForge version lookup failed; no upload was attempted') from None


def check_api(project_id, token):
    if not re.fullmatch(r'[1-9][0-9]*', project_id):
        raise ValueError('CURSEFORGE_PROJECT_ID must be a positive numeric project ID')
    if project_id != CF_PROJECT_ID:
        raise ValueError('CURSEFORGE_PROJECT_ID does not match the Singulo Arsenal project')
    if not token.strip():
        raise ValueError('CURSEFORGE_TOKEN is missing in the curseforge Environment')
    versions = api_json(CF_VERSIONS, token)
    types = api_json('https://minecraft.curseforge.com/api/game/version-types', token)
    if not isinstance(versions, list) or not isinstance(types, list):
        raise ValueError('CurseForge returned an unexpected version catalogue')
    # The same display name can belong to Minecraft, Bukkit or addon version types.
    ids = []
    for name, category in zip(CF_VERSION_NAMES, ['minecraft', 'modloader', 'java']):
        type_ids = {entry['id'] for entry in types if isinstance(entry, dict) and type(entry.get('id')) is int
                    and isinstance(entry.get('slug'), str) and entry['slug'].startswith(category)}
        matches = {entry['id'] for entry in versions if isinstance(entry, dict) and entry.get('name') == name
                   and type(entry.get('id')) is int and entry['id'] > 0 and entry.get('gameVersionTypeID') in type_ids}
        if len(matches) != 1:
            raise ValueError(f'CurseForge has no unique {category} version for {name}')
        ids.append(matches.pop())
    return ids


def upload(plan, project_id, token):
    # Always validate the target and API configuration before the only write request.
    version_ids = check_api(project_id, token)
    metadata = {
        'displayName': plan['artifact_name'],
        'releaseType': plan['release_type'],
        'changelogType': 'markdown',
        'changelog': Path(plan['changelog']).read_text(encoding='utf-8'),
        'gameVersions': version_ids,
        'relations': {'projects': [{'slug': 'singulo', 'type': 'requiredDependency'}]},
    }
    jar = Path(plan['jar'])
    jar_bytes = jar.read_bytes()
    if hashlib.sha256(jar_bytes).hexdigest() != plan['sha256']:
        raise ValueError('The JAR changed after validation')
    boundary = 'singulo-arsenal-' + uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="metadata"\r\n'
            'Content-Type: application/json; charset=utf-8\r\n\r\n').encode()
    body += json.dumps(metadata, ensure_ascii=False).encode('utf-8')
    body += (f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{jar.name}"\r\n'
             'Content-Type: application/java-archive\r\n\r\n').encode()
    body += jar_bytes + f'\r\n--{boundary}--\r\n'.encode()
    request = urllib.request.Request(f'https://minecraft.curseforge.com/api/projects/{project_id}/upload-file',
                                     data=body, method='POST', headers={
                                         'X-Api-Token': token,
                                         'User-Agent': 'Singulo-Arsenal-publisher',
                                         'Content-Type': f'multipart/form-data; boundary={boundary}',
                                     })
    try:
        # No retries, redirects or dependency-removal fallbacks: an ambiguous response may have created a file.
        with urllib.request.build_opener(NoRedirect()).open(request, timeout=120) as response:
            result = json.load(response)
    except urllib.error.HTTPError as error:
        detail = ''
        try:
            response_error = json.loads(error.read(8192))
            if isinstance(response_error, dict):
                code = response_error.get('errorCode')
                message = response_error.get('errorMessage')
                if type(code) is int:
                    detail += f' (API code {code})'
                if isinstance(message, str):
                    # Only the documented error message; never headers or raw response bodies.
                    message = message.replace(token, '[redacted]').replace(project_id, '[project]')
                    detail += ': ' + ' '.join(message.split())[:500]
        except (ValueError, OSError):
            pass
        raise ValueError(f'CurseForge upload returned HTTP {error.code}{detail}; check the Files page before retrying') from None
    except (urllib.error.URLError, TimeoutError, OSError, ValueError):
        raise ValueError('CurseForge upload result is unknown; check the Files page before retrying') from None
    file_id = result.get('id') if isinstance(result, dict) else None
    if type(file_id) is not int or file_id <= 0:
        raise ValueError('CurseForge returned no valid file ID; check the Files page before retrying')
    return f'https://www.curseforge.com/minecraft/mc-mods/singulo-arsenal/files/{file_id}'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar-dir', type=Path, default=Path('build/libs'))
    parser.add_argument('--release-type', choices=('alpha', 'beta', 'release'), default='beta')
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--check-api', action='store_true', help='Check Environment secrets and CF versions with a read-only GET')
    mode.add_argument('--upload', action='store_true', help='Publish the validated JAR with one POST (no retries)')
    args = parser.parse_args()
    try:
        plan = prepare(ROOT, args.jar_dir, args.release_type)
        if args.check_api:
            ids = check_api(os.environ.get('CURSEFORGE_PROJECT_ID', ''), os.environ.get('CURSEFORGE_TOKEN', ''))
            print('CurseForge version IDs: ' + ', '.join(f'{name}={value}' for name, value in zip(CF_VERSION_NAMES, ids)))
        elif args.upload:
            if os.environ.get('GITHUB_REF') != 'refs/heads/1.21.1':
                raise ValueError('Uploads require the 1.21.1 release branch')
            plan['file_url'] = upload(plan, os.environ.get('CURSEFORGE_PROJECT_ID', ''), os.environ.get('CURSEFORGE_TOKEN', ''))
    except (ValueError, KeyError, OSError, zipfile.BadZipFile) as error:
        print(f'Release validation failed: {error}', file=sys.stderr)
        return 1
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a', encoding='utf-8') as output:
            for key, value in plan.items():
                output.write(f'{key}={value}\n')
    summary = (f"JAR: `{Path(plan['jar']).name}`\n\nSHA-256: `{plan['sha256']}`\n\n"
               f"Channel: {plan['release_type']}\n\nMinecraft 1.21.1 / NeoForge / Java 21; required dependency: Singulo.\n\n"
               f"Changelog: `{Path(plan['changelog']).name}`\n")
    if os.environ.get('GITHUB_SHA'):
        summary += f"\nCommit: `{os.environ['GITHUB_SHA']}`\n"
    if args.check_api:
        summary += '\nCurseForge version lookup with the configured token succeeded (GET only; upload permission is not tested).\n'
    if args.upload:
        summary += f"\nCurseForge file: {plan['file_url']}\n"
    print(summary)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as output:
            output.write(summary)
    return 0


if __name__ == '__main__':
    sys.exit(main())
