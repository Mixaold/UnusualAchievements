package dev.semisaint.unusualachievements.fabric.listener.custom;

import dev.semisaint.unusualachievements.fabric.listener.SustainedConditionPoller;

/**
 * Single entry point UnusualAchievementsMod calls - registering a new cluster means adding one
 * line here, not a new line in the mod's onInitialize(). Replaces the old single-file
 * CustomAchievementListener now that the custom-event roster has grown well past one achievement.
 */
public final class CustomAchievementListeners {
	private CustomAchievementListeners() {
	}

	public static void register() {
		CombatDeathAchievements.register();
		InteractionAchievements.register();
		BlockBreakAchievements.register();
		SocialAchievements.register();
		SleepAchievements.register();
		// Conditions are registered by the clusters above (during their own register() calls);
		// the poller itself just needs to start scanning.
		SustainedConditionPoller.register();
	}

	/**
	 * Drops every scrap of per-world listener state. Called when a world is unloaded, because all of
	 * it is measured against MinecraftServer.getTickCount(), which starts again from 0 in the next
	 * one - see CombatDeathAchievements.reset() for what that mismatch actually hands out for free.
	 */
	public static void resetAll() {
		CombatDeathAchievements.reset();
		InteractionAchievements.reset();
		SocialAchievements.reset();
		SleepAchievements.reset();
		SustainedConditionPoller.reset();
	}
}
