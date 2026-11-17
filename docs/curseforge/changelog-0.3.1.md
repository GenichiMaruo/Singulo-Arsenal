# Singulo Arsenal 0.3.1

Compatibility update for Singulo 0.20.0.

- Fix SQUID pick sensitivity modules on Singulo 0.20.0, whose module registry IDs were renamed. Both old and new IDs are accepted.
- Equipment balance, recipes, addon registry IDs and saved data remain unchanged from 0.3.0.
- Requires Singulo 0.19.0 or newer without an upper bound. Integration CI tests pinned 0.20.0 source, not future versions.
- Back up worlds before upgrading. Existing 0.3.0 cannon migration and downgrade limitations remain applicable.
- On Minecraft 1.20.1, external datapack item-tag elements using `singulo_arsenal:jet_lance` must use `singulo_arsenal:jet_cannon`; tag names and recipe IDs stay unchanged.
