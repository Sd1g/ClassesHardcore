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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.sd1g.classes.ClassesHardcore;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.ability.impl.TeleporterAbility;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.network.*;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ClassesHardcoreClient implements ClientModInitializer {
    public static KeyMapping activateAbilityKey;
    public static KeyMapping activateSecondaryKey;
    public static KeyMapping openMenuKey;
    public static KeyMapping openConfigKey;

    public static final Map<String, Integer> cooldownTicks = new HashMap<>();
    public static final Map<String, Integer> cooldownTotal = new HashMap<>();
    public static String currentClass = "none";

    // Токены
    public static int randomChangeTokens = 0;
    public static int pick3ChangeTokens = 0;

    private static ClassDefinition cachedClassDef = null;
    private static String cachedFirstActiveAbility = null;
    private static String cachedSecondActiveAbility = null;

    private static final List<int[]> abilityPositionsCache = new ArrayList<>();
    private static final List<Ability> abilityListCache = new ArrayList<>();

    private static final Map<Identifier, int[]> TEXTURE_SIZE_CACHE = new HashMap<>();

    private static boolean teleportAiming = false;
    private static int previewTickCounter = 0;

    private static final int ICON_INSET = 2;
    private static int ICON_SIZE = 24;
    private static int SQUARE_SIZE = ICON_SIZE + ICON_INSET * 2;

    private static final int SQUARE_SPACING = 3;
    private static final int KEY_LABEL_HEIGHT = 9;
    private static final int TOOLTIP_MAX_WIDTH = 220;

    private static final int BG_COLOR = 0xC8101010;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int CLASS_BORDER_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int KEY_TEXT_COLOR = 0xFFFFAA00;
    private static final int PROGRESS_BAR_BG = 0xC0000000;
    private static final int PROGRESS_BAR_FILL = 0xFFFFAA00;
    private static final int TOOLTIP_BG = 0xF0100010;
    private static final int TOOLTIP_BORDER = 0xFF5000FF;
    private static final int GLINT_COLOR = 0x80AA00FF;

    private static final int COOLDOWN_OVERLAY_COLOR = 0xA0000000;
    private static final int COOLDOWN_TEXT_COLOR = 0xFFFFAA00;

    public static void setIconSize(int size) {
        int clamped = Math.max(1, Math.min(64, size));
        ICON_SIZE = clamped;
        SQUARE_SIZE = ICON_SIZE + ICON_INSET * 2;
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

        ClientPlayNetworking.registerGlobalReceiver(ClassSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                currentClass = payload.classId();
                cachedClassDef = ClassRegistry.get(currentClass);
                cachedFirstActiveAbility = findActiveAbility(cachedClassDef, 0);
                cachedSecondActiveAbility = findActiveAbility(cachedClassDef, 1);

                // Сохраняем кулдаун кристаллов перед очисткой
                int crystalCd = cooldownTicks.getOrDefault("end_crystal", 0);

                cooldownTicks.clear();
                cooldownTotal.clear();

                // Возвращаем кулдаун кристаллов обратно
                if (crystalCd > 0) {
                    cooldownTicks.put("end_crystal", crystalCd);
                    cooldownTotal.put("end_crystal", 1800); // Стандартный максимум 90 сек
                }

                teleportAiming = false;
                previewTickCounter = 0;
                AbilityClientState.luckyDiggerMode = "efficiency";
                AbilityClientState.luckyDiggerTotalOre = 0;
                AbilityClientState.luckyDiggerLevel = 0;
                AbilityClientState.vampireActiveTicks = 0;
                AbilityClientState.invisibilityActiveTicks = 0;
                AbilityClientState.runnerSprintTicks = 0;
                AbilityClientState.assassinBonusDamage = 0.0;
                AbilityClientState.farmerMobKills = 0;
                AbilityClientState.farmerPlayerKills = 0;
                AbilityClientState.heroTrades = 0;
                // AbilityClientState.teleporterCount = 0; (Не сбрасываем, так как сервер пришлет актуальное значение)
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(CooldownSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                String id = payload.abilityId();
                int remaining = payload.remainingTicks();

                // Хак: используем этот пакет для синхронизации счетчика телепортаций
                if (id.equals("teleporter_count")) {
                    AbilityClientState.teleporterCount = remaining;
                } else {
                    cooldownTicks.put(id, remaining);
                    cooldownTotal.put(id, remaining);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(LuckyDiggerSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                AbilityClientState.luckyDiggerMode = payload.mode();
                AbilityClientState.luckyDiggerTotalOre = payload.totalOre();
                AbilityClientState.luckyDiggerLevel = payload.level();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(VampireSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> AbilityClientState.vampireActiveTicks = payload.activeTicks());
        });

        ClientPlayNetworking.registerGlobalReceiver(InvisibilitySyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> AbilityClientState.invisibilityActiveTicks = payload.activeTicks());
        });

        ClientPlayNetworking.registerGlobalReceiver(RunnerSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> AbilityClientState.runnerSprintTicks = payload.activeTicks());
        });

        ClientPlayNetworking.registerGlobalReceiver(AssassinSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> AbilityClientState.assassinBonusDamage = payload.bonusDamage());
        });

        ClientPlayNetworking.registerGlobalReceiver(FarmerSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                AbilityClientState.farmerMobKills = payload.mobKills();
                AbilityClientState.farmerPlayerKills = payload.playerKills();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(HeroSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> AbilityClientState.heroTrades = payload.trades());
        });

        ClientPlayNetworking.registerGlobalReceiver(ClassTokensSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                randomChangeTokens = payload.random();
                pick3ChangeTokens = payload.pick3();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Pick3OptionsPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().gui.screen() instanceof ClassesHardcoreMenuScreen) {
                    context.client().gui.setScreen(new Pick3Screen(context.client().gui.screen(), payload.classIds()));
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.isPaused()) return;

            while (openMenuKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new ClassesHardcoreMenuScreen(null));
                }
            }

            while (openConfigKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new ClassesHardcoreConfigScreen(null));
                }
            }

            if (client.gui.screen() != null && teleportAiming) {
                teleportAiming = false;
                previewTickCounter = 0;
            }

            if (client.gui.screen() == null) {
                while (activateAbilityKey.consumeClick()) {
                    handlePrimaryActivation();
                }
                while (activateSecondaryKey.consumeClick()) {
                    handleSecondaryActivation();
                }
            }

            if (!cooldownTicks.isEmpty()) {
                Iterator<Map.Entry<String, Integer>> it = cooldownTicks.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Integer> e = it.next();
                    int newVal = e.getValue() - 1;
                    if (newVal <= 0) {
                        cooldownTotal.remove(e.getKey());
                        it.remove();
                    } else {
                        e.setValue(newVal);
                    }
                }
            }

            if (teleportAiming) {
                previewTickCounter++;
                if (previewTickCounter % 2 == 0) {
                    renderTeleportPreview(client);
                }
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ClassesHardcore.id("class_hud"),
                (graphics, tickCounter) -> renderClassHud(graphics));
    }

    private void handlePrimaryActivation() {
        if ("teleporter".equals(currentClass)) {
            if (cooldownTicks.getOrDefault("teleport", 0) > 0) return;
            if (!teleportAiming) {
                teleportAiming = true;
                previewTickCounter = 0;
            } else {
                teleportAiming = false;
                previewTickCounter = 0;
                ClientPlayNetworking.send(new UseAbilityPayload("teleport"));
            }
        } else if (cachedFirstActiveAbility != null) {
            ClientPlayNetworking.send(new UseAbilityPayload(cachedFirstActiveAbility));
        }
    }

    private void handleSecondaryActivation() {
        if (cachedSecondActiveAbility != null) {
            ClientPlayNetworking.send(new UseAbilityPayload(cachedSecondActiveAbility));
        }
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

    private static String keyName(KeyMapping mapping, String fallback) {
        try {
            int keyCode = mapping.getDefaultKey().getValue();
            if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
                return String.valueOf((char) keyCode);
            }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                return String.valueOf((char) keyCode);
            }
            if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F12) {
                return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
            }
            return mapping.getTranslatedKeyMessage().getString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private String getPrimaryKeyName() {
        return keyName(activateAbilityKey, "R");
    }

    private String getSecondaryKeyName() {
        return keyName(activateSecondaryKey, "V");
    }

    private static int[] getTextureSize(Identifier icon) {
        return TEXTURE_SIZE_CACHE.computeIfAbsent(icon, id -> {
            try {
                var resource = Minecraft.getInstance().getResourceManager().getResource(id);
                if (resource.isEmpty()) return new int[]{32, 32};
                try (var stream = resource.get().open()) {
                    // Используем быстрый NativeImage вместо ImageIO
                    NativeImage img = NativeImage.read(stream);
                    int w = img.getWidth();
                    int h = img.getHeight();
                    img.close();
                    return new int[]{w, h};
                }
            } catch (Exception e) {
                return new int[]{32, 32};
            }
        });
    }

    private void renderTeleportPreview(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        double distance = TeleporterAbility.computeDistance(player);
        Vec3 horizontalDir = TeleporterAbility.getHorizontalDirection(player);
        Vec3 eye = player.getEyePosition();
        Vec3 rawTarget = eye.add(horizontalDir.scale(distance));

        var standBp = TeleporterAbility.findTargetPosition(mc.level, rawTarget);
        if (standBp == null) return;

        double centerX = rawTarget.x;
        double centerY = standBp.getY() + 0.1;
        double centerZ = rawTarget.z;

        double radius = 0.7;
        int ringPoints = 16;
        for (int i = 0; i < ringPoints; i++) {
            double angle = i * Math.PI * 2 / ringPoints;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            mc.level.addParticle(ParticleTypes.CRIT, x, centerY, z, 0, 0, 0);
        }
    }

    private void renderClassHud(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int hotbarRight = screenWidth / 2 + 91;
        int startX = hotbarRight + 8;
        int startY = screenHeight - SQUARE_SIZE - KEY_LABEL_HEIGHT - 4;

        // Отрисовка КД кристаллов
        int crystalCd = cooldownTicks.getOrDefault("end_crystal", 0);
        if (crystalCd > 0) {
            int seconds = (crystalCd + 19) / 20;
            String text = "§dКристаллы: §c" + seconds + "с";
            graphics.text(font, text, startX, startY - 12, 0xFFFFFFFF, true);
        }

        ClassDefinition def = cachedClassDef;

        int classX = startX;
        drawSquare(graphics, classX, startY, CLASS_BORDER_COLOR);

        if (def != null) {
            drawIcon(graphics, def.icon, classX, startY);
        } else {
            String q = "?";
            graphics.text(font, q,
                    classX + (SQUARE_SIZE - font.width(q)) / 2,
                    startY + (SQUARE_SIZE - 8) / 2,
                    TEXT_COLOR, false);
        }

        abilityPositionsCache.clear();
        abilityListCache.clear();

        if (def != null) {
            int abilityX = classX + SQUARE_SIZE + SQUARE_SPACING;
            int activeCounter = 0;
            for (Ability ability : def.abilities) {
                boolean onCooldown = !ability.isPassive()
                        && cooldownTicks.getOrDefault(ability.getId(), 0) > 0;
                drawSquare(graphics, abilityX, startY, BORDER_COLOR);
                drawIcon(graphics, ability.getIcon(), abilityX, startY);

                boolean highlighted = ability.isHighlighted(mc.player)
                        || ("teleport".equals(ability.getId()) && teleportAiming);
                if (highlighted) {
                    drawGlint(graphics, ability.getIcon(), abilityX, startY);
                }

                if (onCooldown) {
                    int remaining = cooldownTicks.get(ability.getId());
                    int total = cooldownTotal.getOrDefault(ability.getId(), remaining);
                    float ratio = total > 0 ? (float) remaining / total : 0f;

                    graphics.fill(
                            abilityX + 1, startY + 1,
                            abilityX + SQUARE_SIZE - 1, startY + SQUARE_SIZE - 1,
                            COOLDOWN_OVERLAY_COLOR
                    );

                    int barX = abilityX + 2;
                    int barY = startY + SQUARE_SIZE - 4;
                    int barWidth = SQUARE_SIZE - 4;
                    int barHeight = Math.max(2, Math.min(3, ICON_SIZE / 8));
                    graphics.fill(barX, barY, barX + barWidth, barY + barHeight, PROGRESS_BAR_BG);
                    float progress = 1f - ratio;
                    int filledWidth = (int) (barWidth * progress);
                    if (filledWidth > 0) {
                        graphics.fill(barX, barY, barX + filledWidth, barY + barHeight, PROGRESS_BAR_FILL);
                    }

                    int seconds = (remaining + 19) / 20;
                    String timeText = String.valueOf(seconds);

                    float sizeScale = Math.max(0.5f, ICON_SIZE / 24.0f);
                    float scale = sizeScale * (timeText.length() <= 1 ? 1.0f : 0.8f);

                    int textW = font.width(timeText);
                    int textH = 8;

                    int centerX = abilityX + SQUARE_SIZE / 2;
                    int centerY = startY + SQUARE_SIZE / 2;

                    graphics.pose().pushMatrix();
                    graphics.pose().translate((float) centerX, (float) centerY);
                    graphics.pose().scale(scale, scale);
                    graphics.text(font, timeText, -textW / 2, -textH / 2, COOLDOWN_TEXT_COLOR, true);
                    graphics.pose().popMatrix();
                }

                if (!ability.isPassive() && ICON_SIZE >= 12) {
                    String keyText = "";
                    if (activeCounter == 0) keyText = "[" + getPrimaryKeyName() + "]";
                    else if (activeCounter == 1) keyText = "[" + getSecondaryKeyName() + "]";
                    if (!keyText.isEmpty()) {
                        int keyX = abilityX + (SQUARE_SIZE - font.width(keyText)) / 2;
                        int keyY = startY + SQUARE_SIZE + 1;
                        graphics.text(font, keyText, keyX, keyY, KEY_TEXT_COLOR, true);
                    }
                    activeCounter++;
                } else if (!ability.isPassive()) {
                    activeCounter++;
                }

                abilityPositionsCache.add(new int[]{abilityX, startY});
                abilityListCache.add(ability);
                abilityX += SQUARE_SIZE + SQUARE_SPACING;
            }
        }

        if (mc.gui.screen() == null) return;

        long windowHandle = mc.getWindow().handle();
        boolean hovered = GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_HOVERED) != 0;
        boolean focused = GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_FOCUSED) != 0;
        if (!hovered || !focused) return;

        double mouseX = mc.mouseHandler.xpos() * screenWidth / mc.getWindow().getWidth();
        double mouseY = mc.mouseHandler.ypos() * screenHeight / mc.getWindow().getHeight();

        boolean hoveringClass =
                mouseX >= classX && mouseX <= classX + SQUARE_SIZE &&
                        mouseY >= startY && mouseY <= startY + SQUARE_SIZE;

        if (hoveringClass) {
            List<String> lines = buildClassTooltip(def);
            drawTooltip(graphics, font, wrapAll(font, lines, TOOLTIP_MAX_WIDTH),
                    (int) mouseX, (int) mouseY, screenWidth, screenHeight);
            return;
        }

        for (int i = 0; i < abilityPositionsCache.size(); i++) {
            int[] pos = abilityPositionsCache.get(i);
            boolean hovering =
                    mouseX >= pos[0] && mouseX <= pos[0] + SQUARE_SIZE &&
                            mouseY >= pos[1] && mouseY <= pos[1] + SQUARE_SIZE;

            if (hovering) {
                Ability ability = abilityListCache.get(i);
                List<String> lines = buildAbilityTooltip(ability, mc);
                drawTooltip(graphics, font, wrapAll(font, lines, TOOLTIP_MAX_WIDTH),
                        (int) mouseX, (int) mouseY, screenWidth, screenHeight);
                break;
            }
        }
    }

    private void drawIcon(GuiGraphicsExtractor graphics, Identifier icon, int x, int y) {
        int[] size = getTextureSize(icon);
        int srcW = size[0];
        int srcH = size[1];
        if (srcW <= 0 || srcH <= 0) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (x + ICON_INSET), (float) (y + ICON_INSET));
        graphics.pose().scale((float) ICON_SIZE / srcW, (float) ICON_SIZE / srcH);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                icon,
                0, 0, 0.0f, 0.0f,
                srcW, srcH,
                srcW, srcH
        );
        graphics.pose().popMatrix();
    }

    private void drawGlint(GuiGraphicsExtractor graphics, Identifier icon, int x, int y) {
        int[] size = getTextureSize(icon);
        int srcW = size[0];
        int srcH = size[1];
        if (srcW <= 0 || srcH <= 0) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (x + ICON_INSET), (float) (y + ICON_INSET));
        graphics.pose().scale((float) ICON_SIZE / srcW, (float) ICON_SIZE / srcH);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                icon,
                0, 0, 0.0f, 0.0f,
                srcW, srcH,
                srcW, srcH,
                GLINT_COLOR
        );
        graphics.pose().popMatrix();
    }

    private List<String> buildClassTooltip(ClassDefinition def) {
        List<String> lines = new ArrayList<>();
        if (def == null) {
            lines.add("§eКласс не выбран");
            return lines;
        }
        lines.add("§6§l" + def.displayName);
        String desc = def.getDescription();
        if (!desc.isEmpty()) lines.add("§7" + desc);

        List<Ability> actives = new ArrayList<>();
        List<Ability> passives = new ArrayList<>();
        for (Ability a : def.abilities) {
            if (a.isPassive()) passives.add(a);
            else actives.add(a);
        }
        if (!actives.isEmpty()) {
            lines.add("");
            lines.add("§eАктивные способности:");
            int idx = 0;
            for (Ability a : actives) {
                String key = idx == 0 ? "§f[" + getPrimaryKeyName() + "]"
                        : idx == 1 ? "§f[" + getSecondaryKeyName() + "]" : "";
                lines.add(" §a● " + key + " §f" + a.getDisplayName());
                if (!a.getDescription().isEmpty()) lines.add("    §8" + a.getDescription());
                idx++;
            }
        }
        if (!passives.isEmpty()) {
            lines.add("");
            lines.add("§eПассивные способности:");
            for (Ability a : passives) {
                lines.add(" §7● §f" + a.getDisplayName());
                if (!a.getDescription().isEmpty()) lines.add("    §8" + a.getDescription());
            }
        }
        return lines;
    }

    private List<String> buildAbilityTooltip(Ability ability, Minecraft mc) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§l" + ability.getDisplayName());
        if (!ability.getDescription().isEmpty()) lines.add("§7" + ability.getDescription());
        lines.add("");

        if (ability.isPassive()) {
            lines.add("§eТип: §7Пассивная");
        } else {
            lines.add("§eТип: §aАктивная");
            if (ability.getId().equals(cachedFirstActiveAbility)) {
                lines.add("§eКлавиша: §f[" + getPrimaryKeyName() + "]");
            } else if (ability.getId().equals(cachedSecondActiveAbility)) {
                lines.add("§eКлавиша: §f[" + getSecondaryKeyName() + "]");
            }
            int remaining = cooldownTicks.getOrDefault(ability.getId(), 0);
            if (remaining > 0) {
                int seconds = (remaining + 19) / 20;
                lines.add("§eСтатус: §cПерезарядка (" + seconds + "s)");
            } else if ("teleport".equals(ability.getId()) && teleportAiming) {
                lines.add("§eСтатус: §bВыбор точки (§fнажми ещё раз§b)");
            } else if ("vampire_drain".equals(ability.getId()) && AbilityClientState.vampireActiveTicks > 0) {
                int seconds = (AbilityClientState.vampireActiveTicks + 19) / 20;
                lines.add("§eСтатус: §bАктивна (§f" + seconds + "s§b)");
            } else if ("invisibility".equals(ability.getId()) && AbilityClientState.invisibilityActiveTicks > 0) {
                int seconds = (AbilityClientState.invisibilityActiveTicks + 19) / 20;
                lines.add("§eСтатус: §bНевидимость (§f" + seconds + "s§b)");
            } else if ("runner_sprint".equals(ability.getId()) && AbilityClientState.runnerSprintTicks > 0) {
                int seconds = (AbilityClientState.runnerSprintTicks + 19) / 20;
                lines.add("§eСтатус: §bРазгон (§f" + seconds + "s§b)");
            } else {
                lines.add("§eСтатус: §aГотова к использованию");
            }
        }

        List<String> extra = ability.getExtraTooltipLines(mc.player);
        if (!extra.isEmpty()) {
            lines.add("");
            lines.addAll(extra);
        }
        return lines;
    }

    private List<String> wrapAll(Font font, List<String> lines, int maxWidth) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (line.isEmpty() || font.width(line) <= maxWidth) result.add(line);
            else result.addAll(wrapText(font, line, maxWidth));
        }
        return result;
    }

    private List<String> wrapText(Font font, String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        String stylePrefix = "";
        int styleEnd = 0;
        while (styleEnd + 1 < text.length() && text.charAt(styleEnd) == '§') {
            stylePrefix += text.substring(styleEnd, styleEnd + 2);
            styleEnd += 2;
        }
        String content = text.substring(styleEnd);

        StringBuilder currentLine = new StringBuilder(stylePrefix);
        int currentWidth = 0;
        for (String word : content.split(" ")) {
            int wordWidth = font.width(word);
            int spaceWidth = currentWidth > 0 ? font.width(" ") : 0;
            if (currentWidth > 0 && currentWidth + spaceWidth + wordWidth > maxWidth) {
                result.add(currentLine.toString());
                currentLine = new StringBuilder(stylePrefix + word);
                currentWidth = wordWidth;
            } else {
                if (currentWidth > 0) {
                    currentLine.append(" ");
                    currentWidth += spaceWidth;
                }
                currentLine.append(word);
                currentWidth += wordWidth;
            }
        }
        if (currentWidth > 0) result.add(currentLine.toString());
        return result;
    }

    private void drawSquare(GuiGraphicsExtractor graphics, int x, int y, int borderColor) {
        graphics.fill(x, y, x + SQUARE_SIZE, y + SQUARE_SIZE, BG_COLOR);
        graphics.fill(x, y, x + SQUARE_SIZE, y + 1, borderColor);
        graphics.fill(x, y + SQUARE_SIZE - 1, x + SQUARE_SIZE, y + SQUARE_SIZE, borderColor);
        graphics.fill(x, y, x + 1, y + SQUARE_SIZE, borderColor);
        graphics.fill(x + SQUARE_SIZE - 1, y, x + SQUARE_SIZE, y + SQUARE_SIZE, borderColor);
    }

    private void drawTooltip(GuiGraphicsExtractor graphics, Font font, List<String> lines,
                             int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int lineHeight = 11;
        int padding = 5;
        int tooltipWidth = 0;
        for (String line : lines) tooltipWidth = Math.max(tooltipWidth, font.width(line));
        tooltipWidth += padding * 2;
        int tooltipHeight = lines.size() * lineHeight + padding * 2;

        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - tooltipHeight - 6;
        if (tooltipX + tooltipWidth > screenWidth - 4) tooltipX = screenWidth - tooltipWidth - 4;
        if (tooltipX < 4) tooltipX = 4;
        if (tooltipY < 4) tooltipY = mouseY + 12;
        if (tooltipY + tooltipHeight > screenHeight - 4) tooltipY = screenHeight - tooltipHeight - 4;

        graphics.fill(tooltipX - 1, tooltipY - 1,
                tooltipX + tooltipWidth + 1, tooltipY + tooltipHeight + 1, TOOLTIP_BORDER);
        graphics.fill(tooltipX, tooltipY,
                tooltipX + tooltipWidth, tooltipY + tooltipHeight, TOOLTIP_BG);

        int textY = tooltipY + padding;
        for (String line : lines) {
            graphics.text(font, line, tooltipX + padding, textY, TEXT_COLOR, false);
            textY += lineHeight;
        }
    }
}