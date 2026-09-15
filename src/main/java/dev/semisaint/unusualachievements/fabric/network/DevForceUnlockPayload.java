package dev.semisaint.unusualachievements.fabric.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Dev/QA-only: asks the server to force-unlock one achievement id for the sender, bypassing its
 * real trigger condition. With ~50 custom-event achievements (storms, specific mob combos,
 * timing windows), living through each condition for real on every test pass is not realistic -
 * this lets DevAchievementListScreen verify the unlock -> packet -> HUD -> card path in isolation.
 * Gated server-side on operator permission, never on a build flag.
 */
public record DevForceUnlockPayload(String achievementId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<DevForceUnlockPayload> TYPE =
		new CustomPacketPayload.Type<>(UnusualAchievementsPayloads.id("dev_force_unlock"));
	public static final StreamCodec<FriendlyByteBuf, DevForceUnlockPayload> STREAM_CODEC =
		CustomPacketPayload.codec(DevForceUnlockPayload::write, DevForceUnlockPayload::new);

	/** Bounded like SelectThemePayload: every real id is a short snake_case word. */
	private static final int MAX_ACHIEVEMENT_ID_LENGTH = 64;

	private DevForceUnlockPayload(FriendlyByteBuf buf) {
		this(buf.readUtf(MAX_ACHIEVEMENT_ID_LENGTH));
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeUtf(achievementId, MAX_ACHIEVEMENT_ID_LENGTH);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
