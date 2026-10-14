package net.darkz70.skintrim.network;

import net.darkz70.skintrim.Skintrim;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: "this is my current skin/cape, show it to everyone".
 * Empty byte arrays mean "cleared". Server re-broadcasts it as {@link SkinBroadcastPayload}.
 */
public record SkinSyncPayload(byte[] skinPng, byte[] capePng, String model) implements CustomPacketPayload {

	public static final int MAX_TEXTURE_BYTES = 256 * 1024;

	public static final Type<SkinSyncPayload> TYPE = new Type<>(Skintrim.id("skin_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SkinSyncPayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				writeBytes(buf, payload.skinPng());
				writeBytes(buf, payload.capePng());
				buf.writeUtf(payload.model(), 16);
			},
			buf -> new SkinSyncPayload(readBytes(buf), readBytes(buf), buf.readUtf(16))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public boolean isClear() {
		return skinPng.length == 0 && capePng.length == 0;
	}

	static void writeBytes(RegistryFriendlyByteBuf buf, byte[] bytes) {
		buf.writeVarInt(bytes.length);
		buf.writeBytes(bytes);
	}

	static byte[] readBytes(RegistryFriendlyByteBuf buf) {
		int length = buf.readVarInt();
		if (length < 0 || length > MAX_TEXTURE_BYTES) {
			throw new IllegalArgumentException("Invalid texture size: " + length);
		}
		byte[] bytes = new byte[length];
		buf.readBytes(bytes);
		return bytes;
	}
}
