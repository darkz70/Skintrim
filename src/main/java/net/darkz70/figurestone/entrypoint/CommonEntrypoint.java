package net.darkz70.figurestone.entrypoint;

//? if fabric {

import net.darkz70.figurestone.FigureStone;

import net.fabricmc.api.ModInitializer;

public class CommonEntrypoint implements ModInitializer {

	@Override
	public void onInitialize() {
		FigureStone.onInitialize();
	}
}

//?} elif neoforge {
/*import net.darkz70.figurestone.FigureStone;

import net.neoforged.fml.common.Mod;

@Mod(FigureStone.MOD_ID)
public class CommonEntrypoint {

	public CommonEntrypoint() {
		FigureStone.onInitialize();
	}

}

*///?} elif forge {
/*import net.darkz70.figurestone.FigureStone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(FigureStone.MOD_ID)
public class CommonEntrypoint {

	public CommonEntrypoint() {
		FigureStone.onInitialize();
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientEntrypoint::onInitializeClient);
	}

}

*///?}

