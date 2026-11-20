# Singulo 0.21 compatibility

Arsenal 0.3.2 preserves the existing equipment implementation, registry IDs, recipes and save formats. The loader dependency stays `[0.19.0,)`; the open range is not a certification of future releases.

Validation gates: build/offline resource regression, production-JAR metadata/exclusion and publisher regression tests, all 66 required integration GameTests with the matching pinned 0.21 base, and a reviewed final base release. Coverage includes SMES charging, module sensitivity, recipe loading, equipment state retention and material IDs including official legacy aliases.

No new client visual validation is claimed by server GameTests. Existing rendering and migration reports remain historical evidence. Before publication, record exact final base and addon refs and passing checks; candidates are not final releases.
