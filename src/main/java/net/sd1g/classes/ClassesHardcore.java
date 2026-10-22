package net.sd1g.classes;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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
import net.sd1g.classes.party.Party;
import net.sd1g.classes.party.PartyManager;
import net.sd1g.classes.party.PartyStateSaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClassesHardcore implements ModInitializer {
    public static final String MOD_ID = "classeshardcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<UUID, List<String>> PENDING_PICK3 = new ConcurrentHashMap<>();
    private static Thread configWatcherThread = null;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ClassesHardcore...");

        ModConfig.load();
        registerClasses();
        registerNetworking();
        AbilityManager.init();
        ClassCommand.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModConfig.load();
            broadcastConfig(server);
            startConfigWatcher(server);
            LOGGER.info("Config reloaded on server start");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            stopConfigWatcher();
            ClassStateSaver.get(server).setDirty();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 == 0) {
                for (Party p : PartyStateSaver.get(server).parties) {
                    PartyManager.syncParty(server, p);
                }
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayer sp) {
                if (PlayerClassData.get(sp).isCasting) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
            PlayerClassData data = PlayerClassData.get(attacker);

            if ("warrior".equals(data.playerClass) && attacker.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS)) {
                data.warriorSwordKills++;
                PlayerClassData.markDirty(attacker);
                ServerPlayNetworking.send(attacker, new CooldownSyncPayload("warrior_kills", data.warriorSwordKills));
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            PlayerClassData data = PlayerClassData.get(player);

            ServerPlayNetworking.send(player, new SyncConfigPayload(ModConfig.getJson()));

            if (data.playerClass == null || data.playerClass.equals("none") || ClassRegistry.get(data.playerClass) == null) {
                var classes = ClassRegistry.getAll().keySet().toArray(new String[0]);
                if (classes.length > 0) {
                    data.setPlayerClass(classes[new Random().nextInt(classes.length)]);
                } else {
                    data.setPlayerClass("none");
                }
                PlayerClassData.markDirty(player);
            }

            data.isCasting = false;
            data.castingAbilityId = "";
            data.castTicksRemaining = 0;

            ServerPlayNetworking.send(player, new ClassSyncPayload(data.playerClass));
            ServerPlayNetworking.send(player, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));

            Party p = PartyManager.getParty(server, player.getUUID());
            if (p != null) PartyManager.syncParty(server, p);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PENDING_PICK3.remove(handler.getPlayer().getUUID());
            PartyManager.handleDisconnect(handler.getPlayer());
            ClassStateSaver.get(server).setDirty();
        });

        LOGGER.info("ClassesHardcore initialized successfully!");
    }

    private void startConfigWatcher(MinecraftServer server) {
        stopConfigWatcher();
        File file = ModConfig.getConfigFile();
        Thread t = new Thread(() -> {
            long lastMod = file.exists() ? file.lastModified() : -1;
            while (server.isRunning()) {
                try {
                    Thread.sleep(500);
                    if (!file.exists()) continue;
                    long now = file.lastModified();
                    if (lastMod != -1 && now != lastMod) {
                        lastMod = now;
                        server.execute(() -> {
                            ModConfig.load();
                            broadcastConfig(server);
                            LOGGER.info("balance.json hot-reloaded");
                        });
                    } else {
                        lastMod = now;
                    }
                } catch (InterruptedException e) {
                    return;
                } catch (Exception ignored) {}
            }
        }, "classeshardcore-config-watcher");
        t.setDaemon(true);
        t.start();
        configWatcherThread = t;
    }

    private static void stopConfigWatcher() {
        if (configWatcherThread != null) {
            configWatcherThread.interrupt();
            configWatcherThread = null;
        }
    }

    private static void broadcastConfig(MinecraftServer server) {
        String json = ModConfig.getJson();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, new SyncConfigPayload(json));
        }
    }

    private void registerClasses() {
        ClassRegistry.register(new ClassDefinition("warrior", "Воин",
                () -> "Универсальный боец с исцелением и быстрыми атаками.",
                List.of(
                        new WarriorHealAbility(),
                        new WarriorCombatTempoAbility(),
                        new WarriorSpeedBoostAbility(),
                        new WarriorCombatExperienceAbility(),
                        new WarriorCombatVeteranAbility()
                )));

        LOGGER.info("Registered {} classes", ClassRegistry.getAll().size());
    }

    private void registerNetworking() {
        PayloadTypeRegistry.serverboundPlay().register(UseAbilityPayload.TYPE, UseAbilityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestClassChangePayload.TYPE, RequestClassChangePayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(ClassSyncPayload.TYPE, ClassSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CooldownSyncPayload.TYPE, CooldownSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClassTokensSyncPayload.TYPE, ClassTokensSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Pick3OptionsPayload.TYPE, Pick3OptionsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncConfigPayload.TYPE, SyncConfigPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ActiveTimerPayload.TYPE, ActiveTimerPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ArmedStatePayload.TYPE, ArmedStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HudMessagePayload.TYPE, HudMessagePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PartySyncPayload.TYPE, PartySyncPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UseAbilityPayload.TYPE, (payload, context) ->
                context.server().execute(() -> AbilityManager.tryActivate(context.player(), payload.abilityId())));

        ServerPlayNetworking.registerGlobalReceiver(RequestClassChangePayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleClassChange(context.player(), payload)));
    }

    private void handleClassChange(ServerPlayer player, RequestClassChangePayload payload) {
        PlayerClassData data = PlayerClassData.get(player);
        switch (payload.actionType()) {
            case 0 -> {
                if (data.randomChangeTokens > 0) {
                    data.randomChangeTokens--;
                    String[] classes = ClassRegistry.getAll().keySet().toArray(new String[0]);
                    if (classes.length > 0) {
                        ClassCommand.applyClass(player, classes[new Random().nextInt(classes.length)]);
                        ServerPlayNetworking.send(player, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                    }
                } else player.sendSystemMessage(Component.literal("§cУ вас нет попыток."));
            }
            case 1 -> {
                if (data.pick3ChangeTokens > 0) {
                    List<String> all = new ArrayList<>(ClassRegistry.getAll().keySet());
                    all.remove(data.playerClass);
                    Collections.shuffle(all);
                    List<String> options = new ArrayList<>(all.subList(0, Math.min(3, all.size())));
                    PENDING_PICK3.put(player.getUUID(), options);
                    ServerPlayNetworking.send(player, new Pick3OptionsPayload(options));
                } else player.sendSystemMessage(Component.literal("§cУ вас нет попыток."));
            }
            case 2 -> {
                if (Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack())) {
                    if (ClassRegistry.get(payload.classId()) != null) ClassCommand.applyClass(player, payload.classId());
                } else player.sendSystemMessage(Component.literal("§cУ вас нет прав."));
            }
            case 3 -> {
                List<String> pending = PENDING_PICK3.get(player.getUUID());
                if (pending == null || !pending.contains(payload.classId())) {
                    player.sendSystemMessage(Component.literal("§cОпции устарели."));
                    PENDING_PICK3.remove(player.getUUID());
                    return;
                }
                if (data.pick3ChangeTokens <= 0) {
                    player.sendSystemMessage(Component.literal("§cУ вас нет попыток."));
                    PENDING_PICK3.remove(player.getUUID());
                    return;
                }
                if (ClassRegistry.get(payload.classId()) != null) {
                    data.pick3ChangeTokens--;
                    PENDING_PICK3.remove(player.getUUID());
                    ClassCommand.applyClass(player, payload.classId());
                    ServerPlayNetworking.send(player, new ClassTokensSyncPayload(data.randomChangeTokens, data.pick3ChangeTokens));
                }
            }
            case 4 -> PENDING_PICK3.remove(player.getUUID());
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}