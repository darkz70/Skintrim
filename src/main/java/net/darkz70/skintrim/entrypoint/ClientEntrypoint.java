package net.darkz70.skintrim.entrypoint;

import net.darkz70.skintrim.client.SkintrimClient;
import net.fabricmc.api.ClientModInitializer;

public class ClientEntrypoint implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		SkintrimClient.onInitializeClient();
	}
}
