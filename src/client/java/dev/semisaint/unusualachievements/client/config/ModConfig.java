package dev.semisaint.unusualachievements.client.config;

import java.util.ArrayList;
import java.util.List;

public final class ModConfig {
	public boolean overlayEnabled = true;
	public boolean seenOpenCardHint = false;
	/**
	 * Themes the player has already laid eyes on in the picker. The badge on the Themes button counts
	 * earned themes that are NOT in here, so it behaves like a notification - lit until you look,
	 * dark afterwards, and lit again the next time a new one is earned.
	 */
	public List<String> seenThemes = new ArrayList<>();

	/** Gson writes fields straight from the file, so a hand-edited config can hand back a null list. */
	ModConfig sanitized() {
		if (seenThemes == null) {
			seenThemes = new ArrayList<>();
		} else {
			seenThemes.removeIf(id -> id == null || id.isEmpty());
		}
		return this;
	}
}
