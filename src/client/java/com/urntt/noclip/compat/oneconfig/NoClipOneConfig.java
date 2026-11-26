package com.urntt.noclip.compat.oneconfig;

import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.config.EditableSettings;
import com.urntt.noclip.config.NoClipValues;
import com.urntt.noclip.config.SceneRules;
import com.urntt.noclip.config.SettingKeys;
import com.urntt.noclip.config.Toggle;
import org.jspecify.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.MasterToggle;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

/**
 * The settings OneConfig manages in OneConfig mode: the main toggle on the mod's card, off by default, and the
 * behavior settings, with the same defaults as the mod's own. There are no defaults, reset rules, or multiplayer
 * rules; OneConfig's profiles take their place, so once the toggle is on, noclip applies in singleplayer and on every
 * server. Titles, descriptions, and subcategories are the mod's translation keys, which OneConfig translates.
 */
public final class NoClipOneConfig extends Config implements EditableSettings {
	/**
	 * OneConfig keeps its files in the same config directory as the mod's own {@code noclip.json} and watches them
	 * for changes, so its file needs a name of its own.
	 */
	private static final String FILE_NAME = NoClipClient.MOD_ID + "-oneconfig.json";

	/** The mod icon, as named in {@code fabric.mod.json}, as a resource path for OneConfig's mod card. */
	private static final String ICON_PATH = "/assets/" + NoClipClient.MOD_ID + "/icon.png";

	/** The toggle on the mod's OneConfig card, which is the main toggle. */
	@MasterToggle
	public boolean enabled = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.ENABLED);

	@Switch(
			title = SettingKeys.AXIS_X,
			description = SettingKeys.AXIS_X + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_AXES)
	public boolean axisX = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.AXIS_X);

	@Switch(
			title = SettingKeys.AXIS_Y,
			description = SettingKeys.AXIS_Y + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_AXES)
	public boolean axisY = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.AXIS_Y);

	@Switch(
			title = SettingKeys.AXIS_Z,
			description = SettingKeys.AXIS_Z + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_AXES)
	public boolean axisZ = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.AXIS_Z);

	@Switch(
			title = SettingKeys.JUMP_UP,
			description = SettingKeys.JUMP_UP + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_AXES)
	public boolean jumpUp = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.JUMP_UP);

	@Switch(
			title = SettingKeys.SNEAK_DOWN,
			description = SettingKeys.SNEAK_DOWN + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_AXES)
	public boolean sneakDown = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.SNEAK_DOWN);

	@Switch(
			title = SettingKeys.BLOCKS,
			description = SettingKeys.BLOCKS + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_COLLISIONS)
	public boolean blocks = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.BLOCKS);

	@Switch(
			title = SettingKeys.FLUIDS,
			description = SettingKeys.FLUIDS + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_COLLISIONS)
	public boolean fluids = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.FLUIDS);

	@Switch(
			title = SettingKeys.WORLD_BORDER,
			description = SettingKeys.WORLD_BORDER + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_COLLISIONS)
	public boolean worldBorder = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.WORLD_BORDER);

	@Switch(
			title = SettingKeys.SLOWING_BLOCKS,
			description = SettingKeys.SLOWING_BLOCKS + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_COLLISIONS)
	public boolean slowingBlocks = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.SLOWING_BLOCKS);

	@Switch(
			title = SettingKeys.ENTITIES,
			description = SettingKeys.ENTITIES + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_COLLISIONS)
	public boolean entities = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.ENTITIES);

	@Switch(
			title = SettingKeys.ONLY_WHILE_FLYING,
			description = SettingKeys.ONLY_WHILE_FLYING + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_OPTIONS)
	public boolean onlyWhileFlying = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.ONLY_WHILE_FLYING);

	@Switch(
			title = SettingKeys.CLEAR_VIEW,
			description = SettingKeys.CLEAR_VIEW + SettingKeys.DESCRIPTION,
			subcategory = SettingKeys.SECTION_OPTIONS)
	public boolean clearView = NoClipValues.ONECONFIG_DEFAULTS.get(Toggle.CLEAR_VIEW);

	public NoClipOneConfig() {
		super(FILE_NAME, ICON_PATH, NoClipClient.MOD_ID, Category.UTILITY);
	}

	@Override
	public boolean get(final Toggle toggle) {
		return switch (toggle) {
			case ENABLED -> this.enabled;
			case AXIS_X -> this.axisX;
			case AXIS_Y -> this.axisY;
			case AXIS_Z -> this.axisZ;
			case JUMP_UP -> this.jumpUp;
			case SNEAK_DOWN -> this.sneakDown;
			case BLOCKS -> this.blocks;
			case FLUIDS -> this.fluids;
			case WORLD_BORDER -> this.worldBorder;
			case SLOWING_BLOCKS -> this.slowingBlocks;
			case ENTITIES -> this.entities;
			case ONLY_WHILE_FLYING -> this.onlyWhileFlying;
			case CLEAR_VIEW -> this.clearView;
		};
	}

	@Override
	public void set(final Toggle toggle, final boolean value) {
		switch (toggle) {
			case ENABLED -> this.enabled = value;
			case AXIS_X -> this.axisX = value;
			case AXIS_Y -> this.axisY = value;
			case AXIS_Z -> this.axisZ = value;
			case JUMP_UP -> this.jumpUp = value;
			case SNEAK_DOWN -> this.sneakDown = value;
			case BLOCKS -> this.blocks = value;
			case FLUIDS -> this.fluids = value;
			case WORLD_BORDER -> this.worldBorder = value;
			case SLOWING_BLOCKS -> this.slowingBlocks = value;
			case ENTITIES -> this.entities = value;
			case ONLY_WHILE_FLYING -> this.onlyWhileFlying = value;
			case CLEAR_VIEW -> this.clearView = value;
		}
		this.save();
	}

	/** OneConfig has no scene rules; its profiles take their place. */
	@Override
	public @Nullable SceneRules sceneRules() {
		return NoClipValues.ONECONFIG_DEFAULTS.sceneRules();
	}
}
