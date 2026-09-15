package dev.semisaint.unusualachievements.client;

import dev.semisaint.unusualachievements.client.config.ModConfigManager;
import dev.semisaint.unusualachievements.client.keybind.ModKeyBindings;
import dev.semisaint.unusualachievements.client.keybind.OpenCardKeyHandler;
import dev.semisaint.unusualachievements.client.network.ClientNetworkingInit;
import dev.semisaint.unusualachievements.client.overlay.UnlockDotHudElement;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnusualAchievementsClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("unusualachievements/client");

	@Override
	public void onInitializeClient() {
		ModConfigManager.preload();
		ModKeyBindings.register();
		OpenCardKeyHandler.register();
		ClientNetworkingInit.register();
		DevCommands.register();
		HudElementRegistry.addLast(
			Identifier.fromNamespaceAndPath("unusualachievements", "unlock_dot"),
			new UnlockDotHudElement()
		);
		LOGGER.info("Unusual Achievements client initialized");
	}
}
