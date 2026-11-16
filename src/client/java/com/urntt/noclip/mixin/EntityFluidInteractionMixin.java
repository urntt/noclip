package com.urntt.noclip.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.NoClipRules;
import com.urntt.noclip.config.CollisionType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityFluidInteraction.class)
public abstract class EntityFluidInteractionMixin {
	/**
	 * Reports no fluid interaction box for the local player while noclip passes through fluids. {@code update} has
	 * already cleared the previous tick's fluids at that point and then finds none, so the player counts as being
	 * in no fluid at all: no drag, currents, buoyancy, or swimming. Every other entity keeps vanilla fluid physics.
	 */
	@WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getFluidInteractionBox()Lnet/minecraft/world/phys/AABB;"))
	private @Nullable AABB noclip$ignoreFluids(final Entity entity, final Operation<AABB> original) {
		NoClipRules rules = NoClipClient.controller().rulesFor(entity);
		if (rules != null && rules.ignores(CollisionType.FLUIDS)) {
			return null;
		}
		return original.call(entity);
	}
}
