package com.urntt.noclip.gametest;

import static com.urntt.noclip.gametest.GameTestSupport.LOGGER;
import static com.urntt.noclip.gametest.GameTestSupport.PROBE_DISTANCE;
import static com.urntt.noclip.gametest.GameTestSupport.bindKey;
import static com.urntt.noclip.gametest.GameTestSupport.center;
import static com.urntt.noclip.gametest.GameTestSupport.check;
import static com.urntt.noclip.gametest.GameTestSupport.checkProbe;
import static com.urntt.noclip.gametest.GameTestSupport.checkSetBack;
import static com.urntt.noclip.gametest.GameTestSupport.configure;
import static com.urntt.noclip.gametest.GameTestSupport.fill;
import static com.urntt.noclip.gametest.GameTestSupport.get;
import static com.urntt.noclip.gametest.GameTestSupport.loadSavedConfig;
import static com.urntt.noclip.gametest.GameTestSupport.moveBy;
import static com.urntt.noclip.gametest.GameTestSupport.moveWithInput;
import static com.urntt.noclip.gametest.GameTestSupport.prepare;
import static com.urntt.noclip.gametest.GameTestSupport.set;
import static com.urntt.noclip.gametest.GameTestSupport.teleport;
import static com.urntt.noclip.gametest.GameTestSupport.unbindKey;
import static com.urntt.noclip.gametest.GameTestSupport.walkForward;

import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.config.MultiplayerMode;
import com.urntt.noclip.config.NoClipConfigScreen;
import com.urntt.noclip.config.ServerListScreen;
import com.urntt.noclip.config.Toggle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Checks every collision type, the axes, the toggle keys, and the settings screens in a singleplayer world, and that
 * the integrated server still sets the player back when they move into blocks or solid entities.
 */
@SuppressWarnings("UnstableApiUsage")
public final class NoClipClientGameTest implements FabricClientGameTest {
	private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
	private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
	/** Height of the standing player's bounding box. */
	private static final double PLAYER_HEIGHT = 1.8;
	/** Distance between the areas of the individual checks. */
	private static final int AREA_SPACING = 6;
	/** Ticks the forward key is held when comparing walking speeds. */
	private static final int WALK_TICKS = 20;
	/** Ticks to watch the player stay put or be pushed. */
	private static final int OBSERVED_TICKS = 20;
	/** Height of the water column above soul sand that forms a bubble column. */
	private static final int BUBBLE_COLUMN_HEIGHT = 4;
	/** Height of soul sand's collision shape. */
	private static final double SOUL_SAND_HEIGHT = 14.0 / 16.0;
	/** Screenshots it takes to show the whole settings screen, and how far to scroll between them. */
	private static final int SETTINGS_PAGES = 5;
	private static final int SETTINGS_SCROLL_PER_PAGE = 10;

	@Override
	public void runTest(final ClientGameTestContext context) {
		check(!get(context, Toggle.ENABLED), "noclip should be off by default");

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			BlockPos origin = prepare(context, server);

			checkWalls(context, server, origin);
			checkWorldBorder(context, server, origin.south(AREA_SPACING));
			checkFluidsAndSlowingBlocks(context, server, origin.south(2 * AREA_SPACING));
			checkEntities(context, server, origin.south(3 * AREA_SPACING));
			checkInsideBlocks(context, server, origin.south(4 * AREA_SPACING));
			checkKeys(context, server, origin);
		}

		configure(context, config -> config.setEnabled(false));
	}

	/**
	 * Walls to the east and south of the player: noclip off, noclip on, a collision type off, an axis off, the Y
	 * axis, and "only while flying". Every pass into a wall is followed by the server's set-back.
	 */
	private static void checkWalls(final ClientGameTestContext context, final TestServerContext server, final BlockPos area) {
		fill(context, server, area.offset(1, 0, -1), area.offset(1, 1, 1), Blocks.STONE);
		fill(context, server, area.offset(-1, 0, 1), area.offset(0, 1, 1), Blocks.STONE);
		Vec3 start = center(area);
		teleport(context, server, start);

		checkProbe(context, EAST, false, "into a wall with noclip off");
		teleport(context, server, start);

		set(context, Toggle.ENABLED, true);
		checkProbe(context, EAST, true, "into a wall with noclip on");
		checkSetBack(context, start, "after moving into a wall");

		set(context, Toggle.BLOCKS, false);
		checkProbe(context, EAST, false, "into a wall without passing through blocks");
		set(context, Toggle.BLOCKS, true);
		teleport(context, server, start);

		checkProbe(context, SOUTH, true, "into a wall along the Z axis");
		checkSetBack(context, start, "after moving into a wall along the Z axis");
		set(context, Toggle.AXIS_Z, false);
		checkProbe(context, SOUTH, false, "into a wall with the Z axis off");
		set(context, Toggle.AXIS_Z, true);
		teleport(context, server, start);

		double down = moveBy(context, new Vec3(0.0, -0.5, 0.0)).y;
		LOGGER.info("Moved {} into the ground with the Y axis off", down);
		check(Math.abs(down) < 1.0E-6, "the ground should stop the player while the Y axis is off, moved " + down);
		double downWithY = context.computeOnClient(client -> {
			NoClipClient.config().set(Toggle.AXIS_Y, true);
			double y = client.player.getY();
			client.player.move(MoverType.SELF, new Vec3(0.0, -0.5, 0.0));
			// Turn the Y axis off again before the next tick, so gravity does not pull the player through the world.
			NoClipClient.config().set(Toggle.AXIS_Y, false);
			return client.player.getY() - y;
		});
		LOGGER.info("Moved {} into the ground with the Y axis on", downWithY);
		check(Math.abs(downWithY + 0.5) < 1.0E-6, "the Y axis should let the player into the ground, moved " + downWithY);
		checkSetBack(context, start, "after moving into the ground");

		checkJumpAndSneak(context, server, area, start);

		set(context, Toggle.ONLY_WHILE_FLYING, true);
		checkProbe(context, EAST, false, "into a wall walking with only while flying");
		teleport(context, server, start);
		double flying = context.computeOnClient(client -> {
			client.player.getAbilities().flying = true;
			double x = client.player.getX();
			client.player.move(MoverType.SELF, EAST);
			client.player.getAbilities().flying = false;
			return client.player.getX() - x;
		});
		LOGGER.info("Moved {} into a wall flying with only while flying", flying);
		check(Math.abs(flying - 1.0) < 1.0E-6, "only while flying should allow noclip while flying, moved " + flying);
		checkSetBack(context, start, "after moving into a wall while flying");
		set(context, Toggle.ONLY_WHILE_FLYING, false);

		fill(context, server, area.offset(-1, 0, -1), area.offset(1, 2, 1), Blocks.AIR);
	}

	/**
	 * A block right above the player's head and the ground below, with the Y axis off: holding jump passes up through
	 * the block only with "jump to pass up", and holding sneak passes down through the ground only with "sneak to
	 * pass down". Neither key opens the other direction.
	 */
	private static void checkJumpAndSneak(final ClientGameTestContext context, final TestServerContext server,
			final BlockPos area, final Vec3 start) {
		fill(context, server, area.above(2), area.above(2), Blocks.STONE);
		Vec3 up = new Vec3(0.0, PROBE_DISTANCE, 0.0);
		Vec3 down = new Vec3(0.0, -0.5, 0.0);
		double headroom = 2.0 - PLAYER_HEIGHT;

		checkVertical(context, moveWithInput(context, up, true, false).y, headroom,
				"up holding jump without jump to pass up");
		teleport(context, server, start);
		set(context, Toggle.JUMP_UP, true);
		checkVertical(context, moveWithInput(context, up, false, false).y, headroom,
				"up without holding jump");
		teleport(context, server, start);
		checkVertical(context, moveWithInput(context, up, true, false).y, PROBE_DISTANCE,
				"up holding jump with jump to pass up");
		checkSetBack(context, start, "after jumping into a block");
		checkVertical(context, moveWithInput(context, down, true, false).y, 0.0, "down holding jump");
		set(context, Toggle.JUMP_UP, false);

		checkVertical(context, moveWithInput(context, down, false, true).y, 0.0,
				"down holding sneak without sneak to pass down");
		set(context, Toggle.SNEAK_DOWN, true);
		checkVertical(context, moveWithInput(context, down, false, true).y, down.y,
				"down holding sneak with sneak to pass down");
		checkSetBack(context, start, "after sneaking into the ground");
		checkVertical(context, moveWithInput(context, up, false, true).y, headroom, "up holding sneak");
		set(context, Toggle.SNEAK_DOWN, false);
		teleport(context, server, start);
	}

	private static void checkVertical(final ClientGameTestContext context, final double moved, final double expected,
			final String situation) {
		LOGGER.info("Moved {} {}", String.format(Locale.ROOT, "%.3f", moved), situation);
		check(Math.abs(moved - expected) < 1.0E-6, "expected to move " + expected + " " + situation + ", moved " + moved);
	}

	/**
	 * A small world border around the player. The server does not check the world border when it validates
	 * movement, so it keeps the player outside.
	 */
	private static void checkWorldBorder(final ClientGameTestContext context, final TestServerContext server,
			final BlockPos area) {
		Vec3 start = center(area);
		teleport(context, server, start);
		server.runCommand(String.format(Locale.ROOT, "worldborder center %.1f %.1f", start.x, start.z));
		server.runCommand("worldborder set 3");
		context.waitFor(client -> client.level.getWorldBorder().getSize() == 3.0
				&& client.level.getWorldBorder().getCenterX() == start.x);
		Vec3 probe = EAST.scale(2.0);

		set(context, Toggle.WORLD_BORDER, false);
		double blocked = moveBy(context, probe).x;
		LOGGER.info("Moved {} towards the world border without passing through it", blocked);
		check(Math.abs(blocked - 1.2) < 1.0E-3, "the world border should stop the player, moved " + blocked);
		teleport(context, server, start);

		set(context, Toggle.WORLD_BORDER, true);
		double passed = moveBy(context, probe).x;
		context.waitTicks(OBSERVED_TICKS);
		double clientX = context.computeOnClient(client -> client.player.getX());
		double serverX = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getX());
		LOGGER.info("Moved {} through the world border; after {} ticks the client is at {} and the server at {}", passed,
				OBSERVED_TICKS, clientX - start.x, serverX - start.x);
		check(Math.abs(passed - 2.0) < 1.0E-6, "the player should pass through the world border, moved " + passed);
		check(Math.abs(clientX - (start.x + 2.0)) < 1.0E-6 && Math.abs(serverX - clientX) < 1.0E-6,
				"the server should accept a position beyond the world border");

		teleport(context, server, start);
		server.runCommand("worldborder center 0 0");
		server.runCommand("worldborder set 59999968");
		context.waitFor(client -> client.level.getWorldBorder().getSize() > 1000.0);
	}

	/**
	 * A water channel and a cobweb channel, compared with walking on plain ground. The server does not object to
	 * either.
	 */
	private static void checkFluidsAndSlowingBlocks(final ClientGameTestContext context, final TestServerContext server,
			final BlockPos area) {
		Vec3 start = center(area);
		BlockPos channelStart = area.offset(-1, 0, -1);
		BlockPos channelEnd = area.offset(10, 0, 1);
		// Walls around the channel keep the water from flowing out, so the channel holds still source blocks.
		fill(context, server, area.offset(-2, 0, -2), area.offset(11, 0, -2), Blocks.STONE);
		fill(context, server, area.offset(-2, 0, 2), area.offset(11, 0, 2), Blocks.STONE);
		fill(context, server, area.offset(-2, 0, -1), area.offset(-2, 0, 1), Blocks.STONE);
		fill(context, server, area.offset(11, 0, -1), area.offset(11, 0, 1), Blocks.STONE);

		teleport(context, server, start);
		double ground = walkForward(context, WALK_TICKS);
		LOGGER.info("Walked {} in {} ticks on the ground", ground, WALK_TICKS);

		fill(context, server, channelStart, channelEnd, Blocks.WATER);
		set(context, Toggle.FLUIDS, false);
		teleport(context, server, start);
		check(context.computeOnClient(client -> client.player.isInWater()), "the player should be in the water");
		double water = walkForward(context, WALK_TICKS);
		set(context, Toggle.FLUIDS, true);
		teleport(context, server, start);
		check(!context.computeOnClient(client -> client.player.isInWater()),
				"passing through fluids, the player should not count as being in water");
		double waterWithNoclip = walkForward(context, WALK_TICKS);
		LOGGER.info("Walked {} in water and {} in water passing through fluids", water, waterWithNoclip);
		check(water < 0.75 * ground, "water should slow the player down without passing through fluids");
		check(waterWithNoclip > 0.95 * ground, "passing through fluids, water should not slow the player down");

		checkBubbleColumn(context, server, area.offset(0, 0, 4));

		fill(context, server, channelStart, channelEnd, Blocks.AIR);
		fill(context, server, channelStart, channelEnd.above(), Blocks.COBWEB);
		set(context, Toggle.SLOWING_BLOCKS, false);
		teleport(context, server, start);
		double cobweb = walkForward(context, WALK_TICKS);
		set(context, Toggle.SLOWING_BLOCKS, true);
		teleport(context, server, start);
		double cobwebWithNoclip = walkForward(context, WALK_TICKS);
		LOGGER.info("Walked {} in cobwebs and {} in cobwebs passing through slowing blocks", cobweb, cobwebWithNoclip);
		check(cobweb < 0.5 * ground, "cobwebs should slow the player down without passing through slowing blocks");
		check(cobwebWithNoclip > 0.95 * ground, "passing through slowing blocks, cobwebs should not slow the player down");

		fill(context, server, area.offset(-2, 0, -2), area.offset(11, 1, 2), Blocks.AIR);
	}

	/**
	 * An upward bubble column, which lifts the player unless noclip passes through fluids.
	 */
	private static void checkBubbleColumn(final ClientGameTestContext context, final TestServerContext server,
			final BlockPos spot) {
		fill(context, server, spot.offset(-1, 0, -1), spot.offset(1, BUBBLE_COLUMN_HEIGHT - 1, 1), Blocks.STONE);
		fill(context, server, spot.below(), spot.below(), Blocks.SOUL_SAND);
		fill(context, server, spot, spot.above(BUBBLE_COLUMN_HEIGHT - 1), Blocks.WATER);
		context.waitFor(client -> client.level.getBlockState(spot.above(BUBBLE_COLUMN_HEIGHT - 1)).is(Blocks.BUBBLE_COLUMN));
		// Stand on the soul sand, which is lower than a full block.
		Vec3 start = center(spot).subtract(0.0, 1.0 - SOUL_SAND_HEIGHT, 0.0);

		teleport(context, server, start);
		double risenWithNoclip = riseAfterTicks(context, OBSERVED_TICKS);
		set(context, Toggle.FLUIDS, false);
		teleport(context, server, start);
		double risen = riseAfterTicks(context, OBSERVED_TICKS);
		set(context, Toggle.FLUIDS, true);
		LOGGER.info("A bubble column lifted the player {} passing through fluids and {} without", risenWithNoclip, risen);
		check(risenWithNoclip <= 0.0, "passing through fluids, a bubble column should not lift the player");
		check(risen > 1.0, "without passing through fluids, a bubble column should lift the player");

		teleport(context, server, center(spot.offset(0, 0, -3)));
		fill(context, server, spot.offset(-1, 0, -1), spot.offset(1, BUBBLE_COLUMN_HEIGHT - 1, 1), Blocks.AIR);
		fill(context, server, spot.below(), spot.below(), Blocks.GRASS_BLOCK);
	}

	/**
	 * Waits {@code ticks} ticks and returns the highest the player got above where they started.
	 */
	private static double riseAfterTicks(final ClientGameTestContext context, final int ticks) {
		double startY = context.computeOnClient(client -> client.player.getY());
		double highest = Double.NEGATIVE_INFINITY;
		for (int tick = 0; tick < ticks; tick++) {
			context.waitTick();
			highest = Math.max(highest, context.computeOnClient(client -> client.player.getY()) - startY);
		}
		return highest;
	}

	/**
	 * A shulker next to the player, which the server also checks, and a pig on top of the player, which pushes them.
	 */
	private static void checkEntities(final ClientGameTestContext context, final TestServerContext server,
			final BlockPos area) {
		Vec3 start = center(area);
		teleport(context, server, start);
		AABB around = new AABB(area).inflate(3.0);

		BlockPos shulkerPos = area.east();
		server.runCommand(String.format(Locale.ROOT, "summon minecraft:shulker %d %d %d {NoAI:1b}", shulkerPos.getX(),
				shulkerPos.getY(), shulkerPos.getZ()));
		context.waitFor(client -> !client.level.getEntitiesOfClass(Shulker.class, around).isEmpty());

		set(context, Toggle.ENTITIES, false);
		checkProbe(context, EAST, false, "into a shulker without passing through entities");
		set(context, Toggle.ENTITIES, true);
		teleport(context, server, start);
		checkProbe(context, EAST, true, "into a shulker");
		checkSetBack(context, start, "after moving into a shulker");
		server.runCommand("kill @e[type=minecraft:shulker]");
		context.waitFor(client -> client.level.getEntitiesOfClass(Shulker.class, around).isEmpty());

		server.runCommand(String.format(Locale.ROOT, "summon minecraft:pig %.2f %.2f %.2f {NoAI:1b}", start.x + 0.2,
				start.y, start.z));
		context.waitFor(client -> !client.level.getEntitiesOfClass(Pig.class, around).isEmpty());
		double pushedWithNoclip = displacementAfterTicks(context, OBSERVED_TICKS);
		set(context, Toggle.ENTITIES, false);
		double pushed = displacementAfterTicks(context, OBSERVED_TICKS);
		set(context, Toggle.ENTITIES, true);
		LOGGER.info("A pig pushed the player {} passing through entities and {} without", pushedWithNoclip, pushed);
		check(pushedWithNoclip < 1.0E-6, "passing through entities, a pig should not push the player");
		check(pushed > 0.1, "without passing through entities, a pig should push the player");
		server.runCommand("kill @e[type=minecraft:pig]");
		context.waitFor(client -> client.level.getEntitiesOfClass(Pig.class, around).isEmpty());
		// The pig's drops would otherwise end up in the hotbar of every later screenshot.
		server.runCommand("kill @e[type=minecraft:item]");
		server.runCommand("clear @a");
	}

	/**
	 * The player teleported into a pillar of stone, and below a single block at head height.
	 */
	private static void checkInsideBlocks(final ClientGameTestContext context, final TestServerContext server,
			final BlockPos area) {
		Vec3 start = center(area);
		fill(context, server, area, area.above(), Blocks.STONE);
		teleport(context, server, start);

		double moved = displacementAfterTicks(context, OBSERVED_TICKS);
		Pose pose = context.computeOnClient(client -> client.player.getPose());
		LOGGER.info("Inside a pillar passing through blocks, the player moved {} and is {}", moved, pose);
		check(moved < 1.0E-6, "passing through blocks, the player should not be pushed out of a pillar");
		check(pose == Pose.STANDING, "passing through blocks, the player should keep standing, got " + pose);
		checkView(context, true);
		context.takeScreenshot("noclip-inside-block-clear-view");
		set(context, Toggle.CLEAR_VIEW, false);
		checkView(context, false);
		context.takeScreenshot("noclip-inside-block-overlay");
		set(context, Toggle.CLEAR_VIEW, true);

		set(context, Toggle.ENABLED, false);
		int ticks = context.waitFor(client -> client.player.position().distanceTo(start) > 0.5, OBSERVED_TICKS * 5);
		LOGGER.info("With noclip off, the player was pushed out of the pillar after {} ticks", ticks);

		fill(context, server, area, area, Blocks.AIR);
		teleport(context, server, start);
		Pose crawling = context.computeOnClient(client -> client.player.getPose());
		set(context, Toggle.ENABLED, true);
		context.waitTicks(2);
		Pose withNoclip = context.computeOnClient(client -> client.player.getPose());
		LOGGER.info("Below a block at head height the player is {} with noclip off and {} with noclip on", crawling,
				withNoclip);
		check(crawling == Pose.SWIMMING, "below a block at head height vanilla should make the player crawl");
		check(withNoclip == Pose.STANDING, "passing through blocks, the player should stand up inside the block");

		fill(context, server, area, area.above(), Blocks.AIR);
	}

	/**
	 * The main toggle key, one collision type toggle key, the settings key, and screenshots of the settings screens.
	 */
	private static void checkKeys(final ClientGameTestContext context, final TestServerContext server, final BlockPos area) {
		teleport(context, server, center(area));
		KeyMapping enabledKey = bindKey(context, Toggle.ENABLED.keyMappingName(), "key.keyboard.j");
		KeyMapping blocksKey = bindKey(context, Toggle.BLOCKS.keyMappingName(), "key.keyboard.k");
		KeyMapping openSettingsKey = bindKey(context, NoClipClient.OPEN_SETTINGS_KEY_NAME, "key.keyboard.u");

		boolean enabled = get(context, Toggle.ENABLED);
		context.getInput().pressKey(enabledKey);
		context.waitTick();
		check(get(context, Toggle.ENABLED) == !enabled, "the toggle key should flip noclip");
		check(loadSavedConfig().isEnabled() == !enabled, "the toggled state should be saved to the config file");
		context.takeScreenshot("noclip-toggled");
		context.getInput().pressKey(enabledKey);
		context.waitTick();
		check(get(context, Toggle.ENABLED) == enabled, "the toggle key should flip noclip back");

		context.getInput().pressKey(blocksKey);
		context.waitTick();
		check(!get(context, Toggle.BLOCKS), "the blocks key should stop passing through blocks");
		check(!loadSavedConfig().get(Toggle.BLOCKS), "the blocks setting should be saved to the config file");
		context.takeScreenshot("noclip-blocks-toggled");
		context.getInput().pressKey(blocksKey);
		context.waitTick();
		check(get(context, Toggle.BLOCKS), "the blocks key should pass through blocks again");

		context.getInput().pressKey(openSettingsKey);
		context.waitForScreen(NoClipConfigScreen.class);
		context.setScreen(() -> null);

		configure(context, config -> {
			config.setMultiplayerMode(MultiplayerMode.WHITELIST);
			config.setServers(List.of("mc.example.com", "192.168.1.5"));
		});
		takeSettingsScreenshots(context, "en_us");
		switchLanguage(context, "zh_cn");
		takeSettingsScreenshots(context, "zh_cn");
		switchLanguage(context, "en_us");
		configure(context, config -> {
			config.setMultiplayerMode(MultiplayerMode.DISABLED);
			config.setServers(List.of());
		});

		unbindKey(context, enabledKey);
		unbindKey(context, blocksKey);
		unbindKey(context, openSettingsKey);
	}

	/**
	 * Checks what the last rendered frame did with the camera inside a block: with a clear view, no block overlay
	 * and no occlusion culling, as for spectators; otherwise vanilla's overlay and culling.
	 */
	private static void checkView(final ClientGameTestContext context, final boolean clear) {
		context.waitTicks(2);
		boolean overlay = context.computeOnClient(
				client -> client.gameRenderer.gameRenderState().levelRenderState.playerRenderState.blockOverlay != null);
		boolean smartCull = context.computeOnClient(
				client -> client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.smartCull);
		LOGGER.info("Inside a block with clear view {}: overlay {}, occlusion culling {}", clear, overlay, smartCull);
		check(overlay != clear, "the in-wall overlay should be " + (clear ? "hidden" : "shown"));
		check(smartCull != clear, "occlusion culling should be " + (clear ? "off" : "on"));
	}

	/**
	 * Waits {@code ticks} ticks and returns how far the player moved horizontally.
	 */
	private static double displacementAfterTicks(final ClientGameTestContext context, final int ticks) {
		Vec3 before = context.computeOnClient(client -> client.player.position());
		context.waitTicks(ticks);
		Vec3 after = context.computeOnClient(client -> client.player.position());
		return after.subtract(before).horizontalDistance();
	}

	/**
	 * Takes screenshots of the settings screen from top to bottom, and of the server list screen.
	 */
	private static void takeSettingsScreenshots(final ClientGameTestContext context, final String language) {
		context.setScreen(() -> new NoClipConfigScreen(null));
		for (int page = 1; page <= SETTINGS_PAGES; page++) {
			if (page > 1) {
				// The list only scrolls under the cursor.
				context.getInput().setCursorPos(context.computeOnClient(client -> client.getWindow().getWidth() / 2.0),
						context.computeOnClient(client -> client.getWindow().getHeight() / 2.0));
				context.getInput().scroll(page == SETTINGS_PAGES ? -100 : -SETTINGS_SCROLL_PER_PAGE);
			}
			// Keep the cursor away from the widgets so no tooltip covers them.
			context.getInput().setCursorPos(0, 0);
			context.waitTick();
			context.takeScreenshot("noclip-config-screen-" + page + "-" + language);
		}

		context.setScreen(() -> new ServerListScreen(new NoClipConfigScreen(null), NoClipClient.config()));
		context.getInput().setCursorPos(0, 0);
		context.waitTick();
		context.takeScreenshot("noclip-server-list-" + language);
		context.setScreen(() -> null);
	}

	private static void switchLanguage(final ClientGameTestContext context, final String language) {
		CompletableFuture<Void> reload = context.computeOnClient(client -> {
			client.getLanguageManager().setSelected(language);
			client.options.languageCode = language;
			return client.reloadResourcePacks();
		});
		context.waitFor(client -> reload.isDone() && client.gui.overlay() == null);
	}
}
