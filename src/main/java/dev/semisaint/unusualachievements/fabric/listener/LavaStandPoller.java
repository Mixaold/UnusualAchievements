package dev.semisaint.unusualachievements.fabric.listener;

import dev.semisaint.unusualachievements.core.SustainedStateTracker;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Nothing fires an event for "entered lava", so detecting the sustained condition requires a
 * poll - capped at once per second (every 20 ticks) per the mod's performance budget, and only
 * over the currently online player list.
 */
public final class LavaStandPoller {
	private static final int POLL_INTERVAL_TICKS = 20;

	private static final Guard.Site POLL_SITE = new Guard.Site("lava poll tick");
	private static final SustainedStateTracker<UUID> TRACKER = new SustainedStateTracker<>();

	private LavaStandPoller() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> Guard.run(POLL_SITE, () -> {
			if (server.getTickCount() % POLL_INTERVAL_TICKS != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				TRACKER.setState(player.getUUID(), player.isInLava(), server.getTickCount());
			}
		}));
	}

	public static boolean hasStoodInLavaFor(UUID playerId, long currentTick, long minTicks) {
		return TRACKER.isSustainedFor(playerId, currentTick, minTicks);
	}

	/** Drops a leaving player's entry so the map does not grow across a long session. */
	public static void forget(UUID playerId) {
		TRACKER.clear(playerId);
	}

	/**
	 * Entries are keyed by tick counter, which restarts at 0 with each world - carrying them across
	 * would make a stale entry look like an arbitrarily long lava stand in the next world.
	 */
	public static void reset() {
		TRACKER.clearAll();
	}
}
