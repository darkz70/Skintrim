package net.darkz70.skintrim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.darkz70.skintrim.network.SkinBroadcastPayload;
import net.darkz70.skintrim.network.SkinSyncPayload;
import net.darkz70.skintrim.server.SkintrimServer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;

public class Skintrim {

	public static final String MOD_NAME = /*$ mod_name*/ "Skintrim";
	public static final String MOD_ID = /*$ mod_id*/ "skintrim";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(SkinSyncPayload.TYPE, SkinSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SkinBroadcastPayload.TYPE, SkinBroadcastPayload.CODEC);

		SkintrimServer.init();

		LOGGER.info("{} Initialized", MOD_NAME);
	}
}
