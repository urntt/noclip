package com.urntt.noclip.config;

import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * An immutable set of settings.
 *
 * @param enabled    the toggles that are on
 * @param sceneRules where noclip may be active and what a reset restores, or {@code null} for everywhere and nothing
 */
public record NoClipValues(Set<Toggle> enabled, @Nullable SceneRules sceneRules) implements NoClipSettings {
	/** The toggles that are on by default: the X and Z axes, every collision type, and the clear view. */
	private static final Set<Toggle> DEFAULT_TOGGLES = EnumSet.of(Toggle.AXIS_X, Toggle.AXIS_Z, Toggle.BLOCKS,
			Toggle.FLUIDS, Toggle.WORLD_BORDER, Toggle.SLOWING_BLOCKS, Toggle.ENTITIES, Toggle.CLEAR_VIEW);

	/** The defaults of the mod's own settings: noclip is off, and every server is ruled out. */
	public static final NoClipValues DEFAULTS = new NoClipValues(DEFAULT_TOGGLES, SceneRules.DEFAULTS);

	/**
	 * The defaults of the OneConfig settings: noclip is off until the player turns it on with the toggle on the mod's
	 * OneConfig card, and then applies in singleplayer and on every server.
	 */
	public static final NoClipValues ONECONFIG_DEFAULTS = new NoClipValues(DEFAULT_TOGGLES, null);

	/** Applied in disabled mode: noclip is off, and nothing would turn it on. */
	public static final NoClipValues DISABLED = new NoClipValues(EnumSet.noneOf(Toggle.class), SceneRules.DEFAULTS);

	public NoClipValues {
		enabled = Set.copyOf(enabled);
	}

	@Override
	public boolean get(final Toggle toggle) {
		return this.enabled.contains(toggle);
	}

	/** Copies the current values of {@code settings}. */
	public static NoClipValues copyOf(final NoClipSettings settings) {
		Set<Toggle> enabled = EnumSet.noneOf(Toggle.class);
		for (Toggle toggle : Toggle.values()) {
			if (settings.get(toggle)) {
				enabled.add(toggle);
			}
		}
		return new NoClipValues(enabled, settings.sceneRules());
	}
}
