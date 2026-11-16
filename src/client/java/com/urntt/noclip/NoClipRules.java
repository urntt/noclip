package com.urntt.noclip;

import com.urntt.noclip.config.CollisionType;
import java.util.Set;
import net.minecraft.core.Direction;

/**
 * What noclip ignores while it is active: an immutable snapshot of the axis, collision type, and view settings.
 *
 * @param ignoredTypes the collision types noclip passes through
 * @param axes the axes along which shape-based collision types are ignored
 * @param clearView whether the view stays clear while the camera is inside a block
 */
public record NoClipRules(Set<CollisionType> ignoredTypes, Set<Direction.Axis> axes, boolean clearView) {
	public NoClipRules {
		ignoredTypes = Set.copyOf(ignoredTypes);
		axes = Set.copyOf(axes);
	}

	/**
	 * Returns whether noclip passes through {@code type} at all. A shape-based type also needs at least one axis.
	 */
	public boolean ignores(final CollisionType type) {
		return this.ignoredTypes.contains(type) && (!type.isShapeBased() || !this.axes.isEmpty());
	}

	/** Returns whether any collision shapes are ignored, along at least one axis. */
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
