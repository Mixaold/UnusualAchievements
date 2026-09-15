package dev.semisaint.unusualachievements.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public final class PlayerAvatarWidget extends AbstractWidget {
	private static final long FADE_IN_MS = 220;

	private final UUID playerId;
	private final long createdAtMs = System.currentTimeMillis();

	public PlayerAvatarWidget(int x, int y, int size, UUID playerId) {
		super(x, y, size, size, Component.empty());
		this.playerId = playerId;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		PlayerInfo info = connection != null ? connection.getPlayerInfo(playerId) : null;
		if (info != null) {
			float fade = Math.min(1f, (System.currentTimeMillis() - createdAtMs) / (float) FADE_IN_MS);
			int tint = ((int) (fade * 255) << 24) | 0xFFFFFF;
			PlayerFaceExtractor.extractRenderState(graphics, info.getSkin(), getX(), getY(), getWidth(), tint);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}
}
