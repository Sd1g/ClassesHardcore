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
        warrior.cooldowns.putIfAbsent("heal", 440);
        warrior.effects.putIfAbsent("heal_amount", 2.0);
        warrior.effects.putIfAbsent("heal_regen_ticks", 100.0);
        warrior.effects.putIfAbsent("heal_regen_amplifier", 0.0);
        warrior.effects.putIfAbsent("speed_boost_amplifier", 0.0);
        warrior.effects.putIfAbsent("warrior_sword_damage_multiplier", 1.1); // +10%

        // ================== СИЛАЧ ==================
        ClassSettings strongman = cs(c, "strongman");
        strongman.effects.putIfAbsent("power_gain_levels_per_point", 10.0);
        strongman.effects.putIfAbsent("power_gain_damage_tier1", 0.5);
        strongman.effects.putIfAbsent("power_gain_damage_tier2", 0.25);
        strongman.effects.putIfAbsent("power_gain_damage_tier3", 0.1);
        strongman.effects.putIfAbsent("power_gain_tier1_max_level", 100.0);
        strongman.effects.putIfAbsent("power_gain_tier2_max_level", 200.0);

        // ============= УДАЧЛИВЫЙ КОПАТЕЛЬ =============
        ClassSettings luckyDigger = cs(c, "lucky_digger");
        luckyDigger.cooldowns.putIfAbsent("toggle_bonus", 600);
        luckyDigger.effects.putIfAbsent("mining_mastery_blocks_per_level", 200.0);
        luckyDigger.effects.putIfAbsent("mining_mastery_max_level", 4.0);
        luckyDigger.effects.putIfAbsent("mining_mastery_effect_max_amplifier", 4.0);
        luckyDigger.effects.putIfAbsent("fortune_extra_drop_chance_per_level", 0.33);
        luckyDigger.effects.putIfAbsent("fortune_max_extra_drops_per_level", 1.0);
        // Утомление
        luckyDigger.effects.putIfAbsent("digger_exhaust_radius", 20.0);
        luckyDigger.effects.putIfAbsent("digger_exhaust_max_amp", 2.0); // Уровень 3 (0 = I)
        luckyDigger.effects.putIfAbsent("digger_exhaust_min_amp", 0.0); // Уровень 1

        // ================ ТЕЛЕПОРТАТОР ================
        ClassSettings teleporter = cs(c, "teleporter");
        teleporter.cooldowns.putIfAbsent("teleport", 100); // Базовый, переопределяется
        teleporter.effects.putIfAbsent("teleport_max_distance", 30.0);
        teleporter.effects.putIfAbsent("teleport_min_distance", 3.0);
        teleporter.effects.putIfAbsent("teleport_max_pitch", -20.0);
        teleporter.effects.putIfAbsent("teleport_min_pitch", 10.0);
        teleporter.effects.putIfAbsent("teleport_max_up", 5.0);
        teleporter.effects.putIfAbsent("teleport_max_down", 5.0);
        teleporter.effects.putIfAbsent("last_chance_chance", 0.20);
        teleporter.effects.putIfAbsent("last_chance_radius", 10.0);
        teleporter.effects.putIfAbsent("teleport_cooldown_per_block", 40.0); // 2 сек
        teleporter.effects.putIfAbsent("teleport_success_chance", 0.30);

        // ================== ОБЖОРА ==================
        ClassSettings gluttony = cs(c, "gluttony");
        gluttony.cooldowns.putIfAbsent("depletion", 1000);
        gluttony.cooldowns.putIfAbsent("satiate", 600);
        gluttony.effects.putIfAbsent("gluttony_hunger_multiplier", 0.5);
        gluttony.effects.putIfAbsent("depletion_hunger_amplifier", 4.0);
        gluttony.effects.putIfAbsent("depletion_hunger_duration_ticks", 400.0);
        gluttony.effects.putIfAbsent("satiate_food_amount", 4.0); // 4 вместо 2
        gluttony.effects.putIfAbsent("cravings_chance", 0.40);
        gluttony.effects.putIfAbsent("cravings_hunger_amplifier", 2.0);
        gluttony.effects.putIfAbsent("cravings_hunger_duration_ticks", 140.0);

        // ================== ЖИВЧИК ==================
        ClassSettings spry = cs(c, "spry");
        spry.cooldowns.putIfAbsent("second_wind", 900);
        spry.effects.putIfAbsent("vitality_bonus_hp", 10.0);
        spry.effects.putIfAbsent("second_wind_absorption_hp", 4.0);
        spry.effects.putIfAbsent("second_wind_duration_ticks", 300.0);
        spry.effects.putIfAbsent("life_exp_levels_per_point", 5.0);
        spry.effects.putIfAbsent("life_exp_bonus_hp_per_point", 1.0); // 1 HP вместо 2

        // ============== ЛЕДЯНОЙ ВОИН ==============
        ClassSettings iceWarrior = cs(c, "ice_warrior");
        iceWarrior.effects.putIfAbsent("ice_strike_bonus_damage", 1.0);
        iceWarrior.effects.putIfAbsent("ice_strike_slowness_duration_ticks", 100.0);
        iceWarrior.effects.putIfAbsent("ice_strike_slowness_amplifier", 0.0);
        iceWarrior.effects.putIfAbsent("ice_strike_levels_per_amplifier_step", 50.0); // 50 вместо 30
        iceWarrior.effects.putIfAbsent("ice_strike_max_amplifier", 4.0);
        iceWarrior.effects.putIfAbsent("ice_weakening_damage_per_level", 0.5); // 0.5 вместо 1.0
        iceWarrior.effects.putIfAbsent("ice_deep_freeze_chance", 0.025); // 2.5%
        iceWarrior.effects.putIfAbsent("ice_deep_freeze_duration_ticks", 40.0);
        iceWarrior.effects.putIfAbsent("ice_deep_freeze_amplifier", 9.0);

        // ================== ВАМПИР ==================
        ClassSettings vampire = cs(c, "vampire");
        vampire.cooldowns.putIfAbsent("vampire_drain", 1200);
        vampire.effects.putIfAbsent("vampire_heal_percent", 0.60);
        vampire.effects.putIfAbsent("vampire_active_duration_ticks", 200.0);
        vampire.effects.putIfAbsent("vampire_passive_chance", 0.20);
        vampire.effects.putIfAbsent("vampire_steal_heart_chance", 0.004); // 0.4%
        vampire.effects.putIfAbsent("vampire_steal_heart_amount", 2.0); // 1 сердце = 2 HP
        vampire.effects.putIfAbsent("vampire_steal_heart_duration_ticks", 72000.0); // 1 час

        // ================ НЕВИДИМКА ================
        ClassSettings invisible = cs(c, "invisible");
        invisible.cooldowns.putIfAbsent("invisibility", 1200);
        invisible.effects.putIfAbsent("invisibility_duration_ticks", 100.0);
        invisible.effects.putIfAbsent("ambush_damage_multiplier", 2.2);
        invisible.effects.putIfAbsent("invisible_passive_invis_chance", 0.025); // 2.5%

        // ================ ВЕЗУНЧИК =================
        ClassSettings luckyGuy = cs(c, "lucky_guy");
        luckyGuy.cooldowns.putIfAbsent("lucky_swap", 12000);

        // ================== БЕГУН ==================
        ClassSettings runner = cs(c, "runner");
        runner.cooldowns.putIfAbsent("runner_sprint", 800);
        runner.effects.putIfAbsent("runner_sprint_duration_ticks", 300.0);
        runner.effects.putIfAbsent("runner_sprint_amplifier", 4.0);
        runner.effects.putIfAbsent("runner_passive_amplifier", 1.0);
        runner.effects.putIfAbsent("runner_crit_chance", 0.05); // 5%
        runner.effects.putIfAbsent("runner_crit_speed_multiplier", 10.0);

        // ================= КАСКАДЕР =================
        ClassSettings stuntman = cs(c, "stuntman");
        stuntman.cooldowns.putIfAbsent("parkour", 600);
        stuntman.cooldowns.putIfAbsent("climbing_toggle", 100);
        stuntman.effects.putIfAbsent("parkour_jump_amplifier", 4.0);
        stuntman.effects.putIfAbsent("parkour_duration_ticks", 220.0);
        stuntman.effects.putIfAbsent("climbing_speed", 0.1);
        stuntman.effects.putIfAbsent("stuntman_fall_damage_multiplier", 0.3333);

        // ================== ГЕРОЙ ===================
        ClassSettings hero = cs(c, "hero");
        hero.cooldowns.putIfAbsent("hero_gift", 12000);
        hero.effects.putIfAbsent("hero_gift_min_base", 1.0);
        hero.effects.putIfAbsent("hero_gift_max_base", 5.0);
        hero.effects.putIfAbsent("hero_gift_min_per_50", 2.0);
        hero.effects.putIfAbsent("hero_gift_max_per_50", 5.0);
        hero.effects.putIfAbsent("hero_passive_amplifier", 0.0);
        hero.effects.putIfAbsent("hero_totem_chance_at_5", 0.03);
        hero.effects.putIfAbsent("hero_totem_chance_at_10", 0.06);
        hero.effects.putIfAbsent("hero_enchanted_golden_apple_chance_at_10", 0.01); // 1%
        hero.effects.putIfAbsent("hero_raid_damage_multiplier", 3.0); // +200%

        // ================= ДИКОБРАЗ =================
        ClassSettings porcupine = cs(c, "porcupine");
        porcupine.cooldowns.putIfAbsent("porcupine_quills", 400);
        porcupine.effects.putIfAbsent("porcupine_active_damage", 5.0);
        porcupine.effects.putIfAbsent("porcupine_active_radius", 5.0);
        porcupine.effects.putIfAbsent("porcupine_reflect_damage", 3.0);
        porcupine.effects.putIfAbsent("porcupine_reflect_radius", 3.0);
        porcupine.effects.putIfAbsent("porcupine_reflect_chance", 0.30);
        porcupine.effects.putIfAbsent("porcupine_reflect_internal_cooldown_ticks", 20.0);
        porcupine.effects.putIfAbsent("porcupine_thorns_chance", 0.05);
        porcupine.effects.putIfAbsent("porcupine_thorns_percent", 0.30);
        porcupine.effects.putIfAbsent("porcupine_chitin_damage_multiplier", 0.8);

        // ================== ЛУЧНИК ==================
        ClassSettings archer = cs(c, "archer");
        archer.cooldowns.putIfAbsent("archer_jump_back", 700);
        archer.effects.putIfAbsent("archer_draw_speed_multiplier", 1.5);
        archer.effects.putIfAbsent("archer_bow_damage_bonus", 3.0);
        archer.effects.putIfAbsent("archer_jump_back_distance", 12.0);
        archer.effects.putIfAbsent("archer_jump_back_absorption_hp", 4.0);
        archer.effects.putIfAbsent("archer_jump_back_absorption_duration_ticks", 1200.0);
        archer.effects.putIfAbsent("archer_absorption_max_hearts", 6.0); // 6 сердец (12 HP)

        // ============= ОПЫТНЫЙ УБИЙЦА =============
        ClassSettings assassin = cs(c, "assassin");
        assassin.effects.putIfAbsent("assassin_instakill_chance", 0.005); // 0.5%
        assassin.effects.putIfAbsent("assassin_bonus_per_kill", 0.3); // 0.3

        // ================== ТИТАН ===================
        ClassSettings titan = cs(c, "titan");
        titan.cooldowns.putIfAbsent("monolith", 1200);
        titan.effects.putIfAbsent("titan_resistance_amplifier", 0.0); // Снижен на 1 (теперь I)
        titan.effects.putIfAbsent("monolith_resistance_amplifier", 3.0);
        titan.effects.putIfAbsent("monolith_duration_ticks", 100.0);

        // ================= ЛОВКАЧ ===================
        ClassSettings dodger = cs(c, "dodger");
        dodger.cooldowns.putIfAbsent("dodge_speed_boost", 500);
        dodger.effects.putIfAbsent("dodger_dodge_chance", 0.20);
        dodger.effects.putIfAbsent("dodge_speed_amplifier", 1.0);
        dodger.effects.putIfAbsent("dodge_speed_duration_ticks", 200.0);

        // ================= ГРОМИЛА ==================
        ClassSettings brute = cs(c, "brute");
        brute.cooldowns.putIfAbsent("earthshaker", 400);
        brute.effects.putIfAbsent("brute_mace_damage_multiplier", 1.5);
        brute.effects.putIfAbsent("brute_other_damage_multiplier", 0.3);
        brute.effects.putIfAbsent("earthshaker_min_launch", 5.0);
        brute.effects.putIfAbsent("earthshaker_max_launch", 10.0);

        // ============= БЕЗУМНЫЙ ФЕРМЕР =============
        ClassSettings farmer = cs(c, "mad_farmer");
        farmer.cooldowns.putIfAbsent("healing_brew", 600);
        farmer.cooldowns.putIfAbsent("rich_harvest", 3600);
        farmer.effects.putIfAbsent("farmer_hoe_damage_multiplier", 2.0);
        farmer.effects.putIfAbsent("farmer_other_damage_multiplier", 0.2);
        farmer.effects.putIfAbsent("farmer_mobs_per_bonus_point", 100.0);
        farmer.effects.putIfAbsent("farmer_bonus_per_mob_step", 0.25);
        farmer.effects.putIfAbsent("farmer_bonus_per_player_kill", 5.0);
        farmer.effects.putIfAbsent("rich_harvest_radius", 50.0);
        farmer.effects.putIfAbsent("rich_harvest_vertical_range", 10.0);

        // =============== РЕИНКАРНАТОР ===============
        ClassSettings reincarnator = cs(c, "reincarnator");
        reincarnator.effects.putIfAbsent("reincarnator_totem_save_chance", 0.70); // 70%
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