package net.sd1g.classes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.config.ModConfig;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.network.*;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ClassesHardcoreClient implements ClientModInitializer {
    public static KeyMapping activateAbilityKey;
    public static KeyMapping activateSecondaryKey;
    public static KeyMapping openMenuKey;
    public static KeyMapping openConfigKey;

    public static final Map<String, Integer> cooldownTicks = new HashMap<>();
    public static final Map<String, Integer> cooldownTotal = new HashMap<>();
    public static String currentClass = "none";

    public static int randomChangeTokens = 0;
    public static int pick3ChangeTokens = 0;

    private static String cachedFirstActiveAbility = null;
    private static String cachedSecondActiveAbility = null;

    public static final Map<Identifier, int[]> TEXTURE_SIZE_CACHE = new HashMap<>();

    public static void setIconSize(int size) {
        ClientConfig.setIconSize(size);
    }

    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        setIconSize(ClientConfig.getIconSize());

        KeyMapping.Category category = KeyMapping.Category.register(ClassesHardcore.id("classes"));
        activateAbilityKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.classesharcore.activate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category)
        );
        activateSecondaryKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.classesharcore.activate_secondary", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, category)
        );
        openMenuKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.classesharcore.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category)
        );
        openConfigKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.classesharcore.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, category)
        );

        ClientPlayNetworking.registerGlobalReceiver(SyncConfigPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> ModConfig.loadFromJson(payload.json())));

        ClientPlayNetworking.registerGlobalReceiver(ActiveTimerPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> AbilityClientState.activeTimers.put(payload.abilityId(), payload.durationTicks())));

        ClientPlayNetworking.registerGlobalReceiver(ArmedStatePayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> {
                    if (payload.armed()) AbilityClientState.armedAbilities.add(payload.abilityId());
                    else AbilityClientState.armedAbilities.remove(payload.abilityId());
                }));

        ClientPlayNetworking.registerGlobalReceiver(HudMessagePayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> {
                    AbilityClientState.hudMessages.add(new AbilityClientState.HudMessage(payload.text(), payload.color(), payload.durationTicks()));
                    if (AbilityClientState.hudMessages.size() > 5) AbilityClientState.hudMessages.removeFirst();
                }));

        ClientPlayNetworking.registerGlobalReceiver(PartySyncPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> HudRenderer.partyMembers = payload.members()));

        ClientPlayNetworking.registerGlobalReceiver(ClassSyncPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> {
                    currentClass = payload.classId();
                    ClassDefinition cachedClassDef = ClassRegistry.get(currentClass);
                    cachedFirstActiveAbility = findActiveAbility(cachedClassDef, 0);
                    cachedSecondActiveAbility = findActiveAbility(cachedClassDef, 1);

                    int crystalCd = cooldownTicks.getOrDefault("end_crystal", 0);
                    int crystalTotal = cooldownTotal.getOrDefault("end_crystal", 1800);
                    cooldownTicks.clear();
                    cooldownTotal.clear();
                    if (crystalCd > 0) {
                        cooldownTicks.put("end_crystal", crystalCd);
                        cooldownTotal.put("end_crystal", crystalTotal);
                    }

                    AbilityClientState.activeTimers.clear();
                    AbilityClientState.armedAbilities.clear();
                    AbilityClientState.hudMessages.clear();
                }));

        ClientPlayNetworking.registerGlobalReceiver(CooldownSyncPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> {
                    String id = payload.abilityId();
                    int remaining = payload.remainingTicks();

                    if (id.equals("teleporter_count")) AbilityClientState.teleporterCount = remaining;
                    else if (id.equals("stuntman_blocks")) AbilityClientState.stuntmanClimbedBlocks = remaining;
                    else if (id.equals("porcupine_hits")) AbilityClientState.porcupineHitsTaken = remaining;
                    else if (id.equals("archer_kills")) AbilityClientState.archerBowKills = remaining;
                    else if (id.equals("warrior_kills")) AbilityClientState.warriorSwordKills = remaining;
                    else if (id.equals("dodger_dodges")) AbilityClientState.dodgerDodges = remaining;
                    else {
                        if (remaining > 0) {
                            cooldownTicks.put(id, remaining);
                            cooldownTotal.put(id, Math.max(1, payload.totalTicks()));
                        } else {
                            cooldownTicks.remove(id);
                            cooldownTotal.remove(id);
                        }
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(ClassTokensSyncPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> {
                    randomChangeTokens = payload.random();
                    pick3ChangeTokens = payload.pick3();
                }));

        ClientPlayNetworking.registerGlobalReceiver(Pick3OptionsPayload.TYPE, (payload, ctx) ->
                ctx.client().execute(() -> {
                    if (ctx.client().gui.screen() instanceof ClassesHardcoreMenuScreen) {
                        ctx.client().gui.setScreen(new Pick3Screen(ctx.client().gui.screen(), payload.classIds()));
                    }
                }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.isPaused()) return;

            if (!AbilityClientState.activeTimers.isEmpty()) {
                Iterator<Map.Entry<String, Integer>> it = AbilityClientState.activeTimers.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Integer> e = it.next();
                    if (e.getValue() <= 1) it.remove();
                    else e.setValue(e.getValue() - 1);
                }
            }
            if (!AbilityClientState.hudMessages.isEmpty()) {
                AbilityClientState.hudMessages.removeIf(m -> --m.ticksLeft <= 0);
            }

            while (openMenuKey.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(new ClassesHardcoreMenuScreen(null));
            }
            while (openConfigKey.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(new ClassesHardcoreConfigScreen(null));
            }

            if (client.gui.screen() == null) {
                while (activateAbilityKey.consumeClick()) handlePrimaryActivation();
                while (activateSecondaryKey.consumeClick()) handleSecondaryActivation();
            }

            if (!cooldownTicks.isEmpty()) {
                Iterator<Map.Entry<String, Integer>> it = cooldownTicks.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Integer> e = it.next();
                    int newVal = e.getValue() - 1;
                    if (newVal <= 0) {
                        cooldownTotal.remove(e.getKey());
                        it.remove();
                    } else e.setValue(newVal);
                }
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ClassesHardcore.id("class_hud"),
                (graphics, tickCounter) -> HudRenderer.render(graphics, Minecraft.getInstance()));
    }

    private void handlePrimaryActivation() {
        if (cachedFirstActiveAbility != null)
            ClientPlayNetworking.send(new UseAbilityPayload(cachedFirstActiveAbility));
    }

    private void handleSecondaryActivation() {
        if (cachedSecondActiveAbility != null)
            ClientPlayNetworking.send(new UseAbilityPayload(cachedSecondActiveAbility));
    }

    private static String findActiveAbility(ClassDefinition def, int activeIndex) {
        if (def == null) return null;
        int counter = 0;
        for (Ability a : def.abilities) {
            if (!a.isPassive()) {
                if (counter == activeIndex) return a.getId();
                counter++;
            }
        }
        return null;
    }

    public static Identifier getTexturePath(Identifier icon) {
        if (icon == null) return null;
        if (icon.getPath().startsWith("textures/") && icon.getPath().endsWith(".png")) return icon;
        return Identifier.fromNamespaceAndPath(icon.getNamespace(), "textures/" + icon.getPath() + ".png");
    }

    public static int[] getTextureSize(Identifier icon) {
        if (icon == null) return new int[]{0, 0};
        return TEXTURE_SIZE_CACHE.computeIfAbsent(icon, id -> {
            try {
                Identifier actualPath = getTexturePath(id);
                var resource = Minecraft.getInstance().getResourceManager().getResource(actualPath);
                if (resource.isEmpty()) return new int[]{0, 0};
                try (var stream = resource.get().open()) {
                    NativeImage img = NativeImage.read(stream);
                    int w = img.getWidth(); int h = img.getHeight(); img.close();
                    return new int[]{w, h};
                }
            } catch (Exception e) { return new int[]{0, 0}; }
        });
    }
}