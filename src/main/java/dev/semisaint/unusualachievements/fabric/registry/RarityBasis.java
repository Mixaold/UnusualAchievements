package dev.semisaint.unusualachievements.fabric.registry;

import dev.semisaint.unusualachievements.core.AchievementId;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns an unlock into a number about the player's own game: "one time in 3418 kills".
 *
 * <p>Vanilla keeps no counter for most of what this mod rewards - nothing tracks kills made while
 * falling, or trades closed at noon - so each achievement is paired with the closest counter that
 * genuinely exists and honestly describes the opportunities the player had. Where nothing fits at
 * all, the denominator is playtime, which is at least true: it says how long you played before this
 * happened once, and claims nothing more.
 */
public enum RarityBasis {
	MOB_KILLS(Stats.MOB_KILLS, "mob_kills", 1, true),
	PLAYER_KILLS(Stats.PLAYER_KILLS, "player_kills", 1, true),
	DEATHS(Stats.DEATHS, "deaths", 1, true),
	/** Ticks -> hours: 20 ticks a second, 3600 seconds an hour. */
	PLAY_TIME(Stats.PLAY_TIME, "play_time", 72_000, false),
	CAKE(Stats.EAT_CAKE_SLICE, "cake", 1, true),
	ENCHANTS(Stats.ENCHANT_ITEM, "enchants", 1, true),
	BELL_RINGS(Stats.BELL_RING, "bell_rings", 1, true),
	NOTES(Stats.PLAY_NOTEBLOCK, "notes", 1, true),
	SHULKERS(Stats.OPEN_SHULKER_BOX, "shulkers", 1, true),
	TRADES(Stats.TRADED_WITH_VILLAGER, "trades", 1, true),
	VILLAGER_TALKS(Stats.TALKED_TO_VILLAGER, "villager_talks", 1, true),
	NIGHTS(Stats.SLEEP_IN_BED, "nights", 1, true),
	RAIDS_WON(Stats.RAID_WIN, "raids_won", 1, true),
	/** Distance stats are counted in centimetres. */
	MINECART_METRES(Stats.MINECART_ONE_CM, "minecart_metres", 100, false);

	private final Identifier stat;
	private final String key;
	private final int divisor;
	private final boolean percentable;

	RarityBasis(Identifier stat, String key, int divisor, boolean percentable) {
		this.stat = stat;
		this.key = key;
		this.divisor = divisor;
		this.percentable = percentable;
	}

	/**
	 * Whether "one in N" can honestly be restated as a percentage. Countable attempts can - one kill in
	 * 3418 really is 0.03% of your kills. Hours played and metres travelled cannot: they are a rate,
	 * not a share of anything, and "0.03% of your hours" would be a number pretending to be a statistic.
	 */
	public boolean percentable() {
		return percentable;
	}

	/** Lang key holding the whole sentence, so each language can put the number where it belongs. */
	public String translationKey() {
		return "rarity.unusualachievements." + key;
	}

	public long valueFor(ServerPlayer player) {
		return (long) player.getStats().getValue(Stats.CUSTOM.get(stat)) / divisor;
	}

	private static final Map<AchievementId, RarityBasis> BY_ACHIEVEMENT = new HashMap<>();

	private static void map(RarityBasis basis, String... achievementPaths) {
		for (String path : achievementPaths) {
			BY_ACHIEVEMENT.put(new AchievementId(path), basis);
		}
	}

	static {
		map(MOB_KILLS, "pyromancers_handshake", "falling_star", "blind_marksman", "cavalry_duel",
			"triple_kill", "snowball_assassin", "warden_ghost", "dragon_no_totem", "naked_king",
			"lightning_farmer", "staring_contest", "mooshroom_lightning");
		map(PLAYER_KILLS, "hairs_breadth_duel");
		map(DEATHS, "anvil_of_regret", "friendly_fire_apology", "own_tnt_death", "insurance_speedrun",
			"last_words", "elytra_denial");
		map(CAKE, "cake_addict");
		map(ENCHANTS, "paperwork_demon");
		map(BELL_RINGS, "bell_ringer");
		map(NOTES, "noise_complaint");
		map(SHULKERS, "shulker_collector");
		map(TRADES, "noon_deal", "trade_betrayal");
		map(VILLAGER_TALKS, "wrong_target");
		map(NIGHTS, "honest_sleep", "groundhog_day", "wrong_time", "slept_through_the_raid", "bedtime_bomb");
		map(RAIDS_WON, "raid_diplomat");
		map(MINECART_METRES, "minecart_kidnapping");
		// No vanilla counter describes these at all - playtime is the honest fallback.
		map(PLAY_TIME, "no_reason", "bad_food_critic", "just_because", "truce", "fourth_wall",
			"nothing_to_see", "single_question", "polite_to_monsters", "wrong_animal", "rain_fire",
			"goth_phase", "statue", "world_bottom", "buried_alive_escape", "bee_gauntlet",
			"last_ingot_gamble");
	}

	public static RarityBasis forAchievement(AchievementId id) {
		return BY_ACHIEVEMENT.get(id);
	}
}
