# Singulo Arsenal — English guide

[README](../../README.md) · [日本語](ja.md) · [English](en.md) · [Specification and recipes (Japanese)](../arsenal.md)

This describes version 0.2.0. Features marked (0.2.0) are new in this version.

## What is this mod?

Singulo Arsenal is an add-on for [Singulo](https://github.com/GenichiMaruo/Singulo) that turns materials from your Singulo factory into **armor, weapons and tools**.
Each piece of gear is built around one piece of physics from the base mod. The superconducting suit expels magnetic fields and floats; the degeneracy armor stores impacts as pressure; the Collapsar armor carries a black hole on its back and changes with the phase of its nuclear-pasta lining.

The physics is the theme, but the numbers and behaviour are tuned for play.

## Requirements and installation

- Minecraft **1.20.1**, NeoForge **47.1.106+** or Forge **47.4.26+**, Java 17.
- Singulo **0.16.x** is required. Put the Singulo Arsenal JAR in `mods/` next to it.
- In multiplayer, install it on both the client and the server.
- Available in English and Japanese. Other languages fall back to English.

## Tiers

Gear comes in three tiers that follow the base mod's technology stages. Higher tiers are stronger and need more of Singulo's machines to make.

| Stage | Armor | Weapons | Tools (0.2.0) |
| --- | --- | --- | --- |
| 2 (cryogenics) | Meissner suit | Quench Saber | SQUID Prospector |
| 4 (time) | Degeneracy armor | Casimir Blade | Degeneracy Breaker, Shear Saw |
| 5 (singularity, Jet Forge) | Collapsar armor | Tidal Reaver, Accretion Jet Cannon | Accretion Drill |

Each crafting recipe is a drawing of the finished item, with the working part in the place it does its job. Use JEI in game; the full recipe layouts and costs are in the [specification](../arsenal.md) (Japanese).
In creative mode, the mod's own tab has every piece fully charged.

## Energy

- All gear runs on standard FE. Hold the item, **sneak and right-click the output face of a power source**, and it charges by an eighth of its capacity at a time.
- FE cannot be taken back out of gear. It never works as a battery for machines.
- Weapons and tools show their charge as the item bar; worn armor shows it on the HUD and in the tooltip.
- **Accretion**: worn Collapsar armor, and a Tidal Reaver, Accretion Jet Cannon or Accretion Drill in your main hand, turn mass pellets from your inventory into 500,000 FE each, one pellet per item per second, and only when the whole pellet fits.
- **Pellet Canister** (0.2.0): carries up to 4,096 mass pellets. Accretion draws on the canister first, so loose pellets stay free for crafting.
  - Right-click to store every pellet you carry; sneak and right-click to take one stack out.
  - In the inventory, drop pellets on the canister to store them, or right-click it with an empty cursor to take a stack.

## Armor

### Meissner suit (stage 2)

Superconductors expel magnetic fields and pin flux lines.

- **Observer**: shows nearby hostile mobs through walls.
- **Carapace**: deflects incoming arrows and other projectiles.
- **Greaves**: share energy evenly across the worn pieces, and you walk a little faster.
- **Levitators**: sneak in mid-air to sink slowly on pinned flux, with no fall damage.
- **Full set**: throws back anything that hits you in melee.

### Degeneracy armor (stage 4)

Degeneracy pressure will not compress, until the limit is passed and it collapses at once.

- **Cuirass**: turns heavy blows into stored pressure. At the limit the armor collapses, blasting everything nearby, and the effect is offline for a while.
- **Ability key (V)**: releases the stored pressure as a shockwave before it reaches the limit.
- The **Helm** filters out poison and wither, the **Tassets** let you brace while sneaking on the ground to cut damage and knockback, and the **Sabatons** turn a long fall into a shockwave.

### Collapsar armor (stage 5)

The top tier, with a contained black hole on its back.

- **Nuclear-pasta lining**: hits move it through the gnocchi phase (fast), spaghetti phase (longer reach) and lasagna phase (tough but slow). The HUD and the pattern on the chest show the current phase.
- **Energy shield**: with the full set, damage is paid for in FE instead of health, at a steep price.
- **Event horizon**: a killing blow is stopped once, with a short invulnerability and a strike against everything around you. It recharges over ten minutes.
- **Jet burst (V, needs the Jet Boots)**: launches you where you are looking and burns what is behind you. It also sheds built-up load.
- The **Corona** clears every harmful effect, and the **Jet Boots** remove fall damage.

## Weapons

- **Quench Saber** (stage 2): each hit builds up current; the fifth breaks superconductivity and burns everything around.
- **Casimir Blade** (stage 4): always ignores part of the target's armor. Hold right-click to close the mirrors, and the next hit ignores armor completely and hits harder.
- **Tidal Reaver** (stage 5): hits build tidal stress, and stressed bodies visibly stretch. Hold right-click to open a Roche-focus black hole that pulls enemies in; let go to shear them according to their stress.
- **Accretion Jet Cannon** (stage 5): a shoulder-fired weapon.
  - **One left-click, one shot.** After a charge, the jet burns for two seconds and pierces everything ahead. It reloads by itself after four shots.
  - **Right-click** switches between terrain-safe and terrain-breaking mode. In breaking mode, each shot bores a tunnel to a fixed depth the moment it fires.

Allies, teammates, players with PvP disabled and armor stands are never targets of any weapon or ability.

## Tools (0.2.0)

Powered by FE, with no durability. Out of power they dig at the speed of a bare hand. Enchantments work.

- **SQUID Prospector**: right-click to show nearby ores through rock for ten seconds. Which ores show depends on its sensitivity, which works like Singulo's neutrino scanner: hold a Singulo **neutrino sensitivity module** and use it with the pick in your other hand to raise it. A pick crafted from a tuned scanner keeps the scanner's sensitivity.
- **Degeneracy Breaker**: pickaxe and shovel in one, digging 3x3 at a time.
- **Shear Saw**: fells whole trees that still have their leaves. Log buildings without leaves are left alone.
- **Accretion Drill**: every tool in one, sending drops and experience straight to your inventory. Right-click to cycle single, 3x3 and vein mining.

Area mining drops to a single block while you sneak, and never takes chests, machines, Singulo blocks or anything far harder than the block you struck.

## Modules (0.2.0)

The **Module Integrator** builds upgrade modules into gear. Power the integrator, put the gear in the centre, place modules in the sockets around it and press Integrate. Integrating with empty sockets takes modules out.
The number of sockets depends on the gear's tier: one at stage 2, three at stage 4, five at stage 5. Modules are made in Singulo's precision assembler.

| Module | Fits | Effect |
| --- | --- | --- |
| Bit Drone | Chest | Bits hovering behind you shoot enemies |
| Capacitor | Any gear | More energy capacity |
| Plating | Armor | More armor and toughness |
| Inertial Thruster | Boots | Higher jumps and step height |
| Metric Drive | Armor | Flight on FE alone |
| Induction Coil | Chest | Shares the chestpiece's FE with powered items you carry, from any mod |
| Gravity Collector | Armor | Pulls in nearby items and experience; off while sneaking |
| Coolant | Weapons | Shorter cooldowns |
| Output Amplifier | Weapons | More damage |
| Meltdown | Accretion Jet Cannon | The impact point detonates |
| Mining Overclock | Tools | Faster mining |
| Smelting | Tools | Ores and raw metal drop already smelted |

Most modules stack. Exact values and ingredients are in the [specification](../arsenal.md#モジュール統合台).

## Controls

| Input | Action |
| --- | --- |
| V (rebindable) | Degeneracy pressure release, Collapsar jet burst |
| Sneak in mid-air | Slow descent with the Meissner Levitators |
| Hold right-click | Casimir Blade mirrors, Tidal Reaver Roche focus |
| Left-click | Fire the Accretion Jet Cannon |
| Right-click | Cannon terrain mode, pick ore scan, drill mining mode, canister store |
| Sneak + right-click a power source | Charge the held gear |
| Hold Shift over a tooltip | Full description |
