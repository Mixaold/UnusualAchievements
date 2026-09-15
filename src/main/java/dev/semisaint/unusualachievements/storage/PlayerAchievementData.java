package dev.semisaint.unusualachievements.storage;

import dev.semisaint.unusualachievements.core.CardTheme;

import java.util.ArrayList;
import java.util.List;

public final class PlayerAchievementData {
	public List<UnlockedEntry> unlocked = new ArrayList<>();
	public String selectedTheme = CardTheme.NONE;

	/**
	 * Gson populates fields straight from the file, so a hand-edited or truncated save can hand back
	 * a null list or null-id entries that would only blow up later, deep inside a tick.
	 */
	PlayerAchievementData sanitized() {
		if (selectedTheme == null || !CardTheme.isSelectable(selectedTheme)) {
			selectedTheme = CardTheme.NONE;
		}
		if (unlocked == null) {
			unlocked = new ArrayList<>();
			return this;
		}
		unlocked.removeIf(entry -> entry == null || entry.id == null || entry.id.isEmpty());
		return this;
	}

	public static final class UnlockedEntry {
		public String id;
		public long unlockedAt;

		public UnlockedEntry() {
		}

		public UnlockedEntry(String id, long unlockedAt) {
			this.id = id;
			this.unlockedAt = unlockedAt;
		}
	}
}
