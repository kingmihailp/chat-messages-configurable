package com.kingmihailp.broadcastmod;

import com.kingmihailp.broadcastmod.config.BroadcastConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BroadcastMod.MOD_ID)
public class BroadcastMod {

    public static final String MOD_ID = "kingmihailpbroadcast";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public BroadcastMod(IEventBus modEventBus) {
        modEventBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("KingMihailP Broadcast Mod initializing...");
    }

    private void onServerStarting(ServerStartingEvent event) {
        BroadcastConfig.load();
        LOGGER.info("KingMihailP Broadcast Mod: config loaded, {} message(s) active.",
                BroadcastConfig.getMessages().size());
    }
}
