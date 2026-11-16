package com.urntt.noclip;

import com.urntt.noclip.config.CollisionType;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.Direction;

/**
 * What noclip ignores while it is active: an immutable snapshot of the direction, collision type, and view settings.
 *
 * @param ignoredTypes the collision types noclip passes through
 * @param directions the directions of movement in which shape-based collision types are ignored
 * @param clearView whether the view stays clear while the camera is inside a block
 */
public record NoClipRules(Set<CollisionType> ignoredTypes, Set<Direction> directions, boolean clearView) {
	public NoClipRules {
		ignoredTypes = Set.copyOf(ignoredTypes);
		directions = Set.copyOf(directions);
	}

	/**
	 * Returns the directions of movement along {@code axes}, both ways.
	 */
	public static Set<Direction> bothWays(final Set<Direction.Axis> axes) {
		Set<Direction> directions = EnumSet.noneOf(Direction.class);
		for (Direction direction : Direction.values()) {
			if (axes.contains(direction.getAxis())) {
				directions.add(direction);
			}
		}
		return directions;
	}

	/**
	 * Returns these rules with shape-based collision types also ignored in {@code extra} directions.
	 */
	public NoClipRules withDirections(final Set<Direction> extra) {
		if (this.directions.containsAll(extra)) {
			return this;
		}
		Set<Direction> union = EnumSet.noneOf(Direction.class);
		union.addAll(this.directions);
		union.addAll(extra);
		return new NoClipRules(this.ignoredTypes, union, this.clearView);
	}

	/**
	 * Returns whether noclip passes through {@code type} at all. A shape-based type also needs at least one direction.
	 */
	public boolean ignores(final CollisionType type) {
		return this.ignoredTypes.contains(type) && (!type.isShapeBased() || !this.directions.isEmpty());
	}

	/** Returns whether any collision shapes are ignored, in at least one direction. */
	public boolean ignoresAnyShapes() {
		for (CollisionType type : CollisionType.values()) {
			if (type.isShapeBased() && this.ignores(type)) {
				return true;
			}
		}
		return false;
	}

	/** Returns whether the in-wall overlay is hidden and occlusion culling relaxed while inside a block. */
	public boolean keepsViewClear() {
		return this.clearView && this.ignores(CollisionType.BLOCKS);
	}
}
