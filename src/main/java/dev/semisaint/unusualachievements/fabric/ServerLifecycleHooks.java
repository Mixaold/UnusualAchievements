package dev.semisaint.unusualachievements.fabric;

import dev.semisaint.unusualachievements.UnusualAchievementsMod;
import dev.semisaint.unusualachievements.fabric.listener.LavaStandPoller;
import dev.semisaint.unusualachievements.fabric.listener.custom.CustomAchievementListeners;
import dev.semisaint.unusualachievements.storage.AchievementDataManager;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Holds the server-scoped singletons and wires their lifecycle. A fresh AchievementDataManager
 * and RarityTracker are created per server start (world save path is only known then) and torn
 * down (flushed) on stop, mirroring vanilla's per-world stats/advancements storage.
 *
 * <p>Every hook is wrapped in {@link Guard}: these run inside world load, the server tick loop and
 * connection handling, where an escaping exception aborts the join or crashes the game rather than
 * just disabling this mod's feature.
 */
public final class ServerLifecycleHooks {
	private static final int AUTOSAVE_INTERVAL_TICKS = 6000;

	private static final Guard.Site START_SITE = new Guard.Site("server start");
	private static final Guard.Site JOIN_SITE = new Guard.Site("player join");
	private static final Guard.Site DISCONNECT_SITE = new Guard.Site("player disconnect");
	private static final Guard.Site AUTOSAVE_SITE = new Guard.Site("autosave tick");
	private static final Guard.Site STOP_SITE = new Guard.Site("server stop");

	private static volatile AchievementDataManager dataManager;
	private static volatile RarityTracker rarityTracker;

	private ServerLifecycleHooks() {
	}

	public static AchievementDataManager dataManager() {
		return dataManager;
	}

	public static RarityTracker rarityTracker() {
		return rarityTracker;
	}

	/** True once a world is loaded and both server-scoped singletons exist. */
	public static boolean ready() {
		return dataManager != null && rarityTracker != null;
	}

	public static void registerAll() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> Guard.run(START_SITE, () -> {
			Path dataDirectory = server.getWorldPath(LevelResource.ROOT).resolve(UnusualAchievementsMod.MOD_ID);
			AchievementDataManager manager = new AchievementDataManager(dataDirectory);
			RarityTracker tracker = new RarityTracker();
			tracker.initFromSaveDirectory(dataDirectory);
			// Published only once both are fully built, so a partial init can never be observed as ready().
			dataManager = manager;
			rarityTracker = tracker;
		}));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Guard.run(JOIN_SITE, () -> {
			if (!ready()) {
				return;
			}
			UUID playerId = handler.getPlayer().getUUID();
			boolean isNewPlayer = !dataManager.isKnownPlayer(playerId);
			dataManager.loadOrCreate(playerId);
			if (isNewPlayer) {
				// Persist immediately so a player who leaves without unlocking anything is still
				// "known" next join - otherwise every rejoin would count as a new player and inflate
				// the rarity denominator.
				dataManager.markDirty(playerId);
				dataManager.flushOne(playerId);
				rarityTracker.onNewPlayer();
			}
		}));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Guard.run(DISCONNECT_SITE, () -> {
			ServerPlayer player = handler.getPlayer();
			if (player == null) {
				return;
			}
			LavaStandPoller.forget(player.getUUID());
			if (dataManager != null) {
				dataManager.unload(player.getUUID());
			}
		}));

		ServerTickEvents.END_SERVER_TICK.register(server -> Guard.run(AUTOSAVE_SITE, () -> {
			if (dataManager != null && server.getTickCount() % AUTOSAVE_INTERVAL_TICKS == 0) {
				dataManager.flushAllDirty();
			}
		}));

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> Guard.run(STOP_SITE, () -> {
			if (dataManager != null) {
				dataManager.flushAllDirty();
			}
		}));

		// Nulled only after STOPPING has flushed, so leaving a world leaves no state that a later
		// world would inherit (these singletons outlive a single-player session otherwise).
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			dataManager = null;
			rarityTracker = null;
			LavaStandPoller.reset();
			// Everything else the listeners hold is keyed to this world's tick counter too, and used
			// to survive into the next one - where a leftover window reads as wide open.
			CustomAchievementListeners.resetAll();
		});
	}
}
