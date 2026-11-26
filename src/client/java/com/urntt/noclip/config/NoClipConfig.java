package com.urntt.noclip.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.NoClipRules;
import com.urntt.noclip.compat.OneConfigBridge;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's own settings and the configuration mode, persisted as JSON. Every change is written to disk immediately.
 */
public final class NoClipConfig implements EditableSettings {
	private static final Logger LOGGER = LoggerFactory.getLogger(NoClipClient.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path path;
	private final Settings settings;
	/** Cached snapshot of the behavior settings, rebuilt after each change. */
	private @Nullable NoClipRules rules;

	private NoClipConfig(final Path path, final Settings settings) {
		this.path = path;
		this.settings = settings;
	}

	/**
	 * Returns the location of the config file in the Fabric config directory.
	 */
	public static Path defaultPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(NoClipClient.MOD_ID + ".json");
	}

	/**
	 * Loads the configuration from {@code path}. A missing file is created with the defaults. A readable file is
	 * rewritten with any settings it lacks. An unreadable file falls back to the defaults and is left untouched
	 * until the next change is saved.
	 */
	public static NoClipConfig load(final Path path) {
		if (Files.notExists(path)) {
			NoClipConfig config = new NoClipConfig(path, new Settings());
			config.save();
			return config;
		}

		Settings settings;
		try {
			settings = GSON.fromJson(Files.readString(path), Settings.class);
		} catch (IOException | JsonParseException e) {
			LOGGER.warn("Failed to read config file {}, using defaults", path, e);
			return new NoClipConfig(path, new Settings());
		}

		if (settings == null) {
			settings = new Settings();
		}
		settings.normalize();
		NoClipConfig config = new NoClipConfig(path, settings);
		config.save();
		return config;
	}

	/**
	 * The mode the player chose, which may name OneConfig even while OneConfig is not installed, or {@code null} until
	 * the player chooses one.
	 */
	public @Nullable ConfigMode storedMode() {
		return this.settings.mode;
	}

	/**
	 * The mode that applies. Until the player chooses one, OneConfig manages the mod while it is available. OneConfig
	 * mode falls back to the custom settings while OneConfig is not available.
	 */
	public ConfigMode effectiveMode() {
		boolean oneConfig = OneConfigBridge.isAvailable();
		ConfigMode mode = this.settings.mode;
		if (mode == null) {
			return oneConfig ? ConfigMode.ONECONFIG : ConfigMode.CUSTOM;
		}
		return mode == ConfigMode.ONECONFIG && !oneConfig ? ConfigMode.CUSTOM : mode;
	}

	public void setMode(final ConfigMode mode) {
		this.settings.mode = mode;
		this.save();
	}

	/** The settings that apply in the {@linkplain #effectiveMode() effective mode}. */
	public NoClipSettings effective() {
		return switch (this.effectiveMode()) {
			case DISABLED -> NoClipValues.DISABLED;
			case CUSTOM -> this;
			case ONECONFIG -> OneConfigBridge.settings().orElse(this);
		};
	}

	/**
	 * The settings the toggle key bindings and the reset rules change in the effective mode: none in disabled mode,
	 * OneConfig's in OneConfig mode, and the mod's own otherwise.
	 */
	public Optional<EditableSettings> editable() {
		return switch (this.effectiveMode()) {
			case DISABLED -> Optional.empty();
			case CUSTOM -> Optional.of(this);
			case ONECONFIG -> Optional.of(OneConfigBridge.settings().orElse(this));
		};
	}

	@Override
	public boolean get(final Toggle toggle) {
		Settings s = this.settings;
		return switch (toggle) {
			case ENABLED -> s.enabled;
			case AXIS_X -> s.axes.x;
			case AXIS_Y -> s.axes.y;
			case AXIS_Z -> s.axes.z;
			case JUMP_UP -> s.axes.jumpUp;
			case SNEAK_DOWN -> s.axes.sneakDown;
			case BLOCKS -> s.collisions.blocks;
			case FLUIDS -> s.collisions.fluids;
			case WORLD_BORDER -> s.collisions.worldBorder;
			case SLOWING_BLOCKS -> s.collisions.slowingBlocks;
			case ENTITIES -> s.collisions.entities;
			case ONLY_WHILE_FLYING -> s.onlyWhileFlying;
			case CLEAR_VIEW -> s.clearView;
		};
	}

	@Override
	public void set(final Toggle toggle, final boolean value) {
		Settings s = this.settings;
		switch (toggle) {
			case ENABLED -> s.enabled = value;
			case AXIS_X -> s.axes.x = value;
			case AXIS_Y -> s.axes.y = value;
			case AXIS_Z -> s.axes.z = value;
			case JUMP_UP -> s.axes.jumpUp = value;
			case SNEAK_DOWN -> s.axes.sneakDown = value;
			case BLOCKS -> s.collisions.blocks = value;
			case FLUIDS -> s.collisions.fluids = value;
			case WORLD_BORDER -> s.collisions.worldBorder = value;
			case SLOWING_BLOCKS -> s.collisions.slowingBlocks = value;
			case ENTITIES -> s.collisions.entities = value;
			case ONLY_WHILE_FLYING -> s.onlyWhileFlying = value;
			case CLEAR_VIEW -> s.clearView = value;
		}
		this.save();
	}

	/** Cached, because the collision code asks for the rules several times per tick. */
	@Override
	public NoClipRules rules() {
		NoClipRules rules = this.rules;
		if (rules == null) {
			rules = EditableSettings.super.rules();
			this.rules = rules;
		}
		return rules;
	}

	@Override
	public SceneRules sceneRules() {
		Settings s = this.settings;
		return new SceneRules(s.singleplayerDefault, s.multiplayerDefault, s.resetOnWorldExit, s.resetOnGameExit,
				s.multiplayerMode, s.servers);
	}

	public void setSingleplayerDefault(final boolean singleplayerDefault) {
		this.settings.singleplayerDefault = singleplayerDefault;
		this.save();
	}

	public void setMultiplayerDefault(final boolean multiplayerDefault) {
		this.settings.multiplayerDefault = multiplayerDefault;
		this.save();
	}

	public void setResetOnWorldExit(final boolean resetOnWorldExit) {
		this.settings.resetOnWorldExit = resetOnWorldExit;
		this.save();
	}

	public void setResetOnGameExit(final boolean resetOnGameExit) {
		this.settings.resetOnGameExit = resetOnGameExit;
		this.save();
	}

	public MultiplayerMode multiplayerMode() {
		return this.settings.multiplayerMode;
	}

	public void setMultiplayerMode(final MultiplayerMode multiplayerMode) {
		this.settings.multiplayerMode = multiplayerMode;
		this.save();
	}

	/** The server list used by the whitelist and blacklist modes. */
	public List<String> servers() {
		return List.copyOf(this.settings.servers);
	}

	public void setServers(final List<String> servers) {
		this.settings.servers = new ArrayList<>(servers);
		this.settings.normalize();
		this.save();
	}

	private void save() {
		this.rules = null;
		try {
			Files.createDirectories(this.path.getParent());
			Files.writeString(this.path, GSON.toJson(this.settings));
		} catch (IOException e) {
			LOGGER.error("Failed to write config file {}", this.path, e);
		}
	}

	/**
	 * The serialized form. Field initializers are the defaults, which also apply to keys missing from the file.
	 */
	private static final class Settings {
		/** The mode the player chose, or {@code null} until then; Gson leaves it out of the file while it is. */
		private @Nullable ConfigMode mode;
		private boolean enabled = NoClipValues.DEFAULTS.get(Toggle.ENABLED);
		private Axes axes = new Axes();
		private Collisions collisions = new Collisions();
		private boolean onlyWhileFlying = NoClipValues.DEFAULTS.get(Toggle.ONLY_WHILE_FLYING);
		private boolean clearView = NoClipValues.DEFAULTS.get(Toggle.CLEAR_VIEW);
		private boolean singleplayerDefault = SceneRules.DEFAULTS.singleplayerDefault();
		private boolean multiplayerDefault = SceneRules.DEFAULTS.multiplayerDefault();
		private boolean resetOnWorldExit = SceneRules.DEFAULTS.resetOnWorldExit();
		private boolean resetOnGameExit = SceneRules.DEFAULTS.resetOnGameExit();
		private MultiplayerMode multiplayerMode = SceneRules.DEFAULTS.multiplayerMode();
		private List<String> servers = new ArrayList<>(SceneRules.DEFAULTS.servers());

		/**
		 * Replaces values Gson could not map (unknown multiplayer mode names, missing objects) and tidies the server
		 * list. An unknown or missing configuration mode stays {@code null}, which counts as no choice.
		 */
		private void normalize() {
			if (this.axes == null) {
				this.axes = new Axes();
			}
			if (this.collisions == null) {
				this.collisions = new Collisions();
			}
			if (this.multiplayerMode == null) {
				this.multiplayerMode = SceneRules.DEFAULTS.multiplayerMode();
			}
			List<String> entries = new ArrayList<>();
			if (this.servers != null) {
				for (String entry : this.servers) {
					if (entry != null && !entry.isBlank()) {
						entries.add(entry.trim());
					}
				}
			}
			this.servers = entries;
		}
	}

	/**
	 * The axes along which shape-based collision types are ignored, and whether holding jump or sneak also ignores
	 * them for upward or downward movement.
	 */
	private static final class Axes {
		private boolean x = NoClipValues.DEFAULTS.get(Toggle.AXIS_X);
		private boolean y = NoClipValues.DEFAULTS.get(Toggle.AXIS_Y);
		private boolean z = NoClipValues.DEFAULTS.get(Toggle.AXIS_Z);
		private boolean jumpUp = NoClipValues.DEFAULTS.get(Toggle.JUMP_UP);
		private boolean sneakDown = NoClipValues.DEFAULTS.get(Toggle.SNEAK_DOWN);
	}

	/** The collision types noclip passes through. */
	private static final class Collisions {
		private boolean blocks = NoClipValues.DEFAULTS.get(Toggle.BLOCKS);
		private boolean fluids = NoClipValues.DEFAULTS.get(Toggle.FLUIDS);
		private boolean worldBorder = NoClipValues.DEFAULTS.get(Toggle.WORLD_BORDER);
		private boolean slowingBlocks = NoClipValues.DEFAULTS.get(Toggle.SLOWING_BLOCKS);
		private boolean entities = NoClipValues.DEFAULTS.get(Toggle.ENTITIES);
	}
}
