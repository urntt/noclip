package com.urntt.noclip.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.NoClipRules;
import com.urntt.noclip.collision.AxisShapes;
import com.urntt.noclip.config.CollisionType;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/**
	 * Splits the colliders of the local player's movement by axis while noclip is active. {@code collide} gathers
	 * its colliders here, both for the plain move and for stepping up. Along the noclip axes, the colliders of the
	 * ignored types (blocks, the world border, entities) are left out; along the other axes, and for every other
	 * entity, the colliders stay vanilla.
	 */
	@WrapMethod(method = "collectCollidersIgnoringWorldBorder(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")
	private static List<VoxelShape> noclip$splitColliders(final @Nullable Entity source, final Level level,
			final List<VoxelShape> entityColliders, final AABB boundingBox, final Operation<List<VoxelShape>> original) {
		List<VoxelShape> all = original.call(source, level, entityColliders, boundingBox);
		NoClipRules rules = NoClipClient.controller().rulesFor(source);
		if (source == null || rules == null || !rules.ignoresAnyShapes()) {
			return all;
		}
		return AxisShapes.split(all, source, level, entityColliders, boundingBox, rules);
	}

	/**
	 * Hands vanilla's per-axis collision the colliders for that axis, as split by
	 * {@link #noclip$splitColliders}. Lists that were not split, which includes every movement other than the
	 * local player's with noclip active, pass through unchanged.
	 */
	@WrapOperation(method = "collideWithShapes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/Shapes;collide(Lnet/minecraft/core/Direction$Axis;Lnet/minecraft/world/phys/AABB;Ljava/lang/Iterable;D)D"))
	private static double noclip$collideAlongAxis(final Direction.Axis axis, final AABB boundingBox,
			final Iterable<VoxelShape> shapes, final double distance, final Operation<Double> original) {
		return original.call(axis, boundingBox, AxisShapes.forAxis(shapes, axis), distance);
	}

	/**
	 * Skips pushing between two entities when one of them is the local player and noclip passes through entities.
	 * On the client, this covers the player pushing nearby entities as well as nearby entities pushing the player.
	 * {@code LivingEntity} overrides this method but calls it, and the overrides of minecarts, boats, shulkers, and
	 * slimes are left vanilla.
	 */
	@Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
	private void noclip$skipPushing(final Entity entity, final CallbackInfo ci) {
		if (noclip$ignoresEntities((Entity) (Object) this) || noclip$ignoresEntities(entity)) {
			ci.cancel();
		}
	}

	private static boolean noclip$ignoresEntities(final Entity entity) {
		NoClipRules rules = NoClipClient.controller().rulesFor(entity);
		return rules != null && rules.ignores(CollisionType.ENTITIES);
	}

	/**
	 * Keeps cobwebs, sweet berry bushes, and powder snow from slowing the local player down while noclip passes
	 * through slowing blocks. Their other effects, such as damage and freezing, are left to vanilla.
	 */
	@Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
	private void noclip$skipSlowdown(final BlockState blockState, final Vec3 speedMultiplier, final CallbackInfo ci) {
		NoClipRules rules = NoClipClient.controller().rulesFor((Entity) (Object) this);
		if (rules != null && rules.ignores(CollisionType.SLOWING_BLOCKS)) {
			ci.cancel();
		}
	}
}
