package net.sd1g.classes.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.network.RequestClassChangePayload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassesHardcoreMenuScreen extends Screen {
    private final Screen parent;
    private ClassDefinition selectedClass = null;

    private int leftPanelWidth;
    private int rightPanelWidth;
    private int rightPanelX;
    private final int topPanelY = 5;
    private final int topPanelHeight = 30;
    private final int mainContentY = 40;
    private int mainContentHeight;

    private static final int PADDING = 10;
    private static final int ITEM_HEIGHT = 40;
    private static final int ITEM_SPACING = 5;
    private static final int COLUMNS = 3;
    private static final int ICON_SIZE = 28;
    private int columnWidth;

    private static final Map<Identifier, int[]> TEXTURE_SIZE_CACHE = new HashMap<>();

    public ClassesHardcoreMenuScreen(Screen parent) {
        super(Component.literal("Выбор класса"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.leftPanelWidth = (int) (this.width * 0.6);
        this.rightPanelWidth = this.width - leftPanelWidth - PADDING * 3;
        this.rightPanelX = leftPanelWidth + PADDING * 2;
        this.mainContentHeight = this.height - mainContentY - 40;

        this.columnWidth = (leftPanelWidth - PADDING * 2) / COLUMNS;

        if (selectedClass == null) {
            List<ClassDefinition> all = new ArrayList<>(ClassRegistry.getAll().values());
            if (!all.isEmpty()) {
                selectedClass = all.getFirst();
            }
        }

        int buttonY = this.height - 30;
        boolean isAdmin = Minecraft.getInstance().player != null &&
                Minecraft.getInstance().player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);

        // Динамическая ширина кнопок
        int buttonWidth;
        if (isAdmin) {
            buttonWidth = (rightPanelWidth - PADDING * 2) / 3;
        } else {
            buttonWidth = (rightPanelWidth - PADDING) / 2;
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("§eСлучайная смена"),
                _ -> ClientPlayNetworking.send(new RequestClassChangePayload(0, ""))
        ).bounds(rightPanelX, buttonY, buttonWidth, 24).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("§bВыбор из 3-х"),
                _ -> ClientPlayNetworking.send(new RequestClassChangePayload(1, ""))
        ).bounds(rightPanelX + buttonWidth + PADDING, buttonY, buttonWidth, 24).build());

        if (isAdmin) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("§cАдмин: Выбрать"),
                    _ -> {
                        if (selectedClass != null) {
                            ClientPlayNetworking.send(new RequestClassChangePayload(2, selectedClass.id));
                        }
                    }
            ).bounds(rightPanelX + (buttonWidth + PADDING) * 2, buttonY, buttonWidth, 24).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xEE0A0A0A);

        renderTopPanel(gfx, mouseX, mouseY);
        renderLeftPanel(gfx, mouseX, mouseY);
        renderRightPanel(gfx, mouseX, mouseY);

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    private void renderTopPanel(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        drawBeveledPanel(gfx, PADDING, topPanelY, this.width - PADDING * 2, topPanelHeight, 0xCC1A1A1A);

        String currentClassId = ClassesHardcoreClient.currentClass;
        ClassDefinition currentDef = ClassRegistry.get(currentClassId);
        if (currentDef != null) {
            drawIcon(gfx, currentDef.icon, PADDING + 5, topPanelY + 7, 16);
            gfx.text(this.font, "Текущий класс: §e" + currentDef.displayName, PADDING + 28, topPanelY + 11, 0xFFFFFFFF, true);
        } else {
            gfx.text(this.font, "Текущий класс: §7Неизвестно", PADDING + 5, topPanelY + 11, 0xFFFFFFFF, true);
        }

        // Красивый текст токенов
        String tokenText = "§6🎲 Случайный: §a" + ClassesHardcoreClient.randomChangeTokens +
                "   §6🔮 Из 3-х: §a" + ClassesHardcoreClient.pick3ChangeTokens;
        int textWidth = this.font.width(tokenText);
        gfx.text(this.font, tokenText, this.width - PADDING - textWidth - 10, topPanelY + 11, 0xFFFFFFFF, true);
    }

    private void renderLeftPanel(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        drawBeveledPanel(gfx, PADDING, mainContentY, leftPanelWidth, mainContentHeight, 0xCC151515);

        List<ClassDefinition> allClasses = new ArrayList<>(ClassRegistry.getAll().values());
        int startY = mainContentY + 10;
        int startX = PADDING + 10;

        for (int i = 0; i < allClasses.size(); i++) {
            ClassDefinition def = allClasses.get(i);
            int col = i % COLUMNS;
            int row = i / COLUMNS;

            int x = startX + col * columnWidth;
            int y = startY + row * (ITEM_HEIGHT + ITEM_SPACING);

            if (y + ITEM_HEIGHT > mainContentY + mainContentHeight - 5) continue;

            boolean isHovered = mouseX >= x && mouseX <= x + columnWidth - ITEM_SPACING
                    && mouseY >= y && mouseY <= y + ITEM_HEIGHT;

            if (isHovered) {
                gfx.fill(x, y, x + columnWidth - ITEM_SPACING, y + ITEM_HEIGHT, 0x22FFFFFF);
            }
            if (def == selectedClass) {
                gfx.fill(x, y, x + columnWidth - ITEM_SPACING, y + ITEM_HEIGHT, 0x44FFAA00);
                gfx.fill(x, y, x + 3, y + ITEM_HEIGHT, 0xFFFFAA00);
            }

            drawIcon(gfx, def.icon, x + 5, y + 6, ICON_SIZE);

            String displayName = def.displayName;
            int maxTextWidth = columnWidth - ICON_SIZE - 15;
            if (this.font.width(displayName) > maxTextWidth) {
                displayName = this.font.plainSubstrByWidth(displayName, maxTextWidth - 5) + "...";
            }
            gfx.text(this.font, displayName, x + ICON_SIZE + 10, y + 16, 0xFFFFFFFF, true);
        }
    }

    private void renderRightPanel(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        drawBeveledPanel(gfx, rightPanelX, mainContentY, rightPanelWidth, mainContentHeight, 0xCC151515);

        if (selectedClass == null) return;

        int contentX = rightPanelX + PADDING + 5;
        int contentY = mainContentY + PADDING + 5;

        drawIcon(gfx, selectedClass.icon, contentX, contentY, ICON_SIZE);
        gfx.text(this.font, "§l" + selectedClass.displayName, contentX + ICON_SIZE + 10, contentY + 10, 0xFFFFAA00, true);

        int textY = contentY + ICON_SIZE + 15;

        List<net.minecraft.util.FormattedCharSequence> descLines = this.font.split(Component.literal(selectedClass.getDescription()), rightPanelWidth - PADDING * 2 - 10);
        for (net.minecraft.util.FormattedCharSequence line : descLines) {
            gfx.text(this.font, line, contentX, textY, 0xFFAAAAAA, true);
            textY += 10;
        }

        textY += 10;
        gfx.text(this.font, "§lСпособности:", contentX, textY, 0xFFFFAA00, true);
        textY += 15;

        for (Ability ability : selectedClass.abilities) {
            drawIcon(gfx, ability.getIcon(), contentX, textY, 16);
            gfx.text(this.font, "§e" + ability.getDisplayName(), contentX + 22, textY + 4, 0xFFFFFFFF, true);

            List<net.minecraft.util.FormattedCharSequence> abiDesc = this.font.split(Component.literal(ability.getDescription()), rightPanelWidth - PADDING * 2 - 32);
            int abiY = textY + 16;
            for (net.minecraft.util.FormattedCharSequence line : abiDesc) {
                gfx.text(this.font, line, contentX + 22, abiY, 0xFF888888, true);
                abiY += 10;
            }
            textY = abiY + 8;
        }
    }

    private void drawBeveledPanel(GuiGraphicsExtractor gfx, int x, int y, int width, int height, int bgColor) {
        gfx.fill(x, y, x + width, y + height, bgColor);
        gfx.fill(x, y, x + width, y + 1, 0xFF000000);
        gfx.fill(x, y + height - 1, x + width, y + height, 0xFF000000);
        gfx.fill(x, y, x + 1, y + height, 0xFF000000);
        gfx.fill(x + width - 1, y, x + width, y + height, 0xFF000000);
        gfx.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF444444);
        gfx.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF444444);
        gfx.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFF444444);
        gfx.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF444444);
    }

    private void drawIcon(GuiGraphicsExtractor graphics, Identifier icon, int x, int y, int size) {
        if (icon == null) {
            graphics.fill(x, y, x + size, y + size, 0xFF555555);
            return;
        }
        int[] texSize = getTextureSize(icon);
        int srcW = texSize[0], srcH = texSize[1];
        if (srcW <= 0 || srcH <= 0) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale((float) size / srcW, (float) size / srcH);
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, 0, 0, 0.0f, 0.0f, srcW, srcH, srcW, srcH);
        graphics.pose().popMatrix();
    }

    private static int[] getTextureSize(Identifier icon) {
        return TEXTURE_SIZE_CACHE.computeIfAbsent(icon, id -> {
            try {
                var resource = Minecraft.getInstance().getResourceManager().getResource(id);
                if (resource.isEmpty()) return new int[]{32, 32};
                try (var stream = resource.get().open()) {
                    BufferedImage img = ImageIO.read(stream);
                    return img == null ? new int[]{32, 32} : new int[]{img.getWidth(), img.getHeight()};
                }
            } catch (Exception e) { return new int[]{32, 32}; }
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (mouseX < leftPanelWidth && event.button() == 0) {
            List<ClassDefinition> allClasses = new ArrayList<>(ClassRegistry.getAll().values());
            int startY = mainContentY + 10;
            int startX = PADDING + 10;

            for (int i = 0; i < allClasses.size(); i++) {
                ClassDefinition def = allClasses.get(i);
                int x = startX + (i % COLUMNS) * columnWidth;
                int y = startY + (i / COLUMNS) * (ITEM_HEIGHT + ITEM_SPACING);

                if (mouseX >= x && mouseX <= x + columnWidth - ITEM_SPACING && mouseY >= y && mouseY <= y + ITEM_HEIGHT) {
                    selectedClass = def;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}