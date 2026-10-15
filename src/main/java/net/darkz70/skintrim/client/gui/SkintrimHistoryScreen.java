package net.darkz70.skintrim.client.gui;

import net.darkz70.skintrim.client.SkintrimClient;
import net.darkz70.skintrim.client.TextureStore;
import net.darkz70.skintrim.config.SkintrimConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Previously applied skins/capes. Click an entry to wear it again;
 * entries are backed by cached PNG copies, so they survive the original
 * file/URL disappearing.
 */
public class SkintrimHistoryScreen extends Screen {

	private static final int PAGE_SIZE = 8;

	private final @Nullable Screen parent;
	private final boolean capes;
	private final int page;

	private Component status = Component.empty();
	private int statusColor = 0xFFFFFF;

	public SkintrimHistoryScreen(@Nullable Screen parent) {
		this(parent, false, 0);
	}

	public SkintrimHistoryScreen(@Nullable Screen parent, boolean capes, int page) {
		super(Component.translatable("skintrim.history.title"));
		this.parent = parent;
		this.capes = capes;
		this.page = page;
	}

	private void switchTo(boolean capes, int page) {
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(new SkintrimHistoryScreen(parent, capes, page));
		}
	}

	@Override
	protected void init() {
		SkintrimConfig config = SkintrimConfig.getInstance();
		List<SkintrimConfig.HistoryEntry> entries = capes ? nullSafe(config.capeHistory) : nullSafe(config.skinHistory);

		int centerX = this.width / 2;
		int y = 48;

		this.addRenderableWidget(Button.builder(Component.translatable("skintrim.history.tab",
						Component.translatable(capes ? "skintrim.history.capes" : "skintrim.history.skins")),
				b -> switchTo(!capes, 0)).bounds(centerX - 100, y, 200, 20).build());
		y += 28;

		int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		int currentPage = Math.min(page, pages - 1);
		int from = currentPage * PAGE_SIZE;
		int to = Math.min(entries.size(), from + PAGE_SIZE);

		SimpleDateFormat format = new SimpleDateFormat("dd.MM HH:mm");
		for (int i = from; i < to; i++) {
			SkintrimConfig.HistoryEntry entry = entries.get(i);
			String label = labelOf(entry, format);
			this.addRenderableWidget(Button.builder(Component.literal(label), b -> applyEntry(entry))
					.bounds(centerX - 150, y, 300, 20).build());
			y += 24;
		}

		int bottom = this.height - 52;
		if (pages > 1) {
			this.addRenderableWidget(Button.builder(Component.literal("<"), b -> switchTo(capes, Math.max(0, currentPage - 1)))
					.bounds(centerX - 150, bottom, 24, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal(">"), b -> switchTo(capes, Math.min(pages - 1, currentPage + 1)))
					.bounds(centerX + 126, bottom, 24, 20).build());
		}

		this.addRenderableWidget(Button.builder(Component.translatable("skintrim.history.clear"), b -> {
			if (capes) {
				config.clearCapeHistory();
			} else {
				config.clearSkinHistory();
			}
			switchTo(capes, 0);
		}).bounds(centerX - 100, bottom + 24, 98, 20).build());

		this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
				.bounds(centerX + 2, bottom + 24, 98, 20).build());
	}

	private static List<SkintrimConfig.HistoryEntry> nullSafe(List<SkintrimConfig.HistoryEntry> list) {
		return list == null ? List.of() : list;
	}

	private String labelOf(SkintrimConfig.HistoryEntry entry, SimpleDateFormat format) {
		String input = entry.input;
		String shortInput = input;
		int slash = Math.max(shortInput.lastIndexOf('/'), shortInput.lastIndexOf('\\'));
		if (slash >= 0 && slash < shortInput.length() - 1) {
			shortInput = shortInput.substring(slash + 1);
		}
		if (shortInput.length() > 26) {
			shortInput = shortInput.substring(0, 23) + "...";
		}
		StringBuilder label = new StringBuilder(shortInput);
		if (!capes) {
			label.append(" · ").append(Component.translatable("skintrim.model." + entry.model).getString());
		}
		if (entry.time > 0) {
			label.append(" · ").append(format.format(new Date(entry.time)));
		}
		return label.toString();
	}

	private void applyEntry(SkintrimConfig.HistoryEntry entry) {
		SkintrimConfig config = SkintrimConfig.getInstance();
		String source = entry.input;
		if (entry.file != null && !entry.file.isBlank() && Files.isRegularFile(Path.of(entry.file))) {
			source = entry.file;
		}

		setStatus(Component.translatable("skintrim.status.loading"), 0xFFFF55);
		if (capes) {
			config.capeInput = source;
			config.save();
			TextureStore.applyLocalCape(source, (ok, msg) -> {
				if (ok) {
					SkintrimClient.sendSyncToServer();
					setStatus(Component.translatable("skintrim.status.cape_applied"), 0x55FF55);
				} else {
					setStatus(Component.translatable("skintrim.status.error", msg), 0xFF5555);
				}
			});
		} else {
			config.skinInput = source;
			config.skinModel = entry.model;
			config.save();
			TextureStore.applyLocalSkin(source, entry.model, (ok, msg) -> {
				if (ok) {
					SkintrimClient.sendSyncToServer();
					setStatus(Component.translatable("skintrim.status.skin_applied"), 0x55FF55);
				} else {
					setStatus(Component.translatable("skintrim.status.error", msg), 0xFF5555);
				}
			});
		}
	}

	private void setStatus(Component status, int color) {
		this.status = status;
		this.statusColor = color;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFF);

		SkintrimConfig config = SkintrimConfig.getInstance();
		List<SkintrimConfig.HistoryEntry> entries = capes ? nullSafe(config.capeHistory) : nullSafe(config.skinHistory);
		if (entries.isEmpty()) {
			graphics.centeredText(this.font, Component.translatable("skintrim.history.empty"), this.width / 2, 90, 0xAAAAAA);
		}

		if (!status.getString().isEmpty()) {
			graphics.textWithWordWrap(this.font, status, this.width / 2 - 190, this.height - 74, 380, statusColor);
		}
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.gui.setScreen(parent);
		}
	}
}
