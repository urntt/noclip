# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each version carries the targeted Minecraft version as build metadata, for example `1.0.0+26.3`.

## [Unreleased]

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
