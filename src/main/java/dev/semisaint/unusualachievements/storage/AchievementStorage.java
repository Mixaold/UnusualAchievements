package dev.semisaint.unusualachievements.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.semisaint.unusualachievements.UnusualAchievementsMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AchievementStorage {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private AchievementStorage() {
	}

	public static PlayerAchievementData load(Path file) {
		if (!Files.isRegularFile(file)) {
			return new PlayerAchievementData();
		}
		try {
			String json = Files.readString(file, StandardCharsets.UTF_8);
			PlayerAchievementData data = GSON.fromJson(json, PlayerAchievementData.class);
			return data != null ? data.sanitized() : new PlayerAchievementData();
		} catch (IOException | RuntimeException e) {
			UnusualAchievementsMod.LOGGER.warn("Could not read achievement data from {}, starting empty", file, e);
			return new PlayerAchievementData();
		}
	}

	/**
	 * Returns false instead of throwing when the write fails: this is called from the server tick
	 * loop, from disconnect handling and from world shutdown, where an escaping IO error would take
	 * the game down over an unwritable progress file.
	 */
	public static boolean save(Path file, PlayerAchievementData data) {
		Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(file.getParent());
			// Write-then-move, so a crash mid-write (the exact scenario being investigated here)
			// cannot leave a truncated file that would read back as lost progress.
			Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException | RuntimeException e) {
			UnusualAchievementsMod.LOGGER.error("Could not write achievement data to {}", file, e);
			try {
				Files.deleteIfExists(temporary);
			} catch (IOException ignored) {
				// Nothing useful to do; the next successful save overwrites it anyway.
			}
			return false;
		}
	}
}
