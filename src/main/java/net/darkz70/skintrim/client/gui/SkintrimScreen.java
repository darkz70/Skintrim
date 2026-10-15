package net.darkz70.skintrim.client.gui;

import net.darkz70.skintrim.client.MojangSkinService;
import net.darkz70.skintrim.client.SkintrimClient;
import net.darkz70.skintrim.client.TextureStore;
import net.darkz70.skintrim.config.SkintrimConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * In-game skin/cape editor. Open with the keybind (default K) or via ModMenu.
 * An input accepts a PNG file path, an http(s) URL or a Minecraft username;
 * you can also just drag &amp; drop a PNG onto the game window.
 */
public class SkintrimScreen extends Screen {

	private final @Nullable Screen parent;

	private EditBox skinBox;
	private EditBox capeBox;
	private Button modelButton;
	private Button syncButton;
	private Button capeModeButton;

	private Component status = Component.empty();
	private int statusColor = 0xFFFFFF;

	public SkintrimScreen(@Nullable Screen parent) {
		super(Component.translatable("skintrim.gui.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		int centerX = this.width / 2;
		int left = centerX - 60;
		int fieldWidth = 260;
		int y = 56;

		this.skinBox = new EditBox(this.font, left, y, fieldWidth, 20, Component.translatable("skintrim.gui.skin"));
		this.skinBox.setHint(Component.translatable("skintrim.gui.input_hint"));
		this.skinBox.setValue(config.skinInput);
		this.skinBox.setMaxLength(1024);
		this.addRenderableWidget(this.skinBox);
		y += 24;

		this.addRenderableWidget(Button.builder(Component.translatable("skintrim.gui.apply_skin"), b -> applySkin())
				.bounds(left, y, 128, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("skintrim.gui.reset_skin"), b -> resetSkin())
				.bounds(left + 132, y, 128, 20).build());
		y += 24;

		this.modelButton = Button.builder(modelMessage(), b -> toggleModel())
				.bounds(left, y, 128, 20).build();
		this.addRenderableWidget(this.modelButton);
		this.syncButton = Button.builder(syncMessage(), b -> toggleSync())
				.bounds(left + 132, y, 128, 20).build();
		this.addRenderableWidget(this.syncButton);
		y += 32;

		this.capeBox = new EditBox(this.font, left, y, fieldWidth, 20, Component.translatable("skintrim.gui.cape"));
		this.capeBox.setHint(Component.translatable("skintrim.gui.input_hint"));
		this.capeBox.setValue(config.capeInput);
		this.capeBox.setMaxLength(1024);
		this.addRenderableWidget(this.capeBox);
		y += 24;

		this.addRenderableWidget(Button.builder(Component.translatable("skintrim.gui.apply_cape"), b -> applyCape())
				.bounds(left, y, 128, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("skintrim.gui.reset_cape"), b -> resetCape())
				.bounds(left + 132, y, 128, 20).build());
		y += 24;

		this.capeModeButton = Button.builder(capeModeMessage(), b -> toggleCapeMode())
				.bounds(left, y, 260, 20).build();
		this.addRenderableWidget(this.capeModeButton);
		y += 28;

		this.addRenderableWidget(Button.builder(Component.translatable("skintrim.gui.mojang_upload"), b -> uploadToMojang())
				.bounds(left, y, 128, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("skintrim.gui.mojang_reset"), b -> resetMojang())
				.bounds(left + 132, y, 128, 20).build());
		y += 24;

		this.addRenderableWidget(Button.builder(Component.translatable("skintrim.gui.history"), b -> openHistory())
				.bounds(left, y, 128, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("skintrim.gui.open_folder"), b -> openSkinsFolder())
				.bounds(left + 132, y, 128, 20).build());
		y += 24;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(left, y, 260, 20).build());
	}

	private void openHistory() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		config.skinInput = skinBox.getValue().trim();
		config.capeInput = capeBox.getValue().trim();
		config.save();
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(new SkintrimHistoryScreen(this));
		}
	}

	// ------------------------------------------------------------ actions

	private void applySkin() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		config.skinInput = skinBox.getValue().trim();
		config.save();
		setStatus(Component.translatable("skintrim.status.loading"), 0xFFFF55);
		TextureStore.applyLocalSkin(config.skinInput, config.skinModel, (ok, msg) -> {
			if (ok) {
				SkintrimClient.sendSyncToServer();
				setStatus(Component.translatable("skintrim.status.skin_applied"), 0x55FF55);
			} else {
				setStatus(Component.translatable("skintrim.status.error", msg), 0xFF5555);
			}
		});
	}

	private void resetSkin() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		config.skinInput = "";
		config.save();
		skinBox.setValue("");
		TextureStore.clearLocalSkin();
		if (TextureStore.localCapeBytes.length == 0) {
			SkintrimClient.sendClearToServer();
		} else {
			SkintrimClient.sendSyncToServer();
		}
		setStatus(Component.translatable("skintrim.status.skin_reset"), 0xAAAAAA);
	}

	private void toggleCapeMode() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		config.capeInput = capeBox.getValue().trim();
		config.capeMode = switch (config.capeMode) {
			case "custom" -> "mojang";
			case "mojang" -> "hidden";
			default -> "custom";
		};
		config.save();
		capeModeButton.setMessage(capeModeMessage());
		SkintrimClient.applyCapeMode();
		setStatus(Component.translatable("skintrim.status.cape_mode",
				Component.translatable("skintrim.cape_mode." + config.capeMode)), 0xAAAAAA);
	}

	private Component capeModeMessage() {
		return Component.translatable("skintrim.gui.cape_mode",
				Component.translatable("skintrim.cape_mode." + SkintrimConfig.getInstance().capeMode));
	}

	private void applyCape() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		config.capeInput = capeBox.getValue().trim();
		config.capeMode = "custom";
		config.save();
		capeModeButton.setMessage(capeModeMessage());
		setStatus(Component.translatable("skintrim.status.loading"), 0xFFFF55);
		TextureStore.applyLocalCape(config.capeInput, (ok, msg) -> {
			if (ok) {
				SkintrimClient.sendSyncToServer();
				setStatus(Component.translatable("skintrim.status.cape_applied"), 0x55FF55);
			} else {
				setStatus(Component.translatable("skintrim.status.error", msg), 0xFF5555);
			}
		});
	}

	private void resetCape() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		config.capeInput = "";
		config.capeMode = "mojang";
		config.save();
		capeModeButton.setMessage(capeModeMessage());
		capeBox.setValue("");
		TextureStore.clearLocalCape();
		if (TextureStore.localSkinBytes.length == 0) {
			SkintrimClient.sendClearToServer();
		} else {
			SkintrimClient.sendSyncToServer();
		}
		setStatus(Component.translatable("skintrim.status.cape_reset"), 0xAAAAAA);
	}

	private void toggleModel() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		config.skinModel = "slim".equals(config.skinModel) ? "classic" : "slim";
		config.save();
		modelButton.setMessage(modelMessage());
		if (!config.skinInput.isBlank()) {
			applySkin();
		}
	}

	private void toggleSync() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		config.syncEnabled = !config.syncEnabled;
		config.save();
		syncButton.setMessage(syncMessage());
		if (config.syncEnabled) {
			SkintrimClient.sendSyncToServer();
		} else {
			SkintrimClient.sendClearToServer();
		}
	}

	private void uploadToMojang() {
		setStatus(Component.translatable("skintrim.status.loading"), 0xFFFF55);
		MojangSkinService.uploadCurrentSkin(SkintrimConfig.getInstance().skinModel, (ok, msg) ->
				setStatus(ok
						? Component.translatable("skintrim.status.mojang_ok")
						: Component.translatable("skintrim.status.mojang_error", msg), ok ? 0x55FF55 : 0xFF5555));
	}

	private void resetMojang() {
		setStatus(Component.translatable("skintrim.status.loading"), 0xFFFF55);
		MojangSkinService.resetSkin((ok, msg) ->
				setStatus(ok
						? Component.translatable("skintrim.status.mojang_reset_ok")
						: Component.translatable("skintrim.status.mojang_error", msg), ok ? 0x55FF55 : 0xFF5555));
	}

	private void openSkinsFolder() {
		try {
			Path dir = SkintrimConfig.getInstance().skinsDir();
			java.nio.file.Files.createDirectories(dir);
			String os = System.getProperty("os.name", "").toLowerCase();
			if (os.contains("win")) {
				new ProcessBuilder("explorer", dir.toString()).start();
			} else if (os.contains("mac")) {
				new ProcessBuilder("open", dir.toString()).start();
			} else {
				new ProcessBuilder("xdg-open", dir.toString()).start();
			}
		} catch (Exception e) {
			setStatus(Component.translatable("skintrim.status.error", String.valueOf(e.getMessage())), 0xFF5555);
		}
	}

	private Component modelMessage() {
		return Component.translatable("skintrim.gui.model", Component.translatable("skintrim.model." + SkintrimConfig.getInstance().skinModel));
	}

	private Component syncMessage() {
		return Component.translatable("skintrim.gui.sync", Component.translatable(SkintrimConfig.getInstance().syncEnabled ? "options.on" : "options.off"));
	}

	private void setStatus(Component status, int color) {
		this.status = status;
		this.statusColor = color;
	}

	// ------------------------------------------------------------ drag & drop

	@Override
	public void onFilesDrop(List<Path> files) {
		for (Path file : files) {
			if (!file.getFileName().toString().toLowerCase().endsWith(".png")) {
				continue;
			}
			if (file.getFileName().toString().toLowerCase().contains("cape")) {
				capeBox.setValue(file.toString());
				applyCape();
			} else {
				skinBox.setValue(file.toString());
				applySkin();
			}
			return;
		}
	}

	// ------------------------------------------------------------ rendering

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
		graphics.centeredText(this.font, Component.translatable("skintrim.gui.hint"), this.width / 2, 30, 0xAAAAAA);

		int left = this.width / 2 - 60;
		graphics.text(this.font, Component.translatable("skintrim.gui.skin"), left, 46, 0xFFFFFF);
		graphics.text(this.font, Component.translatable("skintrim.gui.cape"), left, 126, 0xFFFFFF);

		if (!status.getString().isEmpty()) {
			graphics.textWithWordWrap(this.font, status, left, this.height - 34, 380, statusColor);
		}

		drawPreview(graphics);
	}

	private void drawPreview(GuiGraphicsExtractor graphics) {
		int panelX = this.width / 2 - 190;
		int panelY = 56;
		graphics.fill(panelX - 6, panelY - 6, panelX + 76, panelY + 196, 0x7F000000);
		graphics.outline(panelX - 6, panelY - 6, 82, 202, 0xDF000000);
		graphics.centeredText(this.font, Component.translatable("skintrim.gui.preview"), panelX + 35, panelY - 18 < 40 ? panelY + 182 : panelY - 18, 0xFFFFFF);

		Identifier skin = TextureStore.localSkinTexture;
		if (skin != null) {
			boolean slim = "slim".equals(SkintrimConfig.getInstance().skinModel);
			drawSkinFront(graphics, skin, panelX, panelY + 4, 4, slim);
		} else {
			graphics.centeredText(this.font, Component.translatable("skintrim.gui.no_skin"), panelX + 35, panelY + 60, 0xAAAAAA);
		}

		Identifier cape = TextureStore.localCapeTexture;
		if (cape != null) {
			// Cape front face is 10x16 texels at (1,1) of a 64x32 texture.
			blit(graphics, cape, panelX + 15, panelY + 146, 40, 64, 1, 1, 10, 16, 64, 32);
		}
	}

	private void drawSkinFront(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int scale, boolean slim) {
		int armWidth = slim ? 3 : 4;
		// legs
		part(graphics, texture, x + 4 * scale, y + 20 * scale, scale, 4, 20, 4, 12);
		part(graphics, texture, x + 8 * scale, y + 20 * scale, scale, 20, 52, 4, 12);
		// body
		part(graphics, texture, x + 4 * scale, y + 8 * scale, scale, 20, 20, 8, 12);
		part(graphics, texture, x + 4 * scale, y + 8 * scale, scale, 20, 32, 8, 12);
		// arms
		part(graphics, texture, x + (4 - armWidth) * scale, y + 8 * scale, scale, 44, 20, armWidth, 12);
		part(graphics, texture, x + 12 * scale, y + 8 * scale, scale, 32 + 4, 52, armWidth, 12);
		// head + overlay
		part(graphics, texture, x + 4 * scale, y, scale, 8, 8, 8, 8);
		part(graphics, texture, x + 4 * scale, y, scale, 40, 8, 8, 8);
	}

	private void part(GuiGraphicsExtractor graphics, Identifier texture, int dx, int dy, int scale, int u, int v, int w, int h) {
		blit(graphics, texture, dx, dy, w * scale, h * scale, u, v, w, h, 64, 64);
	}

	private void blit(GuiGraphicsExtractor graphics, Identifier texture, int dx, int dy, int dw, int dh, int u, int v, int w, int h, int texW, int texH) {
		graphics.blit(texture, dx, dy, dx + dw, dy + dh, u / (float) texW, (u + w) / (float) texW, v / (float) texH, (v + h) / (float) texH);
	}

	@Override
	public void onClose() {
		SkintrimConfig.getInstance().save();
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(parent);
		}
	}
}
