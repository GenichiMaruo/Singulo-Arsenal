# Singulo 0.19 compatibility for Arsenal 0.3.0

## Required dependency

Both sides require Singulo `[0.19.0,0.20.0)`, with the matching Minecraft and
loader artifact. This raises the minimum to 0.19.0 and retains the established
next-minor upper bound: future untested API changes are not silently accepted.
CurseForge also declares Singulo as a required project dependency. The JAR's
loader metadata enforces the version interval; a CurseForge project relation
alone cannot express it.

The add-on does not bundle Singulo classes and accesses its ingredients, tags,
FE capabilities and optional rendering interface by their existing contracts.
The plain nihonium set consumes `singulo:nihonium_ingot`, uses
`c:ingots/nihonium` for repair, and has no FE capability or module slots.

## Release gate

The initially pinned source is the 0.19 candidate
`d12d72c0b312931276b655bb30062a09be591502`. This is not a claim that this candidate
is the final released dependency. Before merge/publication, record the final
source/CI/CurseForge IDs and compare the downloaded public JAR to the CI artifact,
then rerun integration and client verification against that exact dependency.

The migration and rendering checks from earlier 0.3.0 work remain relevant but
do not replace validation against the final 0.19 binaries. Preserve the old
cannon recipe/advancement IDs while remapping its item ID. Backups are required
before upgrades; new canonical IDs are not downgrade-compatible with 0.2.2.
