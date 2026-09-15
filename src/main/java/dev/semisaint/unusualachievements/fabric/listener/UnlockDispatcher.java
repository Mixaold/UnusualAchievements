package dev.semisaint.unusualachievements.fabric.listener;

import dev.semisaint.unusualachievements.UnusualAchievementsMod;
import dev.semisaint.unusualachievements.core.AchievementDefinition;
import dev.semisaint.unusualachievements.core.AchievementId;
import dev.semisaint.unusualachievements.core.AchievementRegistry;
import dev.semisaint.unusualachievements.core.LocalizedText;
import dev.semisaint.unusualachievements.core.PlayerAchievementProgress;
import dev.semisaint.unusualachievements.fabric.ServerLifecycleHooks;
import dev.semisaint.unusualachievements.fabric.network.UnlockNotifyPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class UnlockDispatcher {
	private UnlockDispatcher() {
	}

	public static void unlock(ServerPlayer player, AchievementId id) {
		if (!ServerLifecycleHooks.ready()) {
			return;
		}
		AchievementDefinition definition = AchievementRegistry.get(id).orElse(null);
		if (definition == null) {
			UnusualAchievementsMod.LOGGER.warn("Ignoring unlock of unregistered achievement {}", id.path());
			return;
		}
		PlayerAchievementProgress progress = ServerLifecycleHooks.dataManager().get(player.getUUID());
		if (progress == null) {
			return;
		}
		if (!progress.unlock(id, System.currentTimeMillis())) {
			return;
		}
		ServerLifecycleHooks.dataManager().markDirty(player.getUUID());
		ServerLifecycleHooks.rarityTracker().onUnlock(id);

		// A client without the mod (or a stale one that never registered the channel) would be
		// disconnected by an unsolicited payload - the unlock itself is already recorded server-side.
		if (!ServerPlayNetworking.canSend(player, UnlockNotifyPayload.TYPE)) {
			return;
		}
		LocalizedText text = definition.textFor(player.clientInformation().language());
		ServerPlayNetworking.send(player, new UnlockNotifyPayload(id.path(), text.name()));
	}
}
