package net.darkz70.figurestone.yacl;

import lombok.experimental.ExtensionMethod;
import net.darkz70.figurestone.FigureStone;
import net.darkz70.figurestonelib.yacl.api.*;
import net.darkz70.figurestonelib.yacl.extension.SimpleOptionExtension;
import net.minecraft.client.gui.screens.Screen;

import net.darkz70.figurestone.config.FigureStoneConfig;

@ExtensionMethod(SimpleOptionExtension.class)
public class YACLConfigurationScreen {

	private YACLConfigurationScreen() {
		throw new IllegalStateException("Screen class");
	}

	public static Screen createScreen(Screen parent) {
		FigureStoneConfig defConfig = FigureStoneConfig.getNewInstance();
		FigureStoneConfig config = FigureStoneConfig.getInstance();

		return SimpleYACLScreen.startBuilder(FigureStone.MOD_ID, parent, config::saveAsync)
				.categories(getGeneralCategory(defConfig, config))
				.build();
	}

	private static SimpleCategory getGeneralCategory(FigureStoneConfig defConfig, FigureStoneConfig config) {
		return SimpleCategory.startBuilder("general")
				.groups(getMainGroup(defConfig, config));
	}

	private static SimpleGroup getMainGroup(FigureStoneConfig defConfig, FigureStoneConfig config) {
		return SimpleGroup.startBuilder("main").options(
				SimpleOption.<Boolean>startBuilder("mod_enabled")
						.withBinding(defConfig.isModEnabled(), config::isModEnabled, config::setModEnabled, true)
						.withController()
						.withDescription(SimpleContent.IMAGE)
		);
	}

}


