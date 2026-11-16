package com.urntt.noclip.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.NoClipRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's settings, persisted as JSON. Every change is written to disk immediately.
 */
public final class NoClipConfig {
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

	/** Returns the current value of a toggleable behavior setting. */
	public boolean get(final Toggle toggle) {
		Settings s = this.settings;
		return switch (toggle) {
			case ENABLED -> s.enabled;
			case AXIS_X -> s.axes.x;
			case AXIS_Y -> s.axes.y;
			case AXIS_Z -> s.axes.z;
			case BLOCKS -> s.collisions.blocks;
			case FLUIDS -> s.collisions.fluids;
			case WORLD_BORDER -> s.collisions.worldBorder;
			case SLOWING_BLOCKS -> s.collisions.slowingBlocks;
			case ENTITIES -> s.collisions.entities;
			case ONLY_WHILE_FLYING -> s.onlyWhileFlying;
			case CLEAR_VIEW -> s.clearView;
		};
	}

	/** Changes and saves a toggleable behavior setting. */
	public void set(final Toggle toggle, final boolean value) {
		Settings s = this.settings;
		switch (toggle) {
			case ENABLED -> s.enabled = value;
			case AXIS_X -> s.axes.x = value;
			case AXIS_Y -> s.axes.y = value;
			case AXIS_Z -> s.axes.z = value;
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

	/** The current state of noclip, as toggled by the main key binding. */
	public boolean isEnabled() {
		return this.get(Toggle.ENABLED);
	}

	public void setEnabled(final boolean enabled) {
		this.set(Toggle.ENABLED, enabled);
	}

	/** Whether noclip only applies while the player is flying. */
	public boolean onlyWhileFlying() {
		return this.get(Toggle.ONLY_WHILE_FLYING);
	}

	/**
	 * Returns what noclip ignores while it is active.
	 */
	public NoClipRules rules() {
		NoClipRules rules = this.rules;
		if (rules == null) {
			Set<CollisionType> types = EnumSet.noneOf(CollisionType.class);
			for (CollisionType type : CollisionType.values()) {
				if (this.get(Toggle.forType(type))) {
					types.add(type);
				}
			}
			Set<Direction.Axis> axes = EnumSet.noneOf(Direction.Axis.class);
			for (Direction.Axis axis : Direction.Axis.values()) {
				if (this.get(Toggle.forAxis(axis))) {
					axes.add(axis);
				}
			}
			rules = new NoClipRules(types, axes, this.get(Toggle.CLEAR_VIEW));
			this.rules = rules;
		}
		return rules;
	}

	/** The state a reset restores in singleplayer worlds. */
	public boolean singleplayerDefault() {
		return this.settings.singleplayerDefault;
	}

	public void setSingleplayerDefault(final boolean singleplayerDefault) {
		this.settings.singleplayerDefault = singleplayerDefault;
		this.save();
	}

	/** The state a reset restores on multiplayer servers that the multiplayer mode allows. */
	public boolean multiplayerDefault() {
		return this.settings.multiplayerDefault;
	}

	public void setMultiplayerDefault(final boolean multiplayerDefault) {
		this.settings.multiplayerDefault = multiplayerDefault;
		this.save();
	}

	/** Whether every world starts from its default state. */
	public boolean resetOnWorldExit() {
		return this.settings.resetOnWorldExit;
	}

	public void setResetOnWorldExit(final boolean resetOnWorldExit) {
		this.settings.resetOnWorldExit = resetOnWorldExit;
		this.save();
	}

	/** Whether the first world after starting the game starts from its default state. */
	public boolean resetOnGameExit() {
		return this.settings.resetOnGameExit;
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
		private boolean enabled = false;
		private Axes axes = new Axes();
		private Collisions collisions = new Collisions();
		private boolean onlyWhileFlying = false;
		private boolean clearView = true;
		private boolean singleplayerDefault = false;
		private boolean multiplayerDefault = false;
		private boolean resetOnWorldExit = false;
		private boolean resetOnGameExit = false;
		private MultiplayerMode multiplayerMode = MultiplayerMode.DISABLED;
		private List<String> servers = new ArrayList<>();

		/**
		 * Replaces values Gson could not map (unknown mode names, missing objects) and tidies the server list.
		 */
		private void normalize() {
			if (this.axes == null) {
				this.axes = new Axes();
			}
			if (this.collisions == null) {
				this.collisions = new Collisions();
			}
			if (this.multiplayerMode == null) {
				this.multiplayerMode = MultiplayerMode.DISABLED;
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

	/** The axes along which shape-based collision types are ignored. */
	private static final class Axes {
		private boolean x = true;
		private boolean y = false;
		private boolean z = true;
	}

	/** The collision types noclip passes through. */
	private static final class Collisions {
		private boolean blocks = true;
		private boolean fluids = true;
		private boolean worldBorder = true;
		private boolean slowingBlocks = true;
		private boolean entities = true;
	}
}
