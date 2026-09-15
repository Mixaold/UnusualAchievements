package dev.semisaint.unusualachievements.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Records state enter/exit ticks per key instead of recomputing "has this been true for N ticks"
 * on every tick, so sustained-condition checks (e.g. "stood in lava for N seconds") stay O(1).
 */
public final class SustainedStateTracker<K> {
	private final Map<K, Long> enterTick = new HashMap<>();

	public void setState(K key, boolean active, long currentTick) {
		if (active) {
			enterTick.putIfAbsent(key, currentTick);
		} else {
			enterTick.remove(key);
		}
	}

	public boolean isSustainedFor(K key, long currentTick, long minTicks) {
		Long entered = enterTick.get(key);
		return entered != null && currentTick - entered >= minTicks;
	}

	public void clear(K key) {
		enterTick.remove(key);
	}

	public void clearAll() {
		enterTick.clear();
	}
}
