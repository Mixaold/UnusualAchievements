package dev.semisaint.unusualachievements.client.screen;

import dev.semisaint.unusualachievements.client.config.ModConfigManager;
import dev.semisaint.unusualachievements.client.keybind.ModKeyBindings;
import dev.semisaint.unusualachievements.client.overlay.UnlockOverlayState;
import dev.semisaint.unusualachievements.core.CardTheme;
import dev.semisaint.unusualachievements.fabric.network.CardResponsePayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class AchievementCardScreen extends Screen {
	// TODO: replace once the real theme art canvas size is finalized.
	private static final int CARD_CANVAS_WIDTH = 260;
	private static final int CARD_CANVAS_HEIGHT = 210;
	private static final int AVATAR_SIZE = 32;
	private static final int MARGIN = 8;
	private static final int TITLE_AREA_HEIGHT = 18;
	private static final int FOOTER_HEIGHT = 26;

	private static final int PANEL_BG_RGB = 0x101010;
	private static final int PANEL_BG_BASE_ALPHA = 0xC0;
	/**
	 * Darkening laid over a theme. Lighter than it used to be: each achievement now carries its own
	 * plate, so this only has to keep the header and nickname legible instead of the whole card, and
	 * the art gets to show through.
	 */
	private static final int SCRIM_ALPHA = 0x70;
	private static final int THEMES_BUTTON_WIDTH = 58;
	private static final long FADE_IN_MS = 220;

	private final CardResponsePayload data;
	/**
	 * Reset by added(), not fixed at construction: coming back from the theme picker re-shows this
	 * screen, and without restarting the fade the card snapped in at full opacity the instant the
	 * picker finished fading out - a hard cut in the middle of an otherwise animated transition.
	 */
	private long openedAtMs = System.currentTimeMillis();
	private int cardLeft;
	private int cardTop;
	/** Mutable so picking a theme redraws immediately, without waiting for a fresh card from the server. */
	private String themeId;
	/** Earned-but-unopened themes - drives the notification badge on the Themes button. */
	private int unseenThemeCount;
	private int themesButtonX;
	private int themesButtonY;
	private boolean openSoundPlayed;

	public AchievementCardScreen(CardResponsePayload data) {
		super(Component.literal(data.nickname().isEmpty() ? "Achievements" : data.nickname()));
		this.data = data;
		this.themeId = data.themeId();
	}

	private boolean isOwnCard() {
		return minecraft.player != null && minecraft.player.getUUID().equals(data.playerId());
	}

	private java.util.Set<String> unlockedIds() {
		return data.unlocked().stream()
			.map(CardResponsePayload.UnlockedEntry::id)
			.collect(java.util.stream.Collectors.toSet());
	}

	/** Called by Gui.setScreen every time this screen is put on screen - including on the way back. */
	@Override
	public void added() {
		openedAtMs = System.currentTimeMillis();
	}

	@Override
	protected void init() {
		cardLeft = (width - CARD_CANVAS_WIDTH) / 2;
		cardTop = (height - CARD_CANVAS_HEIGHT) / 2;

		int avatarTop = cardTop + TITLE_AREA_HEIGHT;
		addRenderableWidget(new PlayerAvatarWidget(cardLeft + MARGIN, avatarTop, AVATAR_SIZE, data.playerId()));

		if (!data.unknownPlayer()) {
			int listTop = avatarTop + AVATAR_SIZE + MARGIN;
			int listBottom = cardTop + CARD_CANVAS_HEIGHT - FOOTER_HEIGHT;
			int listHeight = listBottom - listTop;
			addRenderableWidget(new AchievementEntryListWidget(
				minecraft, cardLeft + MARGIN, listTop, CARD_CANVAS_WIDTH - MARGIN * 2, listHeight, data.unlocked(),
				data.totalPlayers()
			));
		}

		addRenderableWidget(Button.builder(Component.translatable("screen.unusualachievements.card.close"), button -> onClose())
			.pos(cardLeft + CARD_CANVAS_WIDTH - 60 - MARGIN, cardTop + CARD_CANVAS_HEIGHT - FOOTER_HEIGHT + 4)
			.width(52)
			.build());

		// Only your own card offers the wardrobe - you can look at someone else's theme, not change it.
		if (isOwnCard()) {
			// Counts only themes that have been earned but never looked at, so the badge reads as
			// "something new is in there" rather than as a running total that never goes away.
			java.util.Set<String> unlocked = unlockedIds();
			java.util.List<String> seen = ModConfigManager.get().seenThemes;
			unseenThemeCount = 0;
			for (CardTheme theme : CardTheme.all()) {
				if (unlocked.contains(theme.requiredAchievement().path()) && !seen.contains(theme.id())) {
					unseenThemeCount++;
				}
			}
			themesButtonX = cardLeft;
			themesButtonY = cardTop + CARD_CANVAS_HEIGHT - FOOTER_HEIGHT + 4;
			addRenderableWidget(Button.builder(Component.translatable("screen.unusualachievements.card.themes"),
					button -> minecraft.gui.setScreen(new ThemePickerScreen(this, themeId, unlockedIds(), id -> themeId = id)))
				.pos(themesButtonX, themesButtonY)
				.width(THEMES_BUTTON_WIDTH)
				.build());
		}

		addRenderableWidget(Button.builder(Component.literal("?"), button -> minecraft.gui.setScreen(AboutScreen.forCard(this)))
			.pos(cardLeft + CARD_CANVAS_WIDTH - 16, cardTop - 4)
			.size(16, 16)
			.build());

		UnlockOverlayState.clear();
		// init() runs again on every resize and whenever we come back from the theme picker, so the
		// page-turn has to be tied to the card actually opening rather than to the layout being built.
		if (!openSoundPlayed) {
			openSoundPlayed = true;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.4f));
		}
	}

	/**
	 * The card's own key closes it again. This has to live on the screen: vanilla only feeds key
	 * mappings while no screen is open, so a KeyMapping.consumeClick() poll outside never fires here.
	 */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (ModKeyBindings.OPEN_CARD.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	/**
	 * Leaving the menu is instant, by request - no fade-out, no tick() waiting one out. This is the
	 * one exit that puts you back in the world (Close, Esc and the card's own key all land here), and
	 * an animation between "I want out" and being out just reads as lag. Moving *between* screens is
	 * a different thing and keeps its transition: those go through setScreen() directly, not here.
	 */
	@Override
	public void onClose() {
		minecraft.gui.setScreen(null);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		float t = Math.min(1f, (System.currentTimeMillis() - openedAtMs) / (float) FADE_IN_MS);
		renderContent(graphics, mouseX, mouseY, a, (int) (easeOutCubic(t) * 255));
	}

	private static float easeOutCubic(float t) {
		float p = 1f - t;
		return 1f - p * p * p;
	}

	private void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, int alpha) {
		int panelAlpha = alpha * PANEL_BG_BASE_ALPHA / 255;

		int panelLeft = cardLeft - MARGIN;
		int panelTop = cardTop - MARGIN;
		int panelRight = cardLeft + CARD_CANVAS_WIDTH + MARGIN;
		int panelBottom = cardTop + CARD_CANVAS_HEIGHT + MARGIN;
		graphics.fill(panelLeft, panelTop, panelRight, panelBottom, (panelAlpha << 24) | PANEL_BG_RGB);

		// The theme fills the whole panel, edge to edge. Drawing it at the canvas size left a dark
		// border ring around it, which read as a picture pasted inside the window rather than as the
		// window's own background.
		CardThemeArt.draw(graphics, themeId, panelLeft, panelTop, panelRight - panelLeft, panelBottom - panelTop,
			(alpha << 24) | 0xFFFFFF);
		if (CardThemeArt.hasArt(themeId)) {
			// Art this busy swallows small text, so everything above it sits on a scrim. Kept dark
			// rather than opaque: the theme still reads, the words stay first.
			graphics.fill(panelLeft, panelTop, panelRight, panelBottom, (alpha * SCRIM_ALPHA / 255) << 24);
		}

		graphics.text(font, Component.translatable("screen.unusualachievements.card.title"),
			cardLeft, cardTop, (alpha << 24) | 0xE0E0E0, true);

		super.extractRenderState(graphics, mouseX, mouseY, a);

		int avatarTop = cardTop + TITLE_AREA_HEIGHT;
		// After super's pass, so it sits on top of the head it frames rather than under it.
		CardThemeArt.drawFrame(graphics, themeId, cardLeft + MARGIN, avatarTop, AVATAR_SIZE,
			(alpha << 24) | 0xFFFFFF);
		drawThemesBadge(graphics, alpha);

		int nameX = cardLeft + MARGIN + AVATAR_SIZE + MARGIN;
		int nameY = avatarTop + AVATAR_SIZE / 2 - font.lineHeight / 2;
		String nickname = data.nickname().isEmpty() ? "?" : data.nickname();
		graphics.text(font, Component.literal(nickname), nameX, nameY, (alpha << 24) | 0xFFFFFF, true);

		int messageY = avatarTop + AVATAR_SIZE + MARGIN;
		if (data.unknownPlayer()) {
			graphics.text(font, Component.translatable("screen.unusualachievements.card.unknown_player"),
				cardLeft + MARGIN, messageY, (alpha << 24) | 0xCCCCCC, true);
		} else if (data.unlocked().isEmpty()) {
			graphics.text(font, Component.translatable("screen.unusualachievements.card.no_achievements"),
				cardLeft + MARGIN, messageY, (alpha << 24) | 0xCCCCCC, true);
		}
	}

	/**
	 * A count badge on the corner of the Themes button. Without it the button looks the same whether
	 * you have earned nothing or half the set, so there is no way to tell from the card that anything
	 * is waiting in there.
	 */
	private void drawThemesBadge(GuiGraphicsExtractor graphics, int alpha) {
		if (unseenThemeCount <= 0) {
			return;
		}
		Component label = Component.literal(Integer.toString(unseenThemeCount));
		int textWidth = font.width(label);
		int badgeWidth = Math.max(textWidth + 6, 11);
		int badgeHeight = 11;
		// Sits on the button's top-right corner, half overhanging it, the way a notification dot does.
		int left = themesButtonX + THEMES_BUTTON_WIDTH - badgeWidth + 3;
		int top = themesButtonY - 4;

		graphics.fill(left - 1, top - 1, left + badgeWidth + 1, top + badgeHeight + 1, (alpha << 24) | 0x1A0A08);
		graphics.fill(left, top, left + badgeWidth, top + badgeHeight, (alpha << 24) | 0xC42B22);
		graphics.fill(left, top, left + badgeWidth, top + 1, (alpha << 24) | 0xE8574C);
		graphics.text(font, label, left + (badgeWidth - textWidth) / 2, top + 2, (alpha << 24) | 0xFFFFFF, false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
