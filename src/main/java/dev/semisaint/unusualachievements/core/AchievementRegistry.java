package dev.semisaint.unusualachievements.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AchievementRegistry {
	private static final Map<AchievementId, AchievementDefinition> DEFINITIONS = new LinkedHashMap<>();

	private AchievementRegistry() {
	}

	public static void register(AchievementDefinition definition) {
		if (DEFINITIONS.containsKey(definition.id())) {
			throw new IllegalStateException("Duplicate achievement id: " + definition.id());
		}
		DEFINITIONS.put(definition.id(), definition);
	}

	public static Optional<AchievementDefinition> get(AchievementId id) {
		return Optional.ofNullable(DEFINITIONS.get(id));
	}

	public static Collection<AchievementDefinition> all() {
		return DEFINITIONS.values();
	}

	public static List<AchievementDefinition> statThresholdDefinitions() {
		List<AchievementDefinition> result = new ArrayList<>();
		for (AchievementDefinition definition : DEFINITIONS.values()) {
			if (definition.rule() instanceof StatThresholdRule) {
				result.add(definition);
			}
		}
		return result;
	}
}
