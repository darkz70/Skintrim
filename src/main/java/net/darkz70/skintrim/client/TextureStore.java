package net.darkz70.skintrim.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;

import net.darkz70.skintrim.Skintrim;
import net.darkz70.skintrim.config.SkintrimConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Resolves skin/cape sources (file, URL, username), validates the PNG,
 * registers it as a dynamic texture and installs it into {@link SkinOverrides}.
 * All network/disk work happens on the IO pool, texture registration on the client thread.
 */
public final class TextureStore {

	public enum Kind {SKIN, CAPE}

	/** Bytes of the currently applied local textures (used for server sync and Mojang upload). */
	public static volatile byte[] localSkinBytes = new byte[0];
	public static volatile byte[] localCapeBytes = new byte[0];
	public static volatile Path localSkinFile;
	public static volatile Identifier localSkinTexture;
	public static volatile Identifier localCapeTexture;

	private static Identifier lastSkinTextureId;
	private static Identifier lastCapeTextureId;

	private TextureStore() {
	}

	// ------------------------------------------------------------- public API

	public static void applyLocalSkin(String input, String model, BiConsumer<Boolean, String> callback) {
		applyLocal(Kind.SKIN, input, model, callback);
	}

	public static void applyLocalCape(String input, BiConsumer<Boolean, String> callback) {
		applyLocal(Kind.CAPE, input, "classic", callback);
	}

	public static void clearLocalSkin() {
		SkinOverrides.local().body = null;
		SkinOverrides.local().bodySet = false;
		localSkinBytes = new byte[0];
		localSkinFile = null;
		if (lastSkinTextureId != null) {
			Minecraft.getInstance().getTextureManager().release(lastSkinTextureId);
			lastSkinTextureId = null;
			localSkinTexture = null;
		}
	}

	public static void clearLocalCape() {
		SkinOverrides.local().cape = null;
		SkinOverrides.local().capeSet = false;
		localCapeBytes = new byte[0];
		if (lastCapeTextureId != null) {
			Minecraft.getInstance().getTextureManager().release(lastCapeTextureId);
			lastCapeTextureId = null;
			localCapeTexture = null;
		}
	}

	/** Called when the server tells us another player's skin. Runs texture registration on the client thread. */
	public static void applyRemote(UUID uuid, byte[] skinPng, byte[] capePng, String model) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> {
			if (skinPng.length == 0 && capePng.length == 0) {
				SkinOverrides.clearRemote(uuid);
				return;
			}
			SkinOverrides.Override override = SkinOverrides.remote(uuid);
			try {
				if (skinPng.length > 0) {
					Identifier id = registerTexture("remote/" + uuid + "/skin", skinPng, Kind.SKIN);
					override.body = SkinOverrides.assetOf(id);
					override.bodySet = true;
					override.model = parseModel(model);
				}
				if (capePng.length > 0) {
					Identifier id = registerTexture("remote/" + uuid + "/cape", capePng, Kind.CAPE);
					override.cape = SkinOverrides.assetOf(id);
					override.capeSet = true;
				}
			} catch (Exception e) {
				Skintrim.LOGGER.warn("Failed to apply remote skin of {}", uuid, e);
			}
		});
	}

	public static PlayerModelType parseModel(String model) {
		return "slim".equalsIgnoreCase(model) ? PlayerModelType.SLIM : PlayerModelType.WIDE;
	}

	// ------------------------------------------------------------- local apply

	private static void applyLocal(Kind kind, String input, String model, BiConsumer<Boolean, String> callback) {
		String trimmed = input == null ? "" : input.trim();
		if (trimmed.isEmpty()) {
			if (kind == Kind.SKIN) {
				clearLocalSkin();
			} else {
				clearLocalCape();
			}
			callback.accept(true, "cleared");
			return;
		}

		CompletableFuture.supplyAsync(() -> fetchBytes(trimmed, kind), Util.ioPool()).thenAccept(result ->
				Minecraft.getInstance().execute(() -> {
					try {
						Identifier id = registerTexture(kind == Kind.SKIN ? "skin/local" : "cape/local", result.bytes(), kind);
						Path cached = cacheBytes(kind, result.bytes());

						if (kind == Kind.SKIN) {
							if (lastSkinTextureId != null && !lastSkinTextureId.equals(id)) {
								Minecraft.getInstance().getTextureManager().release(lastSkinTextureId);
							}
							lastSkinTextureId = id;
							localSkinTexture = id;
							localSkinBytes = result.bytes();
							localSkinFile = cached;
							SkinOverrides.local().body = SkinOverrides.assetOf(id);
							SkinOverrides.local().bodySet = true;
							// A username source may know better than the user which model the skin uses.
							SkinOverrides.local().model = parseModel(result.detectedModel() != null ? result.detectedModel() : model);
						} else {
							if (lastCapeTextureId != null && !lastCapeTextureId.equals(id)) {
								Minecraft.getInstance().getTextureManager().release(lastCapeTextureId);
							}
							lastCapeTextureId = id;
							localCapeTexture = id;
							localCapeBytes = result.bytes();
							SkinOverrides.local().cape = SkinOverrides.assetOf(id);
							SkinOverrides.local().capeSet = true;
						}
						callback.accept(true, "ok");
					} catch (Exception e) {
						Skintrim.LOGGER.warn("Failed to apply {} from '{}'", kind, trimmed, e);
						callback.accept(false, e.getMessage() == null ? "error" : e.getMessage());
					}
				})
		).exceptionally(error -> {
			Minecraft.getInstance().execute(() -> callback.accept(false, rootMessage(error)));
			return null;
		});
	}

	private static String rootMessage(Throwable error) {
		Throwable cause = error;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		return cause.getMessage() == null ? cause.toString() : cause.getMessage();
	}

	// ------------------------------------------------------------- fetching

	private record Fetched(byte[] bytes, String detectedModel) {
	}

	private static Fetched fetchBytes(String input, Kind kind) {
		try {
			if (input.startsWith("http://") || input.startsWith("https://")) {
				return new Fetched(download(input), null);
			}
			Path path = resolvePath(input);
			if (path != null && Files.isRegularFile(path)) {
				return new Fetched(Files.readAllBytes(path), null);
			}
			if (input.endsWith(".png") || input.contains("/") || input.contains("\\")) {
				throw new IllegalArgumentException("File not found: " + input);
			}
			// Treat it as a Minecraft username and pull textures from the Mojang session server.
			return fetchByUsername(input, kind);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
		}
	}

	private static Path resolvePath(String input) {
		Path path = Path.of(input);
		if (Files.isRegularFile(path)) {
			return path;
		}
		Path inSkins = SkintrimConfig.getInstance().skinsDir().resolve(input);
		if (Files.isRegularFile(inSkins)) {
			return inSkins;
		}
		Path inGameDir = FabricLoaderGameDir().resolve(input);
		return Files.isRegularFile(inGameDir) ? inGameDir : path;
	}

	private static Path FabricLoaderGameDir() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
	}

	private static Fetched fetchByUsername(String username, Kind kind) throws Exception {
		String lookup = httpGet("https://api.minecraftservices.com/minecraft/profile/lookup/name/"
				+ URLEncoder.encode(username, StandardCharsets.UTF_8));
		JsonObject profile = JsonParser.parseString(lookup).getAsJsonObject();
		if (!profile.has("id")) {
			throw new IllegalArgumentException("Player not found: " + username);
		}
		String id = profile.get("id").getAsString();

		String session = httpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + id);
		JsonObject sessionJson = JsonParser.parseString(session).getAsJsonObject();
		String value = null;
		for (var element : sessionJson.getAsJsonArray("properties")) {
			JsonObject property = element.getAsJsonObject();
			if ("textures".equals(property.get("name").getAsString())) {
				value = property.get("value").getAsString();
				break;
			}
		}
		if (value == null) {
			throw new IllegalArgumentException("No textures for player: " + username);
		}

		JsonObject textures = JsonParser.parseString(new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8))
				.getAsJsonObject().getAsJsonObject("textures");

		if (kind == Kind.SKIN) {
			if (!textures.has("SKIN")) {
				throw new IllegalArgumentException(username + " has no custom skin");
			}
			JsonObject skin = textures.getAsJsonObject("SKIN");
			String model = null;
			if (skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")) {
				model = "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString()) ? "slim" : "classic";
			}
			return new Fetched(download(skin.get("url").getAsString()), model);
		}

		if (!textures.has("CAPE")) {
			throw new IllegalArgumentException(username + " has no cape");
		}
		return new Fetched(download(textures.getAsJsonObject("CAPE").get("url").getAsString()), null);
	}

	private static byte[] download(String url) throws Exception {
		HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
		connection.setRequestProperty("User-Agent", "Skintrim/1.0");
		connection.setConnectTimeout(10_000);
		connection.setReadTimeout(15_000);
		connection.setInstanceFollowRedirects(true);
		try (InputStream in = connection.getInputStream()) {
			byte[] bytes = in.readAllBytes();
			if (bytes.length > 2 * 1024 * 1024) {
				throw new IllegalArgumentException("Texture is too big");
			}
			return bytes;
		}
	}

	private static String httpGet(String url) throws Exception {
		return new String(download(url), StandardCharsets.UTF_8);
	}

	// ------------------------------------------------------------- textures

	/** Validates the PNG and registers it. Must be called on the client thread. */
	private static Identifier registerTexture(String prefix, byte[] png, Kind kind) throws Exception {
		NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
		if (image == null) {
			throw new IllegalArgumentException("Not a PNG texture");
		}
		boolean valid = kind == Kind.SKIN
				? image.getWidth() == 64 && (image.getHeight() == 64 || image.getHeight() == 32)
				: image.getWidth() == 64 && (image.getHeight() == 32 || image.getHeight() == 64);
		if (!valid) {
			String size = image.getWidth() + "x" + image.getHeight();
			image.close();
			throw new IllegalArgumentException("Invalid " + kind.name().toLowerCase() + " size: " + size);
		}

		Identifier id = Skintrim.id(prefix + "/" + Integer.toHexString(Arrays.hashCode(png)));
		var texture = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "skintrim/" + prefix, image);
		Minecraft.getInstance().getTextureManager().register(id, texture);
		return id;
	}

	private static Path cacheBytes(Kind kind, byte[] bytes) {
		try {
			Path dir = SkintrimConfig.getInstance().cacheDir();
			Files.createDirectories(dir);
			Path file = dir.resolve((kind == Kind.SKIN ? "skin-" : "cape-") + Integer.toHexString(Arrays.hashCode(bytes)) + ".png");
			Files.write(file, bytes);
			return file;
		} catch (Exception e) {
			Skintrim.LOGGER.warn("Failed to cache texture", e);
			return null;
		}
	}
}
