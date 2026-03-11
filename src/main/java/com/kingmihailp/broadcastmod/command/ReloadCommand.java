package com.kingmihailp.broadcastmod.command;

import com.kingmihailp.broadcastmod.BroadcastMod;
import com.kingmihailp.broadcastmod.config.BroadcastConfig;
import com.kingmihailp.broadcastmod.handler.ServerTickHandler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registers the /kingmihailptext reload command.
 *
 * Usage (requires OP level 2):
 *   /kingmihailptext reload
 *
 * Reloads kingmihailpbroadcast.json from disk without restarting the server.
 * The tick counter is also reset so the next broadcast fires after a full interval.
 */
@EventBusSubscriber(modid = BroadcastMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ReloadCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("kingmihailptext")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        BroadcastConfig.load();

                        // Reset tick counter so the next broadcast starts from zero
                        ServerTickHandler.tickCounter = 0;

                        ctx.getSource().sendSuccess(
                            () -> Component.literal(
                                    "[BroadcastMod] Config reloaded successfully!")
                                .withStyle(style -> style.withColor(
                                        net.minecraft.network.chat.TextColor.fromRgb(0x55FF55))),
                            true
                        );

                        BroadcastMod.LOGGER.info("[BroadcastMod] Config reloaded via command by {}",
                                ctx.getSource().getTextName());
                        return 1;
                    })
                )
        );
    }
}
