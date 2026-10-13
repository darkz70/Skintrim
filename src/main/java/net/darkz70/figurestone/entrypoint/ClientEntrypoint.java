package net.darkz70.figurestone.entrypoint;

//? if fabric {

import net.fabricmc.api.ClientModInitializer;

import net.darkz70.figurestone.client.FigureStoneClient;

public class ClientEntrypoint implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		FigureStoneClient.onInitializeClient();
	}
}

//?} elif neoforge {
/*import net.darkz70.figurestone.FigureStone;

import net.darkz70.figurestone.client.FigureStoneClient;
import net.darkz70.figurestone.modmenu.ModMenuIntegration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = FigureStone.MOD_ID, dist = Dist.CLIENT)
public class ClientEntrypoint {

	public ClientEntrypoint(ModContainer container) {
		FigureStoneClient.onInitializeClient();
		ModMenuIntegration integration = new ModMenuIntegration();
		integration.register(container);
	}

}

*///?} elif forge {

/*import net.darkz70.figurestone.client.FigureStoneClient;
import net.darkz70.figurestone.modmenu.ModMenuIntegration;
import net.minecraftforge.fml.ModLoadingContext;

public class ClientEntrypoint {

	public static void onInitializeClient() {
		FigureStoneClient.onInitializeClient();
		ModMenuIntegration integration = new ModMenuIntegration();
		integration.register(ModLoadingContext.get().getActiveContainer());
	}

}

*///?}
