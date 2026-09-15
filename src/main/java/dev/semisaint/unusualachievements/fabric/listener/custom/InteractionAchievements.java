package dev.semisaint.unusualachievements.fabric.listener.custom;

import dev.semisaint.unusualachievements.core.RollingEventLog;
import dev.semisaint.unusualachievements.core.TimedFlag;
import dev.semisaint.unusualachievements.fabric.listener.SustainedConditionPoller;
import dev.semisaint.unusualachievements.fabric.listener.UnlockDispatcher;
import dev.semisaint.unusualachievements.fabric.registry.AchievementDefinitions;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Achievements triggered by a player using a block or an entity, or by their equipped gear
 * changing - fabric-events-interaction-v0's UseBlockCallback/UseEntityCallback plus
 * fabric-lifecycle-events-v1's ServerEntityEvents.EQUIPMENT_CHANGE. Also owns two things that
 * don't cleanly map to any single Fabric event and so get their own small polls on the same
 * 20-tick cadence as SustainedConditionPoller: detecting a completed villager trade (nothing
 * exposes "trade finished", only the menu's offer-use counters - see NetworkingInit's mirror
 * problem never arose there because card requests aren't menu-mediated) and bedtime_bomb's
 * delayed "did they survive the bed explosion" check.
 */
public final class InteractionAchievements {
	private static final long NIGHT_START = 13000;
	private static final long NIGHT_END = 23000;
	private static final long NOON = 6000;
	private static final long NOON_TOLERANCE = 5;
	private static final long TRADE_PARTNER_TTL_TICKS = 600; // 30s - stale if they wander off without trading
	private static final long OWN_TNT_WINDOW_TICKS = 100; // 5s
	private static final long TRADE_BETRAYAL_WINDOW_TICKS = 100; // 5s
	private static final long BEDTIME_BOMB_DELAY_TICKS = 60; // 3s
	private static final long INSURANCE_WINDOW_TICKS = 200; // 10s
	private static final int POLL_INTERVAL_TICKS = 20;
	private static final double STARING_CONTEST_RANGE = 6.0;
	private static final double TRUCE_RANGE = 3.0;
	private static final double MINECART_KIDNAPPING_RANGE = 8.0;
	private static final double MINECART_MOVING_THRESHOLD_SQ = 0.01 * 0.01;

	private static final Guard.Site USE_BLOCK_SITE = new Guard.Site("interaction use-block listener");
	private static final Guard.Site USE_ENTITY_SITE = new Guard.Site("interaction use-entity listener");
	private static final Guard.Site USE_ITEM_SITE = new Guard.Site("interaction use-item listener");
	private static final Guard.Site EQUIPMENT_SITE = new Guard.Site("interaction equipment-change listener");
	private static final Guard.Site POLL_SITE = new Guard.Site("interaction poll tick");
	private static final Guard.Site DISCONNECT_SITE = new Guard.Site("interaction disconnect cleanup");

	static final TimedFlag<UUID> OWN_TNT_ARMED = new TimedFlag<>();
	static final TimedFlag<UUID> INSURANCE_ARMED = new TimedFlag<>();

	private static final TimedFlag<UUID> TRADE_PARTNER_TTL = new TimedFlag<>();
	private static final Map<UUID, UUID> TRADE_PARTNER = new HashMap<>();
	private static final Map<UUID, TradeWatch> TRADE_WATCH = new HashMap<>();
	private static final Map<UUID, Long> BEDTIME_BOMB_ARMED_AT = new HashMap<>();
	private static final RollingEventLog<UUID, net.minecraft.world.level.block.Block> SHULKER_LOG = new RollingEventLog<>();
	private static final long SHULKER_WINDOW_TICKS = 1200; // 60s
	private static final int SHULKER_DISTINCT_COLORS = 5;

	private InteractionAchievements() {
	}

	private record TradeWatch(AbstractContainerMenu menu, int lastTotalUses) {
	}

	public static void register() {
		UseBlockCallback.EVENT.register(InteractionAchievements::onUseBlock);
		UseEntityCallback.EVENT.register(InteractionAchievements::onUseEntity);
		net.fabricmc.fabric.api.event.player.ItemEvents.USE.register(InteractionAchievements::onUseItem);
		ServerEntityEvents.EQUIPMENT_CHANGE.register((entity, slot, previous, current) -> Guard.run(EQUIPMENT_SITE, () ->
			onEquipmentChange(entity, slot)));
		ServerTickEvents.END_SERVER_TICK.register(server -> Guard.run(POLL_SITE, () -> {
			if (server.getTickCount() % POLL_INTERVAL_TICKS != 0) {
				return;
			}
			long currentTick = server.getTickCount();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				pollTrade(player, currentTick);
				pollBedtimeBomb(player, currentTick);
			}
		}));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Guard.run(DISCONNECT_SITE, () -> {
			ServerPlayer player = handler.getPlayer();
			if (player != null) {
				UUID id = player.getUUID();
				TRADE_PARTNER.remove(id);
				TRADE_WATCH.remove(id);
				BEDTIME_BOMB_ARMED_AT.remove(id);
				LAST_POSE.remove(id);
				// Were left behind before: player-keyed, so bounded by the player roster rather than
				// unbounded, but a stale armed window is still a window that reopens on the next join.
				OWN_TNT_ARMED.clear(id);
				INSURANCE_ARMED.clear(id);
				TRADE_PARTNER_TTL.clear(id);
				SHULKER_LOG.clear(id);
			}
		}));

		registerSustainedConditions();
	}

	/** See CombatDeathAchievements.reset() - every window here is measured in per-world server ticks. */
	public static void reset() {
		OWN_TNT_ARMED.clearAll();
		INSURANCE_ARMED.clearAll();
		TRADE_PARTNER_TTL.clearAll();
		TRADE_PARTNER.clear();
		TRADE_WATCH.clear();
		BEDTIME_BOMB_ARMED_AT.clear();
		LAST_POSE.clear();
		SHULKER_LOG.clearAll();
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		Guard.run(USE_BLOCK_SITE, () -> {
			if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
				return;
			}
			handleUseBlock(serverPlayer, serverLevel, hand, hit);
		});
		return InteractionResult.PASS;
	}

	private static void handleUseBlock(ServerPlayer player, ServerLevel level, InteractionHand hand, BlockHitResult hit) {
		var state = level.getBlockState(hit.getBlockPos());
		ItemStack heldItem = player.getItemInHand(hand);

		if (state.getBlock() instanceof BedBlock) {
			if (level.dimension() != Level.OVERWORLD) {
				BEDTIME_BOMB_ARMED_AT.put(player.getUUID(), (long) level.getServer().getTickCount());
			} else {
				// Vanilla sets the respawn point on any successful bed interaction, day or night -
				// sleeping (and the "you can only sleep at night" refusal) is a separate, later step.
				INSURANCE_ARMED.mark(player.getUUID(), level.getServer().getTickCount(), INSURANCE_WINDOW_TICKS);
				if (!isNight(level)) {
					UnlockDispatcher.unlock(player, AchievementDefinitions.WRONG_TIME);
				}
			}
		}
		if (state.getBlock() instanceof NoteBlock && isNight(level)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.NOISE_COMPLAINT);
		}
		if (state.getBlock() instanceof CampfireBlock && heldItem.is(Items.FLINT_AND_STEEL) && level.isRaining()) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.RAIN_FIRE);
		}
		if (state.getBlock() instanceof TntBlock && heldItem.is(Items.FLINT_AND_STEEL)) {
			OWN_TNT_ARMED.mark(player.getUUID(), level.getServer().getTickCount(), OWN_TNT_WINDOW_TICKS);
		}
		if (state.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
			int distinctColors = SHULKER_LOG.recordAndCountDistinct(player.getUUID(), state.getBlock(),
				level.getServer().getTickCount(), SHULKER_WINDOW_TICKS);
			if (distinctColors >= SHULKER_DISTINCT_COLORS) {
				UnlockDispatcher.unlock(player, AchievementDefinitions.SHULKER_COLLECTOR);
			}
		}
	}

	private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity target, EntityHitResult hit) {
		Guard.run(USE_ENTITY_SITE, () -> {
			if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
				return;
			}
			handleUseEntity(serverPlayer, serverLevel, hand, target);
		});
		return InteractionResult.PASS;
	}

	private static void handleUseEntity(ServerPlayer player, ServerLevel level, InteractionHand hand, Entity target) {
		ItemStack heldItem = player.getItemInHand(hand);

		if (target instanceof Piglin && heldItem.is(Items.GOLD_INGOT) && countItem(player, Items.GOLD_INGOT) == 1) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.LAST_INGOT_GAMBLE);
		}
		if (target instanceof Monster && heldItem.isEmpty()) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.POLITE_TO_MONSTERS);
		}
		if (target instanceof Cow && !(target instanceof MushroomCow) && heldItem.is(Items.SHEARS)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.WRONG_ANIMAL);
		}
		if (target instanceof TraderLlama) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.WRONG_TARGET);
		}
		if (target instanceof Villager villager && !villager.isBaby()) {
			long currentTick = level.getServer().getTickCount();
			TRADE_PARTNER.put(player.getUUID(), villager.getUUID());
			TRADE_PARTNER_TTL.mark(player.getUUID(), currentTick, TRADE_PARTNER_TTL_TICKS);
		}
	}

	/**
	 * Unlike UseBlockCallback/UseEntityCallback (an array of listeners, PASS = "next listener,
	 * eventually fall through to vanilla"), ItemEvents.USE is a @WrapOperation mixin around
	 * Item.use() itself: returning null means "didn't handle it, call the real vanilla operation";
	 * returning ANY InteractionResult - PASS included - short-circuits and replaces vanilla's use()
	 * entirely. Returning PASS here previously ate every right-click item use in the game (bows,
	 * crossbows, food, everything) since vanilla's own handling never ran.
	 */
	private static InteractionResult onUseItem(Level level, Player player, InteractionHand hand) {
		Guard.run(USE_ITEM_SITE, () -> {
			if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
				return;
			}
			handleUseItem(serverPlayer, hand);
		});
		return null;
	}

	/**
	 * Fires when eating *starts*, not when it finishes a few ticks later - close enough for these
	 * three: nobody begins eating a chorus fruit / poisonous potato / golden apple and then bails
	 * out of the animation on purpose.
	 */
	private static void handleUseItem(ServerPlayer player, InteractionHand hand) {
		ItemStack heldItem = player.getItemInHand(hand);
		if (heldItem.is(Items.CHORUS_FRUIT) && player.getFoodData().getFoodLevel() >= 20) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.NO_REASON);
		}
		if (heldItem.is(Items.POISONOUS_POTATO)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.BAD_FOOD_CRITIC);
		}
		if (heldItem.is(Items.GOLDEN_APPLE) && player.getHealth() >= player.getMaxHealth()
			&& player.level() instanceof ServerLevel level && noHostilesNearby(level, player)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.JUST_BECAUSE);
		}
	}

	private static boolean noHostilesNearby(ServerLevel level, ServerPlayer player) {
		return level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(8.0)).isEmpty();
	}

	private static void onEquipmentChange(LivingEntity entity, EquipmentSlot slot) {
		if (!(entity instanceof ServerPlayer player) || !slot.isArmor()) {
			return;
		}
		if (isBlackLeather(player.getItemBySlot(EquipmentSlot.HEAD), Items.LEATHER_HELMET)
			&& isBlackLeather(player.getItemBySlot(EquipmentSlot.CHEST), Items.LEATHER_CHESTPLATE)
			&& isBlackLeather(player.getItemBySlot(EquipmentSlot.LEGS), Items.LEATHER_LEGGINGS)
			&& isBlackLeather(player.getItemBySlot(EquipmentSlot.FEET), Items.LEATHER_BOOTS)) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.GOTH_PHASE);
		}
	}

	private static boolean isBlackLeather(ItemStack stack, net.minecraft.world.item.Item expected) {
		return stack.is(expected) && DyedItemColor.getOrDefault(stack, DyedItemColor.LEATHER_COLOR) == DyeColor.BLACK.getTextureDiffuseColor();
	}

	private static void pollTrade(ServerPlayer player, long currentTick) {
		UUID id = player.getUUID();
		if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
			TRADE_WATCH.remove(id);
			return;
		}
		int totalUses = 0;
		for (MerchantOffer offer : merchantMenu.getOffers()) {
			totalUses += offer.getUses();
		}
		TradeWatch previous = TRADE_WATCH.get(id);
		if (previous == null || previous.menu() != merchantMenu) {
			TRADE_WATCH.put(id, new TradeWatch(merchantMenu, totalUses));
			return;
		}
		if (totalUses > previous.lastTotalUses()) {
			onTradeCompleted(player, currentTick);
		}
		TRADE_WATCH.put(id, new TradeWatch(merchantMenu, totalUses));
	}

	private static void onTradeCompleted(ServerPlayer player, long currentTick) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		long timeOfDay = level.getOverworldClockTime() % 24000;
		if (Math.abs(timeOfDay - NOON) <= NOON_TOLERANCE) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.NOON_DEAL);
		}
		UUID villagerId = TRADE_PARTNER.get(player.getUUID());
		if (villagerId != null && TRADE_PARTNER_TTL.isActive(player.getUUID(), currentTick)) {
			CombatDeathAchievements.TRADE_BETRAYAL_ARMED.mark(villagerId, currentTick, TRADE_BETRAYAL_WINDOW_TICKS);
		}
	}

	private static void pollBedtimeBomb(ServerPlayer player, long currentTick) {
		Long armedAt = BEDTIME_BOMB_ARMED_AT.get(player.getUUID());
		if (armedAt == null) {
			return;
		}
		if (currentTick - armedAt < BEDTIME_BOMB_DELAY_TICKS) {
			return;
		}
		BEDTIME_BOMB_ARMED_AT.remove(player.getUUID());
		if (player.isAlive()) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.BEDTIME_BOMB);
		}
	}

	private static void registerSustainedConditions() {
		SustainedConditionPoller.registerCondition("staring_contest",
			player -> player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
				&& player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
				&& player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
				&& player.getItemBySlot(EquipmentSlot.FEET).isEmpty()
				&& isAngryEndermanNearby(player, STARING_CONTEST_RANGE),
			200, // 10s
			player -> UnlockDispatcher.unlock(player, AchievementDefinitions.STARING_CONTEST));

		SustainedConditionPoller.registerCondition("minecart_kidnapping",
			InteractionAchievements::isCartingVillagerNearby,
			1200, // 60s
			player -> UnlockDispatcher.unlock(player, AchievementDefinitions.MINECART_KIDNAPPING));

		SustainedConditionPoller.registerCondition("truce",
			player -> isNonAngryCreeperNearby(player, TRUCE_RANGE),
			600, // 30s
			player -> UnlockDispatcher.unlock(player, AchievementDefinitions.TRUCE));

		SustainedConditionPoller.registerCondition("statue",
			InteractionAchievements::isMotionlessAtNight,
			1200, // 60s
			player -> UnlockDispatcher.unlock(player, AchievementDefinitions.STATUE));
	}

	private record Pose(net.minecraft.world.phys.Vec3 position, float yRot, float xRot) {
	}

	private static final Map<UUID, Pose> LAST_POSE = new HashMap<>();

	/**
	 * SustainedConditionPoller only asks "is the condition true right now" once per poll - motion
	 * has to be judged relative to the *previous* poll, so this predicate keeps its own small
	 * per-player pose cache instead of being purely stateless like the other conditions here.
	 */
	private static boolean isMotionlessAtNight(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level) || !isNight(level)) {
			LAST_POSE.remove(player.getUUID());
			return false;
		}
		Pose current = new Pose(player.position(), player.getYRot(), player.getXRot());
		Pose previous = LAST_POSE.put(player.getUUID(), current);
		if (previous == null) {
			return false;
		}
		return previous.position().closerThan(current.position(), 0.01)
			&& Math.abs(previous.yRot() - current.yRot()) < 0.5f
			&& Math.abs(previous.xRot() - current.xRot()) < 0.5f;
	}

	private static boolean isAngryEndermanNearby(ServerPlayer player, double range) {
		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}
		return level.getEntitiesOfClass(net.minecraft.world.entity.monster.EnderMan.class,
			player.getBoundingBox().inflate(range),
			enderman -> enderman.isCreepy() && player.equals(enderman.getTarget())).size() > 0;
	}

	private static boolean isCartingVillagerNearby(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}
		return level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.minecart.AbstractMinecart.class,
			player.getBoundingBox().inflate(MINECART_KIDNAPPING_RANGE),
			minecart -> minecart.getFirstPassenger() instanceof Villager
				&& minecart.getDeltaMovement().lengthSqr() > MINECART_MOVING_THRESHOLD_SQ).size() > 0;
	}

	private static boolean isNonAngryCreeperNearby(ServerPlayer player, double range) {
		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}
		return level.getEntitiesOfClass(net.minecraft.world.entity.monster.Creeper.class,
			player.getBoundingBox().inflate(range),
			creeper -> creeper.getSwellDir() <= 0).size() > 0;
	}

	private static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
		int count = 0;
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static boolean isNight(ServerLevel level) {
		long t = level.getOverworldClockTime() % 24000;
		return t >= NIGHT_START && t < NIGHT_END;
	}
}
