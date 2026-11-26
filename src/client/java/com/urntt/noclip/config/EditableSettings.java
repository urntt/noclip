package com.urntt.noclip.config;

/**
 * Settings that the toggle key bindings and the reset rules can change: the mod's own settings and the ones
 * registered with OneConfig.
 */
public interface EditableSettings extends NoClipSettings {
	/** Changes and saves a toggleable behavior setting, including the main toggle. */
	void set(Toggle toggle, boolean value);
}
