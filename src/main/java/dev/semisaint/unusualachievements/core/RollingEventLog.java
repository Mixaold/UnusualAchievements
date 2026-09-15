package dev.semisaint.unusualachievements.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keeps a per-key rolling window of recent (value, tick) entries and counts distinct values still
 * inside the window - for achievements shaped like "do N different things within a short window"
 * (open shulker boxes of 5 different colors within a minute; land kills with 3 different weapon
 * types within 10 seconds). Not persisted - losing this history across a server restart is an
 * acceptable simplification at this window scale (seconds to a minute).
 */
public final class RollingEventLog<K, V> {
	private final Map<K, Deque<Entry<V>>> entriesByKey = new HashMap<>();

	public int recordAndCountDistinct(K key, V value, long currentTick, long windowTicks) {
		Deque<Entry<V>> entries = entriesByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
		entries.addLast(new Entry<>(value, currentTick));
		long cutoff = currentTick - windowTicks;
		while (!entries.isEmpty() && entries.peekFirst().tick() < cutoff) {
			entries.pollFirst();
		}
		Set<V> distinct = new HashSet<>();
		for (Entry<V> entry : entries) {
			distinct.add(entry.value());
		}
		return distinct.size();
	}

	public void clear(K key) {
		entriesByKey.remove(key);
	}

	/** Drops keys whose whole window has rolled past, so a log keyed per player never outlives them. */
	public void pruneExpired(long currentTick, long windowTicks) {
		long cutoff = currentTick - windowTicks;
		entriesByKey.values().removeIf(entries -> {
			while (!entries.isEmpty() && entries.peekFirst().tick() < cutoff) {
				entries.pollFirst();
			}
			return entries.isEmpty();
		});
	}

	public void clearAll() {
		entriesByKey.clear();
	}

	private record Entry<V>(V value, long tick) {
	}
}
