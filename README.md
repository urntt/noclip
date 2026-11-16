# noclip

A client-side Fabric mod for Minecraft: Java Edition that removes your player's client-side collisions, so you can move through blocks, fluids, the world border, cobwebs, and entities.

Only your own player is affected. Other players, mobs, and vehicles keep their collisions.

## Multiplayer warning

**This mod is disabled in multiplayer by default. To use it in multiplayer, change the settings on its configuration screen.**

This mod changes player movement. Servers that run anti-cheat systems may detect it, which can get your movement set back, get you kicked, or get you banned, and using it may break a server's rules. This mod does not try to bypass any anti-cheat. Check each server's rules before enabling it there, and use it in multiplayer at your own risk.

## Servers check your movement

This mod only changes your client. The server you play on, including the one built into the game that runs your singleplayer worlds, still checks every move you make:

- **Blocks and entities:** a vanilla server sets you back when you move into blocks or into solid entities such as boats and shulkers. You see your player enter the obstacle and jump back right away, in singleplayer as well.
- **World border, fluids, slowing blocks, and pushing:** vanilla servers do not check these directly, so they work in singleplayer and on servers that allow the mod. In survival mode, moving through the world border or slowing blocks much faster than walking speed can still get you set back.

### Recommended: nomovementcheck

To really pass through blocks and entities, use this mod together with [nomovementcheck](https://github.com/urntt/nomovementcheck). It is a server-side mod that makes the server trust the movement of the players you choose, so it no longer sets them back.

- For singleplayer and for LAN worlds you host, put nomovementcheck into your own `mods` folder next to this mod. Then turn it on with `/nomovementcheck enabled true`, which needs cheats (commands) to be allowed in the world.
- For a server, the server's owner installs it on the server and decides which players it trusts.

nomovementcheck only lifts the movement checks. Everything else stays vanilla: for example, you still take suffocation damage while your head is inside a block in survival mode.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download the jar from this repository's [Releases](https://github.com/urntt/noclip/releases) page. Each release supports a single Minecraft version, shown after the `+` in its version number. For example, `1.0.0+26.3` is for Minecraft 26.3.
3. Put the jar into your `.minecraft/mods` folder.

[Mod Menu](https://modrinth.com/mod/modmenu) is optional. When installed, it opens the mod's configuration screen from its mod list.

## Usage

Noclip is off until you turn it on, and it stays off on multiplayer servers until you allow them in the multiplayer settings.

The mod adds these key bindings in **Options → Controls → Key Binds**, all unbound by default:

- **Toggle Noclip** turns noclip on or off and shows the new state on the action bar. On a server that the multiplayer settings rule out, it only shows that noclip is disabled there.
- One toggle key for every axis, collision type, and option below, for example **Toggle Pass Through Blocks**. Each shows the new state on the action bar.
- **Open noclip Settings** opens the configuration screen. With Mod Menu installed, you can also open it from the mod list.

### Settings

All settings are saved to `config/noclip.json` as soon as you change them.

| Setting | Default | Meaning |
| --- | --- | --- |
| Noclip | Off | The current state, the same one the toggle key switches. |
| Noclip on X Axis | On | Blocks, the world border, and entities that noclip passes through do not stop movement east and west. |
| Noclip on Y Axis | Off | The same for movement up and down. Unless you fly, you fall through the ground. |
| Noclip on Z Axis | On | The same for movement north and south. |
| Jump to Pass Up | Off | While you hold the jump key, blocks, the world border, and entities that noclip passes through do not stop you from moving up, even with the Y axis off. |
| Sneak to Pass Down | Off | While you hold the sneak key, they do not stop you from moving down, even with the Y axis off. Unless you fly, you fall through the ground. |
| Pass Through Blocks | On | Move through blocks along the noclip axes, without being pushed out of them or forced to crouch or crawl inside them. |
| Pass Through Fluids | On | Move through water and lava as if they were air: no drag, currents, buoyancy, swimming, or bubble columns. |
| Pass Through World Border | On | Move through the world border along the noclip axes. |
| Pass Through Slowing Blocks | On | Cobwebs, sweet berry bushes, and powder snow no longer slow you down. Their damage and freezing still apply. |
| Pass Through Entities | On | Move through solid entities such as boats, minecarts, and shulkers along the noclip axes, and stop pushing nearby entities and being pushed by them. |
| Only While Flying | Off | Noclip is only active while you are flying. |
| Clear View Inside Blocks | On | While your head is inside a block, hide the block texture that covers the screen and keep the world around you visible, as in spectator mode. |
| Singleplayer Default | Off | The state of Noclip a reset restores in singleplayer worlds, including worlds you open to LAN. |
| Server Default | Off | The state of Noclip a reset restores on servers that the multiplayer mode allows. |
| Reset on World Exit | Off | Every world starts from its default state instead of keeping the last state. |
| Reset on Game Exit | Off | After restarting the game, the first world where the mod is allowed starts from its default state. |
| Multiplayer mode | Disabled | **Disabled**: never active on servers. **Whitelist**: active only on servers in the server list. **Blacklist**: active on all servers except those in the server list. |
| Server List | Empty | The addresses the whitelist and blacklist modes use. |

The axes, and passing up or down with the jump and sneak keys, only apply to blocks, the world border, and entities. Fluids and slowing blocks are passed through as a whole. The jump and sneak keys are the ones set in the game's controls.

The multiplayer mode is a hard limit: on a server it rules out, noclip stays off whatever the current state is. Joining another player's LAN world or a Realm counts as multiplayer.

Server list entries are compared with the address you connect to, ignoring upper and lower case. An entry without a port, such as `mc.example.com`, matches the server on any port, while an entry with a port, such as `mc.example.com:25566`, matches only that port. The server list screen marks invalid addresses in red and does not save until they are fixed or removed.

### Compatibility

The mod changes how the game collects the colliders of your movement. It works with [Lithium](https://modrinth.com/mod/lithium), which replaces the game's entity collision code: the client game tests also run with Lithium installed. Other mods that rewrite entity collisions may still conflict with it.

## Development

Building requires the JDK version set by `java_version` in `gradle.properties`.

Build the mod:

```bash
./gradlew build
```

The jar is written to `build/libs/`.

Run the client game tests, which start Minecraft and check every collision type, the axes and options, the key bindings, the settings screens, the reset rules, the multiplayer modes on a local dedicated server, and the saved configuration:

```bash
./gradlew runClientGameTest
```

Run them again with Lithium loaded in the development client, the version set by `lithium_version` in `gradle.properties`:

```bash
./gradlew runClientGameTest -PwithLithium
```

The game tests need a display. On a headless Linux machine, run them under Xvfb. Xvfb offers no sRGB-capable OpenGL visuals, so install Mesa's Vulkan driver (`mesa-vulkan-drivers` on Ubuntu) for the game to fall back to:

```bash
xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runClientGameTest
```

Screenshots taken by the tests are saved to `build/run/clientGameTest/screenshots/`.

The multiplayer tests start a local dedicated server, so the test setup in `build.gradle` accepts the [Minecraft EULA](https://aka.ms/MinecraftEULA) for that test server.

## License

[MIT](LICENSE)
