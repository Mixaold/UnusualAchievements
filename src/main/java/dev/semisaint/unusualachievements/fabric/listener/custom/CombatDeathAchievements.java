package dev.semisaint.unusualachievements.fabric.listener.custom;

import dev.semisaint.unusualachievements.core.RollingEventLog;
import dev.semisaint.unusualachievements.core.TimedFlag;
import dev.semisaint.unusualachievements.fabric.listener.LavaStandPoller;
import dev.semisaint.unusualachievements.fabric.listener.UnlockDispatcher;
import dev.semisaint.unusualachievements.fabric.registry.AchievementDefinitions;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every achievement whose trigger is "an entity died" or "an entity took damage", clustered here
 * because they all hang off the same net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
 * class (AFTER_DEATH / AFTER_DAMAGE / MOB_CONVERSION). pyromancers_handshake lives here too - it is
 * the original, already-shipped custom achievement, just relocated out of the single do-everything
 * CustomAchievementListener this cluster replaces.
 */
public final class CombatDeathAchievements {
	private static final long MIN_LAVA_TICKS = 60;
	private static final long FRIENDLY_FIRE_WINDOW_TICKS = 100; // 5s
	private static final long SNOWBALL_ASSASSIN_WINDOW_TICKS = 60; // 3s
	private static final long TRIPLE_KILL_WINDOW_TICKS = 200; // 10s
	private static final int TRIPLE_KILL_DISTINCT_WEAPONS = 3;
	private static final long BEE_GAUNTLET_WINDOW_TICKS = 100; // 5s
	private static final int BEE_GAUNTLET_DISTINCT_BEES = 3;
	private static final long HAIRS_BREADTH_WINDOW_TICKS = 20; // 1s - same-tick in practice, small buffer
	private static final float HALF_HEART = 1f;
	private static final double LIGHTNING_FARMER_RANGE_SQ = 16.0 * 16.0;
	private static final double FALLING_STAR_MIN_FALL_DISTANCE = 4.0;

	private static final Guard.Site DEATH_SITE = new Guard.Site("combat death listener");
	private static final Guard.Site DAMAGE_SITE = new Guard.Site("combat damage listener");
	private static final Guard.Site ALLOW_DAMAGE_SITE = new Guard.Site("combat allow-damage listener");
	private static final Guard.Site CONVERSION_SITE = new Guard.Site("mob conversion listener");
	private static final Guard.Site ELYTRA_POLL_SITE = new Guard.Site("elytra deploy poll tick");
	private static final Guard.Site DISCONNECT_SITE = new Guard.Site("combat disconnect cleanup");

	/** Once every 30s: these windows are 1-10s long, so anything swept is long dead. */
	private static final int HOUSEKEEPING_INTERVAL_TICKS = 600;

	// Marked when a player hits an iron golem / a snowball hits a mob / a villager trade completes
	// (the latter marked from InteractionAchievements) - each is a short window during which a
	// specific follow-up death completes the achievement.
	private static final TimedFlag<UUID> FRIENDLY_FIRE_ARMED = new TimedFlag<>();
	private static final TimedFlag<UUID> SNOWBALLED = new TimedFlag<>();
	private static final TimedFlag<UUID> HAIRS_BREADTH_ARMED = new TimedFlag<>();
	static final TimedFlag<UUID> TRADE_BETRAYAL_ARMED = new TimedFlag<>();
	// Armed by BlockBreakAchievements when a player breaks the block directly under an anvil -
	// keyed by player, not by anvil, since the anvil entity that eventually lands the hit is a
	// different object from the block that started falling.
	static final TimedFlag<UUID> ANVIL_ARMED = new TimedFlag<>();

	// warden_ghost doesn't fit any of the shared primitives: it needs "which wardens have ever hit
	// this player", not a single sustained/timed flag - a dedicated one-off map, as the plan calls for.
	private static final Map<UUID, Set<UUID>> WARDENS_THAT_HIT_PLAYER = new HashMap<>();
	// snowball_assassin: fall damage has no attacker entity, so the credited player can't be read
	// off the fatal DamageSource - it has to be looked up from who threw the snowball earlier.
	private static final Map<UUID, UUID> SNOWBALL_THROWER = new HashMap<>();
	// elytra_denial: per-tick isFallFlying() transition tracking - no confirmed Fabric event fires
	// on elytra deployment, so this is a poll like everything else here without one.
	private static final Map<UUID, Boolean> WAS_FALL_FLYING = new HashMap<>();
	private static final TimedFlag<UUID> ELYTRA_JUST_DEPLOYED = new TimedFlag<>();
	private static final long ELYTRA_DENIAL_WINDOW_TICKS = 40; // 2s

	private static final RollingEventLog<UUID, Object> KILL_WEAPON_LOG = new RollingEventLog<>();
	private static final RollingEventLog<UUID, UUID> BEE_STING_LOG = new RollingEventLog<>();

	private CombatDeathAchievements() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> Guard.run(DEATH_SITE, () ->
			onDeath(entity, damageSource)));
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> Guard.run(DAMAGE_SITE, () ->
			onDamage(entity, source)));
		ServerLivingEntityEvents.MOB_CONVERSION.register((original, converted, params) -> Guard.run(CONVERSION_SITE, () ->
			onConversion(original, converted)));

		// hairs_breadth_duel needs each combatant's health *before* the blow that might kill them -
		// AFTER_DAMAGE/AFTER_DEATH already reflect post-hit (possibly clamped-to-zero) health, so this
		// has to be a pre-check. Always returns true: it only observes, never blocks the hit.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			Guard.run(ALLOW_DAMAGE_SITE, () -> checkHairsBreadthDuel(entity, source));
			return true;
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> Guard.run(ELYTRA_POLL_SITE, () -> {
			long currentTick = server.getTickCount();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				boolean flyingNow = player.isFallFlying();
				boolean flyingBefore = WAS_FALL_FLYING.getOrDefault(player.getUUID(), false);
				if (flyingNow && !flyingBefore) {
					ELYTRA_JUST_DEPLOYED.mark(player.getUUID(), currentTick, ELYTRA_DENIAL_WINDOW_TICKS);
				}
				WAS_FALL_FLYING.put(player.getUUID(), flyingNow);
			}
			if (currentTick % HOUSEKEEPING_INTERVAL_TICKS == 0) {
				pruneExpired(currentTick);
			}
		}));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Guard.run(DISCONNECT_SITE, () -> {
			ServerPlayer player = handler.getPlayer();
			if (player == null) {
				return;
			}
			UUID id = player.getUUID();
			WAS_FALL_FLYING.remove(id);
			WARDENS_THAT_HIT_PLAYER.remove(id);
			ELYTRA_JUST_DEPLOYED.clear(id);
			HAIRS_BREADTH_ARMED.clear(id);
			KILL_WEAPON_LOG.clear(id);
			BEE_STING_LOG.clear(id);
		}));
	}

	/**
	 * The entity-keyed windows are the ones that matter here: SNOWBALLED and SNOWBALL_THROWER gain a
	 * key for every mob any thrown projectile clips, FRIENDLY_FIRE_ARMED for every punched iron golem,
	 * TRADE_BETRAYAL_ARMED for every completed trade - and nothing removes them, because the entity
	 * they name is usually dead or unloaded long before anyone would look the key up again.
	 */
	private static void pruneExpired(long currentTick) {
		FRIENDLY_FIRE_ARMED.pruneExpired(currentTick);
		SNOWBALLED.pruneExpired(currentTick);
		HAIRS_BREADTH_ARMED.pruneExpired(currentTick);
		TRADE_BETRAYAL_ARMED.pruneExpired(currentTick);
		ANVIL_ARMED.pruneExpired(currentTick);
		ELYTRA_JUST_DEPLOYED.pruneExpired(currentTick);
		// Paired with SNOWBALLED - the thrower lookup is only meaningful while that flag is live.
		SNOWBALL_THROWER.keySet().removeIf(victimId -> !SNOWBALLED.isActive(victimId, currentTick));
		KILL_WEAPON_LOG.pruneExpired(currentTick, TRIPLE_KILL_WINDOW_TICKS);
		BEE_STING_LOG.pruneExpired(currentTick, BEE_GAUNTLET_WINDOW_TICKS);
	}

	/**
	 * Every window here is measured against MinecraftServer.getTickCount(), which restarts at 0 with
	 * each world. Carried across, an "expires at tick 500 000" left over from the previous world reads
	 * as active for the whole first seven hours of the next one - so the first death in a fresh world
	 * would hand out insurance_speedrun, own_tnt_death and friends for free. LavaStandPoller already
	 * documented and handled this for its own tracker; the rest of the state needs the same treatment.
	 */
	public static void reset() {
		FRIENDLY_FIRE_ARMED.clearAll();
		SNOWBALLED.clearAll();
		HAIRS_BREADTH_ARMED.clearAll();
		TRADE_BETRAYAL_ARMED.clearAll();
		ANVIL_ARMED.clearAll();
		ELYTRA_JUST_DEPLOYED.clearAll();
		WARDENS_THAT_HIT_PLAYER.clear();
		SNOWBALL_THROWER.clear();
		WAS_FALL_FLYING.clear();
		KILL_WEAPON_LOG.clearAll();
		BEE_STING_LOG.clearAll();
	}

	private static void checkHairsBreadthDuel(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof ServerPlayer victim) || !(source.getEntity() instanceof ServerPlayer attacker)) {
			return;
		}
		if (!(victim.level() instanceof ServerLevel level)) {
			return;
		}
		if (victim.getHealth() <= HALF_HEART && attacker.getHealth() <= HALF_HEART) {
			HAIRS_BREADTH_ARMED.mark(victim.getUUID(), level.getServer().getTickCount(), HAIRS_BREADTH_WINDOW_TICKS);
		}
	}

	private static void onDamage(LivingEntity victim, DamageSource source) {
		if (!(victim.level() instanceof ServerLevel level)) {
			return;
		}
		long currentTick = level.getServer().getTickCount();

		// friendly_fire_apology, direction 1: player hit the golem - arm the return-swing window.
		if (victim instanceof IronGolem golem && source.getEntity() instanceof ServerPlayer) {
			FRIENDLY_FIRE_ARMED.mark(golem.getUUID(), currentTick, FRIENDLY_FIRE_WINDOW_TICKS);
		}

		// snowball_assassin: vanilla still runs a zero-damage hurt() for projectile knockback, so a
		// thrown-projectile hit shows up here even against a target the snowball can't truly harm.
		if (source.is(DamageTypes.THROWN) && source.getEntity() instanceof ServerPlayer thrower && victim instanceof Mob) {
			SNOWBALLED.mark(victim.getUUID(), currentTick, SNOWBALL_ASSASSIN_WINDOW_TICKS);
			SNOWBALL_THROWER.put(victim.getUUID(), thrower.getUUID());
		}

		// warden_ghost bookkeeping: remember every warden that has ever landed a hit on this player.
		if (victim instanceof ServerPlayer player && source.getEntity() instanceof Warden warden) {
			WARDENS_THAT_HIT_PLAYER.computeIfAbsent(player.getUUID(), id -> new HashSet<>()).add(warden.getUUID());
		}

		// buried_alive_escape: no extra state needed - AFTER_DAMAGE already fires post-application,
		// so the entity's current health already reflects this hit.
		if (victim instanceof ServerPlayer player && source.is(DamageTypes.IN_WALL)
			&& player.getHealth() > 0f && player.getHealth() < 4f) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.BURIED_ALIVE_ESCAPE);
		}

		if (victim instanceof ServerPlayer stungPlayer && source.is(DamageTypes.STING) && source.getEntity() instanceof Bee bee) {
			int distinctBees = BEE_STING_LOG.recordAndCountDistinct(stungPlayer.getUUID(), bee.getUUID(), currentTick, BEE_GAUNTLET_WINDOW_TICKS);
			if (distinctBees >= BEE_GAUNTLET_DISTINCT_BEES && stungPlayer.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
				UnlockDispatcher.unlock(stungPlayer, AchievementDefinitions.BEE_GAUNTLET);
			}
		}
	}

	private static void onConversion(Mob original, Mob converted) {
		// mooshroom_lightning: the only thing that ever flips a mooshroom's variant in vanilla is a
		// lightning strike, so the conversion firing at all is already sufficient - no cause check.
		if (!(original instanceof MushroomCow) || !(converted.level() instanceof ServerLevel level)) {
			return;
		}
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			if (player.level() == level && player.distanceToSqr(converted) <= 64.0) {
				UnlockDispatcher.unlock(player, AchievementDefinitions.MOOSHROOM_LIGHTNING);
			}
		}
	}

	private static void onDeath(LivingEntity victim, DamageSource damageSource) {
		if (!(victim.level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer player = damageSource.getEntity() instanceof ServerPlayer p ? p : null;
		long currentTick = level.getServer().getTickCount();

		if (player != null) {
			recordKillForTripleKill(player, damageSource, currentTick);
		}

		if (victim instanceof Creeper creeper) {
			handlePyromancersHandshake(creeper, damageSource, level, currentTick);
		}
		// last_words has no attacker requirement at all - falling, drowning, a zombie, anything
		// qualifies as long as the death follows the chat message closely enough.
		if (victim instanceof ServerPlayer lastWordsVictim && SocialAchievements.LAST_WORDS_ARMED.isActive(lastWordsVictim.getUUID(), currentTick)) {
			UnlockDispatcher.unlock(lastWordsVictim, AchievementDefinitions.LAST_WORDS);
		}
		if (victim instanceof ServerPlayer insuredVictim && InteractionAchievements.INSURANCE_ARMED.isActive(insuredVictim.getUUID(), currentTick)) {
			UnlockDispatcher.unlock(insuredVictim, AchievementDefinitions.INSURANCE_SPEEDRUN);
		}
		// snowball_assassin: fall damage carries no attacker, so this must be resolved independently
		// of the `player` (attacker) variable, via whoever threw the snowball earlier.
		if (damageSource.is(DamageTypes.FALL) && SNOWBALLED.isActive(victim.getUUID(), currentTick)) {
			UUID throwerId = SNOWBALL_THROWER.get(victim.getUUID());
			ServerPlayer thrower = throwerId != null ? level.getServer().getPlayerList().getPlayer(throwerId) : null;
			if (thrower != null) {
				UnlockDispatcher.unlock(thrower, AchievementDefinitions.SNOWBALL_ASSASSIN);
			}
		}
		if (victim instanceof ServerPlayer elytraVictim && damageSource.is(DamageTypes.FALL)
			&& ELYTRA_JUST_DEPLOYED.isActive(elytraVictim.getUUID(), currentTick)) {
			UnlockDispatcher.unlock(elytraVictim, AchievementDefinitions.ELYTRA_DENIAL);
		}
		handleLightningFarmer(victim, damageSource, level);
		if (player == null) {
			return;
		}
		if (victim instanceof Zombie zombie && zombie.isBaby() && zombie.getVehicle() instanceof Chicken
			&& player.getVehicle() instanceof Horse) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.CAVALRY_DUEL);
		}
		if (damageSource.is(DamageTypes.ARROW) && player.hasEffect(MobEffects.BLINDNESS)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.BLIND_MARKSMAN);
		}
		// Plain onGround()==false fired off routine jump-attacks (a single hop is airborne too) -
		// requiring a real fall in progress makes this an actual trick shot, not incidental combat.
		if (!player.onGround() && player.fallDistance >= FALLING_STAR_MIN_FALL_DISTANCE) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.FALLING_STAR);
		}
		if (damageSource.is(DamageTypes.FALLING_ANVIL) && ANVIL_ARMED.isActive(player.getUUID(), currentTick)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.ANVIL_OF_REGRET);
		}
		if (victim instanceof IronGolem golem && FRIENDLY_FIRE_ARMED.isActive(golem.getUUID(), currentTick)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.FRIENDLY_FIRE_APOLOGY);
		}
		if (isExplosion(damageSource) && InteractionAchievements.OWN_TNT_ARMED.isActive(player.getUUID(), currentTick)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.OWN_TNT_DEATH);
		}
		if (victim instanceof Villager villager && TRADE_BETRAYAL_ARMED.isActive(villager.getUUID(), currentTick)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.TRADE_BETRAYAL);
		}
		if (victim instanceof Warden warden) {
			Set<UUID> hitBy = WARDENS_THAT_HIT_PLAYER.get(player.getUUID());
			if (hitBy == null || !hitBy.contains(warden.getUUID())) {
				UnlockDispatcher.unlock(player, AchievementDefinitions.WARDEN_GHOST);
			}
		}
		if (victim instanceof EnderDragon && player.getHealth() <= 1f
			&& !player.getInventory().contains(stack -> stack.is(Items.TOTEM_OF_UNDYING))) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.DRAGON_NO_TOTEM);
		}
		if (victim instanceof WitherBoss && isBareOfArmor(player)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.NAKED_KING);
		}
		if (victim instanceof ServerPlayer victimPlayer && HAIRS_BREADTH_ARMED.isActive(victimPlayer.getUUID(), currentTick)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.HAIRS_BREADTH_DUEL);
		}

		if (player.isDeadOrDying() || player.getHealth() <= 0f) {
			WARDENS_THAT_HIT_PLAYER.remove(player.getUUID());
		}
	}

	private static void handlePyromancersHandshake(Creeper creeper, DamageSource damageSource, ServerLevel level, long currentTick) {
		if (!(damageSource.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		if (!player.getMainHandItem().isEmpty()) {
			return;
		}
		if (!level.isThundering()) {
			return;
		}
		if (!LavaStandPoller.hasStoodInLavaFor(player.getUUID(), currentTick, MIN_LAVA_TICKS)) {
			return;
		}
		UnlockDispatcher.unlock(player, AchievementDefinitions.PYROMANCERS_HANDSHAKE);
	}

	/**
	 * lightning_farmer: vanilla's LightningBolt doesn't record who summoned it, so the credited
	 * player is approximated as "the nearby player who could plausibly have caused this" - holding a
	 * Channeling trident, standing on gold, during the same thunderstorm the bolt required anyway.
	 * Documented approximation, same category as snowball_assassin's thrower lookup.
	 */
	private static void handleLightningFarmer(LivingEntity victim, DamageSource damageSource, ServerLevel level) {
		if (!damageSource.is(DamageTypes.LIGHTNING_BOLT) || !level.isThundering()) {
			return;
		}
		for (ServerPlayer candidate : level.getServer().getPlayerList().getPlayers()) {
			if (candidate.level() != level || candidate.distanceToSqr(victim) > LIGHTNING_FARMER_RANGE_SQ) {
				continue;
			}
			if (!hasChannelingTrident(candidate)) {
				continue;
			}
			if (!level.getBlockState(candidate.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK)) {
				continue;
			}
			UnlockDispatcher.unlock(candidate, AchievementDefinitions.LIGHTNING_FARMER);
			return;
		}
	}

	private static boolean hasChannelingTrident(ServerPlayer player) {
		ItemStack mainHand = player.getMainHandItem();
		if (!mainHand.is(Items.TRIDENT)) {
			return false;
		}
		var registry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
		var channeling = registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.CHANNELING);
		return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(channeling, mainHand) > 0;
	}

	private static void recordKillForTripleKill(ServerPlayer player, DamageSource damageSource, long currentTick) {
		ItemStack weapon = damageSource.getWeaponItem();
		Object category = weapon.isEmpty() ? "hand" : weapon.getItem();
		int distinct = KILL_WEAPON_LOG.recordAndCountDistinct(player.getUUID(), category, currentTick, TRIPLE_KILL_WINDOW_TICKS);
		if (distinct >= TRIPLE_KILL_DISTINCT_WEAPONS) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.TRIPLE_KILL);
		}
	}

	private static boolean isExplosion(DamageSource source) {
		return source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION);
	}

	private static boolean isBareOfArmor(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
			&& player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
			&& player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
			&& player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
	}
}
