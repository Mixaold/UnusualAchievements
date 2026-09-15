package dev.semisaint.unusualachievements.core;

import java.util.Map;

public record AchievementDefinition(
	AchievementId id,
	Map<String, LocalizedText> textByLocale,
	String themeId,
	AchievementRule rule
) {
	public static final String FALLBACK_LOCALE = "en_us";

	/**
	 * Achievement text stays server-authoritative (never shipped in a client lang file) so a locked
	 * achievement's name/description can't be data-mined from the installed mod jar - only the caller's
	 * own unlocked entries ever get sent, already resolved to their locale.
	 */
	public LocalizedText textFor(String locale) {
		LocalizedText text = textByLocale.get(locale);
		if (text != null) {
			return text;
		}
		return textByLocale.getOrDefault(FALLBACK_LOCALE, textByLocale.values().iterator().next());
	}
}
