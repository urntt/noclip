package com.urntt.noclip.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.NoClipRules;
import com.urntt.noclip.collision.DirectionalShapes;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Shadow
	private static List<VoxelShape> collectCollidersIgnoringWorldBorder(final @Nullable Entity source, final Level level,
			final List<VoxelShape> entityColliders, final AABB boundingBox) {
		throw new AssertionError();
	}

	@Shadow
	private static Vec3 collideWithShapes(final Vec3 movement, final AABB boundingBox, final List<VoxelShape> shapes) {
		throw new AssertionError();
	}

	/**
	 * Resolves the local player's plain move (not stepping up) with vanilla's colliders while noclip ignores
	 * collision shapes, so {@link #noclip$splitColliders} and {@link #noclip$collideAlongAxis} apply. This is the
	 * body of vanilla's {@code collideBoundingBox(Entity, Vec3, AABB, Level, List)}. It is called directly because
	 * performance mods such as Lithium replace that call with their own collision code. The entity colliders are
	 * collected again, because such mods may also postpone collecting them. Every other movement, and the local
	 * player's while noclip ignores no shapes, keeps whatever implementation is installed.
	 */
	@WrapOperation(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
	private Vec3 noclip$collideWithVanillaColliders(final Entity source, final Vec3 movement, final AABB boundingBox,
			final Level level, final List<VoxelShape> entityColliders, final Operation<Vec3> original) {
		NoClipRules rules = NoClipClient.controller().rulesFor(source);
		if (rules == null || !rules.ignoresAnyShapes()) {
			return original.call(source, movement, boundingBox, level, entityColliders);
		}
		AABB sweep = boundingBox.expandTowards(movement);
		List<VoxelShape> colliders = collectCollidersIgnoringWorldBorder(source, level,
				level.getEntityCollisions(source, sweep), sweep);
		return collideWithShapes(movement, boundingBox, colliders);
	}

	/**
	 * Splits the colliders of the local player's movement by direction while noclip is active. {@code collide}
	 * gathers its colliders here, both for the plain move and for stepping up. For movement in a noclip direction,
	 * the colliders of the ignored types (blocks, the world border, entities) are left out; for movement in the other
	 * directions, and for every other entity, the colliders stay vanilla.
	 */
	@WrapMethod(method = "collectCollidersIgnoringWorldBorder(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")
	private static List<VoxelShape> noclip$splitColliders(final @Nullable Entity source, final Level level,
			final List<VoxelShape> entityColliders, final AABB boundingBox, final Operation<List<VoxelShape>> original) {
		List<VoxelShape> all = original.call(source, level, entityColliders, boundingBox);
		NoClipRules rules = NoClipClient.controller().rulesFor(source);
		if (source == null || rules == null || !rules.ignoresAnyShapes()) {
			return all;
		}
		return DirectionalShapes.split(all, source, level, entityColliders, boundingBox, rules);
	}

	/**
	 * Hands vanilla's per-axis collision the colliders for the direction of the movement, as split by
	 * {@link #noclip$splitColliders}. Lists that were not split, which includes every movement other than the
	 * local player's with noclip active, pass through unchanged.
	 */
	@WrapOperation(method = "collideWithShapes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/Shapes;collide(Lnet/minecraft/core/Direction$Axis;Lnet/minecraft/world/phys/AABB;Ljava/lang/Iterable;D)D"))
	private static double noclip$collideAlongAxis(final Direction.Axis axis, final AABB boundingBox,
			final Iterable<VoxelShape> shapes, final double distance, final Operation<Double> original) {
		return original.call(axis, boundingBox, DirectionalShapes.forMovement(shapes, axis, distance), distance);
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
