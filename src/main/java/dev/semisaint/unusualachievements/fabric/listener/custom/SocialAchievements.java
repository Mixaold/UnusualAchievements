package dev.semisaint.unusualachievements.fabric.listener.custom;

import dev.semisaint.unusualachievements.core.TimedFlag;
import dev.semisaint.unusualachievements.fabric.listener.UnlockDispatcher;
import dev.semisaint.unusualachievements.fabric.registry.AchievementDefinitions;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.UUID;

/**
 * The one achievement family driven by fabric-message-api-v1's ServerMessageEvents rather than an
 * entity/block/item event.
 */
public final class SocialAchievements {
	private static final long LAST_WORDS_WINDOW_TICKS = 600; // 30s
	/**
	 * The trigger phrase, in each language the mod ships. It used to be the Russian word alone, which
	 * made last_words - and with it the Library theme - unreachable for anyone playing in English, on
	 * a mod that has a full en_us translation. Matched as a suffix so "ну всё, прощайте" still counts.
	 */
	private static final String[] LAST_WORDS_SUFFIXES = {"прощайте", "farewell", "goodbye"};

	private static final Guard.Site CHAT_SITE = new Guard.Site("social chat listener");

	// Read by CombatDeathAchievements.onDeath for last_words.
	static final TimedFlag<UUID> LAST_WORDS_ARMED = new TimedFlag<>();

	private SocialAchievements() {
	}

	public static void register() {
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, boundChatType) -> Guard.run(CHAT_SITE, () ->
			onChatMessage(message.signedContent(), sender)));
	}

	/** Per-world tick windows - see CombatDeathAchievements.reset(). */
	public static void reset() {
		LAST_WORDS_ARMED.clearAll();
	}

	private static void onChatMessage(String content, ServerPlayer sender) {
		if (!(sender.level() instanceof ServerLevel level)) {
			return;
		}
		String trimmed = content.trim();
		if (trimmed.equals("?")) {
			UnlockDispatcher.unlock(sender, AchievementDefinitions.SINGLE_QUESTION);
		}
		if (isFarewell(trimmed)) {
			LAST_WORDS_ARMED.mark(sender.getUUID(), level.getServer().getTickCount(), LAST_WORDS_WINDOW_TICKS);
		}
	}

	private static boolean isFarewell(String trimmed) {
		String lowered = trimmed.toLowerCase(Locale.ROOT);
		for (String suffix : LAST_WORDS_SUFFIXES) {
			if (lowered.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}
}
