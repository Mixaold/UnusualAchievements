package dev.semisaint.unusualachievements.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.semisaint.unusualachievements.UnusualAchievementsMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("unusualachievements.json");

	private static volatile ModConfig instance;

	private ModConfigManager() {
	}

	public static ModConfig get() {
		ModConfig current = instance;
		if (current == null) {
			current = load();
			instance = current;
		}
		return current;
	}

	/**
	 * Called from a packet receiver on the client thread. A config file that cannot be written is
	 * worth a log line, never a crash - the setting still applies for this session.
	 */
	public static void save() {
		ModConfig current = instance;
		if (current == null) {
			return;
		}
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(current), StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException e) {
			UnusualAchievementsMod.LOGGER.error("Could not write config to {}", FILE, e);
		}
	}

	/**
	 * Reads the file up front so the first HUD frame after an unlock does not do the read on the
	 * render thread.
	 */
	public static void preload() {
		get();
	}

	private static ModConfig load() {
		if (!Files.isRegularFile(FILE)) {
			return new ModConfig();
		}
		try {
			String json = Files.readString(FILE, StandardCharsets.UTF_8);
			ModConfig config = GSON.fromJson(json, ModConfig.class);
			return config != null ? config.sanitized() : new ModConfig();
		} catch (IOException | RuntimeException e) {
			UnusualAchievementsMod.LOGGER.warn("Could not read config from {}, using defaults", FILE, e);
			return new ModConfig();
		}
	}
}
