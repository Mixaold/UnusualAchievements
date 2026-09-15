package dev.semisaint.unusualachievements.fabric.listener.custom;

import dev.semisaint.unusualachievements.fabric.listener.UnlockDispatcher;
import dev.semisaint.unusualachievements.fabric.registry.AchievementDefinitions;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Everything hanging off net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents, plus
 * raid_diplomat - it needs no sleep event at all, but Raid tracking (ServerLevel.getRaidAt) is
 * shared machinery with slept_through_the_raid, so it lives alongside it rather than forcing a
 * cluster split over one achievement.
 */
public final class SleepAchievements {
	private static final long HONEST_SLEEP_MIN_TICKS = 3000;
	private static final long HONEST_SLEEP_SKIP_TOLERANCE = 5;
	private static final int GROUNDHOG_STREAK_TARGET = 5;
	private static final int POLL_INTERVAL_TICKS = 20;

	private static final Guard.Site START_SLEEP_SITE = new Guard.Site("sleep start listener");
	private static final Guard.Site STOP_SLEEP_SITE = new Guard.Site("sleep stop listener");
	private static final Guard.Site RAID_POLL_SITE = new Guard.Site("raid diplomat poll tick");
	private static final Guard.Site DISCONNECT_SITE = new Guard.Site("sleep disconnect cleanup");

	private record SleepSession(BlockPos bedPos, long startTick, long startDayTime) {
	}

	private record StreakState(BlockPos bedPos, long lastDayNumber, int streak) {
	}

	private record RaidWatch(float lastHealth, boolean tookDamage) {
	}

	private static final Map<UUID, SleepSession> ACTIVE_SLEEP = new HashMap<>();
	private static final Map<UUID, StreakState> GROUNDHOG_STREAK = new HashMap<>();
	private static final Map<UUID, RaidWatch> RAID_WATCH = new HashMap<>();

	private SleepAchievements() {
	}

	public static void register() {
		EntitySleepEvents.START_SLEEPING.register((entity, pos) -> Guard.run(START_SLEEP_SITE, () -> onStartSleeping(entity, pos)));
		EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> Guard.run(STOP_SLEEP_SITE, () -> onStopSleeping(entity, pos)));

		ServerTickEvents.END_SERVER_TICK.register(server -> Guard.run(RAID_POLL_SITE, () -> {
			if (server.getTickCount() % POLL_INTERVAL_TICKS != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				pollRaidDiplomat(player);
			}
		}));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Guard.run(DISCONNECT_SITE, () -> {
			ServerPlayer player = handler.getPlayer();
			if (player != null) {
				UUID id = player.getUUID();
				ACTIVE_SLEEP.remove(id);
				RAID_WATCH.remove(id);
				// A broken groundhog_day streak on disconnect would be harsher than intended - a
				// player logging off between nights shouldn't lose progress, only skipping a night while
				// online should. The streak map is deliberately left alone here.
			}
		}));
	}

	/**
	 * Sleep sessions are timed in per-world server ticks and the groundhog streak in per-world day
	 * numbers, so neither survives a world change meaningfully - see CombatDeathAchievements.reset().
	 */
	public static void reset() {
		ACTIVE_SLEEP.clear();
		GROUNDHOG_STREAK.clear();
		RAID_WATCH.clear();
	}

	private static void onStartSleeping(LivingEntity entity, BlockPos pos) {
		if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
			return;
		}
		ACTIVE_SLEEP.put(player.getUUID(), new SleepSession(pos, level.getServer().getTickCount(), level.getOverworldClockTime()));
	}

	private static void onStopSleeping(LivingEntity entity, BlockPos pos) {
		if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
			return;
		}
		SleepSession session = ACTIVE_SLEEP.remove(player.getUUID());
		if (session == null) {
			return;
		}
		long tickDelta = level.getServer().getTickCount() - session.startTick();
		long dayTimeDelta = level.getOverworldClockTime() - session.startDayTime();

		if (tickDelta >= HONEST_SLEEP_MIN_TICKS && Math.abs(dayTimeDelta - tickDelta) <= HONEST_SLEEP_SKIP_TOLERANCE) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.HONEST_SLEEP);
		}

		updateGroundhogStreak(player, session.bedPos(), level);

		Raid raid = level.getRaidAt(session.bedPos());
		if (raid != null && raid.isActive()) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.SLEPT_THROUGH_THE_RAID);
		}
	}

	private static void updateGroundhogStreak(ServerPlayer player, BlockPos bedPos, ServerLevel level) {
		long dayNumber = level.getOverworldClockTime() / 24000;
		StreakState previous = GROUNDHOG_STREAK.get(player.getUUID());
		int streak;
		if (previous == null || !previous.bedPos().equals(bedPos)) {
			streak = 1;
		} else if (previous.lastDayNumber() == dayNumber) {
			streak = previous.streak();
		} else if (previous.lastDayNumber() + 1 == dayNumber) {
			streak = previous.streak() + 1;
		} else {
			streak = 1;
		}
		GROUNDHOG_STREAK.put(player.getUUID(), new StreakState(bedPos, dayNumber, streak));
		if (streak >= GROUNDHOG_STREAK_TARGET) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.GROUNDHOG_DAY);
		}
	}

	private static void pollRaidDiplomat(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		Raid raid = level.getRaidAt(player.blockPosition());
		UUID id = player.getUUID();
		if (raid != null && raid.isActive()) {
			RaidWatch existing = RAID_WATCH.get(id);
			if (existing == null) {
				RAID_WATCH.put(id, new RaidWatch(player.getHealth(), false));
			} else {
				boolean tookDamage = existing.tookDamage() || player.getHealth() < existing.lastHealth();
				RAID_WATCH.put(id, new RaidWatch(player.getHealth(), tookDamage));
			}
			return;
		}
		RaidWatch watch = RAID_WATCH.remove(id);
		if (watch != null && !watch.tookDamage() && raid != null && raid.isVictory()) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.RAID_DIPLOMAT);
		}
	}
}
