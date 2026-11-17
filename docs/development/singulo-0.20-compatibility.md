# Singulo 0.20 compatibility

Arsenal 0.3.1 is a compatibility-only update of 0.3.0. Except for accepting renamed scanner-module IDs, production equipment behavior, recipes, addon resource IDs, save formats, and dependency range `[0.19.0,)` remain unchanged.

Integration CI pins matching Singulo 0.20.0 source. Release requires successful build/integration at the final addon head and release-workflow checks, plus availability of the matching base release.

Coverage includes all 65 required GameTests, SMES FE interoperability and equipment state retention, recipe loading, resource/ingredient-ID checks, unit tests, and production-JAR metadata/exclusion checks. A source build is not a client visual review.

Reviewed reflective client entry points `GravitationalLensing.add(Vec3,float,float,float)` and `addDistortion(Vec3,float,float,float)` must be checked in the final base source. No rendering behavior changes. Historical client/migration verification remains in the 0.19 report, not represented as new 0.20 client testing.

Future Singulo versions are not certified by the open dependency range. Existing 0.3.0 migration precautions and the 1.20.1 legacy datapack item-tag limitation remain applicable.

Recipe references retain the old base IDs to support 0.19.0. On 0.20.0 the base registers their official aliases. Ingredient validation accepts only BLOCKS/ITEMS aliases whose canonical target exists; runtime GameTests also verify recipe loading.

The initial 0.20 integration run correctly failed the sensitivity-module GameTest: canonical renamed module IDs did not match the old string switch. The fix accepts both 0.19 and 0.20 names, retaining the same tier/consumption rules. The unchanged regression test exercises real module use and prevents publication until passing.

Initial 1.21.1 verification: addon head `9c27e7f`, workflow run `44021720464`: build and all 65 integration GameTests passed against base candidate `5eddee390c3847a46e372187dc2c6c2105d549e8`. Final release still requires the final merged base pin and a fresh successful run. Local release/review script regression tests passed (26 + 3 tests) on each version branch.

Final 1.20.1 base pin: merged Singulo `f05ed56af936e1285708ee6ef9f3b67fbb81eef2`. Both loader candidate integrations passed; final CI and matching public base files are required before addon publication.
