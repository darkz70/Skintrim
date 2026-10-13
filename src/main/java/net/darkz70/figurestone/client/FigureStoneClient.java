package net.darkz70.figurestone.client;

import net.darkz70.figurestonelib.logger.FigureStoneLogger;

import net.darkz70.figurestone.FigureStone;

public class FigureStoneClient {

	public static FigureStoneLogger LOGGER = FigureStone.LOGGER.extend("Client");

	public static void onInitializeClient() {
		LOGGER.info("{} Client Initialized", FigureStone.MOD_NAME);
	}
}
