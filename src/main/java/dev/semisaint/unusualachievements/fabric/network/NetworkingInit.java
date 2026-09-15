package dev.semisaint.unusualachievements.fabric.network;

import dev.semisaint.unusualachievements.core.AchievementDefinition;
import dev.semisaint.unusualachievements.core.AchievementId;
import dev.semisaint.unusualachievements.core.CardTheme;
import dev.semisaint.unusualachievements.core.LocalizedText;
import dev.semisaint.unusualachievements.core.PlayerAchievementProgress;
import dev.semisaint.unusualachievements.core.RarityInfo;
import dev.semisaint.unusualachievements.fabric.RarityTracker;
import dev.semisaint.unusualachievements.fabric.ServerLifecycleHooks;
import dev.semisaint.unusualachievements.core.AchievementRegistry;
import dev.semisaint.unusualachievements.fabric.listener.UnlockDispatcher;
import dev.semisaint.unusualachievements.fabric.registry.AchievementDefinitions;
import dev.semisaint.unusualachievements.fabric.registry.RarityBasis;
import dev.semisaint.unusualachievements.util.Guard;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkingInit {
	// fourth_wall is deliberately session-only (not written to disk) - a joke meta-achievement
	// isn't worth a new persistent-counter format on top of PlayerAchievementData.
	private static final int OWN_CARD_OPEN_THRESHOLD = 10;
	private static final Map<UUID, Integer> ownCardOpenCounts = new ConcurrentHashMap<>();

	/**
	 * Minimum gap between two card requests from the same player. A card request is a keypress, so
	 * half a second is far longer than anyone can hit the key - but there was no gate at all, and a
	 * client is free to send the payload as fast as it likes. Two things needed one: building a
	 * response walks every unlock, resolves its localized text and its rarity basis, so a tight send
	 * loop turns into real server work; and trackOwnCardOpen() counts requests, which let a scripted
	 * client hand itself fourth_wall in a single tick rather than over ten actual openings.
	 */
	private static final long CARD_REQUEST_MIN_INTERVAL_TICKS = 10;
	private static final Map<UUID, Long> lastCardRequestTick = new ConcurrentHashMap<>();

	private static final Guard.Site CARD_REQUEST_SITE = new Guard.Site("card request receiver");
	private static final Guard.Site DEV_FORCE_UNLOCK_SITE = new Guard.Site("dev force-unlock receiver");
	private static final Guard.Site SELECT_THEME_SITE = new Guard.Site("card theme receiver");
	private static final Guard.Site CARD_DISCONNECT_SITE = new Guard.Site("card request disconnect cleanup");

	private NetworkingInit() {
	}

	public static void registerCommon() {
		PayloadTypeRegistry.serverboundPlay().register(CardRequestPayload.TYPE, CardRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DevForceUnlockPayload.TYPE, DevForceUnlockPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectThemePayload.TYPE, SelectThemePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CardResponsePayload.TYPE, CardResponsePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(UnlockNotifyPayload.TYPE, UnlockNotifyPayload.STREAM_CODEC);
	}

	public static void registerServer() {
		// An exception out of a receiver disconnects the player mid-session, so both are contained.
		ServerPlayNetworking.registerGlobalReceiver(CardRequestPayload.TYPE, (payload, context) -> Guard.run(CARD_REQUEST_SITE, () -> {
			ServerPlayer requester = context.player();
			if (!ServerLifecycleHooks.ready() || !ServerPlayNetworking.canSend(requester, CardResponsePayload.TYPE)
				|| !acceptCardRequest(requester)) {
				return;
			}
			boolean self = payload.targetPlayer().equals(requester.getUUID());
			CardResponsePayload response = buildResponse(requester, payload.targetPlayer());
			if (self) {
				trackOwnCardOpen(requester);
			} else if (!response.unknownPlayer() && response.unlocked().isEmpty()) {
				// nothing_to_see: cards are always visible now (no more hide toggle), so the only
				// "letdown" left to react to is peeking at a real player who has nothing unlocked yet.
				UnlockDispatcher.unlock(requester, AchievementDefinitions.NOTHING_TO_SEE);
			}
			ServerPlayNetworking.send(requester, response);
		}));

		// Gated on operator permission, not a build flag, so the same jar works for dev QA and release.
		ServerPlayNetworking.registerGlobalReceiver(DevForceUnlockPayload.TYPE, (payload, context) -> Guard.run(DEV_FORCE_UNLOCK_SITE, () -> {
			ServerPlayer requester = context.player();
			if (!requester.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || !ServerLifecycleHooks.ready()) {
				return;
			}
			UnlockDispatcher.unlock(requester, new AchievementId(payload.achievementId()));
		}));

		ServerPlayNetworking.registerGlobalReceiver(SelectThemePayload.TYPE, (payload, context) -> Guard.run(SELECT_THEME_SITE, () -> {
			ServerPlayer player = context.player();
			if (!ServerLifecycleHooks.ready()) {
				return;
			}
			PlayerAchievementProgress progress = ServerLifecycleHooks.dataManager().get(player.getUUID());
			if (progress == null || !canEquip(progress, payload.themeId())) {
				return;
			}
			progress.selectTheme(payload.themeId());
			ServerLifecycleHooks.dataManager().markDirty(player.getUUID());
		}));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Guard.run(CARD_DISCONNECT_SITE, () -> {
			ServerPlayer player = handler.getPlayer();
			if (player != null) {
				ownCardOpenCounts.remove(player.getUUID());
				lastCardRequestTick.remove(player.getUUID());
			}
		}));
	}

	/** False when this player asked again too soon; the request is then dropped without a reply. */
	private static boolean acceptCardRequest(ServerPlayer requester) {
		net.minecraft.server.MinecraftServer server = requester.level().getServer();
		if (server == null) {
			return false;
		}
		long now = server.getTickCount();
		Long previous = lastCardRequestTick.put(requester.getUUID(), now);
		// Also lets a request through when the counter has gone backwards, which is what a world
		// change looks like - better than locking the player out for the old world's tick count.
		return previous == null || now - previous >= CARD_REQUEST_MIN_INTERVAL_TICKS || now < previous;
	}

	/** "none" is always allowed (that's taking a theme off); anything else needs its achievement. */
	private static boolean canEquip(PlayerAchievementProgress progress, String themeId) {
		if (CardTheme.NONE.equals(themeId)) {
			return true;
		}
		return CardTheme.byId(themeId)
			.map(theme -> progress.isUnlocked(theme.requiredAchievement()))
			.orElse(false);
	}

	private static void trackOwnCardOpen(ServerPlayer player) {
		int count = ownCardOpenCounts.merge(player.getUUID(), 1, Integer::sum);
		if (count >= OWN_CARD_OPEN_THRESHOLD) {
			UnlockDispatcher.unlock(player, AchievementDefinitions.FOURTH_WALL);
		}
	}

	private static CardResponsePayload buildResponse(ServerPlayer requester, java.util.UUID targetId) {
		boolean self = targetId.equals(requester.getUUID());
		PlayerAchievementProgress progress = ServerLifecycleHooks.dataManager().get(targetId);

		if (progress == null) {
			return new CardResponsePayload(targetId, "", List.of(), 0, true, CardTheme.NONE);
		}

		String locale = requester.clientInformation().language();
		RarityTracker rarity = ServerLifecycleHooks.rarityTracker();
		// Stats belong to the card's owner, not to whoever is reading it - "one in 3418 of YOUR kills"
		// only means anything about the player whose card this is.
		ServerPlayer target = self ? requester : onlinePlayer(requester, targetId);
		List<CardResponsePayload.UnlockedEntry> entries = new ArrayList<>();
		for (Map.Entry<AchievementId, Long> unlocked : progress.unlockedAtEpochMillis().entrySet()) {
			AchievementDefinition definition = AchievementRegistry.get(unlocked.getKey()).orElse(null);
			if (definition == null) {
				continue;
			}
			LocalizedText text = definition.textFor(locale);
			RarityInfo rarityInfo = rarity.rarityFor(unlocked.getKey());
			RarityBasis basis = target == null ? null : RarityBasis.forAchievement(unlocked.getKey());
			long basisValue = basis == null ? 0L : basis.valueFor(target);
			// Below two, "one time in one kill" reads as noise rather than as a statistic.
			boolean usable = basis != null && basisValue >= 2;
			entries.add(new CardResponsePayload.UnlockedEntry(
				unlocked.getKey().path(),
				text.name(),
				text.description(),
				text.flavor(),
				unlocked.getValue(),
				rarityInfo.unlockedCount(),
				usable ? basis.translationKey() : "",
				usable ? basisValue : 0L,
				usable && basis.percentable()
			));
		}

		String nickname = self ? requester.getScoreboardName() : resolveNickname(requester, targetId);
		return new CardResponsePayload(targetId, nickname, entries, rarity.totalPlayers(), false, progress.selectedThemeId());
	}

	private static String resolveNickname(ServerPlayer requester, java.util.UUID targetId) {
		ServerPlayer target = onlinePlayer(requester, targetId);
		return target != null ? target.getScoreboardName() : "";
	}

	/** Null when that player is not online - their stats are simply left out of the card then. */
	private static ServerPlayer onlinePlayer(ServerPlayer requester, java.util.UUID targetId) {
		net.minecraft.server.MinecraftServer server = requester.level().getServer();
		return server == null ? null : server.getPlayerList().getPlayer(targetId);
	}
}
