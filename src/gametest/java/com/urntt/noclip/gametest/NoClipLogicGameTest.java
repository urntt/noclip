package com.urntt.noclip.gametest;

import static com.urntt.noclip.gametest.GameTestSupport.check;

import com.urntt.noclip.NoClipController;
import com.urntt.noclip.NoClipRules;
import com.urntt.noclip.Scene;
import com.urntt.noclip.ServerAddresses;
import com.urntt.noclip.config.CollisionType;
import com.urntt.noclip.config.MultiplayerMode;
import com.urntt.noclip.config.NoClipConfig;
import com.urntt.noclip.config.Toggle;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.Direction;

/**
 * Checks the address matching, the configuration, the rules, and the controller without a world. Each check uses its
 * own configuration file, so the game's configuration is left alone. Restarting the game is not possible in a game
 * test, so this is also where "reset on game exit" is covered.
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
		check(!config.isEnabled(), "noclip should be off by default");
		check(config.get(Toggle.AXIS_X) && !config.get(Toggle.AXIS_Y) && config.get(Toggle.AXIS_Z),
				"only the X and Z axes should be enabled by default");
		for (CollisionType type : CollisionType.values()) {
			check(config.get(Toggle.forType(type)), type + " should be passed through by default");
		}
		check(!config.onlyWhileFlying(), "only while flying should be off by default");
		check(config.get(Toggle.CLEAR_VIEW), "clear view inside blocks should be on by default");
		check(!config.singleplayerDefault(), "the singleplayer default should be off");
		check(!config.multiplayerDefault(), "the server default should be off");
		check(!config.resetOnWorldExit(), "reset on world exit should be off by default");
		check(!config.resetOnGameExit(), "reset on game exit should be off by default");
		check(config.multiplayerMode() == MultiplayerMode.DISABLED, "multiplayer should be disabled by default");
		check(config.servers().isEmpty(), "the server list should be empty by default");
		check(Files.exists(directory.resolve("defaults.json")), "a missing config file should be created");

		NoClipRules rules = config.rules();
		check(rules.axes().equals(EnumSet.of(Direction.Axis.X, Direction.Axis.Z)), "default rules should use X and Z");
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
		check(config.isEnabled(), "existing settings should be kept");
		check(config.get(Toggle.AXIS_X) && config.get(Toggle.AXIS_Y) && config.get(Toggle.AXIS_Z),
				"missing axes should get their defaults next to the saved one");
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
		check(!broken.isEnabled(), "an unreadable file should fall back to the defaults");
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

		NoClipRules fluidsOnly = new NoClipRules(EnumSet.of(CollisionType.FLUIDS), EnumSet.of(Direction.Axis.X), true);
		check(!fluidsOnly.ignoresAnyShapes(), "fluids are not shapes");
		check(!fluidsOnly.keepsViewClear(), "the view should only be kept clear while passing through blocks");

		NoClipRules entitiesOnly = new NoClipRules(EnumSet.of(CollisionType.ENTITIES), EnumSet.of(Direction.Axis.Y), false);
		check(entitiesOnly.ignoresAnyShapes(), "entity hitboxes are shapes");
		check(!entitiesOnly.ignores(CollisionType.BLOCKS), "blocks were not selected");
	}

	private static void checkOnlyWhileFlying(final Path directory) {
		NoClipConfig config = NoClipConfig.load(directory.resolve("flying.json"));
		NoClipController controller = new NoClipController(config);
		controller.onJoin(Scene.SINGLEPLAYER);
		check(!controller.isActive(false) && !controller.isActive(true), "noclip should be off by default");

		config.setEnabled(true);
		check(controller.isActive(false) && controller.isActive(true), "noclip should be active walking and flying");
		config.set(Toggle.ONLY_WHILE_FLYING, true);
		check(!controller.isActive(false), "only while flying should rule out walking");
		check(controller.isActive(true), "only while flying should allow flying");
	}

	private static void checkMultiplayerRules(final Path directory) {
		NoClipConfig config = NoClipConfig.load(directory.resolve("rules.json"));
		config.setEnabled(true);
		NoClipController controller = new NoClipController(config);

		check(!controller.isActive(false), "nothing should be active outside a world");
		controller.onJoin(Scene.SINGLEPLAYER);
		check(controller.isActive(false), "singleplayer should always be allowed");

		controller.onJoin(SERVER_A);
		check(!controller.isAllowed(), "the disabled mode should rule out every server");
		check(controller.toggle() == NoClipController.ToggleResult.BLOCKED, "toggling should be blocked");
		check(config.isEnabled(), "a blocked toggle should leave the state unchanged");

		config.setServers(List.of("a.example.com"));
		config.setMultiplayerMode(MultiplayerMode.WHITELIST);
		check(controller.isActive(false), "a whitelisted server should be allowed");
		check(controller.toggle() == NoClipController.ToggleResult.DISABLED, "toggling should work when allowed");
		check(controller.toggle() == NoClipController.ToggleResult.ENABLED, "toggling should work when allowed");
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

	private static void checkResetRules(final Path directory) {
		NoClipConfig config = NoClipConfig.load(directory.resolve("reset.json"));
		config.setResetOnGameExit(true);
		config.setSingleplayerDefault(true);
		config.setEnabled(false);

		// A fresh controller stands for a game that has just started.
		NoClipController controller = new NoClipController(config);
		controller.onJoin(SERVER_A);
		check(!config.isEnabled(), "a ruled-out server should not apply the game exit reset");
		controller.onDisconnect();
		controller.onJoin(Scene.SINGLEPLAYER);
		check(config.isEnabled(), "the first allowed world after starting should restore the default");
		controller.onDisconnect();
		config.setEnabled(false);
		controller.onJoin(Scene.SINGLEPLAYER);
		check(!config.isEnabled(), "later worlds should keep the state without reset on world exit");
		controller.onDisconnect();

		config.setResetOnWorldExit(true);
		controller.onJoin(Scene.SINGLEPLAYER);
		check(config.isEnabled(), "reset on world exit should restore the singleplayer default");
		controller.onDisconnect();

		config.setMultiplayerMode(MultiplayerMode.WHITELIST);
		config.setServers(List.of("a.example.com"));
		config.setMultiplayerDefault(false);
		controller.onJoin(SERVER_A);
		check(!config.isEnabled(), "an allowed server should restore the server default");
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
