package dev.semisaint.unusualachievements.core;

public record AchievementId(String path) {
	public AchievementId {
		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("Achievement id path must not be empty");
		}
	}
}
