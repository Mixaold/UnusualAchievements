package dev.semisaint.unusualachievements.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerAchievementProgress {
	private final UUID playerId;
	private final Map<AchievementId, Long> unlockedAtEpochMillis = new LinkedHashMap<>();
	private String selectedThemeId = CardTheme.NONE;

	public PlayerAchievementProgress(UUID playerId) {
		this.playerId = playerId;
	}

	public String selectedThemeId() {
		return selectedThemeId;
	}

	/**
	 * Only the theme's own achievement gates it, and that is checked by the caller holding the
	 * progress - here we just refuse ids that no longer exist, so a removed theme can't stick.
	 */
	public void selectTheme(String themeId) {
		if (CardTheme.isSelectable(themeId)) {
			selectedThemeId = themeId;
		}
	}

	public UUID playerId() {
		return playerId;
	}

	public boolean isUnlocked(AchievementId id) {
		return unlockedAtEpochMillis.containsKey(id);
	}

	/**
	 * Returns false if already unlocked (idempotent) so callers can distinguish
	 * a genuinely new unlock from a repeat evaluation.
	 */
	public boolean unlock(AchievementId id, long timestampEpochMillis) {
		if (unlockedAtEpochMillis.containsKey(id)) {
			return false;
		}
		unlockedAtEpochMillis.put(id, timestampEpochMillis);
		return true;
	}

	public Map<AchievementId, Long> unlockedAtEpochMillis() {
		return unlockedAtEpochMillis;
	}
}
