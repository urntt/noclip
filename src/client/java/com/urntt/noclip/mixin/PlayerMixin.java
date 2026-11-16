package com.urntt.noclip.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.NoClipRules;
import com.urntt.noclip.config.CollisionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
	/**
	 * Lets the local player take any pose while noclip passes through blocks, so being inside blocks no longer
	 * forces crouching or crawling. Other players keep the vanilla pose checks.
	 */
	@ModifyReturnValue(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("RETURN"))
	private boolean noclip$fitAnywhere(final boolean fits, final Pose pose) {
		NoClipRules rules = NoClipClient.controller().rulesFor((Player) (Object) this);
		return fits || rules != null && rules.ignores(CollisionType.BLOCKS);
	}

	/**
	 * Keeps bubble columns from pulling the local player while noclip passes through fluids.
	 */
	@Inject(method = "onAboveBubbleColumn", at = @At("HEAD"), cancellable = true)
	private void noclip$skipAboveBubbleColumn(final boolean dragDown, final BlockPos pos, final CallbackInfo ci) {
		this.noclip$skipBubbleColumn(ci);
	}

	/**
	 * Keeps bubble columns from pushing the local player while noclip passes through fluids.
	 */
	@Inject(method = "onInsideBubbleColumn", at = @At("HEAD"), cancellable = true)
	private void noclip$skipInsideBubbleColumn(final boolean dragDown, final CallbackInfo ci) {
		this.noclip$skipBubbleColumn(ci);
	}

	private void noclip$skipBubbleColumn(final CallbackInfo ci) {
		NoClipRules rules = NoClipClient.controller().rulesFor((Player) (Object) this);
		if (rules != null && rules.ignores(CollisionType.FLUIDS)) {
			ci.cancel();
		}
	}
}
