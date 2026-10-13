package net.darkz70.figurestone;

import net.darkz70.figurestonelib.logger.FigureStoneLogger;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;

public class FigureStone {

	public static final String MOD_NAME = /*$ mod_name*/ "FigureStone";
	public static final String MOD_ID = /*$ mod_id*/ "figurestone";

	public static FigureStoneLogger LOGGER = new FigureStoneLogger(FigureStone.MOD_NAME);

	public static Identifier id(String path) {
		//? if >=1.21 {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
		//?} else {
		/*return Identifier.tryBuild(MOD_ID, path);
		 *///?}
	}

	public static Identifier parseId(String path) {
		//? if >=1.21 {
		return Identifier.parse(path);
		//?} else {
		/*return new Identifier(path);
		 *///?}
	}

	public static MutableComponent text(String path, Object... args) {
		return Component.translatable(String.format("%s.%s", MOD_ID, path), args);
	}

	public static void onInitialize() {
		LOGGER.info("{} Initialized", MOD_NAME);
	}
}