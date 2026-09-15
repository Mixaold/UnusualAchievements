package dev.semisaint.unusualachievements.client.screen;

import dev.semisaint.unusualachievements.fabric.network.CardResponsePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class AchievementEntryListWidget extends ObjectSelectionList<AchievementEntryListWidget.Entry> {
	/**
	 * Rows size themselves (every addEntry() below passes its own height), so this only exists for the
	 * one other thing the superclass derives from it: the scrollbar settings are built as
	 * defaultSettings(defaultEntryHeight / 2), and that half becomes scrollRate - the pixels-per-notch
	 * the wheel moves. Passing 0 here left scrollRate at 0, so mouseScrolled() scrolled by zero while
	 * still reporting the event as handled: the bar rendered, arrow keys worked (they scroll via
	 * scrollToEntry(), not scrollRate), and the wheel did nothing at all.
	 */
	private static final int SCROLL_RATE_BASIS = 24;

	public AchievementEntryListWidget(
		Minecraft minecraft, int x, int y, int width, int height,
		List<CardResponsePayload.UnlockedEntry> entries, int totalPlayers
	) {
		super(minecraft, width, height, y, SCROLL_RATE_BASIS);
		// addEntry() bakes in the row's x from getRowLeft() (which reads this.getX()) at the moment
		// each entry is added - setX() must happen before that or every row ends up positioned as if
		// the list were still at its default x=0, and gets scissor-clipped away entirely.
		this.setX(x);
		// Text is wrapped well short of the row so it sits clearly inside the dark plate drawn behind
		// it, instead of running right up to - and visually past - its edge.
		int wrapWidth = getRowWidth() - 14;
		int index = 0;
		for (CardResponsePayload.UnlockedEntry entry : entries) {
			Entry e = new Entry(minecraft, entry, index, wrapWidth, totalPlayers);
			this.addEntry(e, e.height());
			index++;
		}
	}

	@Override
	public int getRowWidth() {
		// AbstractSelectionList centers rows, then puts the scrollbar OUTSIDE them, at
		// getRowRight() + scrollbarWidth() + 2. Vanilla lists keep rows well narrower than the widget,
		// so that still lands inside; with near-full-width rows it landed past our own right edge,
		// where isMouseOver() rejects the click outright - the bar drew but could never be grabbed
		// (the wheel kept working, since the cursor is over the list body when you use it).
		return this.getWidth() - 4 * scrollbarWidth() - 8;
	}

	public static final class Entry extends ObjectSelectionList.Entry<Entry> {
		private static final long STAGGER_DELAY_MS = 70L;
		private static final long FADE_DURATION_MS = 260L;
		private static final int SLIDE_DISTANCE = 10;
		/** Opacity of the plate each row sits on, so text never has to compete with the theme art. */
		private static final int PLATE_ALPHA = 0xB8;
		private static final int PLATE_EDGE_ALPHA = 0x66;
		/**
		 * Breathing room between the plate's edge and the first line of text. The entry's own
		 * getContentY() only leaves 2px, which put the title hard against the plate's lit top edge and
		 * made it read as sticking out of the panel.
		 */
		private static final int TEXT_TOP_PAD = 5;
		private static final int TEXT_BOTTOM_PAD = 7;

		private final Minecraft minecraft;
		private final long createdAtMs = System.currentTimeMillis();
		private final int index;
		private final String name;

		private final List<FormattedCharSequence> nameLines;
		private final List<FormattedCharSequence> descLines;
		private final List<FormattedCharSequence> flavorLines;
		private final List<FormattedCharSequence> rarityLines;
		private final int rowHeight;

		Entry(Minecraft minecraft, CardResponsePayload.UnlockedEntry data, int index, int wrapWidth,
		      int totalPlayers) {
			this.minecraft = minecraft;
			this.index = index;
			this.name = data.secretName();

			this.nameLines = minecraft.font.split(Component.literal(data.secretName()), wrapWidth);
			this.descLines = minecraft.font.split(Component.literal(data.secretDescription()), wrapWidth);
			this.flavorLines = data.flavorText().isEmpty()
				? List.of()
				: minecraft.font.split(Component.literal(data.flavorText()).withStyle(ChatFormatting.ITALIC), wrapWidth);
			this.rarityLines = buildRarity(minecraft, data, wrapWidth, totalPlayers);

			int totalLines = nameLines.size() + descLines.size() + flavorLines.size() + rarityLines.size();
			this.rowHeight = totalLines * minecraft.font.lineHeight + TEXT_TOP_PAD + TEXT_BOTTOM_PAD;
		}

		/**
		 * "One time in 3418 kills" - the achievement's own frequency inside this player's game, taken
		 * from the vanilla counters, plus how many players on the server hold it once there is more
		 * than one player for that to mean anything.
		 */
		private static List<FormattedCharSequence> buildRarity(Minecraft minecraft, CardResponsePayload.UnlockedEntry data,
		                                                       int wrapWidth, int totalPlayers) {
			if (data.rarityKey().isEmpty()) {
				return List.of();
			}
			String line = Component.translatable(data.rarityKey(), formatCount(data.rarityValue())).getString();
			if (data.rarityPercentable()) {
				line += Component.translatable("rarity.unusualachievements.percent",
					formatPercent(100.0 / data.rarityValue())).getString();
			}
			if (totalPlayers > 1) {
				line += Component.translatable("rarity.unusualachievements.among_players",
					data.rarityCount(), totalPlayers).getString();
			}
			return minecraft.font.split(Component.literal(line), wrapWidth);
		}

		/**
		 * Keeps enough decimals for the number to stay meaningful as it gets small - a flat "%.1f"
		 * would render every rare achievement as a uniform, useless "0,0%".
		 */
		private static String formatPercent(double percent) {
			String text;
			if (percent >= 10) {
				text = String.format("%.0f", percent);
			} else if (percent >= 1) {
				text = String.format("%.1f", percent);
			} else if (percent >= 0.1) {
				text = String.format("%.2f", percent);
			} else {
				text = String.format("%.3f", percent);
			}
			return text.replace('.', ',');
		}

		/** Thin spaces every three digits: five-figure kill counts are unreadable as one run. */
		private static String formatCount(long value) {
			String digits = Long.toString(value);
			StringBuilder out = new StringBuilder();
			for (int i = 0; i < digits.length(); i++) {
				if (i > 0 && (digits.length() - i) % 3 == 0) {
					out.append(' ');
				}
				out.append(digits.charAt(i));
			}
			return out.toString();
		}

		int height() {
			return rowHeight;
		}

		/**
		 * Stagger-in only. The rows used to mirror it as a stagger-out while the card faded away, but
		 * the card now leaves the moment you ask it to, so there is no fade left to play that over.
		 */
		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			long elapsed = System.currentTimeMillis() - createdAtMs - index * STAGGER_DELAY_MS;
			float t = clamp01(elapsed / (float) FADE_DURATION_MS);
			if (t <= 0f) {
				return;
			}
			float alphaT = easeOutCubic(t);
			int yOffset = (int) ((1f - alphaT) * SLIDE_DISTANCE);
			draw(graphics, alphaT, yOffset);
		}

		private void draw(GuiGraphicsExtractor graphics, float alphaT, int yOffset) {
			int alpha = (int) (alphaT * 255);
			graphics.pose().pushMatrix();
			graphics.pose().translate(0, yOffset);

			// One plate behind the whole achievement - title, description and flavour together - rather
			// than a strip per line. The theme art underneath is busy enough that unbacked text sinks
			// into it, and per-line strips would read as a barcode.
			int left = getX() - 3;
			int right = getX() + getWidth() + 3;
			int top = getY();
			int bottom = getY() + getHeight() - 2;
			graphics.fill(left, top, right, bottom, (alpha * PLATE_ALPHA / 255) << 24);
			graphics.fill(left, top, right, top + 1, (alpha * PLATE_EDGE_ALPHA / 255) << 24 | 0xFFFFFF);

			int y = getY() + TEXT_TOP_PAD;
			y = drawLines(graphics, nameLines, y, (alpha << 24) | 0xFFD700);
			y = drawLines(graphics, descLines, y, (alpha << 24) | 0xAAAAAA);
			y = drawLines(graphics, flavorLines, y, (alpha << 24) | 0x8FA8B8);
			drawLines(graphics, rarityLines, y, (alpha << 24) | 0x77A06B);
			graphics.pose().popMatrix();
		}

		private int drawLines(GuiGraphicsExtractor graphics, List<FormattedCharSequence> lines, int y, int color) {
			for (FormattedCharSequence line : lines) {
				// Shadowed: rows sit directly on the card's theme art, and unshadowed text disappears
				// into the busier ones (the bookshelf and the copper plate especially).
				graphics.text(minecraft.font, line, getContentX(), y, color, true);
				y += minecraft.font.lineHeight;
			}
			return y;
		}

		private static float clamp01(float v) {
			return Math.max(0f, Math.min(1f, v));
		}

		private static float easeOutCubic(float t) {
			float p = 1f - t;
			return 1f - p * p * p;
		}

		@Override
		public Component getNarration() {
			return Component.literal(name);
		}
	}
}
