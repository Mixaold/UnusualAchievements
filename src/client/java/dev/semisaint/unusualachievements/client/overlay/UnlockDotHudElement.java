package dev.semisaint.unusualachievements.client.overlay;

import dev.semisaint.unusualachievements.client.config.ModConfigManager;
import dev.semisaint.unusualachievements.client.keybind.ModKeyBindings;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Invisible by default (no idle gray dot) - only ever draws while an unlock animation is
 * actively running. Timeline: grow in (gold, spinning in) -> hold with a gentle breathing pulse
 * -> coin-flip into a red "!" (scaleX pinches through 0, like flipping a card edge-on) -> hold ->
 * shrink out (spinning away) -> fully hidden again.
 */
public final class UnlockDotHudElement implements HudElement {
	private static final int BASE_SIZE = 7;
	// Vanilla hotbar is always 182 GUI-scaled px wide and horizontally centered, so its right edge
	// is a stable anchor regardless of window size - unlike screen-edge anchoring, this keeps the
	// indicator visually attached to the hotbar instead of stranded alone in the corner.
	private static final int HOTBAR_HALF_WIDTH = 91;
	private static final int HOTBAR_GAP = 6;
	private static final int BOTTOM_MARGIN = 24;

	private static final long GROW_IN_MS = 250;
	private static final long GOLD_HOLD_MS = 2000;
	private static final long MORPH_MS = 260;
	private static final long EXCLAIM_HOLD_MS = 2000;
	private static final long SHRINK_OUT_MS = 250;

	private static final long T1 = GROW_IN_MS;
	private static final long T2 = T1 + GOLD_HOLD_MS;
	private static final long T3 = T2 + MORPH_MS;
	// The tutorial hint needs longer than a normal exclaim hold to actually be readable - only
	// stretched when it's showing, so every other unlock keeps the regular timing.
	private static final long HINT_EXTRA_HOLD_MS = 1000;

	private static final int GOLD_ARGB = 0xFFFFD700;
	private static final int RED_ARGB = 0xFFFF2B22;
	private static final float EXCLAIM_SIZE_MULTIPLIER = 2.4f;
	private static final long HINT_PULSE_PERIOD_MS = 900L;

	private static final Guard.Site EXTRACT_SITE = new Guard.Site("unlock dot render");

	/**
	 * Runs once per frame on the render thread. Anything thrown here would surface as a render
	 * crash blamed on whatever mod sits above us in the HUD chain, so the whole body is contained
	 * and a failing overlay simply stops drawing.
	 */
	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Guard.run(EXTRACT_SITE, () -> extract(graphics));
	}

	private void extract(GuiGraphicsExtractor graphics) {
		if (!ModConfigManager.get().overlayEnabled) {
			return;
		}
		long elapsed = UnlockOverlayState.elapsedMs();
		if (elapsed < 0) {
			return;
		}
		boolean showHint = UnlockOverlayState.showOpenCardHint();
		long exclaimHoldMs = EXCLAIM_HOLD_MS + (showHint ? HINT_EXTRA_HOLD_MS : 0L);
		long t4 = T3 + exclaimHoldMs;
		long t5 = t4 + SHRINK_OUT_MS;
		if (elapsed >= t5) {
			// Persisting "seen" here, and only here, is the whole point: the flag used to be written
			// the moment the unlock packet arrived, so the very first unlock burned it whether or not
			// a single frame of the hint ever reached the screen - after which no amount of fixing the
			// render path could bring the hint back, because the config on disk already said "seen".
			if (showHint && UnlockOverlayState.hintWasDelivered()) {
				ModConfigManager.get().seenOpenCardHint = true;
				ModConfigManager.save();
			}
			UnlockOverlayState.advance();
			return;
		}

		int windowWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int windowHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		int cx = windowWidth / 2 + HOTBAR_HALF_WIDTH + HOTBAR_GAP + BASE_SIZE / 2;
		int cy = windowHeight - BOTTOM_MARGIN - BASE_SIZE / 2;

		float goldScaleX = 0f;
		float goldScaleY = 0f;
		float goldRotation = 0f;
		float exclaimScaleX = 0f;
		float exclaimScaleY = 0f;
		float exclaimRotation = 0f;

		if (elapsed < T1) {
			float t = elapsed / (float) T1;
			float s = easeOutBack(t);
			goldScaleX = s;
			goldScaleY = s;
			// Almost two full turns unwinding into place - the "vvvzhuh" spin-in, not just a nudge.
			goldRotation = -(float) (Math.PI * 1.85) * (1f - t);
		} else if (elapsed < T2) {
			float s = breathe(elapsed - T1);
			goldScaleX = s;
			goldScaleY = s;
		} else if (elapsed < T3) {
			float t = (elapsed - T2) / (float) MORPH_MS;
			float pinch = Math.abs((float) Math.cos(t * Math.PI));
			if (t < 0.5f) {
				goldScaleX = pinch;
				goldScaleY = 1f;
			} else {
				exclaimScaleX = pinch;
				exclaimScaleY = 1f;
			}
		} else if (elapsed < t4) {
			float s = breathe(elapsed - T3);
			exclaimScaleX = s;
			exclaimScaleY = s;
		} else {
			float t = (elapsed - t4) / (float) SHRINK_OUT_MS;
			float s = 1f - t;
			exclaimScaleX = s;
			exclaimScaleY = s;
			exclaimRotation = 0.6f * t;
		}

		if (goldScaleX > 0.01f) {
			drawDot(graphics, cx, cy, goldScaleX, goldScaleY, goldRotation, GOLD_ARGB);
		}
		if (exclaimScaleX > 0.01f) {
			drawExclaim(graphics, Minecraft.getInstance().font, cx, cy, exclaimScaleX, exclaimScaleY, exclaimRotation, RED_ARGB);
		}
		if (showHint && elapsed >= T3 && elapsed < t4) {
			double phase = ((elapsed - T3) % HINT_PULSE_PERIOD_MS) / (double) HINT_PULSE_PERIOD_MS;
			int hintAlpha = (int) (155 + 100 * Math.sin(phase * Math.PI * 2));
			drawHint(graphics, cx, cy + 12, windowWidth, hintAlpha);
			UnlockOverlayState.noteHintFrameDrawn();
		}
	}

	private void drawHint(GuiGraphicsExtractor graphics, int cx, int y, int windowWidth, int alpha) {
		Font font = Minecraft.getInstance().font;
		Component hint = Component.translatable("hud.unusualachievements.hint_open_card",
			ModKeyBindings.openCardKeyLabel());
		// Centred under the mark - but at this height the line runs through the hotbar's band, so it
		// is never allowed to slide left past the hotbar's right edge; the clear space is all to the right.
		int x = Math.max(cx - font.width(hint) / 2, windowWidth / 2 + HOTBAR_HALF_WIDTH + 4);
		graphics.text(font, hint, x, y, (alpha << 24) | 0xFFFFFF);
	}

	private static float breathe(long elapsedInHold) {
		double phase = (elapsedInHold % 1200L) / 1200.0;
		return 1f + 0.06f * (float) Math.sin(phase * Math.PI * 2);
	}

	private static float easeOutBack(float t) {
		t = Math.max(0f, Math.min(1f, t));
		float c1 = 1.70158f;
		float c3 = c1 + 1f;
		float p = t - 1f;
		return 1f + c3 * p * p * p + c1 * p * p;
	}

	private void drawDot(GuiGraphicsExtractor graphics, int cx, int cy, float scaleX, float scaleY, float rotation, int color) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(cx, cy);
		graphics.pose().rotate(rotation);
		graphics.pose().scale(scaleX, scaleY);
		int half = BASE_SIZE / 2;
		int outlineAlpha = (color >>> 24) & 0xFF;
		// A thin dark outline reads much cleaner at this size than a flat, edgeless color fill.
		graphics.fill(-half - 1, -half - 1, half + 1, half + 1, (outlineAlpha << 24) | 0x2B1900);
		graphics.fill(-half, -half, half, half, color);
		graphics.pose().popMatrix();
	}

	private void drawExclaim(GuiGraphicsExtractor graphics, Font font, int cx, int cy, float scaleX, float scaleY, float rotation, int color) {
		Component mark = Component.literal("!").withStyle(ChatFormatting.BOLD);
		int textWidth = font.width(mark);
		graphics.pose().pushMatrix();
		graphics.pose().translate(cx, cy);
		graphics.pose().rotate(rotation);
		graphics.pose().scale(scaleX * EXCLAIM_SIZE_MULTIPLIER, scaleY * EXCLAIM_SIZE_MULTIPLIER);
		graphics.text(font, mark, -textWidth / 2, -font.lineHeight / 2, color);
		graphics.pose().popMatrix();
	}
}
