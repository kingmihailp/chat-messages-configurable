package com.kingmihailp.broadcastmod.handler;

import com.kingmihailp.broadcastmod.BroadcastMod;
import com.kingmihailp.broadcastmod.config.BroadcastConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Listens to every server tick and broadcasts the configured message
 * once the configured interval has elapsed.
 *
 * Tick counting resets whenever the interval threshold is crossed or when
 * the config is reloaded via /kingmihailptext reload.
 */
@EventBusSubscriber(modid = BroadcastMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ServerTickHandler {

    /** Mutable so the reload command can reset it without a restart. */
    public static volatile int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;

        // 20 ticks per second
        int thresholdTicks = BroadcastConfig.getIntervalSeconds() * 20;

        if (tickCounter >= thresholdTicks) {
            tickCounter = 0;
            sendBroadcast(event.getServer());
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private static void sendBroadcast(MinecraftServer server) {
        String text     = BroadcastConfig.getMessage();
        String colorHex = BroadcastConfig.getColor();

        TextColor textColor = resolveColor(colorHex);
        Style     style     = Style.EMPTY.withColor(textColor);
        Component component = Component.literal(text).withStyle(style);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }

        BroadcastMod.LOGGER.debug("[BroadcastMod] Broadcast sent to {} player(s).",
                server.getPlayerList().getPlayerCount());
    }

    /**
     * Resolves a color string to a {@link TextColor}.
     *
     * Supported formats:
     * <ul>
     *   <li>{@code #RRGGBB} – 6-digit hex</li>
     *   <li>{@code #RGB}    – 3-digit shorthand (expanded to 6 digits)</li>
     *   <li>Named Minecraft colors: white, black, red, green, blue, yellow,
     *       aqua, gold, gray, dark_gray, dark_red, dark_green, dark_blue,
     *       dark_aqua, dark_purple, light_purple</li>
     * </ul>
     * Falls back to white on parse failure.
     */
    static TextColor resolveColor(String colorStr) {
        if (colorStr == null || colorStr.isBlank()) {
            return TextColor.fromRgb(0xFFFFFF);
        }

        String trimmed = colorStr.trim();

        // ── Hex color (#RRGGBB or #RGB) ─────────────────────────────────────────
        if (trimmed.startsWith("#")) {
            String hex = trimmed.substring(1);

            // Expand 3-digit shorthand: #RGB → #RRGGBB
            if (hex.length() == 3) {
                hex = String.valueOf(hex.charAt(0)) + hex.charAt(0)
                    + hex.charAt(1) + hex.charAt(1)
                    + hex.charAt(2) + hex.charAt(2);
            }

            if (hex.length() == 6) {
                try {
                    int rgb = Integer.parseInt(hex, 16);
                    return TextColor.fromRgb(rgb);
                } catch (NumberFormatException e) {
                    BroadcastMod.LOGGER.warn("[BroadcastMod] Invalid hex color '{}', falling back to white.", colorStr);
                }
            } else {
                BroadcastMod.LOGGER.warn("[BroadcastMod] Unsupported hex length in '{}', expected #RGB or #RRGGBB.", colorStr);
            }
            return TextColor.fromRgb(0xFFFFFF);
        }

        // ── Named Minecraft text color ───────────────────────────────────────────
        return TextColor.parseColor(trimmed)
                .resultOrPartial(err ->
                        BroadcastMod.LOGGER.warn("[BroadcastMod] Unknown color name '{}': {}", trimmed, err))
                .orElse(TextColor.fromRgb(0xFFFFFF));
    }
}
