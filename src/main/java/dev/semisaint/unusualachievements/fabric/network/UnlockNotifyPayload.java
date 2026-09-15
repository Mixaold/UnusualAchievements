package dev.semisaint.unusualachievements.fabric.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UnlockNotifyPayload(String achievementId, String secretName) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<UnlockNotifyPayload> TYPE =
		new CustomPacketPayload.Type<>(UnusualAchievementsPayloads.id("unlock_notify"));
	public static final StreamCodec<FriendlyByteBuf, UnlockNotifyPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, UnlockNotifyPayload::achievementId,
		ByteBufCodecs.STRING_UTF8, UnlockNotifyPayload::secretName,
		UnlockNotifyPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
