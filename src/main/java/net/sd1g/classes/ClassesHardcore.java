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
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
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

        ServerLifecycleEvents.SERVER_STARTED.register(_ -> {
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

            if (held.is(Items.END_CRYSTAL)) {
                PlayerClassData data = PlayerClassData.get(sp);
                long gameTime = level.getGameTime();

                if (gameTime < data.crystalCooldownEndTick) {
                    long remainingTicks = data.crystalCooldownEndTick - gameTime;
                    long seconds = (remainingTicks / 20) + 1;
                    sp.sendOverlayMessage(Component.literal("§cКристаллы на перезарядке! §f" + seconds + " сек."));
                    return InteractionResult.FAIL;
                }

                data.endCrystalUses++;
                PlayerClassData.markDirty(sp);

                if (data.endCrystalUses >= 3) {
                    data.endCrystalUses = 0;
                    data.crystalCooldownEndTick = gameTime + 1800;
                    sp.sendOverlayMessage(Component.literal("§eЛимит кристаллов исчерпан. КД 90 сек."));
                    ServerPlayNetworking.send(sp, new CooldownSyncPayload("end_crystal", 1800));
                }
            }

            boolean isIgniter = held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE);
            if (isIgniter) {
                boolean tntNearby = false;
                for (BlockPos p : BlockPos.betweenClosed(clickedPos.offset(-1, -1, -1), clickedPos.offset(1, 1, 1))) {
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

            // Отправляем конфиг
            ServerPlayNetworking.send(player, new SyncConfigPayload(ModConfig.getJson()));

            if ("ice_strike".equals(data.playerClass)) {
                data.setPlayerClass("ice_warrior");
                PlayerClassData.markDirty(player);
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
            ServerPlayNetworking.send(player, new CooldownSyncPayload("teleporter_count", data.teleporterCount));
            ServerPlayNetworking.send(player, new CooldownSyncPayload("stuntman_blocks", data.stuntmanClimbedBlocks));
            ServerPlayNetworking.send(player, new CooldownSyncPayload("porcupine_hits", data.porcupineHitsTaken));
            ServerPlayNetworking.send(player, new CooldownSyncPayload("archer_kills", data.archerBowKills));
            ServerPlayNetworking.send(player, new CooldownSyncPayload("warrior_kills", data.warriorSwordKills));
            ServerPlayNetworking.send(player, new CooldownSyncPayload("dodger_dodges", data.dodgerDodgeCount));

            long gameTime = player.level().getGameTime();
            if (data.crystalCooldownEndTick > gameTime) {
                long remaining = data.crystalCooldownEndTick - gameTime;
                ServerPlayNetworking.send(player, new CooldownSyncPayload("end_crystal", (int) remaining));
            }

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
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ClassStateSaver.get(server).setDirty());

        PlayerBlockBreakEvents.AFTER.register((_, player, _, state, _) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!LuckyDiggerFortuneHandler.isOre(state.getBlock())) return;

            PlayerClassData data = PlayerClassData.get(serverPlayer);
            if (!"lucky_digger".equals(data.playerClass)) return;

            data.luckyDiggerTotalOre++;
            int perLevel = ModConfig.getInstance().getIntEffect("digger_ore_per_level", 200);
            int maxLevel = ModConfig.getInstance().getIntEffect("digger_max_level", 4);
            int newLevel = perLevel > 0 ? Math.min(data.luckyDiggerTotalOre / perLevel, maxLevel) : 0;

            if (newLevel > data.luckyDiggerLevel) {
                serverPlayer.sendOverlayMessage(Component.literal("§aМастерство шахтёра. §fУровень " + newLevel + "."));
            }
            data.luckyDiggerLevel = newLevel;
            PlayerClassData.markDirty(serverPlayer);

            ServerPlayNetworking.send(serverPlayer, new LuckyDiggerSyncPayload(
                    data.luckyDiggerMode, data.luckyDiggerTotalOre, data.luckyDiggerLevel));
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, _, damageTaken, _) -> {
            if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
            if (damageTaken <= 0) return;

            PlayerClassData data = PlayerClassData.get(attacker);
            ClassDefinition def = data.getClassDef();
            if (def == null) return;

            for (Ability ability : def.abilities) {
                ability.onDealDamage(attacker, entity, damageTaken);
            }
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, _, damageTaken, _) -> {
            if (!(entity instanceof ServerPlayer sp)) return;
            if (damageTaken <= 0) return;

            PlayerClassData data = PlayerClassData.get(sp);

            if ("porcupine".equals(data.playerClass)) {
                data.porcupineHitsTaken++;
                PlayerClassData.markDirty(sp);
                ServerPlayNetworking.send(sp, new CooldownSyncPayload("porcupine_hits", data.porcupineHitsTaken));
            }

            ClassDefinition def = data.getClassDef();
            if (def == null) return;

            for (Ability ability : def.abilities) {
                ability.onTakeDamage(sp, source, damageTaken);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
            PlayerClassData data = PlayerClassData.get(attacker);

            if ("warrior".equals(data.playerClass) && attacker.getMainHandItem().is(ItemTags.SWORDS)) {
                data.warriorSwordKills++;
                PlayerClassData.markDirty(attacker);
                ServerPlayNetworking.send(attacker, new CooldownSyncPayload("warrior_kills", data.warriorSwordKills));
            }

            if ("archer".equals(data.playerClass) && source.getDirectEntity() instanceof AbstractArrow) {
                data.archerBowKills++;
                PlayerClassData.markDirty(attacker);
                ServerPlayNetworking.send(attacker, new CooldownSyncPayload("archer_kills", data.archerBowKills));
            }

            if (!"mad_farmer".equals(data.playerClass)) return;
            ItemStack weapon = attacker.getMainHandItem();
            if (!weapon.is(ItemTags.HOES)) return;

            if (entity instanceof ServerPlayer) data.farmerPlayerKills++;
            else data.farmerMobKills++;

            PlayerClassData.markDirty(attacker);
            ServerPlayNetworking.send(attacker, new FarmerSyncPayload(data.farmerMobKills, data.farmerPlayerKills));
        });

        // ИСПРАВЛЕНИЕ: Удален весь лишний код Дикобраза и Ловкача.
        // Урон Дикобраза режется в PorcupineChitinMixin, а уклонение Ловкача в DodgerDodgeMixin.
        // Мы оставляем ALLOW_DAMAGE только на случай, если в будущем понадобится вернуть какую-то логику.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            return true;
        });

        LOGGER.info("ClassesHardcore initialized successfully!");
    }

    private static boolean isRedstoneComponentItem(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        return isRedstoneComponentBlock(bi.getBlock());
    }

    private static boolean isRedstoneComponentBlock(Block block) {
        if (block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH
                || block == Blocks.LEVER || block == Blocks.REDSTONE_BLOCK) return true;
        var state = block.defaultBlockState();
        return state.is(BlockTags.BUTTONS) || state.is(BlockTags.PRESSURE_PLATES);
    }

    private void registerClasses() {
        ClassRegistry.register(new ClassDefinition("warrior", "Воин",
                () -> "Универсальный боец с исцелением и быстрыми атаками.",
                List.of(new HealAbility(), new SpeedBoostAbility(), new CombatExperienceAbility(), new CombatTempoAbility(), new CombatVeteranAbility())));

        ClassRegistry.register(new ClassDefinition("strongman", "Силач", "Усиливает удары по мере накопления опыта.", List.of(new PowerGainAbility())));
        ClassRegistry.register(new ClassDefinition("lucky_digger", "Удачливый копатель", "Ускоренная добыча и контроль противников.", List.of(new LuckyDiggerToggleAbility(), new LuckyDiggerMiningAbility(), new LuckyDiggerSuppressionAbility(), new LuckyDiggerHeavyStrikeAbility())));
        ClassRegistry.register(new ClassDefinition("teleporter", "Телепортатор", "Мобильность и шанс спасения после тотема.", List.of(new TeleporterAbility(), new TeleporterLastChanceAbility(), new TeleporterSpatialExpAbility())));
        ClassRegistry.register(new ClassDefinition("gluttony", "Обжора", "Истощает противника и получает преимущество над голодными целями.", List.of(new GluttonyDepletionAbility(), new GluttonySatiateAbility(), new GluttonyAbility(), new GluttonyCravingsAbility(), new GluttonyHungryHunterAbility())));
        ClassRegistry.register(new ClassDefinition("spry", "Живчик", "Большой запас здоровья и кратковременное поглощение.", List.of(new SecondWindAbility(), new VitalityAbility(), new LifeExperienceAbility())));
        ClassRegistry.register(new ClassDefinition("ice_warrior", "Ледяной воин", "Замедляет противников и ослабляет их атаки.", List.of(new IceStrikeAbility(), new IceWeakeningAbility(), new IceDeepFreezeAbility())));
        ClassRegistry.register(new ClassDefinition("vampire", "Вампир", "Восстанавливает здоровье за счёт нанесённого урона.", List.of(new VampireDrainAbility(), new VampirePassiveAbility(), new VampireBloodKissAbility())));
        ClassRegistry.register(new ClassDefinition("invisible", "Невидимка", "Засады и скрытное перемещение.", List.of(new InvisibilityAbility(), new InvisibleElusivenessAbility())));
        ClassRegistry.register(new ClassDefinition("lucky_guy", "Везунчик", "Получает случайные предметы благодаря удаче.", List.of(new LuckySwapAbility())));
        ClassRegistry.register(new ClassDefinition("runner", "Бегун", "Скорость, рывки и атаки на ходу.", List.of(new RunnerSprintAbility(), new RunnerPassiveAbility(), new RunnerSpeedCritAbility())));
        ClassRegistry.register(new ClassDefinition("stuntman", "Каскадёр", "Преодолевает высоту и стены.", List.of(new ParkourAbility(), new ClimbingAbility(), new StuntmanAbility(), new StuntmanParkourMasterAbility(), new StuntmanAirArmorAbility())));
        ClassRegistry.register(new ClassDefinition("hero", "Герой", "Экономическое развитие через торговлю и рейды.", List.of(new HeroGiftAbility(), new HeroPassiveAbility(), new HeroVillageDefenderAbility())));
        ClassRegistry.register(new ClassDefinition("porcupine", "Дикобраз", "Наказывает врагов за приближение и удары.", List.of(new PorcupineQuillsAbility(), new PorcupineReflectAbility(), new PorcupineThornsAbility(), new PorcupineChitinAbility(), new PorcupineHardeningAbility())));
        ClassRegistry.register(new ClassDefinition("archer", "Лучник", "Дистанционные атаки и отступление.", List.of(new ArcherJumpBackAbility(), new ArcherAbility(), new ArcherHunterMasteryAbility())));
        ClassRegistry.register(new ClassDefinition("assassin", "Опытный убийца", "Развивается за счёт убийств.", List.of(new AssassinAbility())));
        ClassRegistry.register(new ClassDefinition("titan", "Титан", "Очень прочный боец.", List.of(new MonolithAbility(), new TitanAbility())));
        ClassRegistry.register(new ClassDefinition("dodger", "Ловкач", "Уклонение и ускорение.", List.of(new DodgeSpeedBoostAbility(), new DodgerAbility(), new DodgerSurvivalInstinctAbility())));
        ClassRegistry.register(new ClassDefinition("brute", "Громила", "Булава и подбрасывание.", List.of(new EarthshakerAbility(), new BruteAbility())));
        ClassRegistry.register(new ClassDefinition("mad_farmer", "Безумный фермер", "Мотыга и урожай.", List.of(new HealingBrewAbility(), new RichHarvestAbility(), new FarmerAbility())));
        ClassRegistry.register(new ClassDefinition("reincarnator", "Реинкарнатор", "Многократные тотемы.", List.of(new ReincarnatorAbility())));

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

        PayloadTypeRegistry.serverboundPlay().register(RequestClassChangePayload.TYPE, RequestClassChangePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClassTokensSyncPayload.TYPE, ClassTokensSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Pick3OptionsPayload.TYPE, Pick3OptionsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncConfigPayload.TYPE, SyncConfigPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UseAbilityPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> AbilityManager.tryActivate(context.player(), payload.abilityId()));
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestClassChangePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                PlayerClassData data = PlayerClassData.get(player);

                if (payload.actionType() == 0) {
                    if (data.randomChangeTokens > 0) {
                        data.randomChangeTokens--;
                        String[] classes = ClassRegistry.getAll().keySet().toArray(new String[0]);
                        if (classes.length > 0) {
                            ClassCommand.applyClass(player, classes[new Random().nextInt(classes.length)]);
                            ServerPlayNetworking.send(player, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                        }
                    } else player.sendSystemMessage(Component.literal("§cУ вас нет попыток."));
                } else if (payload.actionType() == 1) {
                    if (data.pick3ChangeTokens > 0) {
                        data.pick3ChangeTokens--;
                        List<String> allClasses = new ArrayList<>(ClassRegistry.getAll().keySet());
                        allClasses.remove(data.playerClass);
                        Collections.shuffle(allClasses);
                        ServerPlayNetworking.send(player, new Pick3OptionsPayload(allClasses.subList(0, Math.min(3, allClasses.size()))));
                        ServerPlayNetworking.send(player, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                    } else player.sendSystemMessage(Component.literal("§cУ вас нет попыток."));
                } else if (payload.actionType() == 2) {
                    if (Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack())) {
                        if (ClassRegistry.get(payload.classId()) != null) ClassCommand.applyClass(player, payload.classId());
                    } else player.sendSystemMessage(Component.literal("§cУ вас нет прав."));
                } else if (payload.actionType() == 3) {
                    if (ClassRegistry.get(payload.classId()) != null) ClassCommand.applyClass(player, payload.classId());
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