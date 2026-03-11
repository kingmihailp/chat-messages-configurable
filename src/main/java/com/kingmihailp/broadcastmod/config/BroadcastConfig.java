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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles reading and writing the mod's JSON config file.
 *
 * Config location: &lt;minecraft_dir&gt;/config/kingmihailpbroadcast.json
 *
 * <h2>Format</h2>
 * <pre>{@code
 * {
 *   "messages": {
 *     "welcome": {
 *       "text": "&6Welcome to the server!",
 *       "interval_seconds": 300
 *     },
 *     "rules": {
 *       "text": "&cPlease respect the rules.",
 *       "interval_seconds": 600
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>Color codes in text</h2>
 * {@code &0-&f} legacy colors, {@code &l &o &n &m &k} formatting,
 * {@code &r} reset, {@code &#RRGGBB} / {@code &#RGB} hex colors.
 */
public class BroadcastConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("kingmihailpbroadcast.json");

    /** Default message created when no config file exists yet. */
    private static final String DEFAULT_NAME = "welcome";
    private static final String DEFAULT_TEXT =
            "&6[Server] &r&#FF4444Welcome &r&#FFAAAAto the &r&#FF8800server&r&6!";
    private static final int    DEFAULT_INTERVAL = 300;

    // Mutable in-memory map — mutated only from the server thread
    private static final LinkedHashMap<String, BroadcastMessage> messages = new LinkedHashMap<>();

    // ── Public API ──────────────────────────────────────────────────────────────

    /** Returns an unmodifiable snapshot of all configured messages. */
    public static synchronized Map<String, BroadcastMessage> getMessages() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(messages));
    }

    /**
     * Load (or create) the config file. Safe to call at any time for hot-reload.
     * Migrates the old single-message format automatically.
     */
    public static synchronized void load() {
        if (!Files.exists(CONFIG_PATH)) {
            messages.clear();
            messages.put(DEFAULT_NAME, new BroadcastMessage(DEFAULT_TEXT, DEFAULT_INTERVAL));
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);

            if (root == null) {
                BroadcastMod.LOGGER.warn("[BroadcastMod] Config is empty/invalid – keeping defaults.");
                return;
            }

            // ── Legacy migration: old format had top-level "message" field ──────────
            if (root.has("message") && !root.has("messages")) {
                String text     = root.get("message").getAsString();
                int    interval = root.has("interval_seconds")
                        ? Math.max(1, root.get("interval_seconds").getAsInt())
                        : DEFAULT_INTERVAL;
                messages.clear();
                messages.put(DEFAULT_NAME, new BroadcastMessage(text, interval));
                save(); // rewrite in new format
                BroadcastMod.LOGGER.info("[BroadcastMod] Migrated legacy config to multi-message format.");
                return;
            }

            // ── Normal multi-message load ─────────────────────────────────────────
            if (root.has("messages")) {
                JsonObject msgObj = root.getAsJsonObject("messages");
                LinkedHashMap<String, BroadcastMessage> loaded = new LinkedHashMap<>();

                for (String name : msgObj.keySet()) {
                    JsonObject entry = msgObj.getAsJsonObject(name);
                    String text      = entry.has("text")             ? entry.get("text").getAsString()             : "New message";
                    int    interval  = entry.has("interval_seconds") ? entry.get("interval_seconds").getAsInt()    : DEFAULT_INTERVAL;
                    loaded.put(name, new BroadcastMessage(text, Math.max(1, interval)));
                }

                messages.clear();
                messages.putAll(loaded);
            }

            BroadcastMod.LOGGER.info("[BroadcastMod] Config loaded – {} message(s).", messages.size());

        } catch (Exception e) {
            BroadcastMod.LOGGER.error("[BroadcastMod] Failed to read config, keeping previous values.", e);
        }
    }

    /**
     * Creates a new message entry with default values and saves the config.
     *
     * @return {@code false} if a message with that name already exists.
     */
    public static synchronized boolean createMessage(String name) {
        if (messages.containsKey(name)) return false;
        messages.put(name, new BroadcastMessage("New message – edit in config", DEFAULT_INTERVAL));
        save();
        return true;
    }

    // ── Internals ───────────────────────────────────────────────────────────────

    static synchronized void save() {
        JsonObject msgObj = new JsonObject();

        for (Map.Entry<String, BroadcastMessage> entry : messages.entrySet()) {
            JsonObject item = new JsonObject();
            item.addProperty("text",             entry.getValue().getText());
            item.addProperty("interval_seconds", entry.getValue().getIntervalSeconds());
            msgObj.add(entry.getKey(), item);
        }

        JsonObject root = new JsonObject();
        root.add("messages", msgObj);

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(root, writer);
        } catch (Exception e) {
            BroadcastMod.LOGGER.error("[BroadcastMod] Could not write config.", e);
        }
    }
}
