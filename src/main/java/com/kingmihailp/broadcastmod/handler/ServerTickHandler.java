package com.kingmihailp.broadcastmod.handler;

import com.kingmihailp.broadcastmod.BroadcastMod;
import com.kingmihailp.broadcastmod.config.BroadcastConfig;
import com.kingmihailp.broadcastmod.config.BroadcastMessage;
import com.kingmihailp.broadcastmod.util.MessageParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Fires each configured broadcast message on its own independent timer.
 * Each message tracks its own tick counter, so a 5-minute message and a
 * 10-minute message both fire at exactly the right times regardless of
 * each other.
 */
@EventBusSubscriber(modid = BroadcastMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ServerTickHandler {

    // name → ticks elapsed since last broadcast for that message
    private static final Map<String, Integer> tickCounters = new HashMap<>();

    /** Called by ReloadCommand to start all timers fresh after a reload. */
    public static void resetCounters() {
        tickCounters.clear();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Map<String, BroadcastMessage> messages = BroadcastConfig.getMessages();

        for (Map.Entry<String, BroadcastMessage> entry : messages.entrySet()) {
            String           name    = entry.getKey();
            BroadcastMessage msg     = entry.getValue();
            int              ticks   = tickCounters.merge(name, 1, Integer::sum);
            int              trigger = msg.getIntervalSeconds() * 20; // 20 ticks/s

            if (ticks >= trigger) {
                tickCounters.put(name, 0);
                sendBroadcast(event.getServer(), msg);
            }
        }

        // Drop counters for messages that were removed from config
        tickCounters.keySet().retainAll(messages.keySet());
    }

    private static void sendBroadcast(MinecraftServer server, BroadcastMessage msg) {
        var component = MessageParser.parse(msg.getText());

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }

        BroadcastMod.LOGGER.debug("[BroadcastMod] Broadcast sent to {} player(s).",
                server.getPlayerList().getPlayerCount());
    }
}
