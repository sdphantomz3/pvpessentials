package com.drypted.pvpessentials.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Internal storage for HUD layout positions and anchors.
 * Stored separately from the user-facing DLib config so X/Y/Anchor
 * values are not editable via the config GUI — only via the HUD Layout Editor.
 */
public class HudLayoutStorage {

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("pvpessentials_hud_layout.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, Entry> data = null;

    public static class Entry {
        public float x;
        public float y;
        public String anchor;

        public Entry() {}

        public Entry(float x, float y, String anchor) {
            this.x = x;
            this.y = y;
            this.anchor = anchor;
        }
    }

    /** Load layout data from disk. Called once during init. */
    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            data = new LinkedHashMap<>();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Map<String, Entry> loaded = GSON.fromJson(reader,
                    new TypeToken<Map<String, Entry>>(){}.getType());
            data = loaded != null ? loaded : new LinkedHashMap<>();
        } catch (Exception e) {
            data = new LinkedHashMap<>();
        }
    }

    /** Save layout data to disk. */
    public static void save() {
        try {
            if (!Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception ignored) {}
    }

    public static float getX(String hudId, float defaultVal) {
        ensureLoaded();
        Entry e = data.get(hudId);
        if (e != null) {
            // Legacy migration: values > 2.0 were raw pixel coords
            if (e.x > 2.0f) return defaultVal;
            return Math.max(0f, Math.min(1f, e.x));
        }
        return defaultVal;
    }

    public static float getY(String hudId, float defaultVal) {
        ensureLoaded();
        Entry e = data.get(hudId);
        if (e != null) {
            if (e.y > 2.0f) return defaultVal;
            return Math.max(0f, Math.min(1f, e.y));
        }
        return defaultVal;
    }

    public static Anchor getAnchor(String hudId, Anchor defaultVal) {
        ensureLoaded();
        Entry e = data.get(hudId);
        if (e != null && e.anchor != null && !e.anchor.isEmpty()) {
            try { return Anchor.valueOf(e.anchor.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        return defaultVal;
    }

    public static void set(String hudId, float x, float y, Anchor anchor) {
        ensureLoaded();
        data.put(hudId, new Entry(x, y, anchor.name()));
    }

    public static Map<String, Entry> getAll() {
        ensureLoaded();
        return new LinkedHashMap<>(data);
    }

    private static void ensureLoaded() {
        if (data == null) load();
    }
}
