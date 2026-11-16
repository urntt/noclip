package com.urntt.noclip.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.NoClipRules;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	/**
	 * Treats the local player like a spectator when vanilla decides whether to turn off occlusion culling for a
	 * camera inside a solid block, while noclip keeps the view clear. Without this, the sections around a camera
	 * inside a wall would be culled away.
	 */
	@ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"))
	private boolean noclip$cullLikeSpectator(final boolean spectator) {
		NoClipRules rules = NoClipClient.controller().rulesFor(this.minecraft.player);
		return spectator || rules != null && rules.keepsViewClear();
	}
}
