package net.darkz70.skintrim.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the currently active skin/cape overrides.
 * The mixin asks this class to patch the vanilla {@link PlayerSkin}:
 * everything not overridden (e.g. your Mojang cape when only a skin is set)
 * is kept from the original skin.
 */
public final class SkinOverrides {

	public static final class Override {
		public @Nullable ClientAsset.Texture body;
		public boolean bodySet;
		public @Nullable ClientAsset.Texture cape;
		public boolean capeSet;
		public PlayerModelType model = PlayerModelType.WIDE;

		public boolean isEmpty() {
			return !bodySet && !capeSet;
		}

		PlayerSkin apply(PlayerSkin original) {
			if (isEmpty()) {
				return original;
			}
			return PlayerSkin.insecure(
					bodySet && body != null ? body : original.body(),
					capeSet ? cape : original.cape(),
					capeSet ? cape : original.elytra(),
					bodySet ? model : original.model()
			);
		}
	}

	private static final Override LOCAL = new Override();
	private static final Map<UUID, Override> REMOTE = new ConcurrentHashMap<>();

	private SkinOverrides() {
	}

	public static Override local() {
		return LOCAL;
	}

	public static Override remote(UUID uuid) {
		return REMOTE.computeIfAbsent(uuid, id -> new Override());
	}

	public static void clearRemote(UUID uuid) {
		REMOTE.remove(uuid);
	}

	public static void clearAllRemote() {
		REMOTE.clear();
	}

	@Nullable
	public static PlayerSkin patch(UUID uuid, PlayerSkin original) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null && minecraft.player != null && minecraft.player.getUUID().equals(uuid)) {
			return LOCAL.isEmpty() ? null : LOCAL.apply(original);
		}
		Override remote = REMOTE.get(uuid);
		if (remote == null || remote.isEmpty()) {
			return null;
		}
		return remote.apply(original);
	}

	static ClientAsset.Texture assetOf(net.minecraft.resources.Identifier textureId) {
		return new ClientAsset.Texture() {
			@Override
			public net.minecraft.resources.Identifier texturePath() {
				return textureId;
			}

			@Override
			public net.minecraft.resources.Identifier id() {
				return textureId;
			}
		};
	}
}
