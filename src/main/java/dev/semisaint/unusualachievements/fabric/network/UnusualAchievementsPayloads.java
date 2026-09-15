package dev.semisaint.unusualachievements.fabric.network;

import dev.semisaint.unusualachievements.UnusualAchievementsMod;
import net.minecraft.resources.Identifier;

final class UnusualAchievementsPayloads {
	private UnusualAchievementsPayloads() {
	}

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(UnusualAchievementsMod.MOD_ID, path);
	}
}
