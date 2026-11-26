# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each version carries the targeted Minecraft version as build metadata, for example `1.0.0+26.3`.

## [Unreleased]

### Added

- Add a configuration mode as the first button of the configuration screen. **Disabled** turns noclip off and makes the toggle keys change nothing, **Custom** uses the mod's own settings, and **OneConfig** uses the settings in OneConfig.
- Add optional [OneConfig](https://modrinth.com/mod/oneconfig) support. OneConfig manages the mod's toggle on its card, which is off by default, and the axes, collision types, and options. It has no defaults, reset rules, or multiplayer settings: once noclip is turned on there, it applies in singleplayer and on every server, and OneConfig profiles take the place of server lists. In OneConfig mode, the toggle keys change OneConfig's settings.

### Changed

- Until a configuration mode is chosen, OneConfig mode applies when OneConfig is installed, and custom mode otherwise. Settings from earlier versions are kept.
- The configuration screen shows the settings two per row and only lets you change them in custom mode. In the other modes it shows the values that apply.

## [1.1.0+26.3] - 2026-11-16

### Added

- Add "Jump to Pass Up" and "Sneak to Pass Down" settings with toggle key bindings, both off by default. While the jump or sneak key is held, blocks, the world border, and entities that noclip passes through no longer stop upward or downward movement, even with the Y axis off.

### Fixed

- With Lithium installed, passing through blocks, the world border, and entities along the X and Z axes only worked while stepping up or jumping. It now works all the time.

## [1.0.0+26.3] - 2026-11-16

### Added

- Remove the local player's client-side collisions while noclip is on. It is off by default.
- Choose the collision types to pass through: blocks, fluids, the world border, slowing blocks (cobwebs, sweet berry bushes, powder snow), and entities.
- Choose the axes along which blocks, the world border, and entities are passed through. X and Z are on by default, Y is off.
- Add "Only While Flying" and "Clear View Inside Blocks" options.
- Add a toggle key binding for every one of these settings and one that opens the configuration screen, all unbound by default. Toggling shows the new state on the action bar.
- Add a configuration screen built from vanilla widgets, available through the key binding and through Mod Menu, which is optional.
- Add separate defaults for singleplayer worlds and allowed servers, and options to reset to the default on world exit or game exit.
- Add multiplayer modes (disabled, whitelist, blacklist) and a server list screen. Noclip is disabled on multiplayer servers by default.
- Save all settings to `config/noclip.json`.
- Add English and Simplified Chinese translations.
