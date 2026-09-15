package dev.semisaint.unusualachievements.fabric.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

public record CardResponsePayload(
	UUID playerId,
	String nickname,
	List<UnlockedEntry> unlocked,
	int totalPlayers,
	boolean unknownPlayer,
	String themeId
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CardResponsePayload> TYPE =
		new CustomPacketPayload.Type<>(UnusualAchievementsPayloads.id("card_response"));

	// Written out by hand rather than via StreamCodec.composite: the entry carries more fields than
	// composite's fixed arities cover, and spelling the order out once is clearer than splitting the
	// record just to fit a helper.
	public static final StreamCodec<FriendlyByteBuf, UnlockedEntry> ENTRY_CODEC = StreamCodec.of(
		(buf, entry) -> {
			ByteBufCodecs.STRING_UTF8.encode(buf, entry.id());
			ByteBufCodecs.STRING_UTF8.encode(buf, entry.secretName());
			ByteBufCodecs.STRING_UTF8.encode(buf, entry.secretDescription());
			ByteBufCodecs.STRING_UTF8.encode(buf, entry.flavorText());
			ByteBufCodecs.VAR_LONG.encode(buf, entry.unlockedAtEpochMillis());
			ByteBufCodecs.INT.encode(buf, entry.rarityCount());
			ByteBufCodecs.STRING_UTF8.encode(buf, entry.rarityKey());
			ByteBufCodecs.VAR_LONG.encode(buf, entry.rarityValue());
			ByteBufCodecs.BOOL.encode(buf, entry.rarityPercentable());
		},
		buf -> new UnlockedEntry(
			ByteBufCodecs.STRING_UTF8.decode(buf),
			ByteBufCodecs.STRING_UTF8.decode(buf),
			ByteBufCodecs.STRING_UTF8.decode(buf),
			ByteBufCodecs.STRING_UTF8.decode(buf),
			ByteBufCodecs.VAR_LONG.decode(buf),
			ByteBufCodecs.INT.decode(buf),
			ByteBufCodecs.STRING_UTF8.decode(buf),
			ByteBufCodecs.VAR_LONG.decode(buf),
			ByteBufCodecs.BOOL.decode(buf)
		)
	);

	public static final StreamCodec<FriendlyByteBuf, CardResponsePayload> STREAM_CODEC = StreamCodec.composite(
		UUIDUtil.STREAM_CODEC, CardResponsePayload::playerId,
		ByteBufCodecs.STRING_UTF8, CardResponsePayload::nickname,
		ENTRY_CODEC.apply(ByteBufCodecs.list()), CardResponsePayload::unlocked,
		ByteBufCodecs.VAR_INT, CardResponsePayload::totalPlayers,
		ByteBufCodecs.BOOL, CardResponsePayload::unknownPlayer,
		ByteBufCodecs.STRING_UTF8, CardResponsePayload::themeId,
		CardResponsePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/**
	 * @param rarityCount how many players on this server hold the achievement
	 * @param rarityKey   lang key for the "one time in N ..." line, empty when no vanilla counter fits
	 * @param rarityValue the N for that line, in the units the key names
	 * @param rarityPercentable whether that N describes countable attempts, so it can also be shown
	 *                          as a percentage - false for rates like hours played
	 */
	public record UnlockedEntry(
		String id,
		String secretName,
		String secretDescription,
		String flavorText,
		long unlockedAtEpochMillis,
		int rarityCount,
		String rarityKey,
		long rarityValue,
		boolean rarityPercentable
	) {
	}
}
