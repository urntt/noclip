package com.urntt.noclip.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.urntt.noclip.NoClipClient;
import com.urntt.noclip.NoClipRules;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
	/**
	 * Hides the block texture that covers the screen while the player's head is inside a block, as vanilla does
	 * for spectators, while noclip keeps the view clear.
	 */
	@ModifyReturnValue(method = "getViewBlockingState", at = @At("RETURN"))
	private static @Nullable BlockState noclip$hideInWallOverlay(final @Nullable BlockState state, final LocalPlayer player,
			final Frustum frustum) {
		NoClipRules rules = NoClipClient.controller().rulesFor(player);
		return rules != null && rules.keepsViewClear() ? null : state;
	}
}
