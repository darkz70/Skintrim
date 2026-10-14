package net.darkz70.skintrim.server;

import net.darkz70.skintrim.Skintrim;
import net.darkz70.skintrim.network.SkinBroadcastPayload;
import net.darkz70.skintrim.network.SkinSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated-server / integrated-server side of the sync.
 * Stores every player's chosen skin/cape and re-broadcasts it, so all
 * clients with Skintrim see each other's skins without a Mojang upload.
 * Lives in common code: it registers no client-only classes.
 */
public final class SkintrimServer {

	private static final Map<UUID, SkinSyncPayload> SKINS = new ConcurrentHashMap<>();

	private SkintrimServer() {
	}

	public static void init() {
		ServerPlayNetworking.registerGlobalReceiver(SkinSyncPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			UUID uuid = player.getUUID();

			if (payload.isClear()) {
				SKINS.remove(uuid);
			} else {
				if (payload.skinPng().length == 0 && payload.capePng().length == 0) {
					return;
				}
				SKINS.put(uuid, payload);
			}

			SkinBroadcastPayload broadcast = new SkinBroadcastPayload(uuid, payload.skinPng(), payload.capePng(), payload.model());
			for (ServerPlayer target : context.server().getPlayerList().getPlayers()) {
				if (ServerPlayNetworking.canSend(target, broadcast.type())) {
					ServerPlayNetworking.send(target, broadcast);
				}
			}
			Skintrim.LOGGER.debug("Synced skin of {} ({} skin bytes, {} cape bytes)", player.getName().getString(), payload.skinPng().length, payload.capePng().length);
		});

		// Send everything we know to a freshly joined player.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer joined = handler.getPlayer();
			SKINS.forEach((uuid, data) -> {
				if (!uuid.equals(joined.getUUID())) {
					SkinBroadcastPayload payload = new SkinBroadcastPayload(uuid, data.skinPng(), data.capePng(), data.model());
					if (ServerPlayNetworking.canSend(joined, payload.type())) {
						ServerPlayNetworking.send(joined, payload);
					}
				}
			});
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID uuid = handler.getPlayer().getUUID();
			if (SKINS.remove(uuid) != null) {
				SkinBroadcastPayload clear = new SkinBroadcastPayload(uuid, new byte[0], new byte[0], "classic");
				for (ServerPlayer target : server.getPlayerList().getPlayers()) {
					if (ServerPlayNetworking.canSend(target, clear.type())) {
						ServerPlayNetworking.send(target, clear);
					}
				}
			}
		});
	}
}
