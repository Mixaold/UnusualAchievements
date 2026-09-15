package dev.semisaint.unusualachievements.fabric.listener.custom;

import dev.semisaint.unusualachievements.fabric.listener.UnlockDispatcher;
import dev.semisaint.unusualachievements.fabric.registry.AchievementDefinitions;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class BlockBreakAchievements {
	private static final long WORLD_BOTTOM_Y = -60;
	private static final long ANVIL_WINDOW_TICKS = 100; // 5s

	private static final Guard.Site AFTER_BREAK_SITE = new Guard.Site("block break listener");

	private BlockBreakAchievements() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> Guard.run(AFTER_BREAK_SITE, () ->
			onBlockBroken(level, player, pos)));
	}

	private static void onBlockBroken(Level level, Player player, BlockPos pos) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (level.dimension() == Level.OVERWORLD && pos.getY() <= WORLD_BOTTOM_Y
			&& serverLevel.getServer().getPlayerList().getPlayers().size() == 1) {
			UnlockDispatcher.unlock(serverPlayer, AchievementDefinitions.WORLD_BOTTOM);
		}
		if (level.getBlockState(pos.above()).is(BlockTags.ANVIL)) {
			CombatDeathAchievements.ANVIL_ARMED.mark(serverPlayer.getUUID(), serverLevel.getServer().getTickCount(), ANVIL_WINDOW_TICKS);
		}
	}
}
