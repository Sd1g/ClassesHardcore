package net.sd1g.classes;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.AbilityManager;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.ability.impl.*;
import net.sd1g.classes.command.ClassCommand;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.ClassStateSaver;
import net.sd1g.classes.data.PlayerClassData;
import net.sd1g.classes.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ClassesHardcore implements ModInitializer {
    public static final String MOD_ID = "classeshardcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ClassesHardcore...");

        ModConfig.load();
        registerClasses();
        registerNetworking();
        AbilityManager.init();
        ClassCommand.register();
        LuckyDiggerFortuneHandler.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModConfig.load();
            LOGGER.info("Config reloaded on server start");
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.getTickCount();
            if (now % 200 == 0) TntOwnerTracker.cleanupOld(now);
        });

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            ItemStack held = sp.getItemInHand(hand);
            BlockPos clickedPos = hitResult.getBlockPos();
            long tick = level.getServer() != null ? level.getServer().getTickCount() : 0;

            boolean isIgniter = held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE);
            if (isIgniter) {
                boolean tntNearby = false;
                for (BlockPos p : BlockPos.betweenClosed(
                        clickedPos.offset(-1, -1, -1), clickedPos.offset(1, 1, 1))) {
                    if (level.getBlockState(p).is(Blocks.TNT)) {
                        tntNearby = true;
                        break;
                    }
                }
                if (tntNearby) {
                    TntOwnerTracker.recordIgnition(clickedPos, sp.getUUID(), tick);
                }
            }

            if (held.is(Items.TNT)) {
                BlockPos placeAt = clickedPos.relative(hitResult.getDirection());
                TntOwnerTracker.recordPlacement(placeAt, sp.getUUID(), tick);
                return InteractionResult.PASS;
            }

            if (isRedstoneComponentItem(held)) {
                BlockPos placeAt = clickedPos.relative(hitResult.getDirection());
                TntOwnerTracker.recordPlacement(placeAt, sp.getUUID(), tick);
                return InteractionResult.PASS;
            }

            Block clickedBlock = level.getBlockState(clickedPos).getBlock();
            if (isRedstoneComponentBlock(clickedBlock)) {
                TntOwnerTracker.recordPlacement(clickedPos, sp.getUUID(), tick);
            }

            return InteractionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            PlayerClassData data = PlayerClassData.get(player);

            if ("ice_strike".equals(data.playerClass)) {
                data.setPlayerClass("ice_warrior");
                PlayerClassData.markDirty(player);
                LOGGER.info("Migrated class ice_strike → ice_warrior for {}", player.getName().getString());
            }
            if ("spearman".equals(data.playerClass)) {
                data.playerClass = "none";
                PlayerClassData.markDirty(player);
            }

            if (data.playerClass == null || data.playerClass.equals("none") || ClassRegistry.get(data.playerClass) == null) {
                var classes = ClassRegistry.getAll().keySet().toArray(new String[0]);
                if (classes.length > 0) {
                    String randomClass = classes[new Random().nextInt(classes.length)];
                    data.setPlayerClass(randomClass);
                    PlayerClassData.markDirty(player);
                    LOGGER.info("Assigned random class '{}' to player {}", randomClass, player.getName().getString());
                }
            } else {
                data.setPlayerClass(data.playerClass);
            }

            if (data.vampireActiveTicks > 0) data.vampireActiveTicks = 0;
            if (data.invisibilityActiveTicks > 0) data.invisibilityActiveTicks = 0;
            if (data.runnerSprintTicks > 0) data.runnerSprintTicks = 0;
            data.gluttonyDepletionArmed = false;
            data.invisibleAmbushArmed = false;
            data.bruteShakeArmed = false;
            data.climbingActive = false;
            data.climbingInProgress = false;
            PlayerClassData.markDirty(player);

            ServerPlayNetworking.send(player, new ClassSyncPayload(data.playerClass));
            ServerPlayNetworking.send(player, new AssassinSyncPayload(data.assassinBonusDamage));
            ServerPlayNetworking.send(player, new HeroSyncPayload(data.heroTrades));
            ServerPlayNetworking.send(player, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));

            if ("lucky_digger".equals(data.playerClass)) {
                ServerPlayNetworking.send(player, new LuckyDiggerSyncPayload(
                        data.luckyDiggerMode, data.luckyDiggerTotalOre, data.luckyDiggerLevel));
            }
            if ("vampire".equals(data.playerClass)) {
                ServerPlayNetworking.send(player, new VampireSyncPayload(0));
            }
            if ("invisible".equals(data.playerClass)) {
                ServerPlayNetworking.send(player, new InvisibilitySyncPayload(0));
            }
            if ("runner".equals(data.playerClass)) {
                ServerPlayNetworking.send(player, new RunnerSyncPayload(0));
            }
            if ("mad_farmer".equals(data.playerClass)) {
                ServerPlayNetworking.send(player, new FarmerSyncPayload(
                        data.farmerMobKills, data.farmerPlayerKills));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ClassStateSaver.get(server).setDirty();
            PorcupineReflectAbility.clearTracking(handler.getPlayer().getUUID());
            PorcupineThornsAbility.clearTracking(handler.getPlayer().getUUID());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ClassStateSaver.get(server).setDirty();
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!LuckyDiggerFortuneHandler.isOre(state.getBlock())) return;

            PlayerClassData data = PlayerClassData.get(serverPlayer);
            if (!"lucky_digger".equals(data.playerClass)) return;

            data.luckyDiggerTotalOre++;

            int perLevel = ModConfig.getInstance().getIntEffect("mining_mastery_blocks_per_level", 200);
            int maxLevel = ModConfig.getInstance().getIntEffect("mining_mastery_max_level", 4);
            int newLevel = perLevel > 0
                    ? Math.min(data.luckyDiggerTotalOre / perLevel, maxLevel)
                    : 0;

            boolean leveledUp = newLevel > data.luckyDiggerLevel;
            data.luckyDiggerLevel = newLevel;
            PlayerClassData.markDirty(serverPlayer);

            if (leveledUp) {
                serverPlayer.sendOverlayMessage(Component.literal(
                        "§aМастерство шахтёра. §fУровень " + newLevel + "."));
            }

            ServerPlayNetworking.send(serverPlayer, new LuckyDiggerSyncPayload(
                    data.luckyDiggerMode, data.luckyDiggerTotalOre, data.luckyDiggerLevel));
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
            if (!(entity instanceof LivingEntity target)) return;
            if (damageTaken <= 0) return;

            PlayerClassData data = PlayerClassData.get(attacker);
            ClassDefinition def = data.getClassDef();
            if (def == null) return;

            for (Ability ability : def.abilities) {
                ability.onDealDamage(attacker, target, damageTaken);
            }
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!(entity instanceof ServerPlayer sp)) return;
            if (damageTaken <= 0) return;

            PlayerClassData data = PlayerClassData.get(sp);
            ClassDefinition def = data.getClassDef();
            if (def == null) return;

            for (Ability ability : def.abilities) {
                ability.onTakeDamage(sp, source, damageTaken);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getEntity() instanceof ServerPlayer attacker)) return;

            PlayerClassData data = PlayerClassData.get(attacker);
            if (!"mad_farmer".equals(data.playerClass)) return;

            ItemStack weapon = attacker.getMainHandItem();
            if (!weapon.is(ItemTags.HOES)) return;

            if (entity instanceof ServerPlayer) {
                data.farmerPlayerKills++;
            } else {
                data.farmerMobKills++;
            }
            PlayerClassData.markDirty(attacker);

            ServerPlayNetworking.send(attacker, new FarmerSyncPayload(
                    data.farmerMobKills, data.farmerPlayerKills));
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer sp)) return true;
            if (amount <= 0) return true;
            if (source.getEntity() == null) return true;
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;

            PlayerClassData data = PlayerClassData.get(sp);
            if (!"dodger".equals(data.playerClass)) return true;

            double chance = ModConfig.getInstance().getEffect("dodger_dodge_chance", 0.20);
            if (sp.getRandom().nextDouble() >= chance) return true;

            DodgerAbility.playDodgeEffect(sp);

            if (amount >= 2.0f) {
                sp.sendOverlayMessage(Component.literal("§bУклонение!§f Урон от атаки избежан."));
            }
            return false;
        });

        LOGGER.info("ClassesHardcore initialized successfully!");
    }

    private static boolean isRedstoneComponentItem(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        return isRedstoneComponentBlock(bi.getBlock());
    }

    private static boolean isRedstoneComponentBlock(Block block) {
        if (block == Blocks.REDSTONE_TORCH
                || block == Blocks.REDSTONE_WALL_TORCH
                || block == Blocks.LEVER
                || block == Blocks.REDSTONE_BLOCK) {
            return true;
        }
        var state = block.defaultBlockState();
        return state.is(BlockTags.BUTTONS) || state.is(BlockTags.PRESSURE_PLATES);
    }

    private void registerClasses() {
        ClassRegistry.register(new ClassDefinition("warrior", "Воин",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int amount = (int) cfg.getEffect("heal_amount", 2.0);
                    int cdSec = cfg.getCooldown("heal", 440) / 20;
                    return "Мастер ближнего боя. Исцеление (" + amount + " HP, кулдаун " + cdSec
                            + " сек). Постоянно ускорен.";
                },
                List.of(new HealAbility(), new SpeedBoostAbility())));

        ClassRegistry.register(new ClassDefinition("strongman", "Силач",
                "Чем выше уровень опыта — тем сильнее удары в ближнем бою.",
                List.of(new PowerGainAbility())));

        ClassRegistry.register(new ClassDefinition("lucky_digger", "Удачливый копатель",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int perLevel = cfg.getIntEffect("mining_mastery_blocks_per_level", 200);
                    int maxLevel = cfg.getIntEffect("mining_mastery_max_level", 4);
                    int cdSec = cfg.getCooldown("toggle_bonus", 600) / 20;
                    return "Прокачивает кирки (" + perLevel + " руды = +1 уровень, макс. " + maxLevel
                            + "). Переключает бонус: Эффективность / Удача (кулдаун " + cdSec + " сек).";
                },
                List.of(new LuckyDiggerToggleAbility(), new LuckyDiggerMiningAbility())));

        ClassRegistry.register(new ClassDefinition("teleporter", "Телепортатор",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int min = (int) cfg.getEffect("teleport_min_distance", 3.0);
                    int max = (int) cfg.getEffect("teleport_max_distance", 30.0);
                    int chance = (int) Math.round(cfg.getEffect("teleport_success_chance", 0.30) * 100);
                    return "Мгновенно переносится на " + min + "–" + max + " блоков. Шанс " + chance + "%. КД зависит от дистанции.";
                },
                List.of(new TeleporterAbility(), new TeleporterLastChanceAbility())));

        ClassRegistry.register(new ClassDefinition("gluttony", "Обжора",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int depCd = cfg.getCooldown("depletion", 1000) / 20;
                    int satCd = cfg.getCooldown("satiate", 600) / 20;
                    return "Истощение (Голод V, кулдаун " + depCd + " сек), "
                            + "Утоление (кулдаун " + satCd + " сек). Чревоугодие: шанс Голода III.";
                },
                List.of(
                        new GluttonyDepletionAbility(),
                        new GluttonySatiateAbility(),
                        new GluttonyAbility(),
                        new GluttonyCravingsAbility()
                )));

        ClassRegistry.register(new ClassDefinition("spry", "Живчик",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int bonus = cfg.getIntEffect("vitality_bonus_hp", 10);
                    return "Повышенное здоровье (" + (20 + bonus) + " HP). Растёт с уровнем опыта. "
                            + "Второе дыхание даёт щит.";
                },
                List.of(new SecondWindAbility(), new VitalityAbility(), new LifeExperienceAbility())));

        ClassRegistry.register(new ClassDefinition("ice_warrior", "Ледяной воин",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int bonus = cfg.getIntEffect("ice_strike_bonus_damage", 1);
                    int perStep = cfg.getIntEffect("ice_strike_levels_per_amplifier_step", 50);
                    int chance = (int) Math.round(cfg.getEffect("ice_deep_freeze_chance", 0.025) * 100);
                    return "Каждый удар +" + bonus + " урона и Замедление. Каждые " + perStep
                            + " уровней +1 к Замедлению. Замедлённые враги бьют слабее. Шанс "
                            + chance + "% — полная заморозка.";
                },
                List.of(new IceStrikeAbility(), new IceWeakeningAbility(), new IceDeepFreezeAbility())));

        ClassRegistry.register(new ClassDefinition("vampire", "Вампир",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int dur = cfg.getIntEffect("vampire_active_duration_ticks", 200) / 20;
                    int percent = (int) Math.round(cfg.getEffect("vampire_heal_percent", 0.60) * 100);
                    int cdSec = cfg.getCooldown("vampire_drain", 1200) / 20;
                    return "Восстанавливает " + percent + "% урона в HP. Активная («Вампиризм», "
                            + dur + " сек, кулдаун " + cdSec + " сек) усиливает до 100%.";
                },
                List.of(new VampireDrainAbility(), new VampirePassiveAbility())));

        ClassRegistry.register(new ClassDefinition("invisible", "Невидимка",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int dur = cfg.getIntEffect("invisibility_duration_ticks", 100) / 20;
                    int mult = (int) Math.round(cfg.getEffect("ambush_damage_multiplier", 2.2) * 100) - 100;
                    int cdSec = cfg.getCooldown("invisibility", 1200) / 20;
                    return "Невидимость " + dur + " сек. Первый удар +" + mult
                            + "% урона. Кулдаун " + cdSec + " сек.";
                },
                List.of(new InvisibilityAbility())));

        ClassRegistry.register(new ClassDefinition("lucky_guy", "Везунчик",
                () -> {
                    int cd = ModConfig.getInstance().getCooldown("lucky_swap", 12000);
                    int min = cd / 20 / 60;
                    return "Меняет предмет в руке на случайный. Кулдаун " + min + " мин.";
                },
                List.of(new LuckySwapAbility())));

        ClassRegistry.register(new ClassDefinition("runner", "Бегун",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int passiveAmp = cfg.getIntEffect("runner_passive_amplifier", 1) + 1;
                    int sprintAmp = cfg.getIntEffect("runner_sprint_amplifier", 4) + 1;
                    int dur = cfg.getIntEffect("runner_sprint_duration_ticks", 300) / 20;
                    return "Постоянно даёт Скорость " + roman(passiveAmp) + ". Рывок — Скорость "
                            + roman(sprintAmp) + " на " + dur + " сек.";
                },
                List.of(new RunnerSprintAbility(), new RunnerPassiveAbility())));

        ClassRegistry.register(new ClassDefinition("stuntman", "Каскадер",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int parkourAmp = cfg.getIntEffect("parkour_jump_amplifier", 4) + 1;
                    int parkourDur = cfg.getIntEffect("parkour_duration_ticks", 220) / 20;
                    return "Паркур — Прыгучесть " + roman(parkourAmp) + " на " + parkourDur
                            + " сек. Сцепление — лазание по стенам. Урон от падения снижен.";
                },
                List.of(new ParkourAbility(), new ClimbingAbility(), new StuntmanAbility())));

        ClassRegistry.register(new ClassDefinition("hero", "Герой",
                "Дар героя — даёт изумруды, растёт с числом сделок. Слава героя — скидка у жителей.",
                List.of(new HeroGiftAbility(), new HeroPassiveAbility())));

        ClassRegistry.register(new ClassDefinition("porcupine", "Дикобраз",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int aDmg = (int) cfg.getEffect("porcupine_active_damage", 5.0);
                    int aRad = (int) cfg.getEffect("porcupine_active_radius", 5.0);
                    return "Шипы: " + aDmg + " урона в радиусе " + aRad
                            + " блоков. Иглы, Игольчатый барьер, Хитиновый покров.";
                },
                List.of(
                        new PorcupineQuillsAbility(),
                        new PorcupineReflectAbility(),
                        new PorcupineThornsAbility()
                )));

        ClassRegistry.register(new ClassDefinition("archer", "Лучник",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    double mult = cfg.getEffect("archer_draw_speed_multiplier", 1.5);
                    int bonus = (int) cfg.getEffect("archer_bow_damage_bonus", 3.0);
                    return "Лук натягивается в " + fmt(mult) + " раза быстрее. Урон стрел +" + bonus
                            + ". Разрыв дистанции — прыжок назад.";
                },
                List.of(new ArcherJumpBackAbility(), new ArcherAbility())));

        ClassRegistry.register(new ClassDefinition("assassin", "Опытный убийца",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    double chance = cfg.getEffect("assassin_instakill_chance", 0.005) * 100;
                    return "С шансом " + String.format("%.1f", chance)
                            + "% мгновенно убивает мобов, накапливая бонус к урону.";
                },
                List.of(new AssassinAbility())));

        ClassRegistry.register(new ClassDefinition("titan", "Титан",
                () -> {
                    int amp = ModConfig.getInstance().getIntEffect("titan_resistance_amplifier", 0);
                    return "Постоянно имеет Сопротивление " + roman(amp + 1)
                            + ". Монолит — временное Сопротивление IV.";
                },
                List.of(new MonolithAbility(), new TitanAbility())));

        ClassRegistry.register(new ClassDefinition("dodger", "Ловкач",
                () -> {
                    int chance = (int) Math.round(
                            ModConfig.getInstance().getEffect("dodger_dodge_chance", 0.20) * 100);
                    return "С шансом " + chance + "% уворачивается от атак. Ускорение — Скорость II.";
                },
                List.of(new DodgeSpeedBoostAbility(), new DodgerAbility())));

        ClassRegistry.register(new ClassDefinition("brute", "Громила",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int maceBonus = (int) Math.round((cfg.getEffect("brute_mace_damage_multiplier", 1.5) - 1.0) * 100);
                    int otherPenalty = (int) Math.round((1.0 - cfg.getEffect("brute_other_damage_multiplier", 0.3)) * 100);
                    return "Урон булавы +" + maceBonus + "%, другое оружие −" + otherPenalty
                            + "%. Дрожь земли — подброс врага.";
                },
                List.of(new EarthshakerAbility(), new BruteAbility())));

        ClassRegistry.register(new ClassDefinition("mad_farmer", "Безумный фермер",
                () -> {
                    ModConfig cfg = ModConfig.getInstance();
                    int hoeBonus = (int) Math.round((cfg.getEffect("farmer_hoe_damage_multiplier", 2.0) - 1.0) * 100);
                    int otherPenalty = (int) Math.round((1.0 - cfg.getEffect("farmer_other_damage_multiplier", 0.2)) * 100);
                    int mobsPerStep = cfg.getIntEffect("farmer_mobs_per_bonus_point", 100);
                    return "Урон мотыгой +" + hoeBonus + "%. За каждые " + mobsPerStep
                            + " убийств мобов — +" + fmt(cfg.getEffect("farmer_bonus_per_mob_step", 0.25))
                            + " урона. Другое оружие −" + otherPenalty + "%. Лечебный отвар, Богатый урожай.";
                },
                List.of(new HealingBrewAbility(), new RichHarvestAbility(), new FarmerAbility())));

        ClassRegistry.register(new ClassDefinition("reincarnator", "Реинкарнатор",
                () -> {
                    int chance = (int) Math.round(
                            ModConfig.getInstance().getEffect("reincarnator_totem_save_chance", 0.70) * 100);
                    return "Тотем бессмертия с шансом " + chance + "% не тратится. "
                            + "Запрещена незеритовая броня.";
                },
                List.of(new ReincarnatorAbility())));

        LOGGER.info("Registered {} classes", ClassRegistry.getAll().size());
    }

    private void registerNetworking() {
        PayloadTypeRegistry.serverboundPlay().register(UseAbilityPayload.TYPE, UseAbilityPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClassSyncPayload.TYPE, ClassSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CooldownSyncPayload.TYPE, CooldownSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LuckyDiggerSyncPayload.TYPE, LuckyDiggerSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VampireSyncPayload.TYPE, VampireSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InvisibilitySyncPayload.TYPE, InvisibilitySyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RunnerSyncPayload.TYPE, RunnerSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AssassinSyncPayload.TYPE, AssassinSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FarmerSyncPayload.TYPE, FarmerSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HeroSyncPayload.TYPE, HeroSyncPayload.CODEC);

        // Новые пакеты для меню
        PayloadTypeRegistry.serverboundPlay().register(RequestClassChangePayload.TYPE, RequestClassChangePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClassTokensSyncPayload.TYPE, ClassTokensSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Pick3OptionsPayload.TYPE, Pick3OptionsPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UseAbilityPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                AbilityManager.tryActivate(player, payload.abilityId());
            });
        });

        // Обработчик смены класса
        ServerPlayNetworking.registerGlobalReceiver(RequestClassChangePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                PlayerClassData data = PlayerClassData.get(player);

                if (payload.type() == 0) { // Случайный
                    if (data.randomChangeTokens > 0) {
                        data.randomChangeTokens--;
                        String[] classes = ClassRegistry.getAll().keySet().toArray(new String[0]);
                        if (classes.length > 0) {
                            String randomClass = classes[new Random().nextInt(classes.length)];
                            ClassCommand.applyClass(player, randomClass);
                            ServerPlayNetworking.send(player, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                            player.sendSystemMessage(Component.literal("§aКласс изменён на случайный!"));
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("§cУ вас нет попыток для случайной смены класса."));
                    }
                } else if (payload.type() == 1) { // Выбор из 3
                    if (data.pick3ChangeTokens > 0) {
                        data.pick3ChangeTokens--;
                        List<String> allClasses = new ArrayList<>(ClassRegistry.getAll().keySet());
                        Collections.shuffle(allClasses);
                        List<String> options = allClasses.subList(0, Math.min(3, allClasses.size()));

                        ServerPlayNetworking.send(player, new Pick3OptionsPayload(options));
                        ServerPlayNetworking.send(player, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                    } else {
                        player.sendSystemMessage(Component.literal("§cУ вас нет попыток для выбора из 3 классов."));
                    }
                } else if (payload.type() == 2) { // Админский
                    if (player.hasPermissions(2)) {
                        if (ClassRegistry.get(payload.classId()) != null) {
                            ClassCommand.applyClass(player, payload.classId());
                            player.sendSystemMessage(Component.literal("§aКласс изменён на " + payload.classId() + "!"));
                        }
                    } else {
                        player.sendSystemMessage(Component.literal("§cУ вас нет прав на это действие."));
                    }
                } else if (payload.type() == 3) { // Подтверждение выбора из 3 (без токена)
                    if (ClassRegistry.get(payload.classId()) != null) {
                        ClassCommand.applyClass(player, payload.classId());
                        player.sendSystemMessage(Component.literal("§aВы выбрали класс: " + payload.classId()));
                    }
                }
            });
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }
}