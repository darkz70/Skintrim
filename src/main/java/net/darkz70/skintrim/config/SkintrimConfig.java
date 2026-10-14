package net.darkz70.skintrim.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.darkz70.skintrim.Skintrim;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain JSON config stored in {@code config/skintrim.json}.
 * Skin/cape inputs accept a local PNG path, an http(s) URL or a Minecraft username.
 */
public class SkintrimConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve(Skintrim.MOD_ID + ".json");

	private static SkintrimConfig INSTANCE;

	/** Raw skin input: PNG file path, URL or username. Empty = vanilla skin. */
	public String skinInput = "";
	/** "classic" (wide) or "slim". */
	public String skinModel = "classic";
	/** Raw cape input: PNG file path, URL or username. Empty = no custom cape. */
	public String capeInput = "";
	/** Send the skin/cape to Skintrim servers so other mod users see it. */
	public boolean syncEnabled = true;

	public static SkintrimConfig getInstance() {
		if (INSTANCE == null) {
			INSTANCE = load();
		}
		return INSTANCE;
	}

	private static SkintrimConfig load() {
		try {
			if (Files.isRegularFile(CONFIG_FILE)) {
				try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
					SkintrimConfig config = GSON.fromJson(reader, SkintrimConfig.class);
					if (config != null) {
						return config;
					}
				}
			}
		} catch (Exception e) {
			Skintrim.LOGGER.warn("Failed to read config, using defaults", e);
		}
		return new SkintrimConfig();
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
				GSON.toJson(this, writer);
			}
		} catch (Exception e) {
			Skintrim.LOGGER.warn("Failed to save config", e);
		}
	}

	public Path cacheDir() {
		return FabricLoader.getInstance().getConfigDir().resolve(Skintrim.MOD_ID).resolve("cache");
	}

	public Path skinsDir() {
		return FabricLoader.getInstance().getConfigDir().resolve(Skintrim.MOD_ID).resolve("skins");
	}
}
