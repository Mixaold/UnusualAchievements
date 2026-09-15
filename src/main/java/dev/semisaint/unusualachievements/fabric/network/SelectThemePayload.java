package dev.semisaint.unusualachievements.fabric.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Asks the server to set the sender's card theme. The choice lives server-side rather than in the
 * client config because other players see it on your card - a client-only preference would only ever
 * be visible to yourself. The server re-checks that the theme's achievement is actually unlocked, so
 * a hand-crafted packet can't equip a locked one.
 */
public record SelectThemePayload(String themeId) implements CustomPacketPayload {
	/**
	 * The default STRING_UTF8 accepts up to 32 767 characters. Every id this mod will ever send is a
	 * short lowercase word, so a bounded codec rejects a hand-crafted 32 KB payload at decode time
	 * rather than carrying it into a map lookup that was always going to miss.
	 */
	private static final int MAX_THEME_ID_LENGTH = 64;

	public static final CustomPacketPayload.Type<SelectThemePayload> TYPE =
		new CustomPacketPayload.Type<>(UnusualAchievementsPayloads.id("select_theme"));
	public static final StreamCodec<FriendlyByteBuf, SelectThemePayload> STREAM_CODEC =
		CustomPacketPayload.codec(SelectThemePayload::write, SelectThemePayload::new);

	private SelectThemePayload(FriendlyByteBuf buf) {
		this(buf.readUtf(MAX_THEME_ID_LENGTH));
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeUtf(themeId, MAX_THEME_ID_LENGTH);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
