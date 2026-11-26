package com.urntt.noclip.config;

import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.compat.OneConfigBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The configuration screen, built from vanilla widgets. The mode button comes first. The settings below it can only
 * be edited in custom mode; otherwise they are greyed out and show the values that currently apply. OneConfig mode
 * has no defaults, reset rules, or multiplayer rules, so their sections are left out there.
 */
public final class NoClipConfigScreen extends OptionsSubScreen {
	private static final Component MULTIPLAYER_WARNING = Component.translatable(SettingKeys.MULTIPLAYER_WARNING);

	private final NoClipConfig config = NoClipClient.config();

	public NoClipConfigScreen(final @Nullable Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable(SettingKeys.TITLE));
	}

	@Override
	protected void addOptions() {
		if (this.list == null) {
			return;
		}
		ConfigMode mode = this.config.effectiveMode();
		boolean editable = mode == ConfigMode.CUSTOM;
		NoClipSettings shown = this.config.effective();

		this.list.addBig(this.createModeButton(mode));
		if (mode == ConfigMode.ONECONFIG) {
			this.list.addBig(Button.builder(Component.translatable(SettingKeys.OPEN_ONECONFIG),
					button -> OneConfigBridge.openOneConfig()).build());
		}

		this.addToggles(SettingKeys.SECTION_CURRENT, shown, editable, Toggle.ENABLED);
		this.addToggles(SettingKeys.SECTION_AXES, shown, editable,
				Toggle.AXIS_X, Toggle.AXIS_Y, Toggle.AXIS_Z, Toggle.JUMP_UP, Toggle.SNEAK_DOWN);
		this.addToggles(SettingKeys.SECTION_COLLISIONS, shown, editable,
				Toggle.BLOCKS, Toggle.FLUIDS, Toggle.WORLD_BORDER, Toggle.SLOWING_BLOCKS, Toggle.ENTITIES);
		this.addToggles(SettingKeys.SECTION_OPTIONS, shown, editable, Toggle.ONLY_WHILE_FLYING, Toggle.CLEAR_VIEW);

		SceneRules rules = shown.sceneRules();
		if (rules != null) {
			this.addSceneRules(rules, editable);
		}
	}

	/** Adds the defaults, the reset rules, and the multiplayer rules, which OneConfig mode does not have. */
	private void addSceneRules(final SceneRules rules, final boolean editable) {
		this.addGroup(SettingKeys.SECTION_DEFAULTS, List.of(
				toggle(SettingKeys.SINGLEPLAYER_DEFAULT, rules.singleplayerDefault(), editable,
						this.config::setSingleplayerDefault),
				toggle(SettingKeys.MULTIPLAYER_DEFAULT, rules.multiplayerDefault(), editable,
						this.config::setMultiplayerDefault)));
		this.addGroup(SettingKeys.SECTION_RESET, List.of(
				toggle(SettingKeys.RESET_ON_WORLD_EXIT, rules.resetOnWorldExit(), editable,
						this.config::setResetOnWorldExit),
				toggle(SettingKeys.RESET_ON_GAME_EXIT, rules.resetOnGameExit(), editable,
						this.config::setResetOnGameExit)));

		CycleButton<MultiplayerMode> multiplayerMode = CycleButton.builder(MultiplayerMode::label, rules.multiplayerMode())
				.withValues(List.of(MultiplayerMode.values()))
				.withTooltip(value -> Tooltip.create(
						CommonComponents.joinLines(value.description(), CommonComponents.EMPTY, MULTIPLAYER_WARNING)))
				.create(Component.translatable(SettingKeys.MULTIPLAYER_MODE),
						(button, value) -> this.config.setMultiplayerMode(value));
		multiplayerMode.active = editable;
		Button servers = Button.builder(Component.translatable(SettingKeys.EDIT_SERVERS),
						button -> this.minecraft.gui.setScreen(new ServerListScreen(this, this.config)))
				.tooltip(Tooltip.create(Component.translatable(SettingKeys.EDIT_SERVERS + SettingKeys.DESCRIPTION)))
				.build();
		servers.active = editable;
		this.addGroup(SettingKeys.SECTION_MULTIPLAYER, List.of(multiplayerMode, servers));
	}

	private CycleButton<ConfigMode> createModeButton(final ConfigMode mode) {
		List<ConfigMode> modes = OneConfigBridge.isAvailable()
				? List.of(ConfigMode.values())
				: List.of(ConfigMode.DISABLED, ConfigMode.CUSTOM);
		boolean oneConfigMissing = this.config.storedMode() == ConfigMode.ONECONFIG && !OneConfigBridge.isAvailable();
		return CycleButton.builder(ConfigMode::label, mode)
				.withValues(modes)
				.withTooltip(value -> Tooltip.create(oneConfigMissing && value == mode
						? Component.translatable(SettingKeys.ONECONFIG_MISSING)
						// OneConfig mode has no multiplayer rules, so it warns like the multiplayer mode does.
						: value == ConfigMode.ONECONFIG
								? CommonComponents.joinLines(value.description(), CommonComponents.EMPTY, MULTIPLAYER_WARNING)
								: value.description()))
				.create(Component.translatable(SettingKeys.MODE), (button, value) -> {
					this.config.setMode(value);
					// The settings below change their values and whether they can be edited, so start over.
					this.minecraft.gui.setScreen(new NoClipConfigScreen(this.lastScreen));
				});
	}

	private void addToggles(final String headerKey, final NoClipSettings shown, final boolean editable,
			final Toggle... toggles) {
		List<AbstractWidget> widgets = new ArrayList<>();
		for (Toggle toggle : toggles) {
			widgets.add(toggle(toggle.captionKey(), shown.get(toggle), editable, value -> this.config.set(toggle, value)));
		}
		this.addGroup(headerKey, widgets);
	}

	/** Adds a header and the widgets below it: one full-width widget, or two per row. */
	private void addGroup(final String headerKey, final List<AbstractWidget> widgets) {
		if (this.list == null) {
			return;
		}
		this.list.addHeader(Component.translatable(headerKey));
		if (widgets.size() == 1) {
			this.list.addBig(widgets.getFirst());
		} else {
			this.list.addSmall(widgets);
		}
	}

	private static CycleButton<Boolean> toggle(final String key, final boolean value, final boolean editable,
			final Consumer<Boolean> setter) {
		Tooltip tooltip = Tooltip.create(Component.translatable(key + SettingKeys.DESCRIPTION));
		CycleButton<Boolean> button = CycleButton.onOffBuilder(value)
				.withTooltip(ignored -> tooltip)
				.create(Component.translatable(key), (cycleButton, newValue) -> setter.accept(newValue));
		button.active = editable;
		return button;
	}

	/** Shows the screen on top of whatever screen is open. */
	public static void open(final Minecraft minecraft) {
		minecraft.gui.setScreen(new NoClipConfigScreen(minecraft.gui.screen()));
	}
}
