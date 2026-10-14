package net.darkz70.skintrim.network;

import net.darkz70.skintrim.Skintrim;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Server -> all clients: "player {@code uuid} now wears this skin/cape".
 * Empty byte arrays mean the player cleared their override.
 */
public record SkinBroadcastPayload(UUID uuid, byte[] skinPng, byte[] capePng, String model) implements CustomPacketPayload {

	public static final Type<SkinBroadcastPayload> TYPE = new Type<>(Skintrim.id("skin_broadcast"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SkinBroadcastPayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeUUID(payload.uuid());
				SkinSyncPayload.writeBytes(buf, payload.skinPng());
				SkinSyncPayload.writeBytes(buf, payload.capePng());
				buf.writeUtf(payload.model(), 16);
			},
			buf -> new SkinBroadcastPayload(buf.readUUID(), SkinSyncPayload.readBytes(buf), SkinSyncPayload.readBytes(buf), buf.readUtf(16))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public boolean isClear() {
		return skinPng.length == 0 && capePng.length == 0;
	}
}
