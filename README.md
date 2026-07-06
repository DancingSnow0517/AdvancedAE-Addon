# AdvancedAE Addon

[中文](README.zh_CN.md)

AdvancedAE Addon is a NeoForge addon for Applied Energistics 2 and Advanced AE.

It adds AE2 Crafting Card support to Advanced AE's Quantum Crafter. When a Crafting Card is installed, the Quantum Crafter can request missing pattern inputs from the ME crafting network instead of staying blocked by unavailable ingredients.

## Features

- Adds the AE2 Crafting Card as a Quantum Crafter upgrade.
- Requests missing inputs through AE2's crafting system.
- Supports Quantum Crafter speed upgrades, up to 64 crafts per tick with four
  speed cards.
- Respects minimum input stock settings.
- Respects output limit count when calculating how many inputs to request.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.209 or newer
- Applied Energistics 2 19.2.17 or newer
- Advanced AE 1.6.11-1.21.1 or newer

## Building

```sh
./gradlew build
```
