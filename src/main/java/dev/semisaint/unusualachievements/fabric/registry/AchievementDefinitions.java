package dev.semisaint.unusualachievements.fabric.registry;

import dev.semisaint.unusualachievements.core.AchievementDefinition;
import dev.semisaint.unusualachievements.core.AchievementId;
import dev.semisaint.unusualachievements.core.AchievementRegistry;
import dev.semisaint.unusualachievements.core.CustomEventRule;
import dev.semisaint.unusualachievements.core.LocalizedText;
import dev.semisaint.unusualachievements.core.StatThresholdRule;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;

import java.util.Map;

public final class AchievementDefinitions {
	// --- Flagship (unchanged since the mod's first release) ---
	public static final AchievementId PYROMANCERS_HANDSHAKE = new AchievementId("pyromancers_handshake");

	// --- Tier 1: common, vanilla-stat-driven ---
	public static final AchievementId CAKE_ADDICT = new AchievementId("cake_addict");
	public static final AchievementId PAPERWORK_DEMON = new AchievementId("paperwork_demon");
	public static final AchievementId BELL_RINGER = new AchievementId("bell_ringer");

	// --- Tier 1b: simple but unusual ---
	public static final AchievementId NO_REASON = new AchievementId("no_reason");
	public static final AchievementId BAD_FOOD_CRITIC = new AchievementId("bad_food_critic");
	public static final AchievementId HONEST_SLEEP = new AchievementId("honest_sleep");
	public static final AchievementId FALLING_STAR = new AchievementId("falling_star");
	public static final AchievementId NOON_DEAL = new AchievementId("noon_deal");
	public static final AchievementId JUST_BECAUSE = new AchievementId("just_because");
	public static final AchievementId TRUCE = new AchievementId("truce");
	public static final AchievementId INSURANCE_SPEEDRUN = new AchievementId("insurance_speedrun");
	public static final AchievementId FOURTH_WALL = new AchievementId("fourth_wall");
	public static final AchievementId NOTHING_TO_SEE = new AchievementId("nothing_to_see");
	public static final AchievementId SINGLE_QUESTION = new AchievementId("single_question");
	public static final AchievementId POLITE_TO_MONSTERS = new AchievementId("polite_to_monsters");
	public static final AchievementId WRONG_ANIMAL = new AchievementId("wrong_animal");
	public static final AchievementId RAIN_FIRE = new AchievementId("rain_fire");
	public static final AchievementId GOTH_PHASE = new AchievementId("goth_phase");
	public static final AchievementId STATUE = new AchievementId("statue");
	public static final AchievementId NOISE_COMPLAINT = new AchievementId("noise_complaint");
	public static final AchievementId WRONG_TIME = new AchievementId("wrong_time");
	public static final AchievementId WRONG_TARGET = new AchievementId("wrong_target");
	public static final AchievementId GROUNDHOG_DAY = new AchievementId("groundhog_day");

	// --- Tier 2: uncommon ---
	public static final AchievementId SHULKER_COLLECTOR = new AchievementId("shulker_collector");
	public static final AchievementId STARING_CONTEST = new AchievementId("staring_contest");
	public static final AchievementId CAVALRY_DUEL = new AchievementId("cavalry_duel");
	public static final AchievementId MOOSHROOM_LIGHTNING = new AchievementId("mooshroom_lightning");
	public static final AchievementId LAST_INGOT_GAMBLE = new AchievementId("last_ingot_gamble");
	public static final AchievementId BLIND_MARKSMAN = new AchievementId("blind_marksman");

	// --- Tier 3: rare ---
	public static final AchievementId ANVIL_OF_REGRET = new AchievementId("anvil_of_regret");
	public static final AchievementId FRIENDLY_FIRE_APOLOGY = new AchievementId("friendly_fire_apology");
	public static final AchievementId LIGHTNING_FARMER = new AchievementId("lightning_farmer");
	public static final AchievementId LAST_WORDS = new AchievementId("last_words");
	public static final AchievementId WORLD_BOTTOM = new AchievementId("world_bottom");
	public static final AchievementId BEDTIME_BOMB = new AchievementId("bedtime_bomb");
	public static final AchievementId SLEPT_THROUGH_THE_RAID = new AchievementId("slept_through_the_raid");
	public static final AchievementId OWN_TNT_DEATH = new AchievementId("own_tnt_death");
	public static final AchievementId TRADE_BETRAYAL = new AchievementId("trade_betrayal");
	public static final AchievementId BURIED_ALIVE_ESCAPE = new AchievementId("buried_alive_escape");
	public static final AchievementId BEE_GAUNTLET = new AchievementId("bee_gauntlet");
	public static final AchievementId TRIPLE_KILL = new AchievementId("triple_kill");
	public static final AchievementId SNOWBALL_ASSASSIN = new AchievementId("snowball_assassin");

	// --- Tier 4: very rare ---
	public static final AchievementId MINECART_KIDNAPPING = new AchievementId("minecart_kidnapping");
	public static final AchievementId ELYTRA_DENIAL = new AchievementId("elytra_denial");
	public static final AchievementId HAIRS_BREADTH_DUEL = new AchievementId("hairs_breadth_duel");
	public static final AchievementId WARDEN_GHOST = new AchievementId("warden_ghost");
	public static final AchievementId DRAGON_NO_TOTEM = new AchievementId("dragon_no_totem");
	public static final AchievementId NAKED_KING = new AchievementId("naked_king");
	public static final AchievementId RAID_DIPLOMAT = new AchievementId("raid_diplomat");

	/**
	 * StatType's internal map is identity-keyed, so a freshly-built Identifier.withDefaultNamespace(path)
	 * (equal but not the same instance) fails the lookup - custom stat paths must resolve to the exact
	 * Identifier instance vanilla registered (e.g. Stats.EAT_CAKE_SLICE), not a reconstructed equivalent.
	 */
	private static final Map<String, Identifier> VANILLA_CUSTOM_STAT_IDS = Map.of(
		"eat_cake_slice", Stats.EAT_CAKE_SLICE,
		"enchant_item", Stats.ENCHANT_ITEM,
		"bell_ring", Stats.BELL_RING
	);

	private AchievementDefinitions() {
	}

	/** Null for an unmapped path, so a bad definition skips one achievement instead of failing a tick. */
	public static Identifier vanillaCustomStatId(String path) {
		return VANILLA_CUSTOM_STAT_IDS.get(path);
	}

	public static void registerAll() {
		registerFlagship();
		registerTier1Common();
		registerTier1bSimpleButUnusual();
		registerTier2Uncommon();
		registerTier3Rare();
		registerTier4VeryRare();
	}

	private static void registerFlagship() {
		AchievementRegistry.register(new AchievementDefinition(
			PYROMANCERS_HANDSHAKE,
			Map.of(
				"ru_ru", new LocalizedText("Рукопожатие пиромана",
					"Забей крипера насмерть голыми руками, стоя в лаве во время грозы.",
					"Страховка это не покрывает."),
				"en_us", new LocalizedText("Pyromancer's Handshake",
					"Punch a Creeper to death bare-handed while standing in lava during a thunderstorm.",
					"Your insurance does not cover this.")
			),
			"unthemed",
			new CustomEventRule("bare-handed creeper kill while lava-standing during a thunderstorm")
		));
	}

	private static void registerTier1Common() {
		AchievementRegistry.register(new AchievementDefinition(
			CAKE_ADDICT,
			Map.of(
				"ru_ru", new LocalizedText("Королевство пирожных", "Съешь 50 кусков торта.",
					"Ни одного дня рождения не было. Тебе просто нравится торт."),
				"en_us", new LocalizedText("Cake Kingdom", "Eat 50 slices of cake.",
					"There wasn't a single birthday. You just like cake.")
			),
			"unthemed",
			new StatThresholdRule("eat_cake_slice", 50)
		));
		AchievementRegistry.register(new AchievementDefinition(
			PAPERWORK_DEMON,
			Map.of(
				"ru_ru", new LocalizedText("Бумажный демон", "Зачаруй 25 предметов на столе зачарования.",
					"Библиотекарь давно сменил номер."),
				"en_us", new LocalizedText("Paperwork Demon", "Enchant 25 items at an enchanting table.",
					"The librarian changed their number a while ago.")
			),
			"unthemed",
			new StatThresholdRule("enchant_item", 25)
		));
		AchievementRegistry.register(new AchievementDefinition(
			BELL_RINGER,
			Map.of(
				"ru_ru", new LocalizedText("Звонарь", "Ударь по колоколу деревни 30 раз.",
					"Жители уже не реагируют. Им всё равно."),
				"en_us", new LocalizedText("Bell Ringer", "Ring a village bell 30 times.",
					"The villagers stopped reacting. They just don't care anymore.")
			),
			"unthemed",
			new StatThresholdRule("bell_ring", 30)
		));
	}

	private static void registerTier1bSimpleButUnusual() {
		AchievementRegistry.register(new AchievementDefinition(
			NO_REASON,
			Map.of(
				"ru_ru", new LocalizedText("Без особой причины", "Съешь плод хоруса, будучи полностью сытым.",
					"Голод тут ни при чём."),
				"en_us", new LocalizedText("No Particular Reason", "Eat a chorus fruit while completely full.",
					"Hunger has nothing to do with it.")
			),
			"unthemed",
			new CustomEventRule("eat chorus fruit at max hunger")
		));
		AchievementRegistry.register(new AchievementDefinition(
			BAD_FOOD_CRITIC,
			Map.of(
				"ru_ru", new LocalizedText("Плохой критик", "Съешь ядовитую картошку.",
					"В отзыве ты поставишь один балл. Если доживёшь до отзыва."),
				"en_us", new LocalizedText("Bad Food Critic", "Eat a poisonous potato.",
					"One star in the review. If you live to write it.")
			),
			"unthemed",
			new CustomEventRule("eat a poisonous potato")
		));
		AchievementRegistry.register(new AchievementDefinition(
			HONEST_SLEEP,
			Map.of(
				"ru_ru", new LocalizedText("Спал без обмана",
					"Пролежи в кровати до самого утра так, чтобы ночь прошла естественно, без обычного ускорения времени.",
					"Ты единственный, кто сделал это по-настоящему честно."),
				"en_us", new LocalizedText("Honest Sleep",
					"Stay in bed until morning without the usual time-skip kicking in.",
					"You're the only one who actually did it properly.")
			),
			"unthemed",
			new CustomEventRule("sleep a full night with day-time advancing tick-for-tick, no skip")
		));
		AchievementRegistry.register(new AchievementDefinition(
			FALLING_STAR,
			Map.of(
				"ru_ru", new LocalizedText("Падающая звезда", "Убей моба в прыжке, падая с высоты минимум 4 блоков.",
					"Гравитация — тоже оружие. Причём бесплатное."),
				"en_us", new LocalizedText("Falling Star", "Kill a mob mid-air while falling from at least 4 blocks up.",
					"Gravity is a weapon too. And it's free.")
			),
			"unthemed",
			new CustomEventRule("kill a mob while airborne with fallDistance >= 4 blocks")
		));
		AchievementRegistry.register(new AchievementDefinition(
			NOON_DEAL,
			Map.of(
				"ru_ru", new LocalizedText("Ровно в полдень", "Заверши сделку с жителем точно в игровой полдень.",
					"Житель даже не понял, что стал частью ритуала."),
				"en_us", new LocalizedText("High Noon Deal", "Complete a villager trade at exactly in-game noon.",
					"The villager never realized he was part of a ritual.")
			),
			"unthemed",
			new CustomEventRule("complete a villager trade within a few ticks of day-time 6000")
		));
		AchievementRegistry.register(new AchievementDefinition(
			JUST_BECAUSE,
			Map.of(
				"ru_ru", new LocalizedText("Просто так", "Съешь золотое яблоко на полном здоровье, вне боя.",
					"Экономика мира только что тихо заплакала."),
				"en_us", new LocalizedText("Just Because", "Eat a golden apple at full health, outside of combat.",
					"Somewhere, the world's economy just quietly wept.")
			),
			"unthemed",
			new CustomEventRule("eat a golden apple at full health with no monsters nearby")
		));
		AchievementRegistry.register(new AchievementDefinition(
			TRUCE,
			Map.of(
				"ru_ru", new LocalizedText("Временное перемирие",
					"Простой в 3 блоках от крипера 30 секунд, не спровоцировав взрыв.",
					"Пока что вы оба делаете вид, что всё нормально."),
				"en_us", new LocalizedText("Temporary Truce",
					"Stand within 3 blocks of a creeper for 30 seconds without setting it off.",
					"For now, you're both pretending this is normal.")
			),
			"unthemed",
			new CustomEventRule("stand near a non-swelling creeper for 30s")
		));
		AchievementRegistry.register(new AchievementDefinition(
			INSURANCE_SPEEDRUN,
			Map.of(
				"ru_ru", new LocalizedText("Скоростная страховка",
					"Установи точку возрождения и умри в течение 10 секунд.",
					"Страховка оформлена и использована в рекордный срок."),
				"en_us", new LocalizedText("Insurance Speedrun",
					"Set your respawn point and die within 10 seconds.",
					"Policy issued and claimed in record time.")
			),
			"unthemed",
			new CustomEventRule("die within 10s of setting spawn via a bed")
		));
		AchievementRegistry.register(new AchievementDefinition(
			FOURTH_WALL,
			Map.of(
				"ru_ru", new LocalizedText("Ачивка за ачивки",
					"Открой собственную карточку достижений 10 раз за один заход.",
					"Я честно не думал, что кто-то будет делать это специально."),
				"en_us", new LocalizedText("Achievement For Achievements",
					"Open your own achievement card 10 times in one session.",
					"I honestly didn't think anyone would do this on purpose.")
			),
			"unthemed",
			new CustomEventRule("request your own card 10 times in one session")
		));
		AchievementRegistry.register(new AchievementDefinition(
			NOTHING_TO_SEE,
			Map.of(
				"ru_ru", new LocalizedText("Нечего смотреть", "Открой карточку игрока, у которого пока нет ни одного достижения.",
					"Ты заглянул. Там было пусто."),
				"en_us", new LocalizedText("Nothing To See", "Open another player's card while they have zero achievements unlocked.",
					"You looked. It was empty.")
			),
			"unthemed",
			new CustomEventRule("request another player's card while their unlocked list is empty")
		));
		AchievementRegistry.register(new AchievementDefinition(
			SINGLE_QUESTION,
			Map.of(
				"ru_ru", new LocalizedText("Ёмко", "Напиши в чат сообщение, состоящее ровно из одного символа «?».",
					"Исчерпывающий вопрос. Исчерпывающий ответ так и не пришёл."),
				"en_us", new LocalizedText("Concise", "Send a chat message consisting of exactly one character: \"?\".",
					"A complete question. The complete answer never arrived.")
			),
			"unthemed",
			new CustomEventRule("send a chat message that is exactly \"?\"")
		));
		AchievementRegistry.register(new AchievementDefinition(
			POLITE_TO_MONSTERS,
			Map.of(
				"ru_ru", new LocalizedText("Вежливость превыше всего",
					"Взаимодействуй (правый клик) с враждебным мобом голой рукой.",
					"Он не оценил жест."),
				"en_us", new LocalizedText("Politeness Above All",
					"Right-click a hostile mob with an empty hand.",
					"He did not appreciate the gesture.")
			),
			"unthemed",
			new CustomEventRule("empty-hand right-click a hostile mob")
		));
		AchievementRegistry.register(new AchievementDefinition(
			WRONG_ANIMAL,
			Map.of(
				"ru_ru", new LocalizedText("Не туда постригся",
					"Попробуй постричь ножницами моба, которого нельзя постричь (например, корову).",
					"Шерсти там не было. Ты всё равно попробовал."),
				"en_us", new LocalizedText("Wrong Animal",
					"Try to shear a mob that can't be sheared (a cow, for example).",
					"There was no wool. You tried anyway.")
			),
			"unthemed",
			new CustomEventRule("use shears on a cow")
		));
		AchievementRegistry.register(new AchievementDefinition(
			RAIN_FIRE,
			Map.of(
				"ru_ru", new LocalizedText("Костёр под дождём",
					"Разожги костёр (кремнём и сталью) во время дождя.",
					"Физику это не остановило. Тебя тоже."),
				"en_us", new LocalizedText("Campfire In The Rain",
					"Light a campfire with flint and steel during rainfall.",
					"Physics didn't stop it. Neither did you.")
			),
			"unthemed",
			new CustomEventRule("light a campfire with flint and steel while it's raining")
		));
		AchievementRegistry.register(new AchievementDefinition(
			GOTH_PHASE,
			Map.of(
				"ru_ru", new LocalizedText("Готическая фаза",
					"Надень полный комплект кожаной брони, окрашенной в чёрный цвет.",
					"Все проходят через это. Не все документируют."),
				"en_us", new LocalizedText("Goth Phase",
					"Wear a full set of leather armor dyed black.",
					"Everyone goes through it. Not everyone documents it.")
			),
			"unthemed",
			new CustomEventRule("equip a full set of black-dyed leather armor")
		));
		AchievementRegistry.register(new AchievementDefinition(
			STATUE,
			Map.of(
				"ru_ru", new LocalizedText("Живая статуя",
					"Не двигайся и не поворачивай камеру 60 секунд подряд ночью.",
					"Рекорд не засчитан только потому, что его никто не мерил."),
				"en_us", new LocalizedText("Living Statue",
					"Don't move or turn the camera for 60 straight seconds at night.",
					"The record doesn't count only because nobody was timing it.")
			),
			"unthemed",
			new CustomEventRule("stand motionless at night for 60s")
		));
		AchievementRegistry.register(new AchievementDefinition(
			NOISE_COMPLAINT,
			Map.of(
				"ru_ru", new LocalizedText("Жалоба соседей", "Сыграй на нотном блоке (правый клик) ночью.",
					"Соседей нет. Жалоба всё равно есть."),
				"en_us", new LocalizedText("Noise Complaint", "Play a note block at night.",
					"There are no neighbors. There's a complaint anyway.")
			),
			"unthemed",
			new CustomEventRule("right-click a note block at night")
		));
		AchievementRegistry.register(new AchievementDefinition(
			WRONG_TIME,
			Map.of(
				"ru_ru", new LocalizedText("Не вовремя", "Попробуй лечь спать днём.",
					"Кровать оценила энтузиазм, но не поддержала."),
				"en_us", new LocalizedText("Bad Timing", "Try to go to sleep during the day.",
					"The bed appreciated the enthusiasm. It did not comply.")
			),
			"unthemed",
			new CustomEventRule("right-click a bed during the day")
		));
		AchievementRegistry.register(new AchievementDefinition(
			WRONG_TARGET,
			Map.of(
				"ru_ru", new LocalizedText("Не с тем поговорил",
					"Взаимодействуй с ламой странствующего торговца вместо него самого.",
					"Лама тоже не поняла, что происходит."),
				"en_us", new LocalizedText("Wrong Contact",
					"Interact with the wandering trader's llama instead of the trader.",
					"The llama didn't understand what was happening either.")
			),
			"unthemed",
			new CustomEventRule("right-click a trader llama")
		));
		AchievementRegistry.register(new AchievementDefinition(
			GROUNDHOG_DAY,
			Map.of(
				"ru_ru", new LocalizedText("День сурка", "Проспи 5 ночей подряд в одной и той же кровати.",
					"Однообразие — тоже стиль жизни."),
				"en_us", new LocalizedText("Groundhog Day", "Sleep 5 nights in a row in the same bed.",
					"Monotony is a lifestyle too.")
			),
			"unthemed",
			new CustomEventRule("sleep in the same bed 5 consecutive in-game nights")
		));
	}

	private static void registerTier2Uncommon() {
		AchievementRegistry.register(new AchievementDefinition(
			SHULKER_COLLECTOR,
			Map.of(
				"ru_ru", new LocalizedText("Коллекционер шалкеров",
					"Открой шалкеры пяти разных цветов за одну минуту.",
					"Ты явно готовился к переезду."),
				"en_us", new LocalizedText("Shulker Collector",
					"Open shulker boxes of five different colors within one minute.",
					"You were clearly planning to move.")
			),
			"unthemed",
			new CustomEventRule("open shulker boxes of 5 distinct colors within 60s")
		));
		AchievementRegistry.register(new AchievementDefinition(
			STARING_CONTEST,
			Map.of(
				"ru_ru", new LocalizedText("Гляделки",
					"Разозли эндермена, находясь вообще без брони, и продержись рядом 10 секунд без урона.",
					"Он моргнул первым. Наверное."),
				"en_us", new LocalizedText("Staring Contest",
					"Anger an enderman while wearing no armor at all, and last 10 seconds next to it unharmed.",
					"It blinked first. Probably.")
			),
			"unthemed",
			new CustomEventRule("survive 10s next to an angry enderman targeting you, no armor")
		));
		AchievementRegistry.register(new AchievementDefinition(
			CAVALRY_DUEL,
			Map.of(
				"ru_ru", new LocalizedText("Дуэль наездников",
					"Убей жокея (зомби верхом на курице), сам сидя верхом на лошади.",
					"Кавалерия против пехоты. Не совсем честно."),
				"en_us", new LocalizedText("Cavalry Duel",
					"Kill a chicken jockey while riding a horse yourself.",
					"Cavalry versus infantry. Not exactly fair.")
			),
			"unthemed",
			new CustomEventRule("kill a chicken jockey while mounted on a horse")
		));
		AchievementRegistry.register(new AchievementDefinition(
			MOOSHROOM_LIGHTNING,
			Map.of(
				"ru_ru", new LocalizedText("Грозовая корова",
					"Стой рядом, когда молния ударит в грибную корову и изменит её тип.",
					"Наука бессильна. Молния — нет."),
				"en_us", new LocalizedText("Storm Cow",
					"Be nearby when lightning strikes a mooshroom and changes its type.",
					"Science can't explain it. Lightning can.")
			),
			"unthemed",
			new CustomEventRule("witness a mooshroom variant conversion nearby")
		));
		AchievementRegistry.register(new AchievementDefinition(
			LAST_INGOT_GAMBLE,
			Map.of(
				"ru_ru", new LocalizedText("Последний слиток",
					"Отдай пиглину в обмен свой единственный золотой слиток.",
					"Ты поставил всё на кон ради шанса получить гнилую плоть."),
				"en_us", new LocalizedText("Last Ingot Gamble",
					"Barter with a piglin using your one and only gold ingot.",
					"You bet everything on a chance at rotten flesh.")
			),
			"unthemed",
			new CustomEventRule("barter with a piglin holding exactly 1 gold ingot")
		));
		AchievementRegistry.register(new AchievementDefinition(
			BLIND_MARKSMAN,
			Map.of(
				"ru_ru", new LocalizedText("Слепой снайпер",
					"Убей моба выстрелом из лука или арбалета, находясь под эффектом Слепоты.",
					"Ты не видел, куда стрелял. Оно сработало."),
				"en_us", new LocalizedText("Blind Marksman",
					"Kill a mob with a bow or crossbow shot while under the Blindness effect.",
					"You couldn't see where you were shooting. It worked.")
			),
			"unthemed",
			new CustomEventRule("kill a mob with an arrow while blinded")
		));
	}

	private static void registerTier3Rare() {
		AchievementRegistry.register(new AchievementDefinition(
			ANVIL_OF_REGRET,
			Map.of(
				"ru_ru", new LocalizedText("Наковальня сожалений",
					"Сломай блок под наковальней и погибни от её падения в течение 5 сек.",
					"Физика — точная наука. Ты — не физик."),
				"en_us", new LocalizedText("Anvil of Regret",
					"Break the block under an anvil and die to its fall within 5 seconds.",
					"Physics is an exact science. You are not a physicist.")
			),
			"unthemed",
			new CustomEventRule("break the block under an anvil, then die to falling anvil within 5s")
		));
		AchievementRegistry.register(new AchievementDefinition(
			FRIENDLY_FIRE_APOLOGY,
			Map.of(
				"ru_ru", new LocalizedText("Извини за дружественный огонь",
					"Ударь железного голема и погибни от его ответного удара в течение 5 секунд.",
					"Он не умеет прощать. Он умеет бить."),
				"en_us", new LocalizedText("Sorry About the Friendly Fire",
					"Hit an iron golem and die to its retaliation within 5 seconds.",
					"It doesn't know how to forgive. It knows how to hit.")
			),
			"unthemed",
			new CustomEventRule("hit an iron golem, then die to it within 5s")
		));
		AchievementRegistry.register(new AchievementDefinition(
			LIGHTNING_FARMER,
			Map.of(
				"ru_ru", new LocalizedText("Молния на заказ",
					"Стоя на блоке золота во время грозы, убей моба ударом молнии, вызванной трезубцем, "
						+ "зачарованным на призыв молний.",
					"Ты нашёл законный способ обзавестись личным богом грома."),
				"en_us", new LocalizedText("Lightning On Demand",
					"Standing on a block of gold during a thunderstorm, kill a mob with lightning summoned "
						+ "by a trident enchanted to call it down.",
					"You found a legal way to get your own personal thunder god.")
			),
			"unthemed",
			new CustomEventRule("lightning-bolt kill near a player on gold holding a Channeling trident, during a storm")
		));
		AchievementRegistry.register(new AchievementDefinition(
			LAST_WORDS,
			Map.of(
				"ru_ru", new LocalizedText("Последнее слово",
					"Напиши в чат сообщение, заканчивающееся словом «прощайте», и умри в течение 30 сек.",
					"Драматично. Ты явно это распланировал."),
				"en_us", new LocalizedText("Last Words",
					"Send a chat message ending in \"farewell\", then die within 30 seconds.",
					"Dramatic. You clearly planned this.")
			),
			"unthemed",
			new CustomEventRule("die within 30s of a chat message ending in \"прощайте\"")
		));
		AchievementRegistry.register(new AchievementDefinition(
			WORLD_BOTTOM,
			Map.of(
				"ru_ru", new LocalizedText("Дно мира",
					"Сломай любой блок на глубине Y ≤ -60 в Верхнем мире, будучи единственным игроком на сервере.",
					"Здесь тебя никто не услышит. И это радует."),
				"en_us", new LocalizedText("Bottom Of The World",
					"Break any block at Y ≤ -60 in the Overworld while you're the only player on the server.",
					"No one can hear you down here. That's a comfort.")
			),
			"unthemed",
			new CustomEventRule("break a block at Y<=-60 in the Overworld while alone on the server")
		));
		AchievementRegistry.register(new AchievementDefinition(
			BEDTIME_BOMB,
			Map.of(
				"ru_ru", new LocalizedText("Отбой в аду",
					"Попробуй лечь спать в Нижнем мире или Энде и выживи во взрыве кровати.",
					"Кровать не одобрила локацию. И сказала об этом громко."),
				"en_us", new LocalizedText("Lights Out, Down Under",
					"Try to sleep in the Nether or the End and survive the bed's explosion.",
					"The bed did not approve of the location. It said so loudly.")
			),
			"unthemed",
			new CustomEventRule("use a bed outside the Overworld and survive the resulting explosion")
		));
		AchievementRegistry.register(new AchievementDefinition(
			SLEPT_THROUGH_THE_RAID,
			Map.of(
				"ru_ru", new LocalizedText("Проспал нашествие",
					"Ляг спать во время рейда иллагеров и доспи до утра, пока рейд ещё идёт.",
					"Иллагеры так и не поняли, куда ты делся."),
				"en_us", new LocalizedText("Slept Through The Raid",
					"Go to sleep during an illager raid and sleep through to morning while it's still going.",
					"The illagers never did figure out where you went.")
			),
			"unthemed",
			new CustomEventRule("wake from sleep with an active raid at your bed")
		));
		AchievementRegistry.register(new AchievementDefinition(
			OWN_TNT_DEATH,
			Map.of(
				"ru_ru", new LocalizedText("Сам себе сапёр",
					"Подожги TNT и погибни от его взрыва в течение нескольких секунд.",
					"Инструкция по безопасности лежала рядом. Ты даже не открывал её..."),
				"en_us", new LocalizedText("Self-Taught Demolitionist",
					"Light TNT and die to its blast within a few seconds.",
					"The safety instructions were right there. You didn't even open them...")
			),
			"unthemed",
			new CustomEventRule("ignite TNT, then die to an explosion within 5s")
		));
		AchievementRegistry.register(new AchievementDefinition(
			TRADE_BETRAYAL,
			Map.of(
				"ru_ru", new LocalizedText("Клятвопреступник",
					"Убей жителя в течение 5 секунд после сделки с ним.",
					"Гарантийный талон он выписать не успел."),
				"en_us", new LocalizedText("Oathbreaker",
					"Kill a villager within 5 seconds of trading with him.",
					"He didn't even get to write out the warranty.")
			),
			"unthemed",
			new CustomEventRule("kill a villager within 5s of completing a trade with them")
		));
		AchievementRegistry.register(new AchievementDefinition(
			BURIED_ALIVE_ESCAPE,
			Map.of(
				"ru_ru", new LocalizedText("Похоронен заживо (почти)",
					"Получи урон от удушения в блоке и останься в живых с менее чем 2 сердцами здоровья.",
					"Клаустрофобия — это теперь личное."),
				"en_us", new LocalizedText("Buried Alive (Almost)",
					"Take suffocation damage from being stuck in a block and survive with less than 2 hearts.",
					"Claustrophobia just got personal.")
			),
			"unthemed",
			new CustomEventRule("survive in-wall suffocation damage below 2 hearts")
		));
		AchievementRegistry.register(new AchievementDefinition(
			BEE_GAUNTLET,
			Map.of(
				"ru_ru", new LocalizedText("Пчелиный гнев",
					"Получи урон от трёх разных пчёл в течение 5 секунд, не имея шлема на голове.",
					"Пчёлы запомнили твоё лицо. В прямом смысле."),
				"en_us", new LocalizedText("Bee Gauntlet",
					"Take damage from three different bees within 5 seconds while wearing no helmet.",
					"The bees remember your face. Literally.")
			),
			"unthemed",
			new CustomEventRule("take stings from 3 distinct bees within 5s, no helmet")
		));
		AchievementRegistry.register(new AchievementDefinition(
			TRIPLE_KILL,
			Map.of(
				"ru_ru", new LocalizedText("Тройное попадание",
					"Убей трёх мобов за 10 секунд, каждый раз — новым видом оружия.",
					"Статистика говорит — это либо мастерство, либо паника."),
				"en_us", new LocalizedText("Triple Threat",
					"Kill three mobs within 10 seconds, using a different weapon type each time.",
					"Statistically, this is either skill or panic.")
			),
			"unthemed",
			new CustomEventRule("land kills with 3 distinct weapon types within 10s")
		));
		AchievementRegistry.register(new AchievementDefinition(
			SNOWBALL_ASSASSIN,
			Map.of(
				"ru_ru", new LocalizedText("Снежный убийца",
					"Столкни моба снежком в пропасть так, чтобы он погиб от падения.",
					"Технически ты его не бил. Технически он мёртв."),
				"en_us", new LocalizedText("Snowball Assassin",
					"Knock a mob off a ledge with a snowball so it dies from the fall.",
					"Technically you didn't hit it. Technically it's dead.")
			),
			"unthemed",
			new CustomEventRule("mob dies from fall damage within 3s of being hit by your snowball")
		));
	}

	private static void registerTier4VeryRare() {
		AchievementRegistry.register(new AchievementDefinition(
			MINECART_KIDNAPPING,
			Map.of(
				"ru_ru", new LocalizedText("Вагонетка-похититель",
					"Прокати жителя в вагонетке без остановки минимум минуту.",
					"Согласия у него никто не спрашивал. Едет молча — а что ему остаётся?)"),
				"en_us", new LocalizedText("Minecart Kidnapping",
					"Cart a villager around non-stop for at least a minute.",
					"Nobody asked for his consent. He's riding quietly - what else can he do?")
			),
			"unthemed",
			new CustomEventRule("a moving minecart carrying a villager stays near you for 60s")
		));
		AchievementRegistry.register(new AchievementDefinition(
			ELYTRA_DENIAL,
			Map.of(
				"ru_ru", new LocalizedText("Свобода отменяется",
					"Раскрой элитру и разбейся о землю в течение 2 сек после старта полёта.",
					"Полёт и посадка — почти одновременно. Рекорд, которым не гордятся."),
				"en_us", new LocalizedText("Freedom Denied",
					"Deploy an elytra and crash into the ground within 2 seconds of takeoff.",
					"Takeoff and landing, almost simultaneous. Not a record you brag about.")
			),
			"unthemed",
			new CustomEventRule("die to fall damage within 2s of deploying an elytra")
		));
		AchievementRegistry.register(new AchievementDefinition(
			HAIRS_BREADTH_DUEL,
			Map.of(
				"ru_ru", new LocalizedText("Дуэль на волоске",
					"Убей другого игрока, если у вас обоих было пол-сердца в момент удара (нужен второй игрок).",
					"Что-ж, тебе повезло чуть больше."),
				"en_us", new LocalizedText("Hair's Breadth Duel",
					"Kill another player when you both had half a heart of health at the moment of the blow.",
					"Well, you got slightly luckier.")
			),
			"unthemed",
			new CustomEventRule("PvP kill where both players were at half a heart on the fatal hit")
		));
		AchievementRegistry.register(new AchievementDefinition(
			WARDEN_GHOST,
			Map.of(
				"ru_ru", new LocalizedText("Тише, чем тишина",
					"Нанеси смертельный удар Стражу, ни разу не получив от него урон за всю встречу.",
					"Он тебя даже не услышал. Вот это мастерство — или удача."),
				"en_us", new LocalizedText("Quieter Than Silence",
					"Deal the killing blow to a Warden without ever taking damage from it during the whole encounter.",
					"It never even heard you. That's skill - or luck.")
			),
			"unthemed",
			new CustomEventRule("kill a Warden that has never damaged you")
		));
		AchievementRegistry.register(new AchievementDefinition(
			DRAGON_NO_TOTEM,
			Map.of(
				"ru_ru", new LocalizedText("На честном слове",
					"Нанеси добивающий удар Дракону Края, имея пол-сердца здоровья и ни одного Тотема "
						+ "Бессмертия в инвентаре.",
					"Статистика была против тебя. Ты не слушал статистику."),
				"en_us", new LocalizedText("On A Wing And A Prayer",
					"Land the finishing blow on the Ender Dragon with half a heart of health and no Totem "
						+ "of Undying in your inventory.",
					"The odds were against you. You don't listen to odds.")
			),
			"unthemed",
			new CustomEventRule("kill the Ender Dragon at half a heart with no totem in inventory")
		));
		AchievementRegistry.register(new AchievementDefinition(
			NAKED_KING,
			Map.of(
				"ru_ru", new LocalizedText("Голый король",
					"Убей Иссушителя, не имея на себе ни одного предмета брони.",
					"Броня — это костыль для слабых. Или просто здравый смысл, которым ты пренебрёг."),
				"en_us", new LocalizedText("The Naked King",
					"Kill the Wither while wearing no armor at all.",
					"Armor is a crutch for the weak. Or just common sense, which you ignored.")
			),
			"unthemed",
			new CustomEventRule("kill the Wither with all four armor slots empty")
		));
		AchievementRegistry.register(new AchievementDefinition(
			RAID_DIPLOMAT,
			Map.of(
				"ru_ru", new LocalizedText("Дипломатическая неприкосновенность",
					"Переживи рейд иллагеров от начала до победы, не получив ни единицы урона.",
					"Они не посмели. Или не заметили."),
				"en_us", new LocalizedText("Diplomatic Immunity",
					"Survive an illager raid from start to victory without taking a single point of damage.",
					"They didn't dare. Or they didn't notice.")
			),
			"unthemed",
			new CustomEventRule("survive a raid to victory while never losing health")
		));
	}
}
