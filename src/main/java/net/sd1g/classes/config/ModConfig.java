package net.sd1g.classes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.sd1g.classes.ClassesHardcore;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("classeshardcore/balance.json");
    private static ModConfig INSTANCE;

    public Map<String, ClassSettings> classes = new LinkedHashMap<>();

    private transient Map<String, Integer> cooldowns = new HashMap<>();
    private transient Map<String, Double> effects = new HashMap<>();

    public static class ClassSettings {
        public Map<String, Integer> cooldowns = new LinkedHashMap<>();
        public Map<String, Double> effects = new LinkedHashMap<>();
    }

    public static ModConfig getInstance() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    private static ClassSettings cs(ModConfig c, String classId) {
        return c.classes.computeIfAbsent(classId, k -> new ClassSettings());
    }

    private static void applyDefaults(ModConfig c) {
        // ================== ВОИН ==================
        ClassSettings warrior = cs(c, "warrior");
        warrior.cooldowns.putIfAbsent("warrior_heal_cooldown", 440);
        warrior.cooldowns.putIfAbsent("warrior_tempo_cooldown", 700);
        warrior.effects.putIfAbsent("warrior_heal_amount", 4.0);
        warrior.effects.putIfAbsent("warrior_heal_regen_ticks", 100.0);
        warrior.effects.putIfAbsent("warrior_heal_regen_amp", 0.0);
        warrior.effects.putIfAbsent("warrior_speed_amp", 0.0);
        warrior.effects.putIfAbsent("warrior_tempo_duration", 160.0);
        warrior.effects.putIfAbsent("warrior_tempo_attack_speed_bonus", 0.5);
        warrior.effects.putIfAbsent("warrior_veteran_kills_per_step", 2000.0);
        warrior.effects.putIfAbsent("warrior_veteran_bonus_hp", 0.5);
        warrior.effects.putIfAbsent("warrior_veteran_max_bonus", 10.0);
        warrior.effects.putIfAbsent("warrior_sword_damage_bonus", 0.15);

        // ================== СИЛАЧ ==================
        ClassSettings strongman = cs(c, "strongman");
        strongman.effects.putIfAbsent("strongman_levels_per_step", 10.0);
        strongman.effects.putIfAbsent("strongman_tier1_max_lvl", 100.0);
        strongman.effects.putIfAbsent("strongman_tier2_max_lvl", 200.0);
        strongman.effects.putIfAbsent("strongman_tier1_dmg", 1.0);
        strongman.effects.putIfAbsent("strongman_tier2_dmg", 0.5);
        strongman.effects.putIfAbsent("strongman_tier3_dmg", 0.25);
        strongman.effects.putIfAbsent("strongman_hp_to_damage_multiplier", 0.5);

        // ============= УДАЧЛИВЫЙ КОПАТЕЛЬ =============
        ClassSettings luckyDigger = cs(c, "lucky_digger");
        luckyDigger.cooldowns.putIfAbsent("digger_toggle_cooldown", 600);
        luckyDigger.effects.putIfAbsent("digger_ore_per_level", 200.0);
        luckyDigger.effects.putIfAbsent("digger_max_level", 4.0);
        luckyDigger.effects.putIfAbsent("digger_haste_max_amp", 4.0);
        luckyDigger.effects.putIfAbsent("digger_suppress_radius_1", 4.0);
        luckyDigger.effects.putIfAbsent("digger_suppress_radius_2", 8.0);
        luckyDigger.effects.putIfAbsent("digger_suppress_radius_3", 16.0);
        luckyDigger.effects.putIfAbsent("digger_suppress_amp_1", 2.0);
        luckyDigger.effects.putIfAbsent("digger_suppress_amp_2", 1.0);
        luckyDigger.effects.putIfAbsent("digger_suppress_amp_3", 0.0);
        luckyDigger.effects.putIfAbsent("digger_suppress_duration", 40.0);
        luckyDigger.effects.putIfAbsent("digger_heavy_strike_chance", 0.05);
        luckyDigger.effects.putIfAbsent("digger_heavy_strike_duration", 300.0);
        luckyDigger.effects.putIfAbsent("digger_heavy_strike_amp", 4.0);

        // ================ ТЕЛЕПОРТАТОР ================
        ClassSettings teleporter = cs(c, "teleporter");
        teleporter.cooldowns.putIfAbsent("teleport", 100);
        teleporter.effects.putIfAbsent("teleport_max_distance", 30.0);
        teleporter.effects.putIfAbsent("teleport_min_distance", 3.0);
        teleporter.effects.putIfAbsent("teleport_max_pitch", -20.0);
        teleporter.effects.putIfAbsent("teleport_min_pitch", 10.0);
        teleporter.effects.putIfAbsent("teleport_max_up", 5.0);
        teleporter.effects.putIfAbsent("teleport_max_down", 5.0);
        teleporter.effects.putIfAbsent("last_chance_chance", 0.30);
        teleporter.effects.putIfAbsent("last_chance_radius", 30.0);
        teleporter.effects.putIfAbsent("teleport_cooldown_per_block", 40.0);
        teleporter.effects.putIfAbsent("teleport_min_cooldown", 200.0);
        teleporter.effects.putIfAbsent("teleport_max_cooldown", 1200.0);
        teleporter.effects.putIfAbsent("teleporter_tp_per_bonus", 200.0);
        teleporter.effects.putIfAbsent("teleporter_max_bonus", 10.0);

        // ================== ОБЖОРА ==================
        ClassSettings gluttony = cs(c, "gluttony");
        gluttony.cooldowns.putIfAbsent("depletion", 1000);
        gluttony.cooldowns.putIfAbsent("satiate", 600);
        gluttony.effects.putIfAbsent("gluttony_hunger_multiplier", 0.5);
        gluttony.effects.putIfAbsent("depletion_hunger_amplifier", 4.0);
        gluttony.effects.putIfAbsent("depletion_hunger_duration_ticks", 400.0);
        gluttony.effects.putIfAbsent("satiate_food_amount", 4.0);
        gluttony.effects.putIfAbsent("cravings_chance", 0.40);
        gluttony.effects.putIfAbsent("cravings_hunger_amplifier", 2.0);
        gluttony.effects.putIfAbsent("cravings_hunger_duration_ticks", 140.0);
        gluttony.effects.putIfAbsent("gluttony_hungry_hunter_damage", 2.0);

        // ================== ЖИВЧИК ==================
        ClassSettings spry = cs(c, "spry");
        spry.cooldowns.putIfAbsent("second_wind", 900);
        spry.effects.putIfAbsent("vitality_bonus_hp", 5.0);
        spry.effects.putIfAbsent("second_wind_absorption_hp", 4.0);
        spry.effects.putIfAbsent("second_wind_duration_ticks", 300.0);
        spry.effects.putIfAbsent("life_exp_levels_per_point", 5.0);
        spry.effects.putIfAbsent("life_exp_bonus_hp_per_point", 1.0);

        // ============== ЛЕДЯНОЙ ВОИН ==============
        ClassSettings iceWarrior = cs(c, "ice_warrior");
        iceWarrior.effects.putIfAbsent("ice_strike_bonus_damage", 1.5);
        iceWarrior.effects.putIfAbsent("ice_strike_slowness_duration_ticks", 100.0);
        iceWarrior.effects.putIfAbsent("ice_strike_levels_per_amplifier_step", 50.0);
        iceWarrior.effects.putIfAbsent("ice_strike_max_amplifier", 2.0);
        iceWarrior.effects.putIfAbsent("ice_weakening_damage_per_level", 0.5);
        iceWarrior.effects.putIfAbsent("ice_deep_freeze_chance", 0.03);
        iceWarrior.effects.putIfAbsent("ice_deep_freeze_duration_ticks", 50.0);
        iceWarrior.effects.putIfAbsent("ice_deep_freeze_amplifier", 9.0);

        // ================== ВАМПИР ==================
        ClassSettings vampire = cs(c, "vampire");
        vampire.cooldowns.putIfAbsent("vampire_drain", 1200);
        vampire.effects.putIfAbsent("vampire_heal_percent", 0.70);
        vampire.effects.putIfAbsent("vampire_active_duration_ticks", 200.0);
        vampire.effects.putIfAbsent("vampire_passive_chance", 0.20);
        vampire.effects.putIfAbsent("vampire_passive_heal_percent", 0.60);
        vampire.effects.putIfAbsent("vampire_steal_heart_chance", 0.004);
        vampire.effects.putIfAbsent("vampire_steal_heart_amount", 2.0);
        vampire.effects.putIfAbsent("vampire_steal_heart_duration_ticks", 72000.0);

        // ================ НЕВИДИМКА ================
        ClassSettings invisible = cs(c, "invisible");
        invisible.cooldowns.putIfAbsent("invisibility", 1200);
        invisible.effects.putIfAbsent("invisibility_duration_ticks", 100.0);
        invisible.effects.putIfAbsent("ambush_damage_multiplier", 3.0);
        invisible.effects.putIfAbsent("ambush_pure_damage", 2.0);
        invisible.effects.putIfAbsent("invisible_passive_invis_chance", 0.025);
        invisible.effects.putIfAbsent("invisible_passive_invis_cooldown", 200.0);

        // ================ ВЕЗУНЧИК =================
        ClassSettings luckyGuy = cs(c, "lucky_guy");
        luckyGuy.cooldowns.putIfAbsent("lucky_swap", 12000);

        // ================== БЕГУН ==================
        ClassSettings runner = cs(c, "runner");
        runner.cooldowns.putIfAbsent("runner_sprint", 800);
        runner.effects.putIfAbsent("runner_sprint_duration_ticks", 300.0);
        runner.effects.putIfAbsent("runner_sprint_amplifier", 4.0);
        runner.effects.putIfAbsent("runner_passive_amplifier", 1.0);
        runner.effects.putIfAbsent("runner_crit_chance", 0.05);
        runner.effects.putIfAbsent("runner_crit_t1_mult", 1.0);
        runner.effects.putIfAbsent("runner_crit_t1_dmg", 1.5);
        runner.effects.putIfAbsent("runner_crit_t2_mult", 1.2);
        runner.effects.putIfAbsent("runner_crit_t2_dmg", 2.5);
        runner.effects.putIfAbsent("runner_crit_t3_mult", 1.4);
        runner.effects.putIfAbsent("runner_crit_t3_dmg", 3.0);
        runner.effects.putIfAbsent("runner_crit_t4_mult", 1.6);
        runner.effects.putIfAbsent("runner_crit_t4_dmg", 3.5);
        runner.effects.putIfAbsent("runner_crit_t5_mult", 1.8);
        runner.effects.putIfAbsent("runner_crit_t5_dmg", 4.0);
        runner.effects.putIfAbsent("runner_crit_t6_mult", 2.0);
        runner.effects.putIfAbsent("runner_crit_t6_dmg", 4.5);
        runner.effects.putIfAbsent("runner_crit_t7_mult", 2.2);
        runner.effects.putIfAbsent("runner_crit_t7_dmg", 4.5);
        runner.effects.putIfAbsent("runner_crit_t8_mult", 2.4);
        runner.effects.putIfAbsent("runner_crit_t8_dmg", 5.0);
        runner.effects.putIfAbsent("runner_crit_t9_mult", 3.0);
        runner.effects.putIfAbsent("runner_crit_t9_dmg", 10.0);

        // ================= КАСКАДЕР =================
        ClassSettings stuntman = cs(c, "stuntman");
        stuntman.cooldowns.putIfAbsent("parkour", 600);
        stuntman.cooldowns.putIfAbsent("climbing_toggle", 100);
        stuntman.effects.putIfAbsent("parkour_jump_amplifier", 4.0);
        stuntman.effects.putIfAbsent("parkour_duration_ticks", 220.0);
        stuntman.effects.putIfAbsent("stuntman_fall_damage_multiplier", 0.3333);
        stuntman.effects.putIfAbsent("stuntman_master_blocks_per_step", 500.0);
        stuntman.effects.putIfAbsent("stuntman_master_max_steps", 5.0);
        stuntman.effects.putIfAbsent("stuntman_master_duration_bonus", 20.0);
        stuntman.effects.putIfAbsent("stuntman_master_cooldown_reduction", 20.0);

        // ================== ГЕРОЙ ===================
        ClassSettings hero = cs(c, "hero");
        hero.cooldowns.putIfAbsent("hero_gift", 12000);
        hero.effects.putIfAbsent("hero_gift_min_base", 1.0);
        hero.effects.putIfAbsent("hero_gift_max_base", 5.0);
        hero.effects.putIfAbsent("hero_gift_min_per_50", 2.0);
        hero.effects.putIfAbsent("hero_gift_max_per_50", 5.0);
        hero.effects.putIfAbsent("hero_totem_chance_at_5", 0.03);
        hero.effects.putIfAbsent("hero_totem_chance_at_10", 0.06);
        hero.effects.putIfAbsent("hero_enchanted_golden_apple_chance_at_10", 0.01);
        hero.effects.putIfAbsent("hero_raid_damage_multiplier", 3.0);
        hero.effects.putIfAbsent("hero_trades_per_level", 50.0);

        // ================= ДИКОБРАЗ =================
        ClassSettings porcupine = cs(c, "porcupine");
        porcupine.cooldowns.putIfAbsent("porcupine_quills", 400);
        porcupine.effects.putIfAbsent("porcupine_active_damage", 6.0);
        porcupine.effects.putIfAbsent("porcupine_active_radius", 5.0);
        porcupine.effects.putIfAbsent("porcupine_reflect_damage", 2.0);
        porcupine.effects.putIfAbsent("porcupine_reflect_radius", 3.0);
        porcupine.effects.putIfAbsent("porcupine_reflect_chance", 0.30);
        porcupine.effects.putIfAbsent("porcupine_reflect_internal_cooldown_ticks", 40.0);
        porcupine.effects.putIfAbsent("porcupine_thorns_chance", 0.05);
        porcupine.effects.putIfAbsent("porcupine_thorns_percent", 0.50);
        porcupine.effects.putIfAbsent("porcupine_thorns_pure_damage", 2.0);
        porcupine.effects.putIfAbsent("porcupine_chitin_damage_multiplier", 0.8);
        porcupine.effects.putIfAbsent("porcupine_hardening_hits_per_step", 500.0);
        porcupine.effects.putIfAbsent("porcupine_hardening_radius_per_step", 0.5);
        porcupine.effects.putIfAbsent("porcupine_hardening_max_steps", 4.0);

        // ================== ЛУЧНИК ==================
        ClassSettings archer = cs(c, "archer");
        archer.cooldowns.putIfAbsent("archer_jump_back", 700);
        archer.effects.putIfAbsent("archer_draw_speed_multiplier", 1.5);
        archer.effects.putIfAbsent("archer_pure_damage", 4.0);
        archer.effects.putIfAbsent("archer_jump_back_distance", 12.0);
        archer.effects.putIfAbsent("archer_jump_back_absorption_hp", 4.0);
        archer.effects.putIfAbsent("archer_jump_back_absorption_duration_ticks", 1200.0);
        archer.effects.putIfAbsent("archer_hunter_kills_per_step", 100.0);
        archer.effects.putIfAbsent("archer_hunter_max_reduction_ticks", 200.0);

        // ============= ОПЫТНЫЙ УБИЙЦА =============
        ClassSettings assassin = cs(c, "assassin");
        assassin.effects.putIfAbsent("assassin_instakill_chance", 0.005);
        assassin.effects.putIfAbsent("assassin_bonus_per_kill", 0.3);

        // ================== ТИТАН ===================
        ClassSettings titan = cs(c, "titan");
        titan.cooldowns.putIfAbsent("monolith", 1200);
        titan.effects.putIfAbsent("titan_resistance_amplifier", 0.0);
        titan.effects.putIfAbsent("monolith_resistance_amplifier", 3.0);
        titan.effects.putIfAbsent("monolith_duration_ticks", 100.0);

        // ================= ЛОВКАЧ ===================
        ClassSettings dodger = cs(c, "dodger");
        dodger.cooldowns.putIfAbsent("dodge_speed_boost", 500);
        dodger.effects.putIfAbsent("dodger_dodge_chance", 0.20);
        dodger.effects.putIfAbsent("dodge_speed_amplifier", 1.0);
        dodger.effects.putIfAbsent("dodge_speed_duration_ticks", 200.0);
        dodger.effects.putIfAbsent("dodger_kills_per_step", 300.0);
        dodger.effects.putIfAbsent("dodger_max_bonus_chance", 0.10);

        // ================= ГРОМИЛА ==================
        ClassSettings brute = cs(c, "brute");
        brute.cooldowns.putIfAbsent("earthshaker", 500);
        brute.effects.putIfAbsent("brute_mace_damage_multiplier", 1.5);
        brute.effects.putIfAbsent("brute_other_damage_multiplier", 0.3);
        brute.effects.putIfAbsent("earthshaker_min_launch", 5.0);
        brute.effects.putIfAbsent("earthshaker_max_launch", 10.0);
        brute.effects.putIfAbsent("earthshaker_pure_damage", 2.0);

        // ============= БЕЗУМНЫЙ ФЕРМЕР =============
        ClassSettings farmer = cs(c, "mad_farmer");
        farmer.cooldowns.putIfAbsent("healing_brew", 600);
        farmer.cooldowns.putIfAbsent("rich_harvest", 3600);
        farmer.effects.putIfAbsent("farmer_hoe_damage_multiplier", 2.0);
        farmer.effects.putIfAbsent("farmer_other_damage_multiplier", 0.2);
        farmer.effects.putIfAbsent("farmer_mobs_per_bonus_point", 100.0);
        farmer.effects.putIfAbsent("farmer_bonus_per_mob_step", 0.5);
        farmer.effects.putIfAbsent("farmer_bonus_per_player_kill", 10.0);
        farmer.effects.putIfAbsent("rich_harvest_radius", 50.0);
        farmer.effects.putIfAbsent("rich_harvest_vertical_range", 10.0);

        // =============== РЕИНКАРНАТОР ===============
        ClassSettings reincarnator = cs(c, "reincarnator");
        reincarnator.effects.putIfAbsent("reincarnator_totem_save_chance", 0.70);
    }

    private void rebuildFlatMaps() {
        cooldowns.clear();
        effects.clear();
        for (ClassSettings s : classes.values()) {
            cooldowns.putAll(s.cooldowns);
            effects.putAll(s.effects);
        }
    }

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                INSTANCE = loaded != null ? loaded : new ModConfig();
                if (INSTANCE.classes == null) INSTANCE.classes = new LinkedHashMap<>();
            } catch (IOException e) {
                ClassesHardcore.LOGGER.error("Failed to load config, using defaults", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
        }
        applyDefaults(INSTANCE);
        INSTANCE.rebuildFlatMaps();
        save();
        ClassesHardcore.LOGGER.info("Config loaded: {} classes, {} cooldowns, {} effects",
                INSTANCE.classes.size(), INSTANCE.cooldowns.size(), INSTANCE.effects.size());
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
                INSTANCE = loaded;
                if (INSTANCE.classes == null) INSTANCE.classes = new LinkedHashMap<>();
                INSTANCE.rebuildFlatMaps();
                ClassesHardcore.LOGGER.info("Config synced from server: {} classes", INSTANCE.classes.size());
            }
        } catch (Exception e) {
            ClassesHardcore.LOGGER.error("Failed to load config from JSON", e);
        }
    }

    public int getCooldown(String abilityId, int defaultTicks) {
        return cooldowns.getOrDefault(abilityId, defaultTicks);
    }

    public double getEffect(String key, double def) {
        return effects.getOrDefault(key, def);
    }

    public int getIntEffect(String key, int def) {
        Double v = effects.get(key);
        return v != null ? (int) Math.round(v) : def;
    }
}