package net.darkz70.skintrim.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.darkz70.skintrim.client.SkinOverrides;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {

	@Inject(method = "getSkin", at = @At("TAIL"), cancellable = true)
	private void skintrim$overrideSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		PlayerSkin patched = SkinOverrides.patch(player.getUUID(), cir.getReturnValue());
		if (patched != null) {
			cir.setReturnValue(patched);
		}
	}
}
