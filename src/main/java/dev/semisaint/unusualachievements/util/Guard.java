package dev.semisaint.unusualachievements.util;

import dev.semisaint.unusualachievements.UnusualAchievementsMod;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs a mod callback so that a failure inside it can never propagate into the caller.
 *
 * <p>Every entry point this mod owns is a callback the game invokes from a loop it does not
 * expect to fail: server ticks, HUD extraction, packet receivers. An exception escaping any of
 * those takes the whole game down with a crash report that names whichever mod is on top of the
 * stack - so a defect here would surface as "the modpack crashes", not "the achievements broke".
 * Swallowing (and logging, once per site) keeps a mod-local defect mod-local.
 */
public final class Guard {
	/** Enough repeats to see whether a failure is a one-off or every tick, without flooding the log. */
	private static final int MAX_LOGGED_PER_SITE = 5;

	private Guard() {
	}

	public static void run(Site site, Runnable body) {
		try {
			body.run();
		} catch (Throwable failure) {
			site.report(failure);
		}
	}

	/** One named call site, with its own "logged enough already" counter. */
	public static final class Site {
		private final String name;
		private final AtomicInteger logged = new AtomicInteger();

		public Site(String name) {
			this.name = name;
		}

		private void report(Throwable failure) {
			int seen = logged.incrementAndGet();
			if (seen > MAX_LOGGED_PER_SITE) {
				return;
			}
			UnusualAchievementsMod.LOGGER.error(
				"unusualachievements: {} failed and was contained (occurrence {}{})",
				name, seen, seen == MAX_LOGGED_PER_SITE ? ", further occurrences silenced" : "", failure);
		}
	}
}
