package dev.semisaint.unusualachievements.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * The "?" panel behind every screen in the mod: a short, deliberately unhelpful explainer. The card
 * and the theme picker both open one, with their own body text - hence the body keys being a
 * constructor argument rather than a constant.
 */
public final class AboutScreen extends Screen {
	private static final int CONTENT_WIDTH = 260;
	private static final int PANEL_MARGIN = 12;
	private static final int PANEL_BG_RGB = 0x101010;
	private static final int PANEL_BG_BASE_ALPHA = 0xC0;
	private static final long FADE_IN_MS = 220;
	private static final long CLOSE_MS = 220;

	private static final String[] CARD_BODY_KEYS = {
		"screen.unusualachievements.about.line1",
		"screen.unusualachievements.about.line2",
		"screen.unusualachievements.about.line3",
		"screen.unusualachievements.about.line4"
	};

	private static final String[] THEMES_BODY_KEYS = {
		"screen.unusualachievements.themes.about.line1",
		"screen.unusualachievements.themes.about.line2",
		"screen.unusualachievements.themes.about.line3"
	};

	private final Screen parent;
	private final String[] bodyKeys;
	private final long openedAtMs = System.currentTimeMillis();
	private boolean closing;
	private long closeStartMs;
	/** Grows with the body text instead of being a fixed 150 - the themes page is a different length. */
	private int panelHeight;

	private AboutScreen(Screen parent, String titleKey, String[] bodyKeys) {
		super(Component.translatable(titleKey));
		this.parent = parent;
		this.bodyKeys = bodyKeys;
	}

	/** The card's "?" - what this mod is, and how to look at someone else's card. */
	public static AboutScreen forCard(Screen parent) {
		return new AboutScreen(parent, "screen.unusualachievements.about.title", CARD_BODY_KEYS);
	}

	/** The theme picker's "?" - where themes come from, without naming a single achievement. */
	public static AboutScreen forThemes(Screen parent) {
		return new AboutScreen(parent, "screen.unusualachievements.themes.about.title", THEMES_BODY_KEYS);
	}

	@Override
	protected void init() {
		panelHeight = measurePanelHeight();
		addRenderableWidget(Button.builder(Component.translatable("screen.unusualachievements.card.close"), button -> onClose())
			.pos(width / 2 - 40, contentTop() + panelHeight - 30)
			.width(80)
			.build());
	}

	private int contentTop() {
		return (height - panelHeight) / 2;
	}

	/** Same walk the renderer does, minus the drawing - so the panel is never shorter than its text. */
	private int measurePanelHeight() {
		int y = 16;
		for (String key : bodyKeys) {
			y += font.split(Component.translatable(key), CONTENT_WIDTH).size() * font.lineHeight + 6;
		}
		return y + 34;
	}

	@Override
	public void onClose() {
		if (closing) {
			return;
		}
		closing = true;
		closeStartMs = System.currentTimeMillis();
	}

	@Override
	public void tick() {
		if (closing && System.currentTimeMillis() - closeStartMs >= CLOSE_MS) {
			minecraft.gui.setScreen(parent);
		}
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
		renderContent(graphics, mouseX, mouseY, a, (int) (alphaT * 255));
	}

	private static float easeOutCubic(float t) {
		float p = 1f - t;
		return 1f - p * p * p;
	}

	private static float easeInCubic(float t) {
		return t * t * t;
	}

	private void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, int alpha) {
		int centerX = width / 2;
		int contentLeft = centerX - CONTENT_WIDTH / 2;
		int top = contentTop();
		int panelAlpha = alpha * PANEL_BG_BASE_ALPHA / 255;

		graphics.fill(contentLeft - PANEL_MARGIN, top - PANEL_MARGIN,
			contentLeft + CONTENT_WIDTH + PANEL_MARGIN, top + panelHeight, (panelAlpha << 24) | PANEL_BG_RGB);

		graphics.centeredText(font, getTitle(), centerX, top, (alpha << 24) | 0xE0E0E0);

		int y = top + 16;
		for (String key : bodyKeys) {
			y = drawWrapped(graphics, font, Component.translatable(key), contentLeft, y, CONTENT_WIDTH, (alpha << 24) | 0xCCCCCC);
			y += 6;
		}

		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	private static int drawWrapped(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int color) {
		List<FormattedCharSequence> lines = font.split(text, width);
		for (FormattedCharSequence line : lines) {
			graphics.text(font, line, x, y, color);
			y += font.lineHeight;
		}
		return y;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
