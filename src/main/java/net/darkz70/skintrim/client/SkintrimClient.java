package net.darkz70.skintrim.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.darkz70.skintrim.Skintrim;
import net.darkz70.skintrim.client.gui.SkintrimScreen;
import net.darkz70.skintrim.config.SkintrimConfig;
import net.darkz70.skintrim.network.SkinBroadcastPayload;
import net.darkz70.skintrim.network.SkinSyncPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class SkintrimClient {

	private static KeyMapping openGuiKey;

	private SkintrimClient() {
	}

	public static void onInitializeClient() {
		SkintrimConfig config = SkintrimConfig.getInstance();

		// Restore the previously chosen skin/cape.
		if (!config.skinInput.isBlank()) {
			TextureStore.applyLocalSkin(config.skinInput, config.skinModel, (ok, msg) -> {
				if (ok) {
					sendSyncToServer();
				}
			});
		}
		if (!config.capeInput.isBlank()) {
			TextureStore.applyLocalCape(config.capeInput, (ok, msg) -> {
				if (ok) {
					sendSyncToServer();
				}
			});
		}

		openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.skintrim.open_gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				KeyMapping.Category.register(Skintrim.id("general"))
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openGuiKey.consumeClick()) {
				client.setScreen(new SkintrimScreen(client.gui.screen()));
			}
		});

		// Receive other players' skins from a Skintrim server.
		ClientPlayNetworking.registerGlobalReceiver(SkinBroadcastPayload.TYPE, (payload, context) -> {
			Minecraft client = context.client();
			if (client.player != null && client.player.getUUID().equals(payload.uuid())) {
				return; // our own skin is already applied locally
			}
			if (payload.isClear()) {
				context.client().execute(() -> SkinOverrides.clearRemote(payload.uuid()));
			} else {
				TextureStore.applyRemote(payload.uuid(), payload.skinPng(), payload.capePng(), payload.model());
			}
		});

		// Tell the server about our skin when joining (if it understands us).
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> sendSyncToServer());

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SkinOverrides.clearAllRemote());

		Skintrim.LOGGER.info("{} Client Initialized", Skintrim.MOD_NAME);
	}

	/** Sends our current skin/cape to the server so other Skintrim users can see it. Safe to call anytime. */
	public static void sendSyncToServer() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		if (!config.syncEnabled) {
			return;
		}
		if (TextureStore.localSkinBytes.length == 0 && TextureStore.localCapeBytes.length == 0) {
			return;
		}
		if (!ClientPlayNetworking.canSend(SkinSyncPayload.TYPE)) {
			return;
		}
		ClientPlayNetworking.send(new SkinSyncPayload(TextureStore.localSkinBytes, TextureStore.localCapeBytes, config.skinModel));
	}

	public static void sendClearToServer() {
		if (!ClientPlayNetworking.canSend(SkinSyncPayload.TYPE)) {
			return;
		}
		ClientPlayNetworking.send(new SkinSyncPayload(new byte[0], new byte[0], "classic"));
	}
}
