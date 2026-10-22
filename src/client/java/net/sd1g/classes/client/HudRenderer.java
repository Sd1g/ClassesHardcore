package net.sd1g.classes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.ability.TooltipData;
import net.sd1g.classes.data.AbilityClientState;
import net.sd1g.classes.network.PartySyncPayload;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class HudRenderer {
    public static List<PartySyncPayload.Member> partyMembers = new ArrayList<>();

    private static final int ICON_INSET = 2;
    private static final int BG_COLOR = 0xC8101010;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int CLASS_BORDER_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int KEY_TEXT_COLOR = 0xFFFFAA00;
    private static final int PROGRESS_BAR_BG = 0xC0000000;
    private static final int PROGRESS_BAR_FILL = 0xFFFFAA00;
    private static final int TOOLTIP_MAX_WIDTH = 220;
    private static final int TOOLTIP_BG = 0xF0100010;
    private static final int TOOLTIP_BORDER = 0xFF5000FF;
    private static final int COOLDOWN_OVERLAY_COLOR = 0xA0000000;
    private static final int COOLDOWN_TEXT_COLOR = 0xFFFFAA00;

    public static void render(GuiGraphicsExtractor gfx, Minecraft mc) {
        if (mc.player == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        renderPartyHud(gfx, mc, screenWidth, screenHeight);
        renderClassHud(gfx, mc, screenWidth, screenHeight);
        renderTooltips(gfx, mc, screenWidth, screenHeight);
    }

    private static void renderPartyHud(GuiGraphicsExtractor gfx, Minecraft mc, int screenWidth, int screenHeight) {
        ClientConfig.Data config = ClientConfig.getHudData();
        if (!config.showPartyHud || partyMembers.isEmpty()) return;

        Font font = mc.font;
        int baseWidth = 140;
        int baseEntryHeight = 30;

        float scale = config.partyHudScale;
        int scaledWidth = (int)(baseWidth * scale);
        int totalBaseHeight = partyMembers.size() * baseEntryHeight;
        int scaledHeight = (int)(totalBaseHeight * scale);

        int hotbarLeft = screenWidth / 2 - 91;
        int hotbarRight = screenWidth / 2 + 91;

        int startX, startY;
        switch (config.partyAnchor) {
            case TOP_LEFT -> { startX = 5; startY = 5; }
            case TOP_RIGHT -> { startX = screenWidth - scaledWidth - 5; startY = 5; }
            case BOTTOM_LEFT -> { startX = 5; startY = screenHeight - scaledHeight - 35; }
            case BOTTOM_RIGHT -> { startX = screenWidth - scaledWidth - 5; startY = screenHeight - scaledHeight - 35; }

            case TOP_LEFT_HOTBAR -> { startX = hotbarLeft - 5 - scaledWidth; startY = 5; }
            case TOP_RIGHT_HOTBAR -> { startX = hotbarRight + 5; startY = 5; }
            case BOTTOM_LEFT_HOTBAR -> { startX = hotbarLeft - 5 - scaledWidth; startY = screenHeight - scaledHeight - 35; }
            case BOTTOM_RIGHT_HOTBAR -> { startX = hotbarRight + 5; startY = screenHeight - scaledHeight - 35; }

            default -> { startX = config.partyOffsetX; startY = config.partyOffsetY; }
        }

        gfx.pose().pushMatrix();
        gfx.pose().translate(startX, startY);
        gfx.pose().scale(scale, scale);

        int y = 0;
        for (PartySyncPayload.Member member : partyMembers) {
            gfx.fill(0, y, baseWidth, y + baseEntryHeight - 2, 0xAA000000);

            PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(member.uuid()) : null;
            if (info != null) {
                ClientAsset.Texture body = info.getSkin().body();
                if (body instanceof ClientAsset.ResourceTexture resTex) {
                    Identifier skin = resTex.texturePath();
                    gfx.pose().pushMatrix();
                    gfx.pose().translate(3.0f, y + 3.0f);
                    gfx.pose().scale(20.0f / 8.0f, 20.0f / 8.0f);
                    gfx.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 8, 8, 8, 8, 64, 64);
                    gfx.pose().popMatrix();
                } else {
                    gfx.fill(3, y + 3, 23, y + 23, 0xFF555555);
                }
            } else {
                gfx.fill(3, y + 3, 23, y + 23, 0xFF555555);
            }

            gfx.text(font, member.name(), 28, y + 3, TEXT_COLOR, true);

            String hpText = (int)Math.ceil(member.health()) + "/" + (int)member.maxHealth();
            int hpTextWidth = font.width(hpText);
            int hpColor = 0xFF55FF55;
            float hpPercent = Math.clamp(member.health() / member.maxHealth(), 0f, 1f);
            if (hpPercent <= 0.3f) hpColor = 0xFFFF5555;
            else if (hpPercent <= 0.6f) hpColor = 0xFFFFFF55;

            gfx.text(font, hpText, baseWidth - 5 - hpTextWidth, y + 3, hpColor, true);

            gfx.text(font, "§7" + member.classDisplayName(), 28, y + 14, 0xFFAAAAAA, false);

            int barWidth = 105;
            int barY = y + 24;
            gfx.fill(28, barY, 28 + barWidth, barY + 4, 0xFF333333);
            gfx.fill(28, barY, 28 + (int)(barWidth * hpPercent), barY + 4, 0xFF00FF00);

            y += baseEntryHeight;
        }

        gfx.pose().popMatrix();
    }

    private static void renderClassHud(GuiGraphicsExtractor gfx, Minecraft mc, int screenWidth, int screenHeight) {
        ClassDefinition def = ClassRegistry.get(ClassesHardcoreClient.currentClass);
        if (def == null) return;

        ClientConfig.Data config = ClientConfig.getHudData();
        int iconSize = ClientConfig.getIconSize();
        int squareSize = iconSize + ICON_INSET * 2;
        int spacing = ClientConfig.getIconSpacing();

        List<Ability> actives = new ArrayList<>();
        List<Ability> passives = new ArrayList<>();
        for (Ability a : def.abilities) {
            if (a.isPassive()) passives.add(a); else actives.add(a);
        }

        int totalSquares = 1 + actives.size() + passives.size();
        int totalWidth = totalSquares * squareSize + (totalSquares - 1) * spacing;

        int currentX = getHudStartX(config, totalWidth, squareSize, screenWidth);
        int startY = getHudStartY(config, squareSize, screenHeight);
        int step = getHudStep(config, squareSize, spacing);

        drawSquare(gfx, currentX, startY, squareSize, CLASS_BORDER_COLOR);
        drawIcon(gfx, def.icon, currentX, startY, iconSize);
        currentX += step;

        int activeIndex = 0;
        for (Ability a : actives) {
            renderAbilitySquare(gfx, mc, a, currentX, startY, squareSize, iconSize, activeIndex);
            currentX += step;
            activeIndex++;
        }

        for (Ability a : passives) {
            renderAbilitySquare(gfx, mc, a, currentX, startY, squareSize, iconSize, -1);
            currentX += step;
        }
    }

    private static void renderAbilitySquare(GuiGraphicsExtractor gfx, Minecraft mc, Ability ability, int x, int y, int squareSize, int iconSize, int activeIndex) {
        boolean onCooldown = !ability.isPassive() && ClassesHardcoreClient.cooldownTicks.getOrDefault(ability.getId(), 0) > 0;

        drawSquare(gfx, x, y, squareSize, BORDER_COLOR);
        drawIcon(gfx, ability.getIcon(), x, y, iconSize);

        if (onCooldown) {
            int remaining = ClassesHardcoreClient.cooldownTicks.get(ability.getId());
            int total = ClassesHardcoreClient.cooldownTotal.getOrDefault(ability.getId(), remaining);
            float ratio = total > 0 ? (float) remaining / total : 0f;

            gfx.fill(x + 1, y + 1, x + squareSize - 1, y + squareSize - 1, COOLDOWN_OVERLAY_COLOR);

            int barX = x + 2, barY = y + squareSize - 4, barWidth = squareSize - 4;
            int barHeight = Math.clamp(iconSize / 8, 2, 3);
            gfx.fill(barX, barY, barX + barWidth, barY + barHeight, PROGRESS_BAR_BG);
            int filledWidth = (int) (barWidth * Math.clamp(1f - ratio, 0f, 1f));
            if (filledWidth > 0) gfx.fill(barX, barY, barX + filledWidth, barY + barHeight, PROGRESS_BAR_FILL);

            String timeText = String.valueOf((remaining + 19) / 20);
            float scale = Math.max(0.5f, iconSize / 24.0f);
            int textW = mc.font.width(timeText);
            gfx.pose().pushMatrix();
            gfx.pose().translate(x + squareSize / 2f, y + squareSize / 2f);
            gfx.pose().scale(scale, scale);
            gfx.text(mc.font, timeText, -textW / 2, -4, COOLDOWN_TEXT_COLOR, true);
            gfx.pose().popMatrix();
        }

        if (activeIndex >= 0 && activeIndex <= 1 && iconSize >= 12) {
            String keyText = activeIndex == 0 ? "[R]" : "[V]";
            int keyX = x + (squareSize - mc.font.width(keyText)) / 2;
            gfx.text(mc.font, keyText, keyX, y + squareSize + 1, KEY_TEXT_COLOR, true);
        }
    }

    private static void renderTooltips(GuiGraphicsExtractor gfx, Minecraft mc, int screenWidth, int screenHeight) {
        int altState = GLFW.glfwGetKey(mc.getWindow().handle(), GLFW.GLFW_KEY_LEFT_ALT);
        if (mc.gui.screen() == null && altState != GLFW.GLFW_PRESS) return;

        long windowHandle = mc.getWindow().handle();
        boolean hovered = GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_HOVERED) != 0;
        boolean focused = GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_FOCUSED) != 0;
        if (!hovered || !focused) return;

        double mouseX = mc.mouseHandler.xpos() * screenWidth / mc.getWindow().getWidth();
        double mouseY = mc.mouseHandler.ypos() * screenHeight / mc.getWindow().getHeight();

        ClassDefinition def = ClassRegistry.get(ClassesHardcoreClient.currentClass);
        if (def == null) return;

        ClientConfig.Data config = ClientConfig.getHudData();
        int iconSize = ClientConfig.getIconSize();
        int squareSize = iconSize + ICON_INSET * 2;
        int spacing = ClientConfig.getIconSpacing();

        List<Ability> actives = new ArrayList<>();
        List<Ability> passives = new ArrayList<>();
        for (Ability a : def.abilities) {
            if (a.isPassive()) passives.add(a); else actives.add(a);
        }

        int totalSquares = 1 + actives.size() + passives.size();
        int totalWidth = totalSquares * squareSize + (totalSquares - 1) * spacing;

        int currentX = getHudStartX(config, totalWidth, squareSize, screenWidth);
        int startY = getHudStartY(config, squareSize, screenHeight);
        int step = getHudStep(config, squareSize, spacing);

        if (isHovered(mouseX, mouseY, currentX, startY, squareSize)) {
            List<String> lines = new ArrayList<>();
            lines.add("§6§l" + def.displayName);
            lines.add("§7" + def.getDescription());
            drawTooltip(gfx, mc.font, wrapAll(mc.font, lines, TOOLTIP_MAX_WIDTH), (int) mouseX, (int) mouseY, screenWidth, screenHeight);
            return;
        }
        currentX += step;

        List<Ability> sorted = new ArrayList<>(actives);
        sorted.addAll(passives);

        for (Ability a : sorted) {
            if (isHovered(mouseX, mouseY, currentX, startY, squareSize)) {
                List<String> lines = buildAbilityTooltip(a, mc);
                drawTooltip(gfx, mc.font, wrapAll(mc.font, lines, TOOLTIP_MAX_WIDTH), (int) mouseX, (int) mouseY, screenWidth, screenHeight);
                return;
            }
            currentX += step;
        }
    }

    private static boolean isHovered(double mouseX, double mouseY, int x, int y, int size) {
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }

    private static int getHudStartX(ClientConfig.Data config, int totalWidth, int squareSize, int screenWidth) {
        int hotbarLeft = screenWidth / 2 - 91;
        int hotbarRight = screenWidth / 2 + 91;
        return switch (config.classAnchor) {
            case TOP_LEFT, BOTTOM_LEFT -> 5 + totalWidth - squareSize;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - totalWidth - 5;
            case TOP_LEFT_HOTBAR, BOTTOM_LEFT_HOTBAR -> hotbarLeft - 5;
            case TOP_RIGHT_HOTBAR, BOTTOM_RIGHT_HOTBAR -> hotbarRight + 5;
            default -> config.classOffsetX;
        };
    }

    private static int getHudStartY(ClientConfig.Data config, int squareSize, int screenHeight) {
        return switch (config.classAnchor) {
            case TOP_LEFT, TOP_RIGHT, TOP_LEFT_HOTBAR, TOP_RIGHT_HOTBAR -> 5;
            case BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT_HOTBAR, BOTTOM_RIGHT_HOTBAR -> screenHeight - squareSize - 12;
            default -> config.classOffsetY;
        };
    }

    private static int getHudStep(ClientConfig.Data config, int squareSize, int spacing) {
        return switch (config.classAnchor) {
            case TOP_LEFT, BOTTOM_LEFT, TOP_LEFT_HOTBAR, BOTTOM_LEFT_HOTBAR -> -(squareSize + spacing);
            default -> squareSize + spacing;
        };
    }

    private static List<String> buildAbilityTooltip(Ability ability, Minecraft mc) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§l" + ability.getDisplayName());

        TooltipData data = ability.buildTooltip(mc.player);

        if (data.description() != null && !data.description().isEmpty()) {
            lines.addAll(wrapText(mc.font, "§7" + data.description(), TOOLTIP_MAX_WIDTH));
        }

        if (data.currentBonus() != null)      lines.add("§eТекущий бонус: §a" + data.currentBonus());
        if (data.toNextLevel() != null)       lines.add("§eДо след. ступени: §f" + data.toNextLevel());
        if (data.perLevelBonus() != null)     lines.add("§eЗа ступень: §a" + data.perLevelBonus());
        if (data.requiredForLevel() != null)  lines.add("§eТребуется: §f" + data.requiredForLevel());

        for (String e : data.extra()) {
            lines.addAll(wrapText(mc.font, e, TOOLTIP_MAX_WIDTH));
        }

        lines.add("");
        lines.add(ability.isPassive() ? "§eТип: §7Пассивная" : "§eТип: §aАктивная");

        if (!ability.isPassive()) {
            int baseCd = ability.getBaseCooldownTicks();
            if (baseCd > 0) lines.add("§eКД: §f" + (baseCd / 20) + "с");

            if (data.castTimeTicks() != null) {
                lines.add("§eВремя каста: §f" + String.format("%.1f", data.castTimeTicks() / 20.0) + "с");
            }

            if (data.durationTicks() != null) lines.add("§eДлительность: §f" + (data.durationTicks() / 20) + "с");
        }
        if (data.chance() != null) lines.add("§eШанс: §f" + String.format("%.1f%%", data.chance() * 100));

        int remaining = ClassesHardcoreClient.cooldownTicks.getOrDefault(ability.getId(), 0);
        if (remaining > 0) lines.add("§eСтатус: §cПерезарядка (" + ((remaining + 19) / 20) + "с)");
        else if (AbilityClientState.armedAbilities.contains(ability.getId())) lines.add("§eСтатус: §bАктивирована");
        else if (!ability.isPassive()) lines.add("§eСтатус: §aГотова");

        return lines;
    }

    private static List<String> wrapAll(Font font, List<String> lines, int maxWidth) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (line.isEmpty() || font.width(line) <= maxWidth) result.add(line);
            else result.addAll(wrapText(font, line, maxWidth));
        }
        return result;
    }

    private static List<String> wrapText(Font font, String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        String stylePrefix = "";
        int styleEnd = 0;
        while (styleEnd + 1 < text.length() && text.charAt(styleEnd) == '§') {
            stylePrefix += text.substring(styleEnd, styleEnd + 2); styleEnd += 2;
        }
        String content = text.substring(styleEnd);
        StringBuilder cur = new StringBuilder(stylePrefix);
        int curW = 0;
        for (String word : content.split(" ")) {
            int ww = font.width(word);
            int sw = curW > 0 ? font.width(" ") : 0;
            if (curW > 0 && curW + sw + ww > maxWidth) {
                result.add(cur.toString());
                cur = new StringBuilder(stylePrefix + word);
                curW = ww;
            } else {
                if (curW > 0) { cur.append(" "); curW += sw; }
                cur.append(word); curW += ww;
            }
        }
        if (curW > 0) result.add(cur.toString());
        return result;
    }

    private static void drawTooltip(GuiGraphicsExtractor graphics, Font font, List<String> lines,
                                    int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int lineHeight = 11, padding = 5, tooltipWidth = 0;
        for (String line : lines) tooltipWidth = Math.max(tooltipWidth, font.width(line));
        tooltipWidth += padding * 2;
        int tooltipHeight = lines.size() * lineHeight + padding * 2;

        int tooltipX = mouseX + 12, tooltipY = mouseY - tooltipHeight - 6;
        if (tooltipX + tooltipWidth > screenWidth - 4) tooltipX = screenWidth - tooltipWidth - 4;
        if (tooltipX < 4) tooltipX = 4;
        if (tooltipY < 4) tooltipY = mouseY + 12;
        if (tooltipY + tooltipHeight > screenHeight - 4) tooltipY = screenHeight - tooltipHeight - 4;

        graphics.fill(tooltipX - 1, tooltipY - 1, tooltipX + tooltipWidth + 1, tooltipY + tooltipHeight + 1, TOOLTIP_BORDER);
        graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, TOOLTIP_BG);
        int textY = tooltipY + padding;
        for (String line : lines) {
            graphics.text(font, line, tooltipX + padding, textY, TEXT_COLOR, false);
            textY += lineHeight;
        }
    }

    private static void drawSquare(GuiGraphicsExtractor gfx, int x, int y, int size, int borderColor) {
        gfx.fill(x, y, x + size, y + size, BG_COLOR);
        gfx.fill(x, y, x + size, y + 1, borderColor);
        gfx.fill(x, y + size - 1, x + size, y + size, borderColor);
        gfx.fill(x, y, x + 1, y + size, borderColor);
        gfx.fill(x + size - 1, y, x + size, y + size, borderColor);
    }

    private static void drawIcon(GuiGraphicsExtractor gfx, Identifier icon, int x, int y, int size) {
        if (icon == null) {
            gfx.fill(x + ICON_INSET, y + ICON_INSET, x + ICON_INSET + size, y + ICON_INSET + size, 0xFF333333);
            return;
        }
        int[] texSize = ClassesHardcoreClient.getTextureSize(icon);
        int srcW = texSize[0], srcH = texSize[1];
        if (srcW <= 0 || srcH <= 0) {
            gfx.fill(x + ICON_INSET, y + ICON_INSET, x + ICON_INSET + size, y + ICON_INSET + size, 0xFF333333);
            return;
        }
        Identifier texture = ClassesHardcoreClient.getTexturePath(icon);
        gfx.pose().pushMatrix();
        gfx.pose().translate((float) (x + ICON_INSET), (float) (y + ICON_INSET));
        gfx.pose().scale((float) size / srcW, (float) size / srcH);
        gfx.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0f, 0.0f, srcW, srcH, srcW, srcH);
        gfx.pose().popMatrix();
    }
}