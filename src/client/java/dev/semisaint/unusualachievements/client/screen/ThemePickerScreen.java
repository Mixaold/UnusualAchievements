package dev.semisaint.unusualachievements.client.screen;

import dev.semisaint.unusualachievements.client.config.ModConfigManager;
import dev.semisaint.unusualachievements.core.CardTheme;
import dev.semisaint.unusualachievements.fabric.network.SelectThemePayload;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The wardrobe: every theme in the mod, in one grid. Locked ones are still shown - dimmed, padlocked
 * and named - because a theme you cannot equip yet is a hint that something is out there to find,
 * which is the whole point of a mod built on hidden achievements.
 */
public final class ThemePickerScreen extends Screen {
	private static final int TILE_W = 78;
	private static final int TILE_H = 63;
	private static final int GAP = 8;
	private static final int COLUMNS = 5;

	/** Side padding between the grid and the panel edge. */
	private static final int PANEL_PAD = 14;
	/** Room above the grid for the title, and below it for the buttons. */
	private static final int TITLE_AREA = 28;
	private static final int FOOTER_AREA = 36;
	private static final int PANEL_BG_RGB = 0x101010;
	private static final int PANEL_BG_BASE_ALPHA = 0xC0;
	private static final int PANEL_EDGE_RGB = 0x3A3A3A;

	private static final int LOCKED_TINT = 0xFF4A4A4A;
	private static final int SELECTED_BORDER_RGB = 0xFFD700;
	private static final int HOVER_BORDER_RGB = 0xB8B8B8;
	private static final int IDLE_BORDER_RGB = 0x2A2A2A;

	private static final long FADE_IN_MS = 220;
	private static final long CLOSE_MS = 220;
	/** Ring-and-flash on the tile you just equipped: long enough to read, short enough to stay out of the way. */
	private static final long SELECT_ANIM_MS = 420;
	private static final int SELECT_RING_TRAVEL = 9;

	private static final Guard.Site SELECT_SITE = new Guard.Site("theme select click");

	private final Screen parent;
	private final Set<String> unlockedAchievements;
	private final Consumer<String> onThemeChosen;
	private final List<Tile> tiles = new ArrayList<>();
	/** Restarted by added(), so returning from the "?" panel fades the grid back in instead of cutting to it. */
	private long openedAtMs = System.currentTimeMillis();
	private String selectedThemeId;
	private Tile hovered;
	private boolean closing;
	private long closeStartMs;
	private boolean openSoundPlayed;
	/** The tile whose pick animation is still running, and when it started. Null once it has played out. */
	private Tile selectAnimTile;
	private long selectAnimStartMs;

	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;

	public ThemePickerScreen(Screen parent, String selectedThemeId, Set<String> unlockedAchievements,
	                         Consumer<String> onThemeChosen) {
		super(Component.translatable("screen.unusualachievements.themes.title"));
		this.parent = parent;
		this.selectedThemeId = selectedThemeId;
		this.unlockedAchievements = unlockedAchievements;
		this.onThemeChosen = onThemeChosen;
	}

	@Override
	public void added() {
		openedAtMs = System.currentTimeMillis();
	}

	@Override
	protected void init() {
		tiles.clear();
		List<Tile> pending = new ArrayList<>();
		pending.add(new Tile(CardTheme.NONE, true));
		for (CardTheme theme : CardTheme.all()) {
			pending.add(new Tile(theme.id(), unlockedAchievements.contains(theme.requiredAchievement().path())));
		}

		int rows = (pending.size() + COLUMNS - 1) / COLUMNS;
		int gridWidth = COLUMNS * TILE_W + (COLUMNS - 1) * GAP;
		int rowPitch = TILE_H + GAP + font.lineHeight;
		int gridHeight = rows * rowPitch - GAP;

		panelWidth = gridWidth + PANEL_PAD * 2;
		panelHeight = TITLE_AREA + gridHeight + FOOTER_AREA;
		// Clamped rather than centred blindly: on a small GUI scale the panel would otherwise hang off
		// the top, which is exactly the edge the backdrop is no longer allowed to run past.
		panelLeft = Math.max(0, (width - panelWidth) / 2);
		panelTop = Math.max(0, (height - panelHeight) / 2);

		int gridLeft = panelLeft + PANEL_PAD;
		int gridTop = panelTop + TITLE_AREA;
		for (int i = 0; i < pending.size(); i++) {
			Tile tile = pending.get(i);
			tile.x = gridLeft + (i % COLUMNS) * (TILE_W + GAP);
			tile.y = gridTop + (i / COLUMNS) * rowPitch;
			tiles.add(tile);
		}

		markEarnedThemesSeen();

		// Clamped to the viewport, not just to the panel: at GUI scale 4 the grid is taller than the
		// screen, and a Close button parked below the bottom edge is a button nobody can press.
		addRenderableWidget(Button.builder(Component.translatable("screen.unusualachievements.card.close"), b -> onClose())
			.pos(panelLeft + panelWidth / 2 - 40, Math.min(panelTop + panelHeight - 28, height - 26))
			.width(80)
			.build());

		// Same affordance as the card's corner "?" - the picker is a screen of its own, and "where do
		// these even come from" is the first thing it raises.
		addRenderableWidget(Button.builder(Component.literal("?"), b -> minecraft.gui.setScreen(AboutScreen.forThemes(this)))
			.pos(panelLeft + panelWidth - 20, panelTop + 4)
			.size(16, 16)
			.build());

		// init() runs again on resize and on the way back from the "?" panel, so the open sound is tied
		// to the screen instance rather than to the layout being rebuilt - same reasoning as the card.
		if (!openSoundPlayed) {
			openSoundPlayed = true;
			playUi(SoundEvents.BOOK_PAGE_TURN, 1.15f, 0.35f);
		}
	}

	/**
	 * Opening this screen is the act of looking, so every theme currently earned counts as seen and
	 * the badge on the Themes button goes dark. Earning a new one later puts it back - it will not be
	 * in this list. Unlike the one-time key hint, there is no animation to wait out here: if the
	 * screen was built, the grid is on screen this frame.
	 */
	private void markEarnedThemesSeen() {
		List<String> seen = ModConfigManager.get().seenThemes;
		boolean changed = false;
		for (Tile tile : tiles) {
			if (tile.unlocked && !CardTheme.NONE.equals(tile.themeId) && !seen.contains(tile.themeId)) {
				seen.add(tile.themeId);
				changed = true;
			}
		}
		if (changed) {
			ModConfigManager.save();
		}
	}

	@Override
	public void onClose() {
		if (closing) {
			return;
		}
		closing = true;
		closeStartMs = System.currentTimeMillis();
		playUi(SoundEvents.BOOK_PAGE_TURN, 0.85f, 0.35f);
	}

	@Override
	public void tick() {
		if (closing && System.currentTimeMillis() - closeStartMs >= CLOSE_MS) {
			minecraft.gui.setScreen(parent);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		Guard.run(SELECT_SITE, () -> {
			if (closing) {
				return;
			}
			Tile tile = tileAt(event.x(), event.y());
			if (tile == null) {
				return;
			}
			// Every tile answers back. Silence on a locked or already-worn theme reads as a dead
			// button, which is what made the grid feel unresponsive.
			if (!tile.unlocked) {
				playUi(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 0.5f);
				return;
			}
			if (tile.themeId.equals(selectedThemeId)) {
				playUi(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.35f);
				return;
			}
			selectedThemeId = tile.themeId;
			selectAnimTile = tile;
			selectAnimStartMs = System.currentTimeMillis();
			playUi(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0f, 0.9f);
			onThemeChosen.accept(tile.themeId);
			ClientPlayNetworking.send(new SelectThemePayload(tile.themeId));
		});
		return super.mouseClicked(event, doubleClick);
	}

	private void playUi(net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	private Tile tileAt(double mouseX, double mouseY) {
		for (Tile tile : tiles) {
			if (mouseX >= tile.x && mouseX < tile.x + TILE_W && mouseY >= tile.y && mouseY < tile.y + TILE_H) {
				return tile;
			}
		}
		return null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		float alphaT;
		if (closing) {
			float t = Math.min(1f, (System.currentTimeMillis() - closeStartMs) / (float) CLOSE_MS);
			alphaT = 1f - easeInCubic(t);
		} else {
			float t = Math.min(1f, (System.currentTimeMillis() - openedAtMs) / (float) FADE_IN_MS);
			alphaT = easeOutCubic(t);
		}
		int alpha = (int) (alphaT * 255);

		// A bounded panel, never a viewport-sized fill. The old full-screen scrim was drawn in the
		// content pass, so a GUI-animation mod (easegui / smoothgui are both in the target pack)
		// slid and scaled it along with the rest of the screen - and a screen-sized rectangle that
		// moves stops covering the screen, baring the undarkened world along the top edge. Vanilla
		// already darkens and blurs the world in its own background stratum; the panel only has to
		// cover itself.
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight,
			((alpha * PANEL_BG_BASE_ALPHA / 255) << 24) | PANEL_BG_RGB);
		graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, (alpha << 24) | PANEL_EDGE_RGB);

		graphics.centeredText(font, getTitle(), panelLeft + panelWidth / 2, panelTop + 10, (alpha << 24) | 0xE0E0E0);

		hovered = closing ? null : tileAt(mouseX, mouseY);
		for (Tile tile : tiles) {
			drawTile(graphics, tile, alpha);
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);

		if (hovered != null) {
			drawTooltip(graphics, hovered, mouseX, mouseY, alpha);
		}
	}

	private static float easeOutCubic(float t) {
		float p = 1f - t;
		return 1f - p * p * p;
	}

	private static float easeInCubic(float t) {
		return t * t * t;
	}

	/**
	 * Hover text: the theme's full name (the grid label is cut to the tile, so long ones are only
	 * readable here) and, when it is locked, the line saying so.
	 */
	private void drawTooltip(GuiGraphicsExtractor graphics, Tile tile, int mouseX, int mouseY, int alpha) {
		List<Component> lines = new ArrayList<>(2);
		Component name = themeName(tile);
		if (font.width(name) > TILE_W) {
			lines.add(name);
		}
		if (!tile.unlocked) {
			lines.add(Component.translatable("screen.unusualachievements.themes.locked"));
		}
		if (lines.isEmpty()) {
			return;
		}

		int boxWidth = 0;
		for (Component line : lines) {
			boxWidth = Math.max(boxWidth, font.width(line));
		}
		int boxHeight = lines.size() * font.lineHeight;
		int tipX = Math.min(mouseX + 8, width - boxWidth - 6);
		int tipY = Math.max(mouseY - 4 - boxHeight, 2);

		graphics.fill(tipX - 3, tipY - 3, tipX + boxWidth + 3, tipY + boxHeight + 2, (alpha * 0xE0 / 255) << 24);
		int y = tipY;
		for (int i = 0; i < lines.size(); i++) {
			// The name reads as a label, the locked line as a warning - so they do not run together.
			int color = (!tile.unlocked && i == lines.size() - 1) ? 0xE8C46A : 0xE0E0E0;
			graphics.text(font, lines.get(i), tipX, y, (alpha << 24) | color);
			y += font.lineHeight;
		}
	}

	private static Component themeName(Tile tile) {
		return CardTheme.NONE.equals(tile.themeId)
			? Component.translatable("screen.unusualachievements.themes.none")
			: Component.translatable("theme.unusualachievements." + tile.themeId);
	}

	private void drawTile(GuiGraphicsExtractor graphics, Tile tile, int alpha) {
		boolean selected = tile.themeId.equals(selectedThemeId);
		float pick = selectProgress(tile);

		int borderRgb = selected ? SELECTED_BORDER_RGB : (tile == hovered ? HOVER_BORDER_RGB : IDLE_BORDER_RGB);
		// The gold frame thickens for the length of the pick animation and settles back to 2px, so
		// choosing a theme is something you see happen rather than something that is just true next frame.
		int inset = 2 + (pick > 0f ? Math.round(2f * (1f - easeOutCubic(pick))) : 0);
		graphics.fill(tile.x - inset, tile.y - inset, tile.x + TILE_W + inset, tile.y + TILE_H + inset,
			(alpha << 24) | borderRgb);

		if (CardTheme.NONE.equals(tile.themeId)) {
			graphics.fill(tile.x, tile.y, tile.x + TILE_W, tile.y + TILE_H, (alpha << 24) | 0x101010);
		} else {
			int artTint = tile.unlocked ? ((alpha << 24) | 0xFFFFFF) : ((alpha << 24) | (LOCKED_TINT & 0xFFFFFF));
			CardThemeArt.draw(graphics, tile.themeId, tile.x, tile.y, TILE_W, TILE_H, artTint);
		}

		if (!tile.unlocked) {
			graphics.fill(tile.x, tile.y, tile.x + TILE_W, tile.y + TILE_H, (alpha * 0x70 / 255) << 24);
			drawLock(graphics, tile.x + TILE_W / 2, tile.y + TILE_H / 2, alpha);
		}

		if (pick > 0f) {
			drawPickFlourish(graphics, tile, pick, alpha);
		}

		graphics.centeredText(font, ellipsized(themeName(tile)), tile.x + TILE_W / 2, tile.y + TILE_H + 3,
			(alpha << 24) | (tile.unlocked ? (selected ? SELECTED_BORDER_RGB : 0xD8D8D8) : 0x7A7A7A));
	}

	/**
	 * Labels are cut to the tile they belong to. Centred at full length, "Стол зачарований" and
	 * "Грибной остров" overhang their 78px tile by enough to collide with the name of the tile next to
	 * them, which read as two words printed on top of each other. The full name is in the tooltip.
	 */
	private Component ellipsized(Component name) {
		String text = name.getString();
		if (font.width(text) <= TILE_W) {
			return name;
		}
		String ellipsis = "...";
		String head = font.plainSubstrByWidth(text, TILE_W - font.width(ellipsis));
		return Component.literal(head.stripTrailing() + ellipsis);
	}

	/** 0 while nothing is animating, otherwise 0..1 across the pick animation. */
	private float selectProgress(Tile tile) {
		if (selectAnimTile != tile) {
			return 0f;
		}
		float t = (System.currentTimeMillis() - selectAnimStartMs) / (float) SELECT_ANIM_MS;
		if (t >= 1f) {
			selectAnimTile = null;
			return 0f;
		}
		return Math.max(t, 0.0001f);
	}

	/** A white wash that drains off the tile, and a gold ring travelling outward from its edge. */
	private void drawPickFlourish(GuiGraphicsExtractor graphics, Tile tile, float progress, int alpha) {
		int flashAlpha = (int) (alpha * 0.45f * (1f - progress));
		if (flashAlpha > 0) {
			graphics.fill(tile.x, tile.y, tile.x + TILE_W, tile.y + TILE_H, (flashAlpha << 24) | 0xFFFFFF);
		}

		int travel = Math.round(easeOutCubic(progress) * SELECT_RING_TRAVEL);
		int ringAlpha = (int) (alpha * (1f - progress));
		if (ringAlpha <= 0) {
			return;
		}
		int left = tile.x - 2 - travel;
		int top = tile.y - 2 - travel;
		int right = tile.x + TILE_W + 2 + travel;
		int bottom = tile.y + TILE_H + 2 + travel;
		int ring = (ringAlpha << 24) | SELECTED_BORDER_RGB;
		graphics.fill(left, top, right, top + 1, ring);
		graphics.fill(left, bottom - 1, right, bottom, ring);
		graphics.fill(left, top, left + 1, bottom, ring);
		graphics.fill(right - 1, top, right, bottom, ring);
	}

	/** A padlock drawn from fills - a 12px icon needs no texture, and this scales with the theme grid. */
	private void drawLock(GuiGraphicsExtractor graphics, int cx, int cy, int alpha) {
		int metal = (alpha << 24) | 0xDCDCDC;
		int shadow = (alpha << 24) | 0x2A2A2A;
		graphics.fill(cx - 5, cy - 9, cx + 5, cy - 7, metal);      // shackle top
		graphics.fill(cx - 5, cy - 7, cx - 3, cy - 2, metal);      // shackle left
		graphics.fill(cx + 3, cy - 7, cx + 5, cy - 2, metal);      // shackle right
		graphics.fill(cx - 8, cy - 3, cx + 8, cy + 8, shadow);     // body outline
		graphics.fill(cx - 7, cy - 2, cx + 7, cy + 7, metal);      // body
		graphics.fill(cx - 1, cy + 1, cx + 1, cy + 5, shadow);     // keyhole
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static final class Tile {
		private final String themeId;
		private final boolean unlocked;
		private int x;
		private int y;

		private Tile(String themeId, boolean unlocked) {
			this.themeId = themeId;
			this.unlocked = unlocked;
		}
	}
}
