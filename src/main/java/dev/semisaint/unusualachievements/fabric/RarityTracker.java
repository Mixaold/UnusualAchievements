package dev.semisaint.unusualachievements.fabric;

import dev.semisaint.unusualachievements.UnusualAchievementsMod;
import dev.semisaint.unusualachievements.core.AchievementId;
import dev.semisaint.unusualachievements.core.RarityInfo;
import dev.semisaint.unusualachievements.storage.AchievementStorage;
import dev.semisaint.unusualachievements.storage.PlayerAchievementData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Rarity is maintained as in-memory counters updated incrementally on unlock, rather than
 * rescanning the save directory on every card request - the directory is only scanned once,
 * at server start.
 */
public final class RarityTracker {
	private final Map<AchievementId, Integer> unlockCounts = new HashMap<>();
	private int totalPlayers;

	public void initFromSaveDirectory(Path dataDirectory) {
		unlockCounts.clear();
		totalPlayers = 0;
		if (!Files.isDirectory(dataDirectory)) {
			return;
		}
		try (Stream<Path> files = Files.list(dataDirectory)) {
			files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(file -> {
				PlayerAchievementData data = AchievementStorage.load(file);
				totalPlayers++;
				for (PlayerAchievementData.UnlockedEntry entry : data.unlocked) {
					unlockCounts.merge(new AchievementId(entry.id), 1, Integer::sum);
				}
			});
		} catch (IOException | RuntimeException e) {
			// This runs inside world load. Rarity is a cosmetic percentage on the card, so an
			// unreadable save directory degrades to "everything reads as 0%" rather than aborting
			// the join.
			UnusualAchievementsMod.LOGGER.error("Could not scan {} for rarity counts; rarity will read as empty", dataDirectory, e);
			unlockCounts.clear();
			totalPlayers = 0;
		}
	}

	public void onUnlock(AchievementId id) {
		unlockCounts.merge(id, 1, Integer::sum);
	}

	public void onNewPlayer() {
		totalPlayers++;
	}

	public RarityInfo rarityFor(AchievementId id) {
		return new RarityInfo(unlockCounts.getOrDefault(id, 0), totalPlayers);
	}

	public int totalPlayers() {
		return totalPlayers;
	}
}
