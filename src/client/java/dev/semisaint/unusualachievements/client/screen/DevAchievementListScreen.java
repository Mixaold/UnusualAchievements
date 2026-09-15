package dev.semisaint.unusualachievements.client.screen;

import dev.semisaint.unusualachievements.core.AchievementDefinition;
import dev.semisaint.unusualachievements.core.AchievementRegistry;
import dev.semisaint.unusualachievements.core.AchievementRule;
import dev.semisaint.unusualachievements.core.CustomEventRule;
import dev.semisaint.unusualachievements.core.LocalizedText;
import dev.semisaint.unusualachievements.core.StatThresholdRule;
import dev.semisaint.unusualachievements.fabric.network.DevForceUnlockPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Dev-only cheat screen - dumps every registered achievement (locked or not) with its unlock
 * condition, reading straight from the local AchievementRegistry. Only reachable via the client
 * command in DevCommands; never shown to a regular player through normal play.
 *
 * <p>Built on ObjectSelectionList - the same widget AchievementEntryListWidget already uses for
 * the real card - rather than hand-rolled scroll/click math like the first version of this screen.
 * That first version's manual mouseScrolled override compiled fine but silently never fixed
 * anything real: the list needs proper scroll (and picks up whatever scroll-smoothing the
 * player's other UI mods already apply to standard list widgets) for free this way instead.
 *
 * <p>Click a row to force-unlock that achievement for QA (requires operator permission
 * server-side - see DevForceUnlockPayload).
 */
public final class DevAchievementListScreen extends Screen {
	private static final int MARGIN = 20;
	private static final int LIST_TOP = MARGIN + 14;
	private static final int FOOTER_HEIGHT = 24;

	public DevAchievementListScreen() {
		super(Component.literal("Achievement Pool (dev)"));
	}

	@Override
	protected void init() {
		String locale = minecraft.options.languageCode;
		int listHeight = height - LIST_TOP - FOOTER_HEIGHT;
		EntryList list = new EntryList(minecraft, width, listHeight, LIST_TOP, AchievementRegistry.all(), locale);
		addRenderableWidget(list);

		addRenderableWidget(Button.builder(Component.translatable("screen.unusualachievements.card.close"), button -> onClose())
			.pos(width / 2 - 40, height - MARGIN)
			.width(80)
			.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.text(font, getTitle(), MARGIN, MARGIN, 0xFFFFFFFF);
	}

	private static String conditionText(AchievementRule rule) {
		if (rule instanceof StatThresholdRule stat) {
			return "condition: vanilla stat \"" + stat.vanillaCustomStatPath() + "\" >= " + stat.threshold();
		}
		if (rule instanceof CustomEventRule custom) {
			return "condition: " + custom.description();
		}
		return "condition: unknown";
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static final class EntryList extends ObjectSelectionList<EntryList.Row> {
		/** See AchievementEntryListWidget.SCROLL_RATE_BASIS - a 0 here silently disables wheel scrolling. */
		private static final int SCROLL_RATE_BASIS = 24;

		EntryList(Minecraft minecraft, int width, int height, int y, java.util.Collection<AchievementDefinition> definitions, String locale) {
			super(minecraft, width, height, y, SCROLL_RATE_BASIS);
			int wrapWidth = getRowWidth() - 4;
			for (AchievementDefinition definition : definitions) {
				Row row = new Row(minecraft, definition, locale, wrapWidth);
				addEntry(row, row.height());
			}
		}

		@Override
		public int getRowWidth() {
			// See AchievementEntryListWidget.getRowWidth() - full-width rows push the scrollbar
			// outside the widget rectangle, where clicks on it are never dispatched.
			return getWidth() - 4 * scrollbarWidth() - 8;
		}

		final class Row extends ObjectSelectionList.Entry<Row> {
			private final Minecraft minecraft;
			private final String achievementId;
			private final List<FormattedCharSequence> titleLines;
			private final List<FormattedCharSequence> descLines;
			private final List<FormattedCharSequence> conditionLines;
			private final int rowHeight;

			Row(Minecraft minecraft, AchievementDefinition definition, String locale, int wrapWidth) {
				this.minecraft = minecraft;
				this.achievementId = definition.id().path();
				LocalizedText text = definition.textFor(locale);
				this.titleLines = minecraft.font.split(Component.literal("[" + achievementId + "] " + text.name()), wrapWidth);
				this.descLines = minecraft.font.split(Component.literal(text.description()), wrapWidth);
				this.conditionLines = minecraft.font.split(Component.literal(conditionText(definition.rule())), wrapWidth);
				int totalLines = titleLines.size() + descLines.size() + conditionLines.size();
				this.rowHeight = totalLines * minecraft.font.lineHeight + 10;
			}

			int height() {
				return rowHeight;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
				int titleColor = hovered ? 0xFFFFFFFF : 0xFFFFD700;
				int y = getContentY();
				y = drawLines(graphics, titleLines, y, titleColor);
				y = drawLines(graphics, descLines, y, 0xFFAAAAAA);
				drawLines(graphics, conditionLines, y, 0xFF77CCFF);
			}

			private int drawLines(GuiGraphicsExtractor graphics, List<FormattedCharSequence> lines, int y, int color) {
				for (FormattedCharSequence line : lines) {
					graphics.text(minecraft.font, line, getContentX(), y, color);
					y += minecraft.font.lineHeight;
				}
				return y;
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
				if (!ClientPlayNetworking.canSend(DevForceUnlockPayload.TYPE)) {
					return false;
				}
				ClientPlayNetworking.send(new DevForceUnlockPayload(achievementId));
				return true;
			}

			@Override
			public Component getNarration() {
				return Component.literal(achievementId);
			}
		}
	}
}
