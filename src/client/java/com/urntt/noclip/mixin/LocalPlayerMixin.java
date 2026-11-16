package com.urntt.noclip.mixin;

import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.NoClipRules;
import com.urntt.noclip.config.CollisionType;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	/**
	 * Stops the client from pushing the player out of blocks they are inside while noclip passes through blocks.
	 */
	@Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
	private void noclip$stayInsideBlocks(final double x, final double z, final CallbackInfo ci) {
		NoClipRules rules = NoClipClient.controller().rulesFor((LocalPlayer) (Object) this);
		if (rules != null && rules.ignores(CollisionType.BLOCKS)) {
			ci.cancel();
		}
	}
}
