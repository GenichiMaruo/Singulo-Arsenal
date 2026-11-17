# Singulo 0.20 compatibility

Arsenal 0.3.1 is a compatibility-only update of 0.3.0. Production equipment implementation, recipes, resource IDs, save formats, and dependency range `[0.19.0,)` remain unchanged.

Integration CI pins matching Singulo 0.20.0 source. Release requires successful build/integration at the final addon head and release-workflow checks, plus availability of the matching base release.

Coverage includes all 65 required GameTests, SMES FE interoperability and equipment state retention, recipe loading, resource/ingredient-ID checks, unit tests, and production-JAR metadata/exclusion checks. A source build is not a client visual review.

Reflective client entry points `GravitationalLensing.add(Vec3,float,float,float)` and `addDistortion(Vec3,float,float,float)` must be checked in the final base source. No rendering behavior changes. Historical client/migration verification remains in the 0.19 report, not represented as new 0.20 client testing.

Future Singulo versions are not certified by the open dependency range. Existing 0.3.0 migration precautions and the 1.20.1 legacy datapack item-tag limitation remain applicable.

Recipe references retain the old base IDs to support 0.19.0. On 0.20.0 the base registers their official aliases. Ingredient validation accepts only BLOCKS/ITEMS aliases whose canonical target exists; runtime GameTests also verify recipe loading.
