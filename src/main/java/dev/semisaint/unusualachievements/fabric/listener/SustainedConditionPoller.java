package dev.semisaint.unusualachievements.fabric.listener;

import dev.semisaint.unusualachievements.core.SustainedStateTracker;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Generalizes LavaStandPoller's "polled sustained condition" pattern so new "stood in/near X for
 * N ticks" achievements don't each need their own poller class and their own once-per-20-ticks
 * scan of the player list. LavaStandPoller itself is left as-is (already proven in production)
 * rather than rebuilt on top of this.
 *
 * <p>Once a condition has been sustained long enough, onSustained keeps firing every poll for as
 * long as it stays true - callers are expected to route into UnlockDispatcher.unlock, which is
 * idempotent, so repeat firings after the first are harmless no-ops.
 */
public final class SustainedConditionPoller {
	private static final int POLL_INTERVAL_TICKS = 20;

	private static final Guard.Site POLL_SITE = new Guard.Site("sustained condition poll tick");
	private static final Guard.Site DISCONNECT_SITE = new Guard.Site("sustained condition disconnect cleanup");
	private static final List<Condition> CONDITIONS = new ArrayList<>();

	private SustainedConditionPoller() {
	}

	public static void registerCondition(String name, Predicate<ServerPlayer> isActive, long minTicks, Consumer<ServerPlayer> onSustained) {
		CONDITIONS.add(new Condition(name, isActive, minTicks, onSustained, new SustainedStateTracker<>()));
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> Guard.run(POLL_SITE, () -> {
			if (server.getTickCount() % POLL_INTERVAL_TICKS != 0) {
				return;
			}
			long currentTick = server.getTickCount();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				for (Condition condition : CONDITIONS) {
					boolean active = condition.isActive().test(player);
					condition.tracker().setState(player.getUUID(), active, currentTick);
					if (active && condition.tracker().isSustainedFor(player.getUUID(), currentTick, condition.minTicks())) {
						condition.onSustained().accept(player);
					}
				}
			}
		}));

		// The server tick counter keeps climbing while a player is offline - an un-cleared "entered
		// at tick X" would look like it had been sustained the entire time they were disconnected,
		// satisfying the threshold on the very first poll after they reconnect. Confirmed live: a
		// player who had been motionless right before disconnecting got `statue` back instantly on
		// rejoin, nowhere near having actually stood still for 60s this session.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Guard.run(DISCONNECT_SITE, () -> {
			ServerPlayer player = handler.getPlayer();
			if (player == null) {
				return;
			}
			for (Condition condition : CONDITIONS) {
				condition.tracker().clear(player.getUUID());
			}
		}));
	}

	/**
	 * Same reasoning as the disconnect cleanup above, one level up: an "entered at tick X" from the
	 * previous world is nonsense against the next world's counter, which restarts at 0. Left behind, a
	 * stale entry blocks its condition until the new world's tick count climbs past the old one.
	 */
	public static void reset() {
		for (Condition condition : CONDITIONS) {
			condition.tracker().clearAll();
		}
	}

	private record Condition(
		String name,
		Predicate<ServerPlayer> isActive,
		long minTicks,
		Consumer<ServerPlayer> onSustained,
		SustainedStateTracker<UUID> tracker
	) {
	}
}
