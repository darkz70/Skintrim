package net.darkz70.figurestone.config;

import lombok.*;
import net.darkz70.figurestonelib.loader.FigureStoneLoader;
import net.darkz70.figurestonelib.utils.*;
import org.slf4j.*;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.darkz70.figurestone.FigureStone;

import java.io.*;
import java.util.concurrent.CompletableFuture;

import static net.darkz70.figurestonelib.utils.CodecUtils.option;

@Getter
@Setter
@AllArgsConstructor
public class FigureStoneConfig {

	public static final Codec<FigureStoneConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			option("mod_enabled", false, Codec.BOOL, FigureStoneConfig::isModEnabled)
	).apply(instance, FigureStoneConfig::new));

	private static final File CONFIG_FILE = FigureStoneLoader.getConfigDir().resolve(FigureStone.MOD_ID + ".json5").toFile();
	private static final Logger LOGGER = LoggerFactory.getLogger(FigureStone.MOD_NAME + "/Config");
	private static FigureStoneConfig INSTANCE;
	
	private boolean modEnabled;

	@SuppressWarnings("unused")
	private FigureStoneConfig() {
		throw new IllegalArgumentException();
	}

	public static FigureStoneConfig getInstance() {
		return INSTANCE == null ? reload() : INSTANCE;
	}

	public static FigureStoneConfig reload() {
		return INSTANCE = FigureStoneConfig.read();
	}

	public static FigureStoneConfig getNewInstance() {
		return CodecUtils.parseNewInstanceHacky(CODEC);
	}

	private static FigureStoneConfig read() {
		return ConfigUtils.readConfig(CODEC, CONFIG_FILE, LOGGER);
	}

	public void saveAsync() {
		CompletableFuture.runAsync(this::save);
	}

	public void save() {
		ConfigUtils.saveConfig(this, CODEC, CONFIG_FILE, LOGGER);
	}
}
