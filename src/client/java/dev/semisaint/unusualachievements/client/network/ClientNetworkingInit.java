package dev.semisaint.unusualachievements.client.network;

import dev.semisaint.unusualachievements.client.config.ModConfigManager;
import dev.semisaint.unusualachievements.client.overlay.UnlockOverlayState;
import dev.semisaint.unusualachievements.client.screen.AchievementCardScreen;
import dev.semisaint.unusualachievements.fabric.network.CardResponsePayload;
import dev.semisaint.unusualachievements.fabric.network.UnlockNotifyPayload;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class ClientNetworkingInit {
	private static final Guard.Site CARD_RESPONSE_SITE = new Guard.Site("card response receiver");
	private static final Guard.Site UNLOCK_NOTIFY_SITE = new Guard.Site("unlock notify receiver");
	private static final Guard.Site DISCONNECT_SITE = new Guard.Site("client disconnect");

	private ClientNetworkingInit() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(CardResponsePayload.TYPE, (payload, context) ->
			Guard.run(CARD_RESPONSE_SITE, () -> context.client().gui.setScreen(new AchievementCardScreen(payload))));

		ClientPlayNetworking.registerGlobalReceiver(UnlockNotifyPayload.TYPE, (payload, context) -> Guard.run(UNLOCK_NOTIFY_SITE, () -> {
			// Deliberately does NOT mark the hint as seen here - the overlay persists that only after
			// the hint has actually spent real frames on screen. Writing it on arrival meant a hint
			// lost to a stall, a disconnect or an immediately opened card was gone permanently.
			UnlockOverlayState.markUnseenUnlock(!ModConfigManager.get().seenOpenCardHint);
			context.client().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 0.35f));
		}));

		// An unlock animation started just before leaving would otherwise keep drawing over the
		// main menu, since the overlay's state is static and outlives the world.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
			Guard.run(DISCONNECT_SITE, UnlockOverlayState::clear));
	}
}
