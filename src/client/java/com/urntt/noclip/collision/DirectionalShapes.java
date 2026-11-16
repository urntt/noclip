package com.urntt.noclip.collision;

import com.google.common.collect.ImmutableList;
import com.urntt.noclip.NoClipRules;
import com.urntt.noclip.config.CollisionType;
import java.util.AbstractList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The colliders of one movement, split by direction. To vanilla code it is the complete list of colliders, but
 * {@link #forMovement(Iterable, Direction.Axis, double)} hands vanilla's per-axis collision only the colliders noclip
 * keeps when the movement goes in a noclip direction.
 */
public final class DirectionalShapes extends AbstractList<VoxelShape> {
	private final List<VoxelShape> all;
	private final List<VoxelShape> kept;
	private final Set<Direction> noclipDirections;

	private DirectionalShapes(final List<VoxelShape> all, final List<VoxelShape> kept,
			final Set<Direction> noclipDirections) {
		this.all = all;
		this.kept = kept;
		this.noclipDirections = noclipDirections;
	}

	/**
	 * Returns the colliders for the movement of {@code source} under {@code rules}: {@code all}, the list vanilla
	 * collected, for movement in the directions noclip does not apply to, and only the colliders of the types noclip
	 * keeps for the others.
	 *
	 * <p>The kept colliders are collected the same way as in vanilla's
	 * {@code Entity.collectCollidersIgnoringWorldBorder(Entity, Level, List, AABB)}, minus the ignored sources.
	 */
	public static List<VoxelShape> split(final List<VoxelShape> all, final Entity source, final Level level,
			final List<VoxelShape> entityColliders, final AABB boundingBox, final NoClipRules rules) {
		ImmutableList.Builder<VoxelShape> kept = ImmutableList.builder();
		if (!rules.ignores(CollisionType.ENTITIES)) {
			kept.addAll(entityColliders);
		}
		WorldBorder worldBorder = level.getWorldBorder();
		if (!rules.ignores(CollisionType.WORLD_BORDER) && worldBorder.isInsideCloseToBorder(source, boundingBox)) {
			kept.add(worldBorder.getCollisionShape());
		}
		if (!rules.ignores(CollisionType.BLOCKS)) {
			kept.addAll(level.getBlockCollisions(source, boundingBox));
		}

		if (rules.directions().size() == Direction.values().length) {
			return kept.build();
		}
		return new DirectionalShapes(all, kept.build(), rules.directions());
	}

	/**
	 * Returns the colliders vanilla should test for a movement of {@code distance} along {@code axis}: the kept ones
	 * when {@code shapes} were split and the movement goes in a noclip direction, and {@code shapes} itself otherwise.
	 */
	public static Iterable<VoxelShape> forMovement(final Iterable<VoxelShape> shapes, final Direction.Axis axis,
			final double distance) {
		if (shapes instanceof DirectionalShapes split) {
			Direction direction = Direction.get(
					distance > 0.0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE, axis);
			if (split.noclipDirections.contains(direction)) {
				return split.kept;
			}
		}
		return shapes;
	}

	@Override
	public VoxelShape get(final int index) {
		return this.all.get(index);
	}

	@Override
	public int size() {
		return this.all.size();
	}
}
