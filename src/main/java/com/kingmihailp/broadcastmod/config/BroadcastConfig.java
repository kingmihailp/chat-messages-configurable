package com.kingmihailp.broadcastmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.kingmihailp.broadcastmod.BroadcastMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles reading and writing the mod's JSON config file.
 *
 * Config location: &lt;minecraft_dir&gt;/config/kingmihailpbroadcast.json
 *
 * Fields:
 *   message          – text to broadcast; supports inline color codes:
 *                        &0-&9, &a-&f  legacy Minecraft colors
 *                        &k &l &m &n &o  formatting (obfuscate/bold/strike/underline/italic)
 *                        &r             reset formatting
 *                        &#RRGGBB       24-bit hex color
 *                        &#RGB          3-digit hex shorthand
 *   interval_seconds – how often the message fires (seconds, minimum 1)
 */
public class BroadcastConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("kingmihailpbroadcast.json");

    // ── Default values ──────────────────────────────────────────────────────────
    private static String message =
            "&6[Server] &r&#FF4444Welcome &r&#FFAAAAto the &r&#FF8800server&r&6!";
    private static int intervalSeconds = 300;

    // ── Public API ──────────────────────────────────────────────────────────────

    /** Load (or create) the config file. Safe to call at any time for hot-reload. */
    public static synchronized void load() {
        if (!Files.exists(CONFIG_PATH)) {
            writeDefaults();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json == null) {
                BroadcastMod.LOGGER.warn("[BroadcastMod] Config file is empty or invalid – using defaults.");
                return;
            }

            if (json.has("message"))          message         = json.get("message").getAsString();
            if (json.has("interval_seconds")) intervalSeconds = Math.max(1, json.get("interval_seconds").getAsInt());

            BroadcastMod.LOGGER.info(
                    "[BroadcastMod] Config loaded – interval={}s, message='{}'",
                    intervalSeconds, message);

        } catch (Exception e) {
            BroadcastMod.LOGGER.error("[BroadcastMod] Failed to read config, keeping previous values.", e);
        }
    }

    public static String getMessage()         { return message; }
    public static int    getIntervalSeconds() { return intervalSeconds; }

    // ── Internals ───────────────────────────────────────────────────────────────

    private static void writeDefaults() {
        JsonObject json = new JsonObject();
        json.addProperty("message",          message);
        json.addProperty("interval_seconds", intervalSeconds);

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(json, writer);
            BroadcastMod.LOGGER.info("[BroadcastMod] Default config created at {}", CONFIG_PATH);
        } catch (Exception e) {
            BroadcastMod.LOGGER.error("[BroadcastMod] Could not write default config.", e);
        }
    }
}
