# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0h] - 2026-02-24

### Fixed

- Fixed crash when breaking chains with Shears on Forge.

## [1.2.0] - 2026-02-22

### Added

- Added `/connectchain` command to connect a chain between two blocks.
- Abyssal Decor and Fang's Textiles and Trinkets now have their chains detected automatically.
- Improved detection for modded chain textures.

### Fixed

- Chains not rendering beyond a specific distance.
- Fixed Copper Age Backport compatibility.

## [1.1.3] - 2026-02-18

### Changed

- Chain knots now adapt to the shape of the block they're attached to.
    - This means they'll now be visible and have dynamic models for iron bars, walls, etc.

## [1.1.2] - 2026-02-18

### Changed

- Chains can now be connected to Supplementaries Gold Bars.

### Fixed

- Fixed chain rendering resetting when reloading world.
- Fixed chain interactions not working properly on long chains.

## [1.1.1] - 2026-02-17

### Changed

- Default chain slack now dynamically changes with chain length.
- Improved hit detection for chain breaking.

### Fixed

- Fixed Shears taking durability when attempting to adjust slack beyond maximum/minimum values.
- Fixed chain links being unbreakable with Shears when disabling collision.
- Increased minimum slack.

## [1.1.0] - 2026-02-16

### Added

- Slack can now be adjusted on a per-chain basis by right-clicking or shift right-clicking the chain length with Shears.

### Fixed

- Fixed odd lighting on certain Chain types.
- Fixed modded Chains sometimes not resetting their models properly.

## [1.0.3] - 2026-02-12

### Fixed

- Fixed tooltip for max chain distance in Cloth Config.
- Fixed held chain rendering being too high.

## [1.0.2] - 2026-02-09

### Fixed

- Fixed chains/ropes sometimes not breaking.

## [1.0.1] - 2026-02-05

### Changed

* Increased cap for max chain distance from 32 to 128.
* Small memory improvements.

## [1.0.0] - 2026-02-02

* Initial multiloader release.