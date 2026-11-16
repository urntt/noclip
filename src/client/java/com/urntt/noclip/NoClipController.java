package com.urntt.noclip;

import com.urntt.noclip.config.NoClipConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Decides whether noclip is active right now, from the configuration, the current {@link Scene}, and whether the
 * player is flying. It is the only place that combines the toggle state, the defaults, the reset rules, the
 * multiplayer rules, and "only while flying".
 */
public final class NoClipController {
	/** Outcome of {@link #toggle()}. */
	public enum ToggleResult {
		ENABLED,
		DISABLED,
		/** The current server is not allowed, so the toggle state was left unchanged. */
		BLOCKED
	}

	private final NoClipConfig config;
	private Scene scene = Scene.NONE;
	private boolean allowedWorldJoinedSinceStart = false;

	public NoClipController(final NoClipConfig config) {
		this.config = config;
	}

	public Scene scene() {
		return this.scene;
	}

	/**
	 * Called when the player joins a world. On an allowed scene, restores the scene's default state if a reset rule
	 * applies: always with "reset on world exit", and for the first allowed world since the game started with
	 * "reset on game exit". Applying the reset on join instead of on exit lets it pick the next scene's default and
	 * also works after a crash.
	 */
	public void onJoin(final Scene scene) {
		this.scene = scene;
		if (!this.isAllowed()) {
			return;
		}

		boolean firstWorld = !this.allowedWorldJoinedSinceStart;
		this.allowedWorldJoinedSinceStart = true;
		if (this.config.resetOnWorldExit() || (this.config.resetOnGameExit() && firstWorld)) {
			this.config.setEnabled(this.defaultFor(scene));
		}
	}

	public void onDisconnect() {
		this.scene = Scene.NONE;
	}

	/**
	 * Returns whether noclip is active for a player who is or is not {@code flying}.
	 */
	public boolean isActive(final boolean flying) {
		return this.config.isEnabled() && this.isAllowed() && (flying || !this.config.onlyWhileFlying());
	}

	/**
	 * Returns what noclip ignores for {@code entity} right now, or {@code null} if noclip does not apply to it. Only
	 * the local player is ever affected.
	 */
	public @Nullable NoClipRules rulesFor(final @Nullable Entity entity) {
		if (entity instanceof LocalPlayer player && this.isActive(player.getAbilities().flying)) {
			return this.config.rules();
		}
		return null;
	}

	/**
	 * Returns whether the multiplayer rules allow noclip in the current scene.
	 */
	public boolean isAllowed() {
		return switch (this.scene) {
			case Scene.None none -> false;
			case Scene.Singleplayer singleplayer -> true;
			case Scene.Multiplayer multiplayer -> switch (this.config.multiplayerMode()) {
				case DISABLED -> false;
				case WHITELIST -> this.isListed(multiplayer.address());
				case BLACKLIST -> !this.isListed(multiplayer.address());
			};
		};
	}

	/**
	 * Flips and saves the main toggle state, unless the current server is not allowed.
	 */
	public ToggleResult toggle() {
		if (this.scene instanceof Scene.Multiplayer && !this.isAllowed()) {
			return ToggleResult.BLOCKED;
		}
		boolean enabled = !this.config.isEnabled();
		this.config.setEnabled(enabled);
		return enabled ? ToggleResult.ENABLED : ToggleResult.DISABLED;
	}

	private boolean defaultFor(final Scene scene) {
		return scene instanceof Scene.Singleplayer ? this.config.singleplayerDefault() : this.config.multiplayerDefault();
	}

	private boolean isListed(final @Nullable String address) {
		return address != null && this.config.servers().stream().anyMatch(entry -> ServerAddresses.matches(entry, address));
	}
}
