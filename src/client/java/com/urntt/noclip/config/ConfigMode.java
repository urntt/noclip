package com.urntt.noclip.config;

import com.google.gson.annotations.SerializedName;
import net.minecraft.network.chat.Component;

/**
 * Where the settings come from.
 */
public enum ConfigMode {
	/** Noclip is never active, and the toggle keys change nothing. */
	@SerializedName("disabled")
	DISABLED("disabled"),

	/** The mod's own settings apply. */
	@SerializedName("custom")
	CUSTOM("custom"),

	/** The settings registered with OneConfig apply. Only available while OneConfig is installed. */
	@SerializedName("oneconfig")
	ONECONFIG("oneconfig");

	private final String id;

	ConfigMode(final String id) {
		this.id = id;
	}

	public Component label() {
		return Component.translatable(SettingKeys.MODE + "." + this.id);
	}

	public Component description() {
		return Component.translatable(SettingKeys.MODE + "." + this.id + SettingKeys.DESCRIPTION);
	}
}
