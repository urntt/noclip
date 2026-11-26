package com.urntt.noclip;

import com.mojang.blaze3d.platform.InputConstants;
import com.urntt.noclip.config.NoClipConfig;
import com.urntt.noclip.config.NoClipConfigScreen;
import com.urntt.noclip.config.Toggle;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class NoClipClient implements ClientModInitializer {
	public static final String MOD_ID = "noclip";
	public static final String OPEN_SETTINGS_KEY_NAME = "key.noclip.open_settings";

	/** Action bar message shown when the main toggle key is pressed on a server the multiplayer rules rule out. */
	public static final Component BLOCKED_MESSAGE = Component.translatable("message.noclip.blocked");
	/** Action bar message shown when a toggle key is pressed while disabled mode is selected. */
	public static final Component UNAVAILABLE_MESSAGE = Component.translatable("message.noclip.unavailable");

	private static NoClipConfig config;
	private static NoClipController controller;

	@Override
	public void onInitializeClient() {
		config = NoClipConfig.load(NoClipConfig.defaultPath());
		controller = new NoClipController(config::effective, config::editable);

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "general"));
		Map<Toggle, KeyMapping> toggleKeys = new EnumMap<>(Toggle.class);
		for (Toggle toggle : Toggle.values()) {
			toggleKeys.put(toggle, KeyMappingHelper.registerKeyMapping(
					new KeyMapping(toggle.keyMappingName(), InputConstants.UNKNOWN.getValue(), category)));
		}
		KeyMapping openSettingsKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping(OPEN_SETTINGS_KEY_NAME, InputConstants.UNKNOWN.getValue(), category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			toggleKeys.forEach((toggle, key) -> {
				while (key.consumeClick()) {
					toggle(client, toggle);
				}
			});
			while (openSettingsKey.consumeClick()) {
				NoClipConfigScreen.open(client);
			}
		});

		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> controller.onJoin(Scene.of(client)));
		// The disconnect event may arrive on the network thread; the controller is only used on the client thread.
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> client.execute(controller::onDisconnect));
	}

	public static NoClipConfig config() {
		return config;
	}

	public static NoClipController controller() {
		return controller;
	}

	/**
	 * Flips {@code toggle} in the settings of the configuration mode and shows the outcome on the action bar.
	 */
	private static void toggle(final Minecraft client, final Toggle toggle) {
		Component message = switch (controller.toggle(toggle)) {
			case ON -> CommonComponents.optionStatus(toggle.caption(), true);
			case OFF -> CommonComponents.optionStatus(toggle.caption(), false);
			case BLOCKED -> BLOCKED_MESSAGE;
			case UNAVAILABLE -> UNAVAILABLE_MESSAGE;
		};
		if (client.player != null) {
			client.player.sendOverlayMessage(message);
		}
	}
}
