package com.kingmihailp.broadcastmod.command;

import com.kingmihailp.broadcastmod.BroadcastMod;
import com.kingmihailp.broadcastmod.config.BroadcastConfig;
import com.kingmihailp.broadcastmod.handler.ServerTickHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Admin commands under /kingmihailptext (requires OP level 2).
 *
 * <pre>
 * /kingmihailptext reload
 *     – reload kingmihailpbroadcast.json from disk; reset all timers
 *
 * /kingmihailptext create &lt;name&gt;
 *     – add a new named broadcast message with default values,
 *       save config, and activate it immediately
 * </pre>
 */
@EventBusSubscriber(modid = BroadcastMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ReloadCommand {

    // ── Colors for feedback messages ─────────────────────────────────────────
    private static final int GREEN  = 0x55FF55;
    private static final int RED    = 0xFF5555;
    private static final int YELLOW = 0xFFAA00;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("kingmihailptext")
                .requires(source -> source.hasPermission(2))

                // ── /kingmihailptext reload ──────────────────────────────────
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        BroadcastConfig.load();
                        ServerTickHandler.resetCounters();

                        int count = BroadcastConfig.getMessages().size();

                        ctx.getSource().sendSuccess(
                            () -> colored("[BroadcastMod] Config reloaded – " + count
                                    + " message(s) active.", GREEN),
                            true
                        );

                        BroadcastMod.LOGGER.info("[BroadcastMod] Config reloaded via command by {}",
                                ctx.getSource().getTextName());
                        return 1;
                    })
                )

                // ── /kingmihailptext create <name> ───────────────────────────
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");

                            boolean created = BroadcastConfig.createMessage(name);

                            if (!created) {
                                ctx.getSource().sendFailure(
                                    colored("[BroadcastMod] A message named \"" + name
                                            + "\" already exists.", RED)
                                );
                                return 0;
                            }

                            ctx.getSource().sendSuccess(
                                () -> colored("[BroadcastMod] Message \"" + name + "\" created! "
                                        + "Edit its text and interval in the config file, "
                                        + "then run /kingmihailptext reload.", YELLOW),
                                true
                            );

                            BroadcastMod.LOGGER.info(
                                    "[BroadcastMod] Message '{}' created by {}",
                                    name, ctx.getSource().getTextName());
                            return 1;
                        })
                    )
                )
        );
    }

    private static Component colored(String text, int rgb) {
        return Component.literal(text)
                .withStyle(style -> style.withColor(TextColor.fromRgb(rgb)));
    }
}
