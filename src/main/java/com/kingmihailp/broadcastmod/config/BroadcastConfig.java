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
 * Config location: <minecraft_dir>/config/kingmihailpbroadcast.json
 *
 * Fields:
 *   message          – text sent to all players
 *   color            – hex color string, e.g. "#FF5500", or a Minecraft color name like "white"
 *   interval_seconds – how often the message is broadcast (in seconds, minimum 1)
 */
public class BroadcastConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("kingmihailpbroadcast.json");

    // ── Default values ──────────────────────────────────────────────────────────
    private static String  message         = "Hello, welcome to the server!";
    private static String  color           = "#FFFFFF";
    private static int     intervalSeconds = 300;

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
                BroadcastMod.LOGGER.warn("Config file is empty or invalid – using defaults.");
                return;
            }

            if (json.has("message"))          message         = json.get("message").getAsString();
            if (json.has("color"))            color           = json.get("color").getAsString();
            if (json.has("interval_seconds")) intervalSeconds = Math.max(1, json.get("interval_seconds").getAsInt());

            BroadcastMod.LOGGER.info(
                    "[BroadcastMod] Config loaded – message='{}', color='{}', interval={}s",
                    message, color, intervalSeconds);

        } catch (Exception e) {
            BroadcastMod.LOGGER.error("[BroadcastMod] Failed to read config, keeping previous values.", e);
        }
    }

    public static String getMessage()         { return message; }
    public static String getColor()           { return color; }
    public static int    getIntervalSeconds() { return intervalSeconds; }

    // ── Internals ───────────────────────────────────────────────────────────────

    private static void writeDefaults() {
        JsonObject json = new JsonObject();
        json.addProperty("message",          message);
        json.addProperty("color",            color);
        json.addProperty("interval_seconds", intervalSeconds);

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(json, writer);
            BroadcastMod.LOGGER.info("[BroadcastMod] Default config created at {}", CONFIG_PATH);
        } catch (Exception e) {
            BroadcastMod.LOGGER.error("[BroadcastMod] Could not write default config.", e);
        }
    }
}
