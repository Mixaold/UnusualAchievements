package dev.semisaint.unusualachievements.client.overlay;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Queues unlock announcements instead of holding a single overwritable slot. Two achievements can
 * unlock in the same evaluation tick (confirmed in testing: hop_to_it and sneaky_business landed
 * 2ms apart) - with a single slot, the second unlock's markUnseenUnlock() call clobbered the
 * first's state before a single frame ever rendered it, silently discarding both its animation and
 * (since only the first-ever unlock claims it) the one-time "press key to open card" hint.
 */
public final class UnlockOverlayState {
	private record PendingUnlock(boolean showOpenCardHint) {
	}

	/**
	 * The animation runs on wall-clock time, so it can "finish" during a stall in which not one frame
	 * was drawn - a chunk-load hitch right after joining is enough, and that is exactly when a first
	 * unlock tends to land. Counting frames the hint was really painted in is immune to that: below
	 * this many, we treat it as never delivered and let a later unlock carry it again.
	 */
	private static final int HINT_FRAMES_FOR_DELIVERY = 20;

	private static final Object LOCK = new Object();
	private static final Deque<PendingUnlock> QUEUE = new ArrayDeque<>();

	private static long activeStartMs = -1L;
	private static boolean activeShowOpenCardHint = false;
	/** Some queued or active unlock already carries the one-time hint - don't hand it out twice. */
	private static boolean hintClaimed = false;
	private static int activeHintFramesDrawn = 0;

	private UnlockOverlayState() {
	}

	public static void markUnseenUnlock(boolean hintAllowed) {
		synchronized (LOCK) {
			boolean carriesHint = hintAllowed && !hintClaimed;
			if (carriesHint) {
				hintClaimed = true;
			}
			QUEUE.addLast(new PendingUnlock(carriesHint));
			if (activeStartMs < 0) {
				activateNext();
			}
		}
	}

	/** Called from the HUD for every frame the hint text was actually drawn in. */
	public static void noteHintFrameDrawn() {
		synchronized (LOCK) {
			activeHintFramesDrawn++;
		}
	}

	public static boolean hintWasDelivered() {
		synchronized (LOCK) {
			return activeHintFramesDrawn >= HINT_FRAMES_FOR_DELIVERY;
		}
	}

	public static long elapsedMs() {
		synchronized (LOCK) {
			return activeStartMs < 0 ? -1 : System.currentTimeMillis() - activeStartMs;
		}
	}

	public static boolean showOpenCardHint() {
		synchronized (LOCK) {
			return activeShowOpenCardHint;
		}
	}

	/**
	 * Called by the HUD once the current animation has fully played out - starts the next queued
	 * unlock, if any. An unlock that was carrying the hint but never actually got it on screen
	 * releases the claim, so the next unlock retries instead of the hint being lost for good.
	 */
	public static void advance() {
		synchronized (LOCK) {
			if (activeShowOpenCardHint && activeHintFramesDrawn < HINT_FRAMES_FOR_DELIVERY) {
				hintClaimed = false;
			}
			activateNext();
		}
	}

	/**
	 * Full reset (e.g. leaving a world): drops any queued unlocks too, not just the active one.
	 * Releases the hint claim as well - a hint dropped this way was never read by anyone.
	 */
	public static void clear() {
		synchronized (LOCK) {
			QUEUE.clear();
			activeStartMs = -1L;
			activeShowOpenCardHint = false;
			activeHintFramesDrawn = 0;
			hintClaimed = false;
		}
	}

	private static void activateNext() {
		PendingUnlock next = QUEUE.pollFirst();
		activeHintFramesDrawn = 0;
		if (next == null) {
			activeStartMs = -1L;
			activeShowOpenCardHint = false;
			return;
		}
		activeStartMs = System.currentTimeMillis();
		activeShowOpenCardHint = next.showOpenCardHint();
	}
}
