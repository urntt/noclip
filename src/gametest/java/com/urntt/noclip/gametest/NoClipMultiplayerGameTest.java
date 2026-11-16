package com.urntt.noclip.gametest;

import static com.urntt.noclip.gametest.GameTestSupport.LOGGER;
import static com.urntt.noclip.gametest.GameTestSupport.bindKey;
import static com.urntt.noclip.gametest.GameTestSupport.center;
import static com.urntt.noclip.gametest.GameTestSupport.check;
import static com.urntt.noclip.gametest.GameTestSupport.checkProbe;
import static com.urntt.noclip.gametest.GameTestSupport.checkSetBack;
import static com.urntt.noclip.gametest.GameTestSupport.configure;
import static com.urntt.noclip.gametest.GameTestSupport.fill;
import static com.urntt.noclip.gametest.GameTestSupport.get;
import static com.urntt.noclip.gametest.GameTestSupport.prepare;
import static com.urntt.noclip.gametest.GameTestSupport.teleport;
import static com.urntt.noclip.gametest.GameTestSupport.unbindKey;

import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.Scene;
import com.urntt.noclip.config.MultiplayerMode;
import com.urntt.noclip.config.Toggle;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Checks the multiplayer modes on a local dedicated server, which the client reaches as {@code localhost}, and that
 * the vanilla server sets the player back when they move into a block.
 */
@SuppressWarnings("UnstableApiUsage")
public final class NoClipMultiplayerGameTest implements FabricClientGameTest {
	private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);

	@Override
	public void runTest(final ClientGameTestContext context) {
		configure(context, config -> {
			config.setEnabled(true);
			config.setResetOnWorldExit(false);
			config.setMultiplayerMode(MultiplayerMode.DISABLED);
			config.setServers(List.of());
		});
		KeyMapping toggleKey = bindKey(context, Toggle.ENABLED.keyMappingName(), "key.keyboard.j");

		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
				TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();
			BlockPos area = prepare(context, server);
			Scene scene = context.computeOnClient(client -> NoClipClient.controller().scene());
			LOGGER.info("Connected to {}", scene);
			check(scene instanceof Scene.Multiplayer, "a dedicated server should count as multiplayer, got " + scene);

			fill(context, server, area.offset(1, 0, -1), area.offset(1, 1, 1), Blocks.STONE);
			Vec3 start = center(area);
			teleport(context, server, start);

			checkProbe(context, EAST, false, "into a wall on a server with multiplayer disabled");
			teleport(context, server, start);
			context.getInput().pressKey(toggleKey);
			context.waitTick();
			check(get(context, Toggle.ENABLED), "a blocked toggle should leave the state unchanged");
			context.takeScreenshot("noclip-blocked-on-server");

			configure(context, config -> {
				config.setServers(List.of("localhost"));
				config.setMultiplayerMode(MultiplayerMode.WHITELIST);
			});
			checkProbe(context, EAST, true, "into a wall on a whitelisted server");
			checkSetBack(context, start, "by the dedicated server after moving into a wall");

			configure(context, config -> config.setMultiplayerMode(MultiplayerMode.BLACKLIST));
			checkProbe(context, EAST, false, "into a wall on a blacklisted server");
		}

		configure(context, config -> {
			config.setEnabled(false);
			config.setMultiplayerMode(MultiplayerMode.DISABLED);
			config.setServers(List.of());
		});
		unbindKey(context, toggleKey);
	}
}
