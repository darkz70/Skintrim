package net.darkz70.skintrim.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.darkz70.skintrim.Skintrim;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
	/** Cape source, independent of Mojang: "custom" (this mod), "mojang" (official cape) or "hidden" (no cape). */
	public String capeMode = "custom";
	/** Send the skin/cape to Skintrim servers so other mod users see it. */
	public boolean syncEnabled = true;
	/** Previously applied skins, most recent first. */
	public List<HistoryEntry> skinHistory = new ArrayList<>();
	/** Previously applied capes, most recent first. */
	public List<HistoryEntry> capeHistory = new ArrayList<>();

	public static final int MAX_HISTORY = 16;

	/** One remembered skin/cape. {@code file} points to the cached PNG copy, so the entry still works if the original source disappears. */
	public static class HistoryEntry {
		public String input = "";
		public String model = "classic";
		public String file = "";
		public long time = 0;

		public HistoryEntry() {
		}

		public HistoryEntry(String input, String model, String file, long time) {
			this.input = input;
			this.model = model;
			this.file = file;
			this.time = time;
		}
	}

	public void recordSkinHistory(String input, String model, Path file) {
		if (skinHistory == null) {
			skinHistory = new ArrayList<>();
		}
		record(skinHistory, input, model, file);
		save();
	}

	public void recordCapeHistory(String input, Path file) {
		if (capeHistory == null) {
			capeHistory = new ArrayList<>();
		}
		record(capeHistory, input, "classic", file);
		save();
	}

	private static void record(List<HistoryEntry> list, String input, String model, Path file) {
		String fileStr = file == null ? "" : file.toString();
		HistoryEntry existing = null;
		for (HistoryEntry entry : list) {
			if (entry.input.equals(input) || (!fileStr.isEmpty() && fileStr.equals(entry.file))) {
				existing = entry;
				break;
			}
		}
		if (existing == null) {
			existing = new HistoryEntry(input, model, fileStr, 0);
		} else {
			list.remove(existing);
			// Keep the original label/model: re-applying from history passes the cached file as input.
			if (!fileStr.isEmpty()) {
				existing.file = fileStr;
			}
		}
		existing.time = System.currentTimeMillis();
		list.add(0, existing);
		while (list.size() > MAX_HISTORY) {
			list.remove(list.size() - 1);
		}
	}

	public void clearSkinHistory() {
		if (skinHistory != null) {
			skinHistory.clear();
		}
		save();
	}

	public void clearCapeHistory() {
		if (capeHistory != null) {
			capeHistory.clear();
		}
		save();
	}

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
