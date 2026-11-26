package com.urntt.noclip.config;

import com.urntt.noclip.NoClipRules;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The values noclip reads. Implemented by the mod's own configuration, by the OneConfig integration, and by the
 * constant {@link NoClipValues} sets.
 */
public interface NoClipSettings {
	/** The current value of a toggleable behavior setting, including the main toggle. */
	boolean get(Toggle toggle);

	/**
	 * Where noclip may be active and what a reset restores, or {@code null} if it may be active everywhere and nothing
	 * is reset: OneConfig has no such rules, because its profiles take their place.
	 */
	@Nullable SceneRules sceneRules();

	/**
	 * Returns what noclip ignores while it is active, before holding jump or sneak adds a direction.
	 */
	default NoClipRules rules() {
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
		return new NoClipRules(types, NoClipRules.bothWays(axes), this.get(Toggle.CLEAR_VIEW));
	}
}
