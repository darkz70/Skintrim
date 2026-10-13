package net.darkz70.figurestone.mixin;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.darkz70.figurestone.client.FigureStoneClient;

@Mixin(TitleScreen.class)
public class ExampleMixin {

	@Inject(at = @At("HEAD"), method = "init")
	private void init(CallbackInfo info) {
		FigureStoneClient.LOGGER.info("Hello from FigureStone Mixins!");
	}
}