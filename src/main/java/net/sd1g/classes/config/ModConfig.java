package net.sd1g.classes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.sd1g.classes.ClassesHardcore;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModConfig {
    public static final int CURRENT_VERSION = 2;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("classeshardcore/balance.json");
    private static ModConfig INSTANCE;

    public int version = CURRENT_VERSION;
    public Map<String, ClassSettings> classes = new LinkedHashMap<>();
    public Map<String, Double> global = new LinkedHashMap<>();

    public static class ClassSettings {
        public Map<String, Double> values = new LinkedHashMap<>();
    }

    public static ModConfig getInstance() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static File getConfigFile() { return CONFIG_PATH.toFile(); }

    public double get(String classId, String key, double def) {
        ClassSettings cs = classes.get(classId);
        if (cs == null) return def;
        Double v = cs.values.get(key);
        return v != null ? v : def;
    }

    public int getInt(String classId, String key, int def) {
        return (int) Math.round(get(classId, key, def));
    }

    public int getTicks(String classId, String key, int def) {
        return Math.max(0, (int) Math.round(get(classId, key, def)));
    }

    public double getGlobal(String key, double def) {
        Double v = global.get(key);
        return v != null ? v : def;
    }

    public int getGlobalInt(String key, int def) {
        return (int) Math.round(getGlobal(key, def));
    }

    public boolean getGlobalBool(String key, boolean def) {
        return getGlobalInt(key, def ? 1 : 0) != 0;
    }

    public void set(String classId, String key, double value) {
        classes.computeIfAbsent(classId, k -> new ClassSettings()).values.put(key, value);
    }

    public void setGlobal(String key, double value) {
        global.put(key, value);
    }

    public static void load() {
        File file = CONFIG_PATH.toFile();
        boolean needsSave = false;

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    int v = root.has("version") ? root.get("version").getAsInt() : 1;
                    if (v < CURRENT_VERSION) {
                        needsSave = true;
                        try {
                            Files.move(CONFIG_PATH, CONFIG_PATH.resolveSibling("balance.json.v1.backup"));
                            ClassesHardcore.LOGGER.warn("Old balance.json backed up to balance.json.v1.backup (format v1 → v2)");
                        } catch (IOException ignored) {}
                    }
                    INSTANCE = GSON.fromJson(root, ModConfig.class);
                }
            } catch (Exception e) {
                ClassesHardcore.LOGGER.error("Failed to load config, using defaults", e);
            }
        } else {
            needsSave = true;
        }

        if (INSTANCE == null) INSTANCE = new ModConfig();
        if (INSTANCE.classes == null) INSTANCE.classes = new LinkedHashMap<>();
        if (INSTANCE.global == null) INSTANCE.global = new LinkedHashMap<>();

        int oldSize = INSTANCE.classes.size();
        int oldGlobalSize = INSTANCE.global.size();

        applyDefaults(INSTANCE);

        if (INSTANCE.classes.size() > oldSize || INSTANCE.global.size() > oldGlobalSize) {
            needsSave = true; // Добавились новые ключи
        }

        if (needsSave) {
            save();
        }
        ClassesHardcore.LOGGER.info("Config loaded: {} classes, {} global keys",
                INSTANCE.classes.size(), INSTANCE.global.size());
    }

    public static void save() {
        try {
            CONFIG_PATH.getParent().toFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            ClassesHardcore.LOGGER.error("Failed to save config", e);
        }
    }

    public static String getJson() {
        return GSON.toJson(INSTANCE);
    }

    public static void loadFromJson(String json) {
        try {
            ModConfig loaded = GSON.fromJson(json, ModConfig.class);
            if (loaded != null) {
                if (loaded.classes == null) loaded.classes = new LinkedHashMap<>();
                if (loaded.global == null) loaded.global = new LinkedHashMap<>();
                INSTANCE = loaded;
                ClassesHardcore.LOGGER.info("Config synced from server: {} classes", loaded.classes.size());
            }
        } catch (Exception e) {
            ClassesHardcore.LOGGER.error("Failed to load config from JSON", e);
        }
    }

    private static ClassSettings cs(ModConfig c, String classId) {
        return c.classes.computeIfAbsent(classId, k -> new ClassSettings());
    }

    private static void applyDefaults(ModConfig c) {
        // ---- Глобальные ----
        c.global.putIfAbsent("party.enabled", 1.0);
        c.global.putIfAbsent("party.allow_friendly_fire", 0.0);
        c.global.putIfAbsent("party.negative_abilities_affect_allies", 0.0);
        c.global.putIfAbsent("party.max_size", 2.0);
        c.global.putIfAbsent("party.disband_on_leader_leave", 0.0);
        c.global.putIfAbsent("party.disband_on_member_leave", 0.0);

        // ---- Warrior ----
        ClassSettings warrior = cs(c, "warrior");

        warrior.values.putIfAbsent("warrior_heal.cooldown_ticks", 440.0);
        warrior.values.putIfAbsent("warrior_heal.base_cast_time_ticks", 0.0);
        warrior.values.putIfAbsent("warrior_heal.amount_hp", 4.0);
        warrior.values.putIfAbsent("warrior_heal.regen_duration_ticks", 100.0);
        warrior.values.putIfAbsent("warrior_heal.regen_amplifier", 0.0);

        warrior.values.putIfAbsent("warrior_veteran.kills_per_step", 2000.0);
        warrior.values.putIfAbsent("warrior_veteran.hp_per_step", 0.5);
        warrior.values.putIfAbsent("warrior_veteran.max_hp_bonus", 10.0);
        warrior.values.putIfAbsent("warrior_veteran.cast_time_per_step_ticks", 4.0);
        warrior.values.putIfAbsent("warrior_veteran.max_cast_time_bonus_ticks", 80.0);

        warrior.values.putIfAbsent("warrior_speed_boost.amplifier", 0.0);
        warrior.values.putIfAbsent("warrior_combat_experience.sword_bonus_percent", 15.0);
        warrior.values.putIfAbsent("warrior_combat_tempo.cooldown_ticks", 700.0);
        warrior.values.putIfAbsent("warrior_combat_tempo.duration_ticks", 160.0);
        warrior.values.putIfAbsent("warrior_combat_tempo.attack_speed_bonus", 0.5);
    }
}