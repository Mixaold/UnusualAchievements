package dev.semisaint.unusualachievements.core;

public record RarityInfo(int unlockedCount, int totalPlayers) {
	public double fraction() {
		return totalPlayers <= 0 ? 0.0 : (double) unlockedCount / totalPlayers;
	}
}
