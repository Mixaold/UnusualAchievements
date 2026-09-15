package dev.semisaint.unusualachievements;

import dev.semisaint.unusualachievements.core.CardTheme;
import dev.semisaint.unusualachievements.fabric.ServerLifecycleHooks;
import dev.semisaint.unusualachievements.fabric.listener.LavaStandPoller;
import dev.semisaint.unusualachievements.fabric.listener.StatThresholdEvaluator;
import dev.semisaint.unusualachievements.fabric.listener.custom.CustomAchievementListeners;
import dev.semisaint.unusualachievements.fabric.network.NetworkingInit;
import dev.semisaint.unusualachievements.fabric.registry.AchievementDefinitions;
import dev.semisaint.unusualachievements.fabric.registry.RarityBasis;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnusualAchievementsMod implements ModInitializer {
	public static final String MOD_ID = "unusualachievements";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AchievementDefinitions.registerAll();
		// Fail-fast, same policy as duplicate achievement ids: a theme pointing at a renamed
		// achievement would otherwise just be silently impossible to earn.
		CardTheme.verifyPairings();
		// Same reason: an achievement with no rarity basis loses its "once in N" line silently, which
		// is exactly the kind of miss nobody spots until a player asks why one entry has no number.
		RarityBasis.verifyCoverage();
		NetworkingInit.registerCommon();
		NetworkingInit.registerServer();
		ServerLifecycleHooks.registerAll();
		StatThresholdEvaluator.register();
		LavaStandPoller.register();
		CustomAchievementListeners.register();
		LOGGER.info("Unusual Achievements initialized");
	}
}
