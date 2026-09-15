package dev.semisaint.unusualachievements.storage;

import dev.semisaint.unusualachievements.core.AchievementId;
import dev.semisaint.unusualachievements.core.PlayerAchievementProgress;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory cache of per-player progress for the currently loaded world. Unlocks and
 * privacy changes only touch memory + the dirty set; disk writes happen solely through
 * flush(), which callers trigger on disconnect / periodic autosave / server stop.
 */
public final class AchievementDataManager {
	private final Path dataDirectory;
	private final Map<UUID, PlayerAchievementProgress> progressByPlayer = new HashMap<>();
	private final Set<UUID> dirty = new HashSet<>();

	public AchievementDataManager(Path dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	private Path fileFor(UUID playerId) {
		return dataDirectory.resolve(playerId + ".json");
	}

	public PlayerAchievementProgress loadOrCreate(UUID playerId) {
		return progressByPlayer.computeIfAbsent(playerId, id -> {
			PlayerAchievementData data = AchievementStorage.load(fileFor(id));
			PlayerAchievementProgress progress = new PlayerAchievementProgress(id);
			for (PlayerAchievementData.UnlockedEntry entry : data.unlocked) {
				progress.unlock(new AchievementId(entry.id), entry.unlockedAt);
			}
			progress.selectTheme(data.selectedTheme);
			return progress;
		});
	}

	public boolean isKnownPlayer(UUID playerId) {
		return progressByPlayer.containsKey(playerId) || Files.isRegularFile(fileFor(playerId));
	}

	public PlayerAchievementProgress get(UUID playerId) {
		return progressByPlayer.get(playerId);
	}

	public void markDirty(UUID playerId) {
		dirty.add(playerId);
	}

	public void unload(UUID playerId) {
		flushOne(playerId);
		progressByPlayer.remove(playerId);
	}

	public void flushOne(UUID playerId) {
		if (!dirty.remove(playerId)) {
			return;
		}
		PlayerAchievementProgress progress = progressByPlayer.get(playerId);
		if (progress == null) {
			return;
		}
		if (!AchievementStorage.save(fileFor(playerId), toData(progress))) {
			// Keep it queued so the next autosave / world shutdown retries instead of silently
			// dropping the unlock that made it dirty.
			dirty.add(playerId);
		}
	}

	public void flushAllDirty() {
		for (UUID playerId : Set.copyOf(dirty)) {
			flushOne(playerId);
		}
	}

	private static PlayerAchievementData toData(PlayerAchievementProgress progress) {
		PlayerAchievementData data = new PlayerAchievementData();
		progress.unlockedAtEpochMillis().forEach((id, timestamp) ->
			data.unlocked.add(new PlayerAchievementData.UnlockedEntry(id.path(), timestamp)));
		data.selectedTheme = progress.selectedThemeId();
		return data;
	}
}
