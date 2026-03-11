package com.kingmihailp.broadcastmod.handler;

import com.kingmihailp.broadcastmod.BroadcastMod;
import com.kingmihailp.broadcastmod.config.BroadcastConfig;
import com.kingmihailp.broadcastmod.util.MessageParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Listens to every server tick and broadcasts the configured message
 * once the configured interval has elapsed.
 */
@EventBusSubscriber(modid = BroadcastMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ServerTickHandler {

    /** Exposed so /kingmihailptext reload can reset it immediately. */
    public static volatile int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;

        int thresholdTicks = BroadcastConfig.getIntervalSeconds() * 20; // 20 ticks/s

        if (tickCounter >= thresholdTicks) {
            tickCounter = 0;
            sendBroadcast(event.getServer());
        }
    }

    private static void sendBroadcast(MinecraftServer server) {
        var component = MessageParser.parse(BroadcastConfig.getMessage());

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }

        BroadcastMod.LOGGER.debug("[BroadcastMod] Broadcast sent to {} player(s).",
                server.getPlayerList().getPlayerCount());
    }
}
