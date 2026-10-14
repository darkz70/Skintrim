package net.darkz70.skintrim.entrypoint;

import net.darkz70.skintrim.Skintrim;
import net.fabricmc.api.ModInitializer;

public class CommonEntrypoint implements ModInitializer {

	@Override
	public void onInitialize() {
		Skintrim.onInitialize();
	}
}
