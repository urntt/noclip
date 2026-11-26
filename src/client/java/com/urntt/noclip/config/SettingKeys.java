package com.urntt.noclip.config;

/**
 * Translation keys of the settings, shared by the configuration screen, the key bindings, and the OneConfig
 * integration. They are compile-time constants so that OneConfig's option annotations can use them.
 */
public final class SettingKeys {
	private static final String PREFIX = "options.noclip.";

	public static final String TITLE = PREFIX + "title";
	public static final String MODE = PREFIX + "mode";
	public static final String ONECONFIG_MISSING = PREFIX + "mode.oneconfig.missing";
	public static final String OPEN_ONECONFIG = PREFIX + "open_oneconfig";

	public static final String SECTION_CURRENT = PREFIX + "section.current";
	public static final String SECTION_AXES = PREFIX + "section.axes";
	public static final String SECTION_COLLISIONS = PREFIX + "section.collisions";
	public static final String SECTION_OPTIONS = PREFIX + "section.options";
	public static final String SECTION_DEFAULTS = PREFIX + "section.defaults";
	public static final String SECTION_RESET = PREFIX + "section.reset";
	public static final String SECTION_MULTIPLAYER = PREFIX + "section.multiplayer";

	public static final String ENABLED = PREFIX + "enabled";
	public static final String AXIS_X = PREFIX + "axis_x";
	public static final String AXIS_Y = PREFIX + "axis_y";
	public static final String AXIS_Z = PREFIX + "axis_z";
	public static final String JUMP_UP = PREFIX + "jump_up";
	public static final String SNEAK_DOWN = PREFIX + "sneak_down";
	public static final String BLOCKS = PREFIX + "blocks";
	public static final String FLUIDS = PREFIX + "fluids";
	public static final String WORLD_BORDER = PREFIX + "world_border";
	public static final String SLOWING_BLOCKS = PREFIX + "slowing_blocks";
	public static final String ENTITIES = PREFIX + "entities";
	public static final String ONLY_WHILE_FLYING = PREFIX + "only_while_flying";
	public static final String CLEAR_VIEW = PREFIX + "clear_view";

	public static final String SINGLEPLAYER_DEFAULT = PREFIX + "singleplayer_default";
	public static final String MULTIPLAYER_DEFAULT = PREFIX + "multiplayer_default";
	public static final String RESET_ON_WORLD_EXIT = PREFIX + "reset_on_world_exit";
	public static final String RESET_ON_GAME_EXIT = PREFIX + "reset_on_game_exit";

	public static final String MULTIPLAYER_MODE = PREFIX + "multiplayer_mode";
	public static final String MULTIPLAYER_WARNING = MULTIPLAYER_MODE + ".warning";
	public static final String EDIT_SERVERS = PREFIX + "edit_servers";

	/** Suffix of the key that describes a setting, shown as its tooltip. */
	public static final String DESCRIPTION = ".description";

	private SettingKeys() {
	}
}
