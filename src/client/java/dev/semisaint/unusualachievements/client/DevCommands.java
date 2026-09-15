package dev.semisaint.unusualachievements.client;

import dev.semisaint.unusualachievements.client.screen.DevAchievementListScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.permissions.Permissions;

public final class DevCommands {
	private DevCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommands.literal("uadev")
				// This screen spells out every hidden condition in the mod, which is the one thing the
				// whole design depends on not being public. Gated on the same permission the server
				// already demands for force-unlocking, and as a `requires` so it does not even show up
				// in tab-completion for a regular player. The permission set is sent by the server, so
				// this is not a client-side honour system.
				.requires(source -> {
					LocalPlayer player = source.getPlayer();
					return player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
				})
				.executes(context -> {
					// The chat screen closes itself (setScreen(null)) right after a command runs,
					// which would immediately clobber a screen opened synchronously here - defer to
					// the next tick so our screen wins.
					context.getSource().getClient().execute(() ->
						context.getSource().getClient().gui.setScreen(new DevAchievementListScreen()));
					return 1;
				})));
	}
}
