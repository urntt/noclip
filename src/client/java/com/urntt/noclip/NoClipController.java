package com.urntt.noclip;

import com.urntt.noclip.config.EditableSettings;
import com.urntt.noclip.config.NoClipSettings;
import com.urntt.noclip.config.SceneRules;
import com.urntt.noclip.config.Toggle;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import org.jspecify.annotations.Nullable;

/**
 * Decides whether noclip is active right now, from the settings of the configuration mode, the current {@link Scene},
 * whether the player is flying, and whether they hold jump or sneak. It is the only place that combines the toggle
 * state, the scene rules (defaults, reset rules, and multiplayer rules), "only while flying", and the jump and sneak
 * settings. Settings without scene rules, which OneConfig's are, allow every scene and reset nothing.
 */
public final class NoClipController {
	/** Outcome of {@link #toggle(Toggle)}. */
	public enum ToggleResult {
		/** The setting is now on. */
		ON,
		/** The setting is now off. */
		OFF,
		/** The current server is not allowed, so the main toggle was left unchanged. */
		BLOCKED,
		/** Disabled mode is selected, so nothing was changed. */
		UNAVAILABLE
	}

	private final Supplier<NoClipSettings> settings;
	private final Supplier<Optional<EditableSettings>> editable;
	private Scene scene = Scene.NONE;
	private boolean allowedWorldJoinedSinceStart = false;

	/**
	 * @param settings the settings that apply right now
	 * @param editable the settings the toggle keys and the reset rules change right now, or none if nothing may
	 *                 change them
	 */
	public NoClipController(final Supplier<NoClipSettings> settings, final Supplier<Optional<EditableSettings>> editable) {
		this.settings = settings;
		this.editable = editable;
	}

	public Scene scene() {
		return this.scene;
	}

	/**
	 * Called when the player joins a world. On an allowed scene, restores the scene's default state if a reset rule
	 * applies: always with "reset on world exit", and for the first allowed world since the game started with
	 * "reset on game exit". Applying the reset on join instead of on exit lets it pick the next scene's default and
	 * also works after a crash. Disabled mode and settings without scene rules change nothing.
	 */
	public void onJoin(final Scene scene) {
		this.scene = scene;
		if (!this.isAllowed()) {
			return;
		}

		boolean firstWorld = !this.allowedWorldJoinedSinceStart;
		this.allowedWorldJoinedSinceStart = true;
		this.editable.get().ifPresent(settings -> {
			SceneRules rules = settings.sceneRules();
			if (rules != null && (rules.resetOnWorldExit() || (rules.resetOnGameExit() && firstWorld))) {
				settings.set(Toggle.ENABLED, scene instanceof Scene.Singleplayer
						? rules.singleplayerDefault()
						: rules.multiplayerDefault());
			}
		});
	}

	public void onDisconnect() {
		this.scene = Scene.NONE;
	}

	/**
	 * Returns whether noclip is active for a player who is or is not {@code flying}.
	 */
	public boolean isActive(final boolean flying) {
		NoClipSettings settings = this.settings.get();
		return settings.get(Toggle.ENABLED) && this.isAllowed(settings)
				&& (flying || !settings.get(Toggle.ONLY_WHILE_FLYING));
	}

	/**
	 * Returns what noclip ignores for {@code entity} right now, or {@code null} if noclip does not apply to it. Only
	 * the local player is ever affected. Holding the jump or sneak key adds the upward or downward direction when
	 * the corresponding setting is on.
	 */
	public @Nullable NoClipRules rulesFor(final @Nullable Entity entity) {
		if (!(entity instanceof LocalPlayer player) || !this.isActive(player.getAbilities().flying)) {
			return null;
		}
		NoClipSettings settings = this.settings.get();
		NoClipRules rules = settings.rules();
		Input input = player.input.keyPresses;
		boolean up = input.jump() && settings.get(Toggle.JUMP_UP);
		boolean down = input.shift() && settings.get(Toggle.SNEAK_DOWN);
		if (!up && !down) {
			return rules;
		}
		Set<Direction> held = EnumSet.noneOf(Direction.class);
		if (up) {
			held.add(Direction.UP);
		}
		if (down) {
			held.add(Direction.DOWN);
		}
		return rules.withDirections(held);
	}

	/**
	 * Returns whether the multiplayer rules allow noclip in the current scene.
	 */
	public boolean isAllowed() {
		return this.isAllowed(this.settings.get());
	}

	private boolean isAllowed(final NoClipSettings settings) {
		SceneRules rules = settings.sceneRules();
		return switch (this.scene) {
			case Scene.None none -> false;
			case Scene.Singleplayer singleplayer -> true;
			case Scene.Multiplayer multiplayer -> rules == null || switch (rules.multiplayerMode()) {
				case DISABLED -> false;
				case WHITELIST -> isListed(rules.servers(), multiplayer.address());
				case BLACKLIST -> !isListed(rules.servers(), multiplayer.address());
			};
		};
	}

	/**
	 * Flips and saves {@code toggle} in the settings of the configuration mode. Disabled mode changes nothing, and
	 * the main toggle stays unchanged on a server the multiplayer rules rule out.
	 */
	public ToggleResult toggle(final Toggle toggle) {
		Optional<EditableSettings> editable = this.editable.get();
		if (editable.isEmpty()) {
			return ToggleResult.UNAVAILABLE;
		}
		if (toggle == Toggle.ENABLED && this.scene instanceof Scene.Multiplayer && !this.isAllowed()) {
			return ToggleResult.BLOCKED;
		}
		EditableSettings settings = editable.get();
		boolean value = !settings.get(toggle);
		settings.set(toggle, value);
		return value ? ToggleResult.ON : ToggleResult.OFF;
	}

	private static boolean isListed(final List<String> servers, final @Nullable String address) {
		return address != null && servers.stream().anyMatch(entry -> ServerAddresses.matches(entry, address));
	}
}
