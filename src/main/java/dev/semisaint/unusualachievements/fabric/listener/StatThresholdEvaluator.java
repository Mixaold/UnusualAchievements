package dev.semisaint.unusualachievements.fabric.listener;

import dev.semisaint.unusualachievements.core.AchievementDefinition;
import dev.semisaint.unusualachievements.core.AchievementRegistry;
import dev.semisaint.unusualachievements.core.PlayerAchievementProgress;
import dev.semisaint.unusualachievements.core.StatThresholdRule;
import dev.semisaint.unusualachievements.fabric.ServerLifecycleHooks;
import dev.semisaint.unusualachievements.fabric.registry.AchievementDefinitions;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

/**
 * Batches the stat-threshold check to once every EVALUATION_INTERVAL_TICKS instead of every
 * tick - Fabric API has no generic "a stat changed" callback, and a mixin into the stats
 * counter would be more invasive than this scale of check needs.
 */
public final class StatThresholdEvaluator {
	private static final int EVALUATION_INTERVAL_TICKS = 10;

	private static final Guard.Site EVALUATE_SITE = new Guard.Site("stat threshold tick");

	private StatThresholdEvaluator() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> Guard.run(EVALUATE_SITE, () -> {
			if (server.getTickCount() % EVALUATION_INTERVAL_TICKS != 0 || !ServerLifecycleHooks.ready()) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				evaluate(player);
			}
		}));
	}

	private static void evaluate(ServerPlayer player) {
		PlayerAchievementProgress progress = ServerLifecycleHooks.dataManager().get(player.getUUID());
		if (progress == null) {
			return;
		}
		for (AchievementDefinition definition : AchievementRegistry.statThresholdDefinitions()) {
			if (progress.isUnlocked(definition.id())) {
				continue;
			}
			StatThresholdRule rule = (StatThresholdRule) definition.rule();
			Identifier statId = AchievementDefinitions.vanillaCustomStatId(rule.vanillaCustomStatPath());
			if (statId == null) {
				continue;
			}
			int value = player.getStats().getValue(Stats.CUSTOM.get(statId));
			if (value >= rule.threshold()) {
				UnlockDispatcher.unlock(player, definition.id());
			}
		}
	}
}
