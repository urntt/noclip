package com.urntt.noclip.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.config.NoClipConfig;
import com.urntt.noclip.config.Toggle;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared helpers for the client game tests.
 */
@SuppressWarnings("UnstableApiUsage")
final class GameTestSupport {
	static final Logger LOGGER = LoggerFactory.getLogger("noclip-gametest");

	/** Half the width of the player's bounding box. */
	static final double HALF_PLAYER_WIDTH = 0.3;
	/** How far the player tries to move into an obstacle one block away, from the center of their block. */
	static final double PROBE_DISTANCE = 1.0;
	/** The distance from the center of a block to a neighboring block's face, minus the player's half width. */
	static final double DISTANCE_TO_NEIGHBOR = 0.5 - HALF_PLAYER_WIDTH;
	/** Ticks to wait for the server to react to a move. */
	static final int SERVER_REACTION_TICKS = 100;

	private GameTestSupport() {
	}

	/**
	 * Switches to creative mode, so being inside blocks does no damage, and returns the block the player stands in.
	 */
	static BlockPos prepare(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("gamemode creative @a");
		context.waitFor(client -> client.player != null && client.player.isCreative() && client.player.onGround());
		return context.computeOnClient(client -> client.player.blockPosition());
	}

	/** The center of the bottom face of {@code pos}, where a player standing in that block is placed. */
	static Vec3 center(final BlockPos pos) {
		return Vec3.atBottomCenterOf(pos);
	}

	/**
	 * Teleports the player to {@code target}, facing east (positive X), and waits until the client is there.
	 */
	static void teleport(final ClientGameTestContext context, final TestServerContext server, final Vec3 target) {
		server.runCommand(String.format(Locale.ROOT, "tp @a %.5f %.5f %.5f -90 0", target.x, target.y, target.z));
		context.waitFor(client -> client.player.position().distanceTo(target) < 1.0E-4);
		context.waitTicks(2);
	}

	/**
	 * Fills the box from {@code from} to {@code to} with {@code block} and waits until the client sees it.
	 */
	static void fill(final ClientGameTestContext context, final TestServerContext server, final BlockPos from,
			final BlockPos to, final Block block) {
		server.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d %s", from.getX(), from.getY(), from.getZ(),
				to.getX(), to.getY(), to.getZ(), BuiltInRegistries.BLOCK.getKey(block)));
		context.waitFor(client -> client.level.getBlockState(from).is(block) && client.level.getBlockState(to).is(block));
	}

	/**
	 * Moves the local player by {@code delta} the way the game moves them, with collisions, on the client thread.
	 * The result is read right away, before the server can react.
	 *
	 * @return how far the player actually moved
	 */
	static Vec3 moveBy(final ClientGameTestContext context, final Vec3 delta) {
		return moveWithInput(context, delta, false, false);
	}

	/**
	 * Like {@link #moveBy}, but with the jump and sneak keys held as given while the player moves. The player's
	 * input is restored afterwards, before the next tick.
	 */
	static Vec3 moveWithInput(final ClientGameTestContext context, final Vec3 delta, final boolean jump,
			final boolean sneak) {
		return context.computeOnClient(client -> {
			LocalPlayer player = client.player;
			Input previous = player.input.keyPresses;
			player.input.keyPresses = new Input(false, false, false, false, jump, sneak, false);
			Vec3 start = player.position();
			player.move(MoverType.SELF, delta);
			player.input.keyPresses = previous;
			return player.position().subtract(start);
		});
	}

	/**
	 * Tries to move one block along {@code direction} into an obstacle and checks whether the player was stopped by
	 * it or passed into it.
	 */
	static void checkProbe(final ClientGameTestContext context, final Vec3 direction, final boolean passes,
			final String situation) {
		Vec3 moved = moveBy(context, direction.scale(PROBE_DISTANCE));
		double distance = moved.dot(direction);
		LOGGER.info("Moved {} towards the obstacle {}", String.format(Locale.ROOT, "%.3f", distance), situation);
		if (passes) {
			check(Math.abs(distance - PROBE_DISTANCE) < 1.0E-6,
					"expected to move " + PROBE_DISTANCE + " " + situation + ", moved " + distance);
		} else {
			check(Math.abs(distance - DISTANCE_TO_NEIGHBOR) < 1.0E-3,
					"expected to stop after " + DISTANCE_TO_NEIGHBOR + " " + situation + ", moved " + distance);
		}
	}

	/**
	 * Waits until the server has set the player back to {@code start}.
	 */
	static void checkSetBack(final ClientGameTestContext context, final Vec3 start, final String situation) {
		int ticks = context.waitFor(client -> client.player.position().distanceTo(start) < 1.0E-3, SERVER_REACTION_TICKS);
		LOGGER.info("The server set the player back after {} ticks {}", ticks, situation);
	}

	/**
	 * Holds the forward key for {@code ticks} ticks and returns the distance walked along X.
	 */
	static double walkForward(final ClientGameTestContext context, final int ticks) {
		double startX = context.computeOnClient(client -> client.player.getX());
		context.getInput().holdKeyFor(options -> options.keyUp, ticks);
		return context.computeOnClient(client -> client.player.getX()) - startX;
	}

	/**
	 * Returns the mod's key mapping registered under {@code name}, bound to {@code key} for the test.
	 */
	static KeyMapping bindKey(final ClientGameTestContext context, final String name, final String key) {
		KeyMapping mapping = Objects.requireNonNull(KeyMapping.get(name), name);
		context.runOnClient(client -> {
			mapping.setKey(InputConstants.getKey(key));
			KeyMapping.resetMapping();
		});
		return mapping;
	}

	static void unbindKey(final ClientGameTestContext context, final KeyMapping mapping) {
		context.runOnClient(client -> {
			mapping.setKey(mapping.getDefaultKey());
			KeyMapping.resetMapping();
		});
	}

	/**
	 * Changes the mod's configuration on the client thread.
	 */
	static void configure(final ClientGameTestContext context, final Consumer<NoClipConfig> change) {
		context.runOnClient(client -> change.accept(NoClipClient.config()));
	}

	static void set(final ClientGameTestContext context, final Toggle toggle, final boolean value) {
		configure(context, config -> config.set(toggle, value));
	}

	static boolean get(final ClientGameTestContext context, final Toggle toggle) {
		return context.computeOnClient(client -> NoClipClient.config().get(toggle));
	}

	static NoClipConfig loadSavedConfig() {
		return NoClipConfig.load(NoClipConfig.defaultPath());
	}

	static void check(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
