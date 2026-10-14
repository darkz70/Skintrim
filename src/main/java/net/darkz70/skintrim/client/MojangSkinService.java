package net.darkz70.skintrim.client;

import net.darkz70.skintrim.Skintrim;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Uploads the current skin to the official Mojang profile API.
 * This is the only way a new skin becomes visible to <b>everyone</b>,
 * including vanilla clients on any server. Capes cannot be uploaded:
 * Mojang only allows equipping capes the account already owns.
 * The access token is read from the current session and never logged or stored.
 */
public final class MojangSkinService {

	private static final String SKINS_URL = "https://api.minecraftservices.com/minecraft/profile/skins";
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

	private MojangSkinService() {
	}

	public static void uploadCurrentSkin(String model, BiConsumer<Boolean, String> callback) {
		Path file = TextureStore.localSkinFile;
		if (file == null || !Files.isRegularFile(file)) {
			callback.accept(false, "no_skin");
			return;
		}
		String variant = "slim".equalsIgnoreCase(model) ? "slim" : "classic";
		CompletableFuture.runAsync(() -> {
			try {
				String token = Minecraft.getInstance().getUser().getAccessToken();
				if (token == null || token.isBlank() || "0".equals(token)) {
					report(callback, false, "offline");
					return;
				}

				String boundary = "Skintrim-" + UUID.randomUUID();
				byte[] head = ("--" + boundary + "\r\n"
						+ "Content-Disposition: form-data; name=\"variant\"\r\n\r\n" + variant + "\r\n"
						+ "--" + boundary + "\r\n"
						+ "Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n"
						+ "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8);
				byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
				byte[] png = Files.readAllBytes(file);
				byte[] body = new byte[head.length + png.length + tail.length];
				System.arraycopy(head, 0, body, 0, head.length);
				System.arraycopy(png, 0, body, head.length, png.length);
				System.arraycopy(tail, 0, body, head.length + png.length, tail.length);

				HttpRequest request = HttpRequest.newBuilder(URI.create(SKINS_URL))
						.timeout(Duration.ofSeconds(30))
						.header("Authorization", "Bearer " + token)
						.header("Content-Type", "multipart/form-data; boundary=" + boundary)
						.POST(HttpRequest.BodyPublishers.ofByteArray(body))
						.build();

				HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() >= 200 && response.statusCode() < 300) {
					report(callback, true, "ok");
				} else {
					Skintrim.LOGGER.warn("Mojang skin upload failed: HTTP {}", response.statusCode());
					report(callback, false, "http_" + response.statusCode());
				}
			} catch (Exception e) {
				Skintrim.LOGGER.warn("Mojang skin upload failed", e);
				report(callback, false, "error");
			}
		});
	}

	public static void resetSkin(BiConsumer<Boolean, String> callback) {
		CompletableFuture.runAsync(() -> {
			try {
				String token = Minecraft.getInstance().getUser().getAccessToken();
				if (token == null || token.isBlank() || "0".equals(token)) {
					report(callback, false, "offline");
					return;
				}
				HttpRequest request = HttpRequest.newBuilder(URI.create(SKINS_URL + "/active"))
						.timeout(Duration.ofSeconds(30))
						.header("Authorization", "Bearer " + token)
						.DELETE()
						.build();
				HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
				report(callback, response.statusCode() >= 200 && response.statusCode() < 300,
						response.statusCode() >= 200 && response.statusCode() < 300 ? "ok" : "http_" + response.statusCode());
			} catch (Exception e) {
				Skintrim.LOGGER.warn("Mojang skin reset failed", e);
				report(callback, false, "error");
			}
		});
	}

	private static void report(BiConsumer<Boolean, String> callback, boolean success, String message) {
		Minecraft.getInstance().execute(() -> callback.accept(success, message));
	}
}
