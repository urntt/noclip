package com.urntt.noclip.config;

import com.urntt.noclip.NoClipClient;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Configuration screen built from vanilla widgets. Every change is saved immediately.
 */
public final class NoClipConfigScreen extends OptionsSubScreen {
	private static final Component TITLE = Component.translatable("options.noclip.title");
	private static final Component MULTIPLAYER_WARNING = Component.translatable("options.noclip.multiplayer_mode.warning");

	public NoClipConfigScreen(final @Nullable Screen parent) {
		super(parent, Minecraft.getInstance().options, TITLE);
	}

	@Override
	protected void addOptions() {
		if (this.list == null) {
			return;
		}
		NoClipConfig config = NoClipClient.config();

		this.addToggles("options.noclip.section.current", config, Toggle.ENABLED);
		this.addToggles("options.noclip.section.axes", config, Toggle.AXIS_X, Toggle.AXIS_Y, Toggle.AXIS_Z);
		this.addToggles("options.noclip.section.collisions", config, Toggle.BLOCKS, Toggle.FLUIDS, Toggle.WORLD_BORDER,
				Toggle.SLOWING_BLOCKS, Toggle.ENTITIES);
		this.addToggles("options.noclip.section.options", config, Toggle.ONLY_WHILE_FLYING, Toggle.CLEAR_VIEW);

		this.list.addHeader(Component.translatable("options.noclip.section.defaults"));
		this.list.addBig(option("options.noclip.singleplayer_default", config.singleplayerDefault(),
				config::setSingleplayerDefault));
		this.list.addBig(option("options.noclip.multiplayer_default", config.multiplayerDefault(),
				config::setMultiplayerDefault));

		this.list.addHeader(Component.translatable("options.noclip.section.reset"));
		this.list.addBig(option("options.noclip.reset_on_world_exit", config.resetOnWorldExit(),
				config::setResetOnWorldExit));
		this.list.addBig(option("options.noclip.reset_on_game_exit", config.resetOnGameExit(),
				config::setResetOnGameExit));

		this.list.addHeader(Component.translatable("options.noclip.section.multiplayer"));
		this.list.addBig(new OptionInstance<>(
				"options.noclip.multiplayer_mode",
				mode -> Tooltip.create(CommonComponents.joinLines(mode.description(), CommonComponents.EMPTY,
						MULTIPLAYER_WARNING)),
				// The button itself prepends the caption, so this only names the value.
				(caption, mode) -> mode.label(),
				new OptionInstance.Enum<>(List.of(MultiplayerMode.values()), MultiplayerMode.CODEC),
				config.multiplayerMode(),
				config::setMultiplayerMode));
		this.list.addBig(Button.builder(Component.translatable("options.noclip.edit_servers"),
						button -> this.minecraft.gui.setScreen(new ServerListScreen(this, config)))
				.tooltip(Tooltip.create(Component.translatable("options.noclip.edit_servers.tooltip")))
				.build());
	}

	private void addToggles(final String headerKey, final NoClipConfig config, final Toggle... toggles) {
		if (this.list == null) {
			return;
		}
		this.list.addHeader(Component.translatable(headerKey));
		for (Toggle toggle : toggles) {
			this.list.addBig(OptionInstance.createBoolean(
					toggle.captionKey(),
					OptionInstance.cachedConstantTooltip(toggle.tooltip()),
					config.get(toggle),
					value -> config.set(toggle, value)));
		}
	}

	/**
	 * Creates an on/off option whose tooltip is the translation of {@code captionKey + ".tooltip"}.
	 */
	private static OptionInstance<Boolean> option(final String captionKey, final boolean value,
			final Consumer<Boolean> onChange) {
		return OptionInstance.createBoolean(
				captionKey,
				OptionInstance.cachedConstantTooltip(Component.translatable(captionKey + ".tooltip")),
				value,
				onChange::accept);
	}
}
