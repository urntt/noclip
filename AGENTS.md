# AGENTS.md

Instructions for AI coding agents working in this repository. `CLAUDE.md` imports this file, so keep all agent guidance here.

## Project

`noclip` is a client-side [Fabric](https://fabricmc.net/) mod for Minecraft: Java Edition that removes the local player's client-side collisions, so the player can move through obstacles.

Related repositories:

- [urntt/nojumpdelay](https://github.com/urntt/nojumpdelay) is the model for the configuration: the settings screen, the scene defaults and reset rules, the multiplayer modes, and the server list screen. Keep the two mods consistent in style and behavior where they overlap.
- [urntt/nomovementcheck](https://github.com/urntt/nomovementcheck) is a server-side mod that makes a server trust selected players' movement. `README.md` recommends it, because vanilla servers, including the integrated server of a singleplayer world, set back a player who moves into blocks. Mention it by name only; noclip must not depend on it or contain any server-side code of its own.

## Project decisions

These decisions are settled. Do not deviate from them without the user's explicit approval.

### Minecraft and toolchain

- Development started on Minecraft 26.3 with Java 25. `gradle.properties` is the single source of truth for the mod, Minecraft, Fabric Loader, Loom, Fabric API, Mod Menu, and Java versions. `build.gradle`, `fabric.mod.json`, the mixin config, and the CI workflows read them from there; do not restate them elsewhere.
- Follow the latest official Fabric template ([FabricMC/fabric-example-mod](https://github.com/FabricMC/fabric-example-mod), also available from the [template generator](https://fabricmc.net/develop/template/)): the `net.fabricmc.fabric-loom` Gradle plugin, Mojang's official names with no `mappings` dependency, and `implementation` (not `modImplementation`) for dependencies. Do not use Yarn.
- Pin `loom_version` to a release version instead of the template's `-SNAPSHOT`, so builds are reproducible.
- Target only the latest stable (release) Minecraft version. Updates, fixes, and new features are always developed against it. Snapshots, pre-releases, and release candidates are not supported targets.
- Do not maintain older Minecraft versions and do not set up multi-version builds (no per-version branches, no Stonecutter or other preprocessors). When a new stable version is released, port the mod to it and drop the previous one.

### Identity

| Item | Value |
| --- | --- |
| Mod ID | `noclip` |
| Display name | `noclip` |
| Maven group | `com.urntt` |
| Base package | `com.urntt.noclip` |
| Version format | `<SemVer>+<Minecraft version>`, for example `1.0.0+26.3` |
| License | MIT |

The mod version itself follows [Semantic Versioning](https://semver.org/); the `+<Minecraft version>` suffix is build metadata naming the Minecraft version the build targets. Version numbers start at `1.0.0`.

### Distribution

- Releases are published only as GitHub Releases. Do not publish to Modrinth, CurseForge, or any other mod platform, and do not add publishing tooling for them.
- `README.md` must clearly warn that this is a movement modification: it may conflict with server anti-cheat systems, and using it in multiplayer may get the player set back, kicked, or banned. It must also state that the mod does not try to bypass any anti-cheat, and that it is disabled in multiplayer until the player enables it on the configuration screen.

### Scope and behavior

- Client-only: `fabric.mod.json` declares `"environment": "client"`. There is no server-side component and no networking. Do not change server behavior, not even the integrated server of a singleplayer world; the user decided this explicitly.
- Consequently, servers keep validating movement. Vanilla servers, including the integrated server, set the player back when they move into blocks or into solid entities (`ServerGamePacketListenerImpl.isEntityCollidingWithAnythingNew`), and in survival also when the reported position differs too much from the server's own simulation ("moved wrongly"). `README.md` states this honestly and recommends nomovementcheck.
- Only the local player (`LocalPlayer`) is affected. Every other entity, including other players, mobs, and vehicles, keeps vanilla collisions.
- Non-goals: flight, speed changes, fall damage or other damage changes, packet manipulation, and anything that tries to get past anti-cheat or server movement checks.
- `NoClipController` is the single owner of whether noclip is active: the main toggle is on, the current scene (singleplayer or a multiplayer server, determined on join) is allowed, and, if "only while flying" is on, the player is flying.
- Collision types, each toggled on its own and all passed through by default:
  - blocks: block collision shapes, plus the client pushing the player out of blocks and forcing a crouching or swimming pose inside them;
  - fluids: drag, currents, buoyancy, and swimming in water and lava, including bubble columns;
  - world border: the border's collision wall;
  - slowing blocks: blocks the player can already pass through but that slow them down (cobwebs, sweet berry bushes, powder snow);
  - entities: solid entity hitboxes (boats, minecarts, shulkers) and pushing between the player and nearby entities.
- Axes: X, Y, and Z are toggled on their own. They apply only to the shape-based types (blocks, world border, entities): along an enabled axis those types are ignored, along the others they collide as usual. Fluids and slowing blocks are effects without an axis. By default X and Z are enabled and Y is not, so the player passes through walls but still stands on the ground.
- "Jump to pass up" and "sneak to pass down" (both off by default) also ignore the shape-based types for upward movement while the jump key is held, and for downward movement while the sneak key is held, even with the Y axis off. They read the player's movement input, so they follow the player's key bindings. Internally, `NoClipRules` holds the directions of movement in which shapes are ignored.
- "Only while flying" (off by default) limits noclip to while the player is flying.
- "Clear view inside blocks" (on by default) hides the in-wall screen overlay and disables occlusion culling while the camera is inside a solid block, like vanilla does for spectators. It applies only while noclip is active and passes through blocks.
- The main toggle is off by default. Defaults depend on the scene: a singleplayer default (worlds hosted by this client, including ones opened to LAN) and a server default (servers the multiplayer mode allows). Both are off by default.
- Reset rules restore the scene's default when the player joins an allowed scene: "reset on world exit" for every world, and "reset on game exit" for the first allowed world after the game starts. Both are off by default. Resets happen on join so that they use the next scene's default and still work after a crash.
- The multiplayer mode is a hard limit: `DISABLED` (the default) rules out every server, `WHITELIST` allows only servers in the server list, and `BLACKLIST` allows every server except those in it. On a ruled-out server noclip stays off and the main toggle key only reports that it is disabled there. Joining another player's LAN world or a Realm counts as multiplayer.
- Server list entries match the connected address by host (case-insensitive, after IDN conversion, and required to be a valid domain name or IP address) and by port only when the entry specifies one.
- Every boolean behavior setting (the main toggle, each axis, each collision type, "only while flying", and "clear view inside blocks") has its own toggle key binding, unbound by default. `Toggle` is the single source for these settings: their key binding names, captions, and configuration access. Each toggle shows the new state on the action bar and saves it to the configuration file. A further key binding, also unbound by default, opens the configuration screen.
- The configuration screen is built from vanilla widgets and opens through Mod Menu or the "open settings" key binding.

### Localization

- All user-facing text, including key binding names, the key binding category, action bar messages, and the configuration screen, uses translation keys. Never hard-code display strings.
- Provide translations for `en_us` and `zh_cn`, and keep both complete whenever a translation key is added or changed.

### Dependencies

- Required: Fabric Loader and Fabric API.
- Optional: Mod Menu, declared under `suggests` in `fabric.mod.json`. The mod must load and work normally without it, so Mod Menu classes may only be referenced from the Mod Menu entrypoint. nomovementcheck is also listed under `suggests` as a hint; nothing references it.
- Configuration is hand-written without a config library: a JSON file in the Fabric config directory, serialized with Gson (bundled with Minecraft). Any configuration screen uses vanilla widgets.
- Test-only: [Lithium](https://modrinth.com/mod/lithium), from the Modrinth Maven repository at `lithium_version`, is loaded into the development client only with `-PwithLithium`. It replaces the game's entity collision code (it `@Redirect`s the `collideBoundingBox` call in `Entity.collide`), so the client game tests run against it as well. The mod itself never depends on it.
- Do not add other dependencies without the user's explicit approval.

### Implementation

- Language: Java only.
- Source sets: `src/main` holds only `fabric.mod.json` and the icon. All code and client resources live in `src/client`, and the client game tests live in `src/gametest`.
- Mixins: prefer the MixinExtras injectors bundled with Fabric Loader (for example `@WrapOperation`, `@WrapMethod` and `@ModifyReturnValue`) over `@Redirect` and `@Overwrite`, to stay compatible with other mods and keep porting work small. Gate every change with `(Object) this instanceof LocalPlayer` (or the equivalent parameter check in static methods) and the controller's active rules.

### Testing

- The client game tests in `src/gametest` cover the address matching, the configuration defaults and upgrades, and the controller's rules (`NoClipLogicGameTest`), every collision type, the axes, the toggle keys, the settings screens, and the server set-back in singleplayer (`NoClipClientGameTest`), and each multiplayer mode on a local dedicated server (`NoClipMultiplayerGameTest`). Keep them passing and extend them when behavior changes.
- Collision checks call `LocalPlayer.move` on the client thread and read the position right away, so they measure the client's own collision handling independently of when the server sets the player back. Checks that need the jump or sneak key set the player's movement input around that call.
- Run the client game tests both without and with `-PwithLithium`; CI does both.
- The dedicated server needs `eula = true` in the `configureTests` block of `build.gradle`; it accepts the Minecraft EULA only for that local test server.
- After porting to a new Minecraft version, run the client game tests. A successful build does not prove that the mixins still have the intended effect.
- `README.md` describes how to run them, including on a headless machine.

### CI, releases, and changelog

- GitHub Actions (`.github/workflows/build.yml`) builds the project and runs the client game tests on every push and pull request, once without and once with Lithium.
- Maintain `CHANGELOG.md` following [Keep a Changelog](https://keepachangelog.com/). Record every user-visible change under `Unreleased` in the same change that introduces it.
- `.github/workflows/release.yml` builds the mod and publishes a GitHub Release for the project version, with the jar attached and the matching `CHANGELOG.md` section as release notes. It runs when a tag `v<version>` (for example `v1.0.0+26.3`) is pushed, or when started manually on a branch, in which case it creates that tag on the branch's latest commit. It fails if a pushed tag does not match the project version, if the changelog has no section for the version, or if the release already exists.
- Release only when the user asks. To release, set `mod_version` in `gradle.properties`, rename `Unreleased` in `CHANGELOG.md` to `[<version>] - <YYYY-MM-DD>` above a new empty `Unreleased` section, commit, and push. Then start the release workflow on `main`. Claude Code cloud sessions cannot push tags, so start the workflow through the GitHub Actions API instead.

## Engineering principles

- Fix root causes, not symptoms. Diagnose the underlying cause before implementing a permanent fix. If an immediate mitigation is necessary, treat it as temporary and follow through with a root-cause fix.
- Prefer configuration-driven design for values that are expected to vary by environment, deployment, or product requirements. Avoid unexplained or duplicated magic values, but do not introduce configuration where a well-named constant is the clearer source of truth.
- Preserve a single source of truth and clear ownership for data, state, configuration, business logic, and authoritative documentation. Avoid duplicating canonical information across multiple locations.
- Do not maintain parallel legacy and replacement implementations without an explicit migration and removal plan.

## Documentation

- Keep documentation aligned with the code. When a code change affects documented behavior, APIs, architecture, configuration, workflows, or usage, update the relevant documentation in the same change.
- Keep each document's responsibility clear. For example, use `README.md` for project overview and usage, and `VISION.md` for product direction, architectural principles, or long-term decisions.
- Always specify a language identifier for fenced code blocks in Markdown.

## Language

- Communicate with the user in Chinese, including explanations, progress updates, and user-facing planning.
- Use English for development artifacts, including source code, comments, docstrings, documentation, READMEs, Git branch names, commit messages, and other deliverables intended to live in the repository.

## Git

- Do not change or override the Git author or committer identity. When an identity must be configured for commits created during the task, use:
  - Name: `urntt`
  - Email: `urntts@gmail.com`
- Do all actions on the user's behalf. Do not rewrite existing commit authorship unless explicitly requested. Do not add `Co-Authored-By` trailers or session links to commit messages or pull request descriptions.
- Develop on `main` and push directly to it. Branches and pull requests are not required.
- Because changes land on `main` without review, make sure `./gradlew build` and the client game tests pass locally before pushing.
- If a branch is used, give it a category-based prefix that reflects the purpose of the change, such as `feat/`, `fix/`, `refactor/`, `docs/`, `test/`, or `chore/`.
- Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification for commit messages.
