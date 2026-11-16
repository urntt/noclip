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
 * The colliders of one movement, split by axis. To vanilla code it is the complete list of colliders, but
 * {@link #forAxis(Iterable, Direction.Axis)} hands vanilla's per-axis collision only the colliders noclip keeps
 * along the noclip axes.
 */
public final class AxisShapes extends AbstractList<VoxelShape> {
	private final List<VoxelShape> all;
	private final List<VoxelShape> kept;
	private final Set<Direction.Axis> noclipAxes;

	private AxisShapes(final List<VoxelShape> all, final List<VoxelShape> kept, final Set<Direction.Axis> noclipAxes) {
		this.all = all;
		this.kept = kept;
		this.noclipAxes = noclipAxes;
	}

	/**
	 * Returns the colliders for the movement of {@code source} under {@code rules}: {@code all}, the list vanilla
	 * collected, along the axes noclip does not apply to, and only the colliders of the types noclip keeps along the
	 * others.
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

		if (rules.axes().size() == Direction.Axis.values().length) {
			return kept.build();
		}
		return new AxisShapes(all, kept.build(), rules.axes());
	}

	/**
	 * Returns the colliders vanilla should test along {@code axis}: the kept ones for a noclip axis of an
	 * {@link AxisShapes}, and {@code shapes} itself otherwise.
	 */
	public static Iterable<VoxelShape> forAxis(final Iterable<VoxelShape> shapes, final Direction.Axis axis) {
		if (shapes instanceof AxisShapes axisShapes && axisShapes.noclipAxes.contains(axis)) {
			return axisShapes.kept;
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
