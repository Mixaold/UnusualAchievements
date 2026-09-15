package dev.semisaint.unusualachievements.core;

/**
 * Marker rule for achievements whose trigger logic lives in bespoke event-listener code
 * rather than in generically-evaluated data.
 */
public record CustomEventRule(String description) implements AchievementRule {
}
