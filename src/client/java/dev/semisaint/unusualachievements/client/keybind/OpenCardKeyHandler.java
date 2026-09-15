package dev.semisaint.unusualachievements.client.keybind;

import dev.semisaint.unusualachievements.fabric.network.CardRequestPayload;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class OpenCardKeyHandler {
	private static final Guard.Site TICK_SITE = new Guard.Site("open card key tick");

	private OpenCardKeyHandler() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> Guard.run(TICK_SITE, () -> {
			// Only ever fires with no screen open - vanilla stops feeding key mappings once one is up,
			// so closing the card with the same key is handled by the card screen itself, not here.
			while (ModKeyBindings.OPEN_CARD.consumeClick()) {
				requestCard(client);
			}
		}));
	}

	/**
	 * How far you can be from someone and still open their card. The vanilla crosshair target
	 * (client.hitResult) only reaches interaction distance - about three blocks - which made looking
	 * someone up feel like you had to walk into them first. Looking at a card is not an interaction
	 * with the player, so it gets its own, far more generous reach.
	 */
	private static final double LOOKUP_REACH = 32.0;

	private static void requestCard(Minecraft client) {
		// Silently ignored on a server without the mod, rather than sending a payload it would
		// disconnect us for.
		if (client.player == null || !ClientPlayNetworking.canSend(CardRequestPayload.TYPE)) {
			return;
		}
		Player targetPlayer = playerUnderCrosshair(client);
		UUID target = targetPlayer != null ? targetPlayer.getUUID() : client.player.getUUID();
		ClientPlayNetworking.send(new CardRequestPayload(target));
	}

	/** Null when nothing is being looked at, or when a wall is in the way. */
	private static Player playerUnderCrosshair(Minecraft client) {
		LocalPlayer self = client.player;
		Vec3 eye = self.getEyePosition(1.0f);
		Vec3 look = self.getViewVector(1.0f);
		Vec3 end = eye.add(look.scale(LOOKUP_REACH));

		// Stop the ray at the first solid block, so you cannot read cards through terrain.
		BlockHitResult blocks = self.level().clip(new ClipContext(
			eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
		if (blocks.getType() != HitResult.Type.MISS) {
			end = blocks.getLocation();
		}

		AABB search = self.getBoundingBox().expandTowards(look.scale(LOOKUP_REACH)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(self, eye, end, search,
			entity -> entity instanceof Player && entity != self && !entity.isSpectator(),
			LOOKUP_REACH * LOOKUP_REACH);
		return hit != null && hit.getEntity() instanceof Player player ? player : null;
	}
}
