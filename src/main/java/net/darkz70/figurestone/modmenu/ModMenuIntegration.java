package net.darkz70.figurestone.modmenu;

import net.darkz70.figurestone.yacl.YACLConfigurationScreen;
import net.darkz70.figurestonelib.FigureStoneLib;
import net.darkz70.figurestonelib.modmenu.AbstractModMenuIntegration;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration extends AbstractModMenuIntegration {

	@Override
	protected String getModId() {
		return FigureStoneLib.MOD_ID;
	}

	@Override
	protected Screen createConfigScreen(Screen screen) {
		return YACLConfigurationScreen.createScreen(screen);
	}
}
