package dev.semisaint.unusualachievements.fabric.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CardRequestPayload(UUID targetPlayer) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CardRequestPayload> TYPE =
		new CustomPacketPayload.Type<>(UnusualAchievementsPayloads.id("card_request"));
	public static final StreamCodec<FriendlyByteBuf, CardRequestPayload> STREAM_CODEC =
		CustomPacketPayload.codec(CardRequestPayload::write, CardRequestPayload::new);

	private CardRequestPayload(FriendlyByteBuf buf) {
		this(UUIDUtil.STREAM_CODEC.decode(buf));
	}

	private void write(FriendlyByteBuf buf) {
		UUIDUtil.STREAM_CODEC.encode(buf, targetPlayer);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
