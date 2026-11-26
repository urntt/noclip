package com.urntt.noclip.gametest;

import static com.urntt.noclip.gametest.GameTestSupport.check;

import com.urntt.noclip.NoClipController;
import com.urntt.noclip.NoClipRules;
import com.urntt.noclip.Scene;
import com.urntt.noclip.ServerAddresses;
import com.urntt.noclip.compat.OneConfigBridge;
import com.urntt.noclip.config.CollisionType;
import com.urntt.noclip.config.ConfigMode;
import com.urntt.noclip.config.EditableSettings;
import com.urntt.noclip.config.MultiplayerMode;
import com.urntt.noclip.config.NoClipConfig;
import com.urntt.noclip.config.NoClipValues;
import com.urntt.noclip.config.SceneRules;
import com.urntt.noclip.config.Toggle;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * Checks the address matching, the configuration, the rules, and the controller without a world. Each check uses its
 * own configuration file, so the game's configuration is left alone. Restarting the game is not possible in a game
 * test, so this is also where "reset on game exit" is covered. Most controllers here read the custom settings directly,
 * whatever the configuration mode, because a new file is in OneConfig mode while OneConfig is installed.
 */
@SuppressWarnings("UnstableApiUsage")
public final class NoClipLogicGameTest implements FabricClientGameTest {
	private static final Scene SERVER_A = Scene.multiplayer("a.example.com");
	private static final Scene SERVER_B = Scene.multiplayer("b.example.com:25570");
	private static final Scene UNKNOWN_SERVER = Scene.multiplayer(null);

	@Override
	public void runTest(final ClientGameTestContext context) {
		Path directory = createTempDirectory();
		checkAddressMatching();
		checkDefaults(directory);
		checkTogglesPersist(directory);
		checkUpgradeFromOlderConfig(directory);
		checkRules();
		checkOnlyWhileFlying(directory);
		checkMultiplayerRules(directory);
		checkResetRules(directory);
		checkModes(directory);
		checkWithoutSceneRules();
		GameTestSupport.LOGGER.info("Logic checks passed");
	}

	private static void checkAddressMatching() {
		checkMatch("mc.example.com", "mc.example.com", true);
		checkMatch("MC.Example.com", "mc.example.COM:25565", true);
		checkMatch("mc.example.com", "mc.example.com:25566", true);
		checkMatch("mc.example.com:25565", "mc.example.com", true);
		checkMatch("mc.example.com:25566", "mc.example.com", false);
		checkMatch("mc.example.com", "play.example.com", false);
		checkMatch("  mc.example.com  ", "mc.example.com", true);
		checkMatch("192.168.1.5", "192.168.1.5:51234", true);
		checkMatch("[::1]:25565", "[::1]", true);
		checkMatch("::1", "[::1]:25570", true);
		checkMatch("bücher.example", "xn--bcher-kva.example", true);
		checkMatch("localhost", "localhost:41234", true);

		for (String valid : List.of("mc.example.com:25566", "localhost", "192.168.1.5", "[::1]:25565", "bücher.example")) {
			check(ServerAddresses.isValid(valid), "'" + valid + "' should be valid");
		}
		for (String invalid : List.of("", "   ", "mc.example.com:abc", "mc.example.com:99999", "[::1", "mc example.com",
				"not a host:port:x")) {
			check(!ServerAddresses.isValid(invalid), "'" + invalid + "' should be invalid");
		}
	}

	private static void checkMatch(final String entry, final String address, final boolean expected) {
		check(ServerAddresses.matches(entry, address) == expected,
				"'" + entry + "' should " + (expected ? "" : "not ") + "match '" + address + "'");
	}

	private static void checkDefaults(final Path directory) {
		NoClipConfig config = NoClipConfig.load(directory.resolve("defaults.json"));
		check(!config.get(Toggle.ENABLED), "noclip should be off by default");
		check(config.get(Toggle.AXIS_X) && !config.get(Toggle.AXIS_Y) && config.get(Toggle.AXIS_Z),
				"only the X and Z axes should be enabled by default");
		check(!config.get(Toggle.JUMP_UP) && !config.get(Toggle.SNEAK_DOWN),
				"jump to pass up and sneak to pass down should be off by default");
		for (CollisionType type : CollisionType.values()) {
			check(config.get(Toggle.forType(type)), type + " should be passed through by default");
		}
		check(!config.get(Toggle.ONLY_WHILE_FLYING), "only while flying should be off by default");
		check(config.get(Toggle.CLEAR_VIEW), "clear view inside blocks should be on by default");
		SceneRules scene = config.sceneRules();
		check(!scene.singleplayerDefault(), "the singleplayer default should be off");
		check(!scene.multiplayerDefault(), "the server default should be off");
		check(!scene.resetOnWorldExit(), "reset on world exit should be off by default");
		check(!scene.resetOnGameExit(), "reset on game exit should be off by default");
		check(scene.multiplayerMode() == MultiplayerMode.DISABLED, "multiplayer should be disabled by default");
		check(scene.servers().isEmpty(), "the server list should be empty by default");
		check(NoClipValues.copyOf(config).equals(NoClipValues.DEFAULTS),
				"a new configuration should hold the defaults, was " + NoClipValues.copyOf(config));
		check(Files.exists(directory.resolve("defaults.json")), "a missing config file should be created");

		NoClipRules rules = config.rules();
		check(rules.directions().equals(EnumSet.of(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH)),
				"default rules should pass through along X and Z");
		check(rules.ignoredTypes().equals(EnumSet.allOf(CollisionType.class)), "default rules should ignore every type");
		check(rules.keepsViewClear(), "default rules should keep the view clear");
	}

	/**
	 * Flips every toggle on its own and checks that exactly that setting changed and was saved.
	 */
	private static void checkTogglesPersist(final Path directory) {
		Path path = directory.resolve("toggles.json");
		NoClipConfig config = NoClipConfig.load(path);
		for (Toggle toggle : Toggle.values()) {
			boolean before = config.get(toggle);
			config.set(toggle, !before);
			NoClipConfig reloaded = NoClipConfig.load(path);
			for (Toggle other : Toggle.values()) {
				boolean expected = other == toggle ? !before : config.get(other);
				check(reloaded.get(other) == expected, "after flipping " + toggle + ", " + other + " should be saved as "
						+ expected);
			}
			check(config.get(toggle) == !before, toggle + " should be flipped");
			config.set(toggle, before);
		}
	}

	private static void checkUpgradeFromOlderConfig(final Path directory) {
		Path path = directory.resolve("upgrade.json");
		write(path, "{\"enabled\": true, \"axes\": {\"y\": true}, \"collisions\": null, \"multiplayerMode\": \"bogus\", "
				+ "\"servers\": [\" a.example.com \", \"\"]}");

		NoClipConfig config = NoClipConfig.load(path);
		check(config.storedMode() == null, "a file without a mode should count as no choice");
		check(config.get(Toggle.ENABLED), "existing settings should be kept");
		check(config.get(Toggle.AXIS_X) && config.get(Toggle.AXIS_Y) && config.get(Toggle.AXIS_Z),
				"missing axes should get their defaults next to the saved one");
		check(!config.get(Toggle.JUMP_UP) && !config.get(Toggle.SNEAK_DOWN),
				"settings added in 1.1.0 should get their defaults when upgrading a 1.0.0 file");
		for (CollisionType type : CollisionType.values()) {
			check(config.get(Toggle.forType(type)), "a missing collision object should fall back to defaults");
		}
		check(config.multiplayerMode() == MultiplayerMode.DISABLED, "an unknown mode should fall back to disabled");
		check(config.servers().equals(List.of("a.example.com")), "entries should be trimmed and blanks dropped");
		String saved = read(path);
		check(saved.contains("\"clearView\"") && saved.contains("\"slowingBlocks\""),
				"missing settings should be written to the file");

		write(path, "not json {");
		NoClipConfig broken = NoClipConfig.load(path);
		check(!broken.get(Toggle.ENABLED), "an unreadable file should fall back to the defaults");
		check(read(path).equals("not json {"), "an unreadable file should be left untouched");
	}

	private static void checkRules() {
		Set<CollisionType> all = EnumSet.allOf(CollisionType.class);
		NoClipRules noAxes = new NoClipRules(all, Set.of(), true);
		check(!noAxes.ignores(CollisionType.BLOCKS), "shape-based types need an axis");
		check(!noAxes.ignoresAnyShapes(), "without axes no shapes should be ignored");
		check(noAxes.ignores(CollisionType.FLUIDS) && noAxes.ignores(CollisionType.SLOWING_BLOCKS),
				"effect types should not depend on the axes");
		check(!noAxes.keepsViewClear(), "the view should only be kept clear while passing through blocks");

		NoClipRules fluidsOnly = new NoClipRules(EnumSet.of(CollisionType.FLUIDS),
				NoClipRules.bothWays(EnumSet.of(Direction.Axis.X)), true);
		check(!fluidsOnly.ignoresAnyShapes(), "fluids are not shapes");
		check(!fluidsOnly.keepsViewClear(), "the view should only be kept clear while passing through blocks");

		NoClipRules entitiesOnly = new NoClipRules(EnumSet.of(CollisionType.ENTITIES),
				NoClipRules.bothWays(EnumSet.of(Direction.Axis.Y)), false);
		check(entitiesOnly.directions().equals(EnumSet.of(Direction.UP, Direction.DOWN)), "the Y axis means up and down");
		check(entitiesOnly.ignoresAnyShapes(), "entity hitboxes are shapes");
		check(!entitiesOnly.ignores(CollisionType.BLOCKS), "blocks were not selected");

		NoClipRules upOnly = noAxes.withDirections(EnumSet.of(Direction.UP));
		check(upOnly.directions().equals(EnumSet.of(Direction.UP)), "holding jump should add only the upward direction");
		check(upOnly.ignores(CollisionType.BLOCKS) && upOnly.keepsViewClear(),
				"a single direction should be enough to pass through blocks");
		NoClipRules horizontal = new NoClipRules(all, NoClipRules.bothWays(EnumSet.of(Direction.Axis.X)), true);
		check(horizontal.withDirections(EnumSet.of(Direction.EAST)) == horizontal,
				"adding a direction that is already there should keep the rules");
	}

	private static void checkOnlyWhileFlying(final Path directory) {
		NoClipConfig config = NoClipConfig.load(directory.resolve("flying.json"));
		NoClipController controller = customController(config);
		controller.onJoin(Scene.SINGLEPLAYER);
		check(!controller.isActive(false) && !controller.isActive(true), "noclip should be off by default");

		config.set(Toggle.ENABLED, true);
		check(controller.isActive(false) && controller.isActive(true), "noclip should be active walking and flying");
		config.set(Toggle.ONLY_WHILE_FLYING, true);
		check(!controller.isActive(false), "only while flying should rule out walking");
		check(controller.isActive(true), "only while flying should allow flying");
	}

	private static void checkMultiplayerRules(final Path directory) {
		NoClipConfig config = NoClipConfig.load(directory.resolve("rules.json"));
		config.set(Toggle.ENABLED, true);
		NoClipController controller = customController(config);

		check(!controller.isActive(false), "nothing should be active outside a world");
		controller.onJoin(Scene.SINGLEPLAYER);
		check(controller.isActive(false), "singleplayer should always be allowed");

		controller.onJoin(SERVER_A);
		check(!controller.isAllowed(), "the disabled mode should rule out every server");
		check(controller.toggle(Toggle.ENABLED) == NoClipController.ToggleResult.BLOCKED, "toggling should be blocked");
		check(config.get(Toggle.ENABLED), "a blocked toggle should leave the state unchanged");

		config.setServers(List.of("a.example.com"));
		config.setMultiplayerMode(MultiplayerMode.WHITELIST);
		check(controller.isActive(false), "a whitelisted server should be allowed");
		check(controller.toggle(Toggle.ENABLED) == NoClipController.ToggleResult.OFF, "toggling should work when allowed");
		check(controller.toggle(Toggle.ENABLED) == NoClipController.ToggleResult.ON, "toggling should work when allowed");
		controller.onJoin(SERVER_B);
		check(!controller.isAllowed(), "a server missing from the whitelist should be ruled out");
		controller.onJoin(UNKNOWN_SERVER);
		check(!controller.isAllowed(), "an unknown address should not count as whitelisted");

		config.setMultiplayerMode(MultiplayerMode.BLACKLIST);
		check(controller.isAllowed(), "an unknown address should not count as blacklisted");
		controller.onJoin(SERVER_B);
		check(controller.isAllowed(), "a server missing from the blacklist should be allowed");
		controller.onJoin(SERVER_A);
		check(!controller.isAllowed(), "a blacklisted server should be ruled out");

		controller.onDisconnect();
		check(!controller.isActive(false), "nothing should be active after disconnecting");
	}

	/**
	 * Checks the configuration modes: until the player chooses one, OneConfig mode while OneConfig is installed and
	 * custom mode otherwise; OneConfig mode only while OneConfig is installed; and disabled mode turning noclip off
	 * and ignoring the toggle keys and reset rules.
	 */
	private static void checkModes(final Path directory) {
		ConfigMode unchosen = OneConfigBridge.isAvailable() ? ConfigMode.ONECONFIG : ConfigMode.CUSTOM;
		Path path = directory.resolve("modes.json");
		NoClipConfig config = NoClipConfig.load(path);
		check(config.storedMode() == null, "no mode should be chosen by default");
		check(!read(path).contains("\"mode\""), "a mode that was not chosen should be left out of the file");
		check(config.effectiveMode() == unchosen, "without a choice, the mode should be " + unchosen + ", was "
				+ config.effectiveMode());

		Path older = directory.resolve("modes-1.1.0.json");
		write(older, "{\"enabled\": true}");
		NoClipConfig upgraded = NoClipConfig.load(older);
		check(upgraded.storedMode() == null && upgraded.get(Toggle.ENABLED),
				"a 1.1.0 file should load without a chosen mode and keep its settings");

		config.setMode(ConfigMode.CUSTOM);
		check(read(path).contains("\"mode\": \"custom\""), "a chosen mode should be saved");
		check(config.effectiveMode() == ConfigMode.CUSTOM, "a chosen custom mode should apply");

		config.setMode(ConfigMode.ONECONFIG);
		NoClipConfig reloaded = NoClipConfig.load(path);
		check(reloaded.storedMode() == ConfigMode.ONECONFIG, "OneConfig mode should be saved");
		check(reloaded.effectiveMode() == unchosen, "OneConfig mode should apply only while OneConfig is installed, "
				+ "expected " + unchosen + ", was " + reloaded.effectiveMode());

		config.set(Toggle.ENABLED, true);
		config.setResetOnWorldExit(true);
		config.setMode(ConfigMode.DISABLED);
		NoClipController controller = new NoClipController(config::effective, config::editable);
		controller.onJoin(Scene.SINGLEPLAYER);
		check(config.get(Toggle.ENABLED), "disabled mode should not apply the reset rules");
		check(!controller.isActive(true), "disabled mode should keep noclip off");
		check(NoClipValues.copyOf(config.effective()).equals(NoClipValues.DISABLED),
				"disabled mode should apply the disabled values");
		check(config.editable().isEmpty(), "disabled mode should offer nothing to edit");
		for (Toggle toggle : Toggle.values()) {
			boolean before = config.get(toggle);
			check(controller.toggle(toggle) == NoClipController.ToggleResult.UNAVAILABLE,
					"toggling " + toggle + " should be unavailable in disabled mode");
			check(config.get(toggle) == before, "toggling " + toggle + " in disabled mode should change nothing");
		}

		config.setMode(ConfigMode.CUSTOM);
		check(controller.isActive(false), "custom mode should apply the custom settings again");
	}

	/**
	 * Settings without scene rules, as OneConfig's are, start with noclip off. Once it is on, every scene allows it,
	 * and joining a world resets nothing.
	 */
	private static void checkWithoutSceneRules() {
		check(NoClipValues.ONECONFIG_DEFAULTS.sceneRules() == null, "OneConfig's settings should have no scene rules");
		SettingsWithoutSceneRules settings = new SettingsWithoutSceneRules();
		check(!settings.get(Toggle.ENABLED), "OneConfig's toggle should be off by default");
		NoClipController controller = new NoClipController(() -> settings, () -> Optional.of(settings));
		controller.onJoin(SERVER_A);
		check(controller.isAllowed() && !controller.isActive(false),
				"without scene rules, a server should be allowed while noclip stays off");
		check(controller.toggle(Toggle.ENABLED) == NoClipController.ToggleResult.ON,
				"the main toggle should work on any server without scene rules");
		controller.onDisconnect();

		for (Scene scene : List.of(Scene.SINGLEPLAYER, SERVER_A, SERVER_B, UNKNOWN_SERVER)) {
			controller.onJoin(scene);
			check(controller.isActive(false), "without scene rules, noclip should be active in " + scene);
			check(settings.get(Toggle.ENABLED), "without scene rules, joining " + scene + " should reset nothing");
			controller.onDisconnect();
		}
	}

	/** A controller that reads the custom settings of {@code config}, whatever its mode. */
	private static NoClipController customController(final NoClipConfig config) {
		return new NoClipController(() -> config, () -> Optional.of(config));
	}

	/** Editable settings without scene rules, standing in for OneConfig's, which only exist while it is installed. */
	private static final class SettingsWithoutSceneRules implements EditableSettings {
		private final Set<Toggle> enabled = EnumSet.noneOf(Toggle.class);

		SettingsWithoutSceneRules() {
			this.enabled.addAll(NoClipValues.ONECONFIG_DEFAULTS.enabled());
		}

		@Override
		public boolean get(final Toggle toggle) {
			return this.enabled.contains(toggle);
		}

		@Override
		public void set(final Toggle toggle, final boolean value) {
			if (value) {
				this.enabled.add(toggle);
			} else {
				this.enabled.remove(toggle);
			}
		}

		@Override
		public @Nullable SceneRules sceneRules() {
			return null;
		}
	}

	private static void checkResetRules(final Path directory) {
		NoClipConfig config = NoClipConfig.load(directory.resolve("reset.json"));
		config.setResetOnGameExit(true);
		config.setSingleplayerDefault(true);
		config.set(Toggle.ENABLED, false);

		// A fresh controller stands for a game that has just started.
		NoClipController controller = customController(config);
		controller.onJoin(SERVER_A);
		check(!config.get(Toggle.ENABLED), "a ruled-out server should not apply the game exit reset");
		controller.onDisconnect();
		controller.onJoin(Scene.SINGLEPLAYER);
		check(config.get(Toggle.ENABLED), "the first allowed world after starting should restore the default");
		controller.onDisconnect();
		config.set(Toggle.ENABLED, false);
		controller.onJoin(Scene.SINGLEPLAYER);
		check(!config.get(Toggle.ENABLED), "later worlds should keep the state without reset on world exit");
		controller.onDisconnect();

		config.setResetOnWorldExit(true);
		controller.onJoin(Scene.SINGLEPLAYER);
		check(config.get(Toggle.ENABLED), "reset on world exit should restore the singleplayer default");
		controller.onDisconnect();

		config.setMultiplayerMode(MultiplayerMode.WHITELIST);
		config.setServers(List.of("a.example.com"));
		config.setMultiplayerDefault(false);
		controller.onJoin(SERVER_A);
		check(!config.get(Toggle.ENABLED), "an allowed server should restore the server default");
		controller.onDisconnect();
	}

	private static Path createTempDirectory() {
		try {
			return Files.createTempDirectory("noclip-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void write(final Path path, final String content) {
		try {
			Files.writeString(path, content);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String read(final Path path) {
		try {
			return Files.readString(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
