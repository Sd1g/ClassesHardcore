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

    private transient ClassDefinition cachedClassDef = null;
    private transient String cachedClassDefId = null;

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

    // --- Герой ---
    public int heroTrades = 0;
    public int heroTradeBaseline = 0;

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
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("class", playerClass);
        CompoundTag cdTag = new CompoundTag();
        abilityCooldowns.forEach(cdTag::putLong);
        tag.put("cooldowns", cdTag);
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
        tag.putInt("heroTrades", heroTrades);
        tag.putInt("heroTradeBaseline", heroTradeBaseline);
        return tag;
    }

    public static PlayerClassData fromNbt(CompoundTag tag) {
        PlayerClassData data = new PlayerClassData();
        data.playerClass = tag.getStringOr("class", "none");
        CompoundTag cdTag = tag.getCompoundOrEmpty("cooldowns");
        for (String key : cdTag.keySet()) {
            data.abilityCooldowns.put(key, cdTag.getLongOr(key, 0L));
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
        data.heroTrades = tag.getIntOr("heroTrades", 0);
        data.heroTradeBaseline = tag.getIntOr("heroTradeBaseline", 0);
        return data;
    }
}