package com.urntt.noclip.config;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/**
 * Every boolean behavior setting that has its own toggle key binding. This is the single source for the settings'
 * key binding names, captions, and descriptions; {@link NoClipSettings#get(Toggle)} and
 * {@link EditableSettings#set(Toggle, boolean)} read and write them.
 */
public enum Toggle {
	ENABLED(SettingKeys.ENABLED),
	AXIS_X(SettingKeys.AXIS_X),
	AXIS_Y(SettingKeys.AXIS_Y),
	AXIS_Z(SettingKeys.AXIS_Z),
	JUMP_UP(SettingKeys.JUMP_UP),
	SNEAK_DOWN(SettingKeys.SNEAK_DOWN),
	BLOCKS(SettingKeys.BLOCKS),
	FLUIDS(SettingKeys.FLUIDS),
	WORLD_BORDER(SettingKeys.WORLD_BORDER),
	SLOWING_BLOCKS(SettingKeys.SLOWING_BLOCKS),
	ENTITIES(SettingKeys.ENTITIES),
	ONLY_WHILE_FLYING(SettingKeys.ONLY_WHILE_FLYING),
	CLEAR_VIEW(SettingKeys.CLEAR_VIEW);

	private final String captionKey;

	Toggle(final String captionKey) {
		this.captionKey = captionKey;
	}

	/** Returns the toggle for whether noclip applies along {@code axis}. */
	public static Toggle forAxis(final Direction.Axis axis) {
		return switch (axis) {
			case X -> AXIS_X;
			case Y -> AXIS_Y;
			case Z -> AXIS_Z;
		};
	}

	/** Returns the toggle for whether noclip passes through {@code type}. */
	public static Toggle forType(final CollisionType type) {
		return switch (type) {
			case BLOCKS -> BLOCKS;
			case FLUIDS -> FLUIDS;
			case WORLD_BORDER -> WORLD_BORDER;
			case SLOWING_BLOCKS -> SLOWING_BLOCKS;
			case ENTITIES -> ENTITIES;
		};
	}

	/** Translation key of the setting's caption, used on the configuration screen, the action bar, and OneConfig. */
	public String captionKey() {
		return this.captionKey;
	}

	public Component caption() {
		return Component.translatable(this.captionKey);
	}

	public Component description() {
		return Component.translatable(this.captionKey + SettingKeys.DESCRIPTION);
	}

	/** Translation key and registered name of the setting's toggle key binding, from the last part of its caption key. */
	public String keyMappingName() {
		return "key.noclip.toggle." + this.captionKey.substring(this.captionKey.lastIndexOf('.') + 1);
	}
}
