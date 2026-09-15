package dev.semisaint.unusualachievements.client.keybind;

import dev.semisaint.unusualachievements.UnusualAchievementsMod;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ModKeyBindings {
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(UnusualAchievementsMod.MOD_ID, "main"));

	// J sits under the right index finger, is unbound in vanilla, and carries a letter in both
	// layouts - unlike the old semicolon, which the Russian layout renders as a bare ";".
	public static final KeyMapping OPEN_CARD = new KeyMapping(
		"key.unusualachievements.open_card",
		GLFW.GLFW_KEY_J,
		CATEGORY
	);

	private ModKeyBindings() {
	}

	public static void register() {
		KeyMappingHelper.registerKeyMapping(OPEN_CARD);
	}

	/**
	 * Always says "J" while the binding is untouched, whatever the game language or keyboard layout
	 * is. GLFW names letter keys from the active layout, so the vanilla name can come back as the
	 * Cyrillic "о" on a Russian layout - one key, two different-looking hints. Only after a rebind do
	 * we defer to whatever the game calls the new key, since then there is nothing fixed to print.
	 */
	public static Component openCardKeyLabel() {
		return OPEN_CARD.isDefault()
			? Component.literal("J")
			: OPEN_CARD.getTranslatedKeyMessage();
	}
}
