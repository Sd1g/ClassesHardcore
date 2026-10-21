package net.sd1g.classes.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AbilityClientState {
    public static String luckyDiggerMode = "efficiency";
    public static int luckyDiggerTotalOre = 0;
    public static int luckyDiggerLevel = 0;
    public static int vampireActiveTicks = 0;
    public static int invisibilityActiveTicks = 0;
    public static int runnerSprintTicks = 0;
    public static double assassinBonusDamage = 0.0;
    public static int farmerMobKills = 0;
    public static int farmerPlayerKills = 0;
    public static int heroTrades = 0;
    public static int teleporterCount = 0;
    public static int stuntmanClimbedBlocks = 0;
    public static int porcupineHitsTaken = 0;
    public static int archerBowKills = 0;
    public static int warriorSwordKills = 0;
    public static int dodgerDodges = 0;

    // --- НОВОЕ ДЛЯ HUD ---
    public static final Map<String, Integer> activeTimers = new HashMap<>();
    public static final Set<String> armedAbilities = new HashSet<>();
    public static final List<HudMessage> hudMessages = new ArrayList<>();

    public static class HudMessage {
        public String text;
        public int color;
        public int ticksLeft;

        public HudMessage(String text, int color, int ticksLeft) {
            this.text = text;
            this.color = color;
            this.ticksLeft = ticksLeft;
        }
    }
}