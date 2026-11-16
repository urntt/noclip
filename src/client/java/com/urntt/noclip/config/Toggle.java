package com.urntt.noclip.config;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/**
 * Every boolean behavior setting that has its own toggle key binding. This is the single source for the settings'
 * key binding names, captions, and tooltips; {@link NoClipConfig#get(Toggle)} and {@link NoClipConfig#set(Toggle,
 * boolean)} read and write them.
 */
public enum Toggle {
	ENABLED("enabled"),
	AXIS_X("axis_x"),
	AXIS_Y("axis_y"),
	AXIS_Z("axis_z"),
	JUMP_UP("jump_up"),
	SNEAK_DOWN("sneak_down"),
	BLOCKS("blocks"),
	FLUIDS("fluids"),
	WORLD_BORDER("world_border"),
	SLOWING_BLOCKS("slowing_blocks"),
	ENTITIES("entities"),
	ONLY_WHILE_FLYING("only_while_flying"),
	CLEAR_VIEW("clear_view");

	private final String id;

	Toggle(final String id) {
		this.id = id;
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

	/** Translation key of the setting's caption, used on the configuration screen and the action bar. */
	public String captionKey() {
		return "options.noclip." + this.id;
	}

	public Component caption() {
		return Component.translatable(this.captionKey());
	}

	public Component tooltip() {
		return Component.translatable(this.captionKey() + ".tooltip");
	}

	/** Translation key and registered name of the setting's toggle key binding. */
	public String keyMappingName() {
		return "key.noclip.toggle." + this.id;
	}
}
