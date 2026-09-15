package dev.semisaint.unusualachievements.core;

import java.util.HashMap;
import java.util.Map;

/**
 * "Mark key K, the flag stays valid for N ticks after this call" - for achievements shaped like
 * "do X, then Y within N seconds" (hit an iron golem, then die to its retaliation within 5
 * seconds). Distinct from SustainedStateTracker, which tracks a *continuous* state rather than a
 * short window opened by a one-off event.
 */
public final class TimedFlag<K> {
	private final Map<K, Long> expiresAtTick = new HashMap<>();

	public void mark(K key, long currentTick, long durationTicks) {
		expiresAtTick.put(key, currentTick + durationTicks);
	}

	public boolean isActive(K key, long currentTick) {
		Long expires = expiresAtTick.get(key);
		return expires != null && currentTick <= expires;
	}

	public void clear(K key) {
		expiresAtTick.remove(key);
	}

	/**
	 * Drops entries whose window has already closed. Needed because several of these flags are keyed
	 * by the UUID of a short-lived *mob*, not of a player: every snowball that clips any mob, every
	 * punch landed on an iron golem and every completed villager trade adds a key that nothing else
	 * would ever remove. isActive() reads correctly either way - this only stops the map from being a
	 * slow leak on a long-running server.
	 */
	public void pruneExpired(long currentTick) {
		expiresAtTick.values().removeIf(expires -> currentTick > expires);
	}

	public void clearAll() {
		expiresAtTick.clear();
	}
}
