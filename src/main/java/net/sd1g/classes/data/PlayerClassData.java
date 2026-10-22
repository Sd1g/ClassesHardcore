package net.sd1g.classes.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;

import java.util.HashMap;
import java.util.Map;

public class PlayerClassData {
    public String playerClass = "none";
    public Map<String, Long> abilityCooldowns = new HashMap<>();
    public Map<String, Integer> abilityCooldownTotals = new HashMap<>();

    private transient ClassDefinition cachedClassDef = null;
    private transient String cachedClassDefId = null;

    // --- Состояние каста ---
    public boolean isCasting = false;
    public int castTicksRemaining = 0;
    public int totalCastTicks = 0;
    public String castingAbilityId = "";
    public double castX, castY, castZ; // Координаты для заморозки

    // --- Удачливый копатель ---
    public String luckyDiggerMode = "efficiency";
    public int luckyDiggerTotalOre = 0;
    public int luckyDiggerLevel = 0;

    public int vampireActiveTicks = 0;
    public int invisibilityActiveTicks = 0;
    public int runnerSprintTicks = 0;

    public double assassinBonusDamage = 0.0;

    // --- Безумный фермер ---
    public int farmerMobKills = 0;
    public int farmerPlayerKills = 0;

    // --- Обжора ---
    public boolean gluttonyDepletionArmed = false;

    // --- Невидимка ---
    public boolean invisibleAmbushArmed = false;

    // --- Громила ---
    public boolean bruteShakeArmed = false;

    // --- Каскадер ---
    public boolean climbingActive = false;
    public boolean climbingInProgress = false;
    public int stuntmanClimbedBlocks = 0;
    public double stuntmanClimbAccumulator = 0.0;

    // --- Герой ---
    public int heroTrades = 0;
    public int heroTradeBaseline = 0;

    // --- Токены смены класса ---
    public int randomChangeTokens = 0;
    public int pick3ChangeTokens = 0;

    // --- Счетчики для ребаланса ---
    public int warriorSwordKills = 0;
    public int teleporterCount = 0;
    public int porcupineHitsTaken = 0;
    public int archerBowKills = 0;
    public int dodgerDodgeCount = 0;

    // --- Кристаллы Энда ---
    public int endCrystalUses = 0;
    public long crystalCooldownEndTick = 0;

    // --- Реинкарнатор ---
    public int reincarnatorTotemSaves = 0;

    public static PlayerClassData get(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return new PlayerClassData();
        ClassStateSaver saver = ClassStateSaver.get(server);
        return saver.playerData.computeIfAbsent(player.getUUID(), k -> new PlayerClassData());
    }

    public static void markDirty(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server != null) ClassStateSaver.get(server).setDirty();
    }

    public ClassDefinition getClassDef() {
        String cls = playerClass;
        if (cls == null) cls = "none";
        if (cachedClassDef != null && cls.equals(cachedClassDefId)) {
            return cachedClassDef;
        }
        cachedClassDef = ClassRegistry.get(cls);
        cachedClassDefId = cls;
        return cachedClassDef;
    }

    public void setPlayerClass(String classId) {
        this.playerClass = classId;
        this.cachedClassDef = ClassRegistry.get(classId);
        this.cachedClassDefId = classId;
    }

    public boolean isOnCooldown(String abilityId, long currentTick) {
        Long endTick = abilityCooldowns.get(abilityId);
        return endTick != null && currentTick < endTick;
    }

    public long getRemainingCooldown(String abilityId, long currentTick) {
        Long endTick = abilityCooldowns.get(abilityId);
        if (endTick == null) return 0;
        return Math.max(0, endTick - currentTick);
    }

    public void setCooldown(String abilityId, long currentTick, int ticks) {
        abilityCooldowns.put(abilityId, currentTick + ticks);
        abilityCooldownTotals.put(abilityId, ticks);
    }

    public void clearCooldowns() {
        abilityCooldowns.clear();
        abilityCooldownTotals.clear();
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("class", playerClass);
        CompoundTag cdTag = new CompoundTag();
        abilityCooldowns.forEach(cdTag::putLong);
        tag.put("cooldowns", cdTag);
        CompoundTag cdTotalTag = new CompoundTag();
        abilityCooldownTotals.forEach(cdTotalTag::putInt);
        tag.put("cooldown_totals", cdTotalTag);

        tag.putString("luckyDiggerMode", luckyDiggerMode);
        tag.putInt("luckyDiggerTotalOre", luckyDiggerTotalOre);
        tag.putInt("luckyDiggerLevel", luckyDiggerLevel);
        tag.putInt("vampireActiveTicks", vampireActiveTicks);
        tag.putInt("invisibilityActiveTicks", invisibilityActiveTicks);
        tag.putInt("runnerSprintTicks", runnerSprintTicks);
        tag.putDouble("assassinBonusDamage", assassinBonusDamage);
        tag.putInt("farmerMobKills", farmerMobKills);
        tag.putInt("farmerPlayerKills", farmerPlayerKills);
        tag.putBoolean("gluttonyDepletionArmed", gluttonyDepletionArmed);
        tag.putBoolean("invisibleAmbushArmed", invisibleAmbushArmed);
        tag.putBoolean("bruteShakeArmed", bruteShakeArmed);
        tag.putBoolean("climbingActive", climbingActive);
        tag.putBoolean("climbingInProgress", climbingInProgress);
        tag.putInt("stuntmanClimbedBlocks", stuntmanClimbedBlocks);
        tag.putDouble("stuntmanClimbAccumulator", stuntmanClimbAccumulator);
        tag.putInt("heroTrades", heroTrades);
        tag.putInt("heroTradeBaseline", heroTradeBaseline);
        tag.putInt("randomChangeTokens", randomChangeTokens);
        tag.putInt("pick3ChangeTokens", pick3ChangeTokens);

        tag.putInt("warriorSwordKills", warriorSwordKills);
        tag.putInt("teleporterCount", teleporterCount);
        tag.putInt("porcupineHitsTaken", porcupineHitsTaken);
        tag.putInt("archerBowKills", archerBowKills);
        tag.putInt("dodgerDodgeCount", dodgerDodgeCount);
        tag.putInt("endCrystalUses", endCrystalUses);
        tag.putLong("crystalCooldownEndTick", crystalCooldownEndTick);
        tag.putInt("reincarnatorTotemSaves", reincarnatorTotemSaves);
        return tag;
    }

    public static PlayerClassData fromNbt(CompoundTag tag) {
        PlayerClassData data = new PlayerClassData();
        data.playerClass = tag.getStringOr("class", "none");
        CompoundTag cdTag = tag.getCompoundOrEmpty("cooldowns");
        for (String key : cdTag.keySet()) {
            data.abilityCooldowns.put(key, cdTag.getLongOr(key, 0L));
        }
        CompoundTag cdTotalTag = tag.getCompoundOrEmpty("cooldown_totals");
        for (String key : cdTotalTag.keySet()) {
            data.abilityCooldownTotals.put(key, cdTotalTag.getIntOr(key, 0));
        }
        data.luckyDiggerMode = tag.getStringOr("luckyDiggerMode", "efficiency");
        data.luckyDiggerTotalOre = tag.getIntOr("luckyDiggerTotalOre", 0);
        data.luckyDiggerLevel = tag.getIntOr("luckyDiggerLevel", 0);
        data.vampireActiveTicks = tag.getIntOr("vampireActiveTicks", 0);
        data.invisibilityActiveTicks = tag.getIntOr("invisibilityActiveTicks", 0);
        data.runnerSprintTicks = tag.getIntOr("runnerSprintTicks", 0);
        data.assassinBonusDamage = tag.getDoubleOr("assassinBonusDamage", 0.0);
        data.farmerMobKills = tag.getIntOr("farmerMobKills", 0);
        data.farmerPlayerKills = tag.getIntOr("farmerPlayerKills", 0);
        data.gluttonyDepletionArmed = tag.getBooleanOr("gluttonyDepletionArmed", false);
        data.invisibleAmbushArmed = tag.getBooleanOr("invisibleAmbushArmed", false);
        data.bruteShakeArmed = tag.getBooleanOr("bruteShakeArmed", false);
        data.climbingActive = tag.getBooleanOr("climbingActive", false);
        data.climbingInProgress = tag.getBooleanOr("climbingInProgress", false);
        data.stuntmanClimbedBlocks = tag.getIntOr("stuntmanClimbedBlocks", 0);
        data.stuntmanClimbAccumulator = tag.getDoubleOr("stuntmanClimbAccumulator", 0.0);
        data.heroTrades = tag.getIntOr("heroTrades", 0);
        data.heroTradeBaseline = tag.getIntOr("heroTradeBaseline", 0);
        data.randomChangeTokens = tag.getIntOr("randomChangeTokens", 0);
        data.pick3ChangeTokens = tag.getIntOr("pick3ChangeTokens", 0);
        data.warriorSwordKills = tag.getIntOr("warriorSwordKills", 0);
        data.teleporterCount = tag.getIntOr("teleporterCount", 0);
        data.porcupineHitsTaken = tag.getIntOr("porcupineHitsTaken", 0);
        data.archerBowKills = tag.getIntOr("archerBowKills", 0);
        data.dodgerDodgeCount = tag.getIntOr("dodgerDodgeCount", 0);
        data.endCrystalUses = tag.getIntOr("endCrystalUses", 0);
        data.crystalCooldownEndTick = tag.getLongOr("crystalCooldownEndTick", 0);
        data.reincarnatorTotemSaves = tag.getIntOr("reincarnatorTotemSaves", 0);
        return data;
    }
}