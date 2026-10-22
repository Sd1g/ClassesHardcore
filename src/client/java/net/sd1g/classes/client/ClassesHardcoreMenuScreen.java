package net.sd1g.classes.client;

import com.mojang.blaze3d.platform.NativeImage;
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
import net.minecraft.util.FormattedCharSequence;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.network.RequestClassChangePayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassesHardcoreMenuScreen extends Screen {
    private final Screen parent;
    private ClassDefinition selectedClass = null;

    private List<ClassDefinition> allClasses = new ArrayList<>();
    private List<FormattedCharSequence> cachedDescLines = new ArrayList<>();
    private final List<List<FormattedCharSequence>> cachedAbilityLines = new ArrayList<>();

    private int leftPanelWidth;
    private int rightPanelWidth;
    private int rightPanelX;
    private final int topPanelY = 5;
    private final int topPanelHeight = 30;
    private final int mainContentY = 40;
    private int mainContentHeight;

    private static final int PADDING = 10;
    private static final int ITEM_HEIGHT = 56;
    private static final int ITEM_SPACING = 8;
    private int columns;
    private int columnWidth;

    private int leftScrollOffset = 0;
    private int rightScrollOffset = 0;
    private int rightMaxScroll = 0;

    private boolean draggingLeft = false;
    private boolean draggingRight = false;
    private double dragStartY = 0;
    private int dragStartOffset = 0;

    private static final Map<Identifier, int[]> TEXTURE_SIZE_CACHE = new HashMap<>();

    public ClassesHardcoreMenuScreen(Screen parent) {
        super(Component.literal("Выбор класса"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.leftPanelWidth = (int) (this.width * 0.5);
        this.rightPanelWidth = this.width - leftPanelWidth - PADDING * 3;
        this.rightPanelX = leftPanelWidth + PADDING * 2;
        this.mainContentHeight = this.height - mainContentY - 50;

        this.columns = Math.max(1, Math.min(4, this.width / 250));
        this.columnWidth = (leftPanelWidth - PADDING * 2) / columns;

        this.allClasses = new ArrayList<>(ClassRegistry.getAll().values());
        this.allClasses.sort(Comparator.comparing(def -> def.displayName.toLowerCase()));

        if (selectedClass == null && !allClasses.isEmpty()) {
            selectedClass = allClasses.getFirst();
        }

        this.leftScrollOffset = 0;
        this.rightScrollOffset = 0;
        updateCachedText();

        int buttonY = this.height - 35;
        boolean isAdmin = Minecraft.getInstance().player != null &&
                Minecraft.getInstance().player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);

        int btnWidth = 120;
        int gap = 10;
        int totalBtnWidth = isAdmin ? (btnWidth * 3 + gap * 2) : (btnWidth * 2 + gap);
        int startX = (this.width - totalBtnWidth) / 2;

        this.addRenderableWidget(new ModButton(
                startX, buttonY, btnWidth, 24,
                Component.literal("§eСлучайная смена"),
                _ -> ClientPlayNetworking.send(new RequestClassChangePayload(0, ""))
        ));

        this.addRenderableWidget(new ModButton(
                startX + btnWidth + gap, buttonY, btnWidth, 24,
                Component.literal("§bВыбор из 3-х"),
                _ -> ClientPlayNetworking.send(new RequestClassChangePayload(1, ""))
        ));

        if (isAdmin) {
            this.addRenderableWidget(new ModButton(
                    startX + (btnWidth + gap) * 2, buttonY, btnWidth, 24,
                    Component.literal("§cАдмин: Выбрать"),
                    _ -> {
                        if (selectedClass != null) {
                            ClientPlayNetworking.send(new RequestClassChangePayload(2, selectedClass.id));
                        }
                    }
            ));
        }
    }

    private int getLeftMaxScroll() {
        int rows = (int) Math.ceil(allClasses.size() / (double) columns);
        int totalHeight = rows * (ITEM_HEIGHT + ITEM_SPACING);
        return Math.max(0, totalHeight - mainContentHeight + 20);
    }

    private void updateCachedText() {
        if (selectedClass == null) return;
        var player = Minecraft.getInstance().player;
        cachedDescLines = this.font.split(Component.literal(selectedClass.getDescription()), rightPanelWidth - PADDING * 2 - 10);
        cachedAbilityLines.clear();
        for (Ability ability : selectedClass.abilities) {
            cachedAbilityLines.add(this.font.split(
                    Component.literal(ability.getDescription(player)),
                    rightPanelWidth - PADDING * 2 - 42));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xEE0A0A0A);

        renderTopPanel(gfx);
        renderLeftPanel(gfx, mouseX, mouseY);
        renderRightPanel(gfx, mouseX, mouseY);

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    private void renderTopPanel(GuiGraphicsExtractor gfx) {
        drawBeveledPanel(gfx, PADDING, topPanelY, this.width - PADDING * 2, topPanelHeight, 0xCC1A1A1A);

        String currentClassId = ClassesHardcoreClient.currentClass;
        ClassDefinition currentDef = ClassRegistry.get(currentClassId);
        if (currentDef != null) {
            drawIcon(gfx, currentDef.icon, PADDING + 5, topPanelY + 7, 16);
            gfx.text(this.font, "Текущий класс: §e" + currentDef.displayName, PADDING + 28, topPanelY + 11, 0xFFFFFFFF, true);
        } else {
            gfx.text(this.font, "Текущий класс: §7Неизвестно", PADDING + 5, topPanelY + 11, 0xFFFFFFFF, true);
        }

        String tokenText = "§6🎲 Случайный: §a" + ClassesHardcoreClient.randomChangeTokens +
                "   §6🔮 Из 3-х: §a" + ClassesHardcoreClient.pick3ChangeTokens;
        int textWidth = this.font.width(tokenText);
        gfx.text(this.font, tokenText, this.width - PADDING - textWidth - 10, topPanelY + 11, 0xFFFFFFFF, true);
    }

    private void renderLeftPanel(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        drawBeveledPanel(gfx, PADDING, mainContentY, leftPanelWidth, mainContentHeight, 0xCC151515);

        int startY = mainContentY + 10;
        int startX = PADDING + 10;
        int actualColumnWidth = (leftPanelWidth - PADDING * 2) / columns;
        int currentIconSize = Math.max(32, Math.min(48, actualColumnWidth / 4));

        gfx.enableScissor(PADDING, mainContentY, PADDING + leftPanelWidth, mainContentY + mainContentHeight);

        for (int i = 0; i < allClasses.size(); i++) {
            ClassDefinition def = allClasses.get(i);
            int col = i % columns;
            int row = i / columns;

            int x = startX + col * actualColumnWidth;
            int y = (int) (startY + row * (ITEM_HEIGHT + ITEM_SPACING) - leftScrollOffset);

            if (y + ITEM_HEIGHT < mainContentY || y > mainContentY + mainContentHeight) continue;

            boolean isHovered = mouseX >= x && mouseX <= x + actualColumnWidth - ITEM_SPACING
                    && mouseY >= y && mouseY <= y + ITEM_HEIGHT;

            if (isHovered) {
                gfx.fill(x, y, x + actualColumnWidth - ITEM_SPACING, y + ITEM_HEIGHT, 0x22FFFFFF);
            }
            if (def == selectedClass) {
                gfx.fill(x, y, x + actualColumnWidth - ITEM_SPACING, y + ITEM_HEIGHT, 0x44FFAA00);
                gfx.fill(x, y, x + 3, y + ITEM_HEIGHT, 0xFFFFAA00);
            }

            drawIcon(gfx, def.icon, x + 8, y + (ITEM_HEIGHT - currentIconSize) / 2, currentIconSize);

            String displayName = def.displayName;
            int maxTextWidth = actualColumnWidth - currentIconSize - 20;
            if (this.font.width(displayName) > maxTextWidth) {
                displayName = this.font.plainSubstrByWidth(displayName, maxTextWidth - 5) + "...";
            }
            gfx.text(this.font, displayName, x + currentIconSize + 20, y + (ITEM_HEIGHT - 8) / 2, 0xFFFFFFFF, true);
        }

        gfx.disableScissor();
        drawScrollbar(gfx, PADDING + leftPanelWidth - 8, mainContentY, leftScrollOffset, getLeftMaxScroll());
    }

    private void renderRightPanel(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        drawBeveledPanel(gfx, rightPanelX, mainContentY, rightPanelWidth, mainContentHeight, 0xCC151515);

        if (selectedClass == null) return;

        int contentX = rightPanelX + PADDING + 5;
        int contentY = mainContentY + PADDING + 5;

        gfx.enableScissor(rightPanelX, mainContentY, rightPanelX + rightPanelWidth, mainContentY + mainContentHeight);

        int yOffset = contentY - rightScrollOffset;

        drawIcon(gfx, selectedClass.icon, contentX, yOffset, 48);
        gfx.text(this.font, "§l" + selectedClass.displayName, contentX + 48 + 15, yOffset + 16, 0xFFFFAA00, true);

        int textY = yOffset + 48 + 15;

        for (FormattedCharSequence line : cachedDescLines) {
            gfx.text(this.font, line, contentX, textY, 0xFFAAAAAA, true);
            textY += 12;
        }

        textY += 10;
        gfx.text(this.font, "§lСпособности:", contentX, textY, 0xFFFFAA00, true);
        textY += 18;

        for (int i = 0; i < selectedClass.abilities.size(); i++) {
            Ability ability = selectedClass.abilities.get(i);
            drawIcon(gfx, ability.getIcon(), contentX, textY, 24);
            gfx.text(this.font, "§e" + ability.getDisplayName(), contentX + 32, textY + 8, 0xFFFFFFFF, true);

            int abiY = textY + 26;
            for (FormattedCharSequence line : cachedAbilityLines.get(i)) {
                gfx.text(this.font, line, contentX + 32, abiY, 0xFF888888, true);
                abiY += 11;
            }
            textY = abiY + 10;
        }

        gfx.disableScissor();

        // ИСПРАВЛЕНИЕ СКРОЛЛА: Высчитываем высоту контента независимо от текущего скролла
        int totalContentHeight = 48 + 15; // иконка + отступ
        totalContentHeight += cachedDescLines.size() * 12 + 10; // описание
        totalContentHeight += 18; // заголовок "Способности"
        for (List<FormattedCharSequence> lines : cachedAbilityLines) {
            totalContentHeight += 26 + lines.size() * 11 + 10;
        }

        rightMaxScroll = Math.max(0, totalContentHeight - mainContentHeight + 10);
        rightScrollOffset = Math.max(0, Math.min(rightScrollOffset, rightMaxScroll));

        drawScrollbar(gfx, rightPanelX + rightPanelWidth - 8, mainContentY, rightScrollOffset, rightMaxScroll);
    }

    private void drawScrollbar(GuiGraphicsExtractor gfx, int x, int y, int offset, int maxScroll) {
        if (maxScroll > 0) {
            int barHeight = (int) ((float) mainContentHeight / (mainContentHeight + maxScroll) * mainContentHeight);
            int barY = y + (int) ((float) offset / maxScroll * (mainContentHeight - barHeight));

            gfx.fill(x, y, x + 4, y + mainContentHeight, 0x44FFFFFF);
            gfx.fill(x, barY, x + 4, barY + barHeight, 0xFFFFAA00);
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
        if (srcW <= 0 || srcH <= 0) {
            graphics.fill(x, y, x + size, y + size, 0xFF555555);
            return;
        }

        Identifier texture = ClassesHardcoreClient.getTexturePath(icon);

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale((float) size / srcW, (float) size / srcH);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0f, 0.0f, srcW, srcH, srcW, srcH);
        graphics.pose().popMatrix();
    }

    private static int[] getTextureSize(Identifier icon) {
        return TEXTURE_SIZE_CACHE.computeIfAbsent(icon, id -> {
            try {
                Identifier actualPath = ClassesHardcoreClient.getTexturePath(id);
                var resource = Minecraft.getInstance().getResourceManager().getResource(actualPath);
                if (resource.isEmpty()) return new int[]{0, 0};
                try (var stream = resource.get().open()) {
                    NativeImage img = NativeImage.read(stream);
                    int w = img.getWidth();
                    int h = img.getHeight();
                    img.close();
                    return new int[]{w, h};
                }
            } catch (Exception e) { return new int[]{0, 0}; }
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int leftBarX = PADDING + leftPanelWidth - 8;
        if (getLeftMaxScroll() > 0 && mouseX >= leftBarX && mouseX <= leftBarX + 4 && mouseY >= mainContentY && mouseY <= mainContentY + mainContentHeight) {
            draggingLeft = true;
            dragStartY = mouseY;
            dragStartOffset = leftScrollOffset;
            return true;
        }

        int rightBarX = rightPanelX + rightPanelWidth - 8;
        if (rightMaxScroll > 0 && mouseX >= rightBarX && mouseX <= rightBarX + 4 && mouseY >= mainContentY && mouseY <= mainContentY + mainContentHeight) {
            draggingRight = true;
            dragStartY = mouseY;
            dragStartOffset = rightScrollOffset;
            return true;
        }

        if (mouseX < leftPanelWidth && event.button() == 0) {
            int startY = mainContentY + 10;
            int startX = PADDING + 10;
            int actualColumnWidth = (leftPanelWidth - PADDING * 2) / columns;

            for (int i = 0; i < allClasses.size(); i++) {
                ClassDefinition def = allClasses.get(i);
                int x = startX + (i % columns) * actualColumnWidth;
                int y = (int) (startY + (i / columns) * (ITEM_HEIGHT + ITEM_SPACING) - leftScrollOffset);

                if (mouseX >= x && mouseX <= x + actualColumnWidth - ITEM_SPACING && mouseY >= y && mouseY <= y + ITEM_HEIGHT) {
                    if (selectedClass != def) {
                        selectedClass = def;
                        rightScrollOffset = 0;
                        updateCachedText();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        double mouseY = event.y();

        if (draggingLeft) {
            int maxScroll = getLeftMaxScroll();
            double scrollableArea = mainContentHeight - 20;
            if (scrollableArea > 0 && maxScroll > 0) {
                double deltaY = mouseY - dragStartY;
                float scrollAmount = (float) (deltaY / scrollableArea * maxScroll);
                leftScrollOffset = (int) (dragStartOffset + scrollAmount);
                leftScrollOffset = Math.max(0, Math.min(leftScrollOffset, maxScroll));
            }
            return true;
        }

        if (draggingRight) {
            double scrollableArea = mainContentHeight - 20;
            if (scrollableArea > 0 && rightMaxScroll > 0) {
                double deltaY = mouseY - dragStartY;
                float scrollAmount = (float) (deltaY / scrollableArea * rightMaxScroll);
                rightScrollOffset = (int) (dragStartOffset + scrollAmount);
                rightScrollOffset = Math.max(0, Math.min(rightScrollOffset, rightMaxScroll));
            }
            return true;
        }

        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingLeft = false;
        draggingRight = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= PADDING && mouseX <= PADDING + leftPanelWidth && mouseY >= mainContentY && mouseY <= mainContentY + mainContentHeight) {
            int maxScroll = getLeftMaxScroll();
            if (maxScroll > 0) {
                leftScrollOffset -= scrollY * 20;
                leftScrollOffset = Math.max(0, Math.min(leftScrollOffset, maxScroll));
                return true;
            }
        }

        if (mouseX >= rightPanelX && mouseX <= rightPanelX + rightPanelWidth && mouseY >= mainContentY && mouseY <= mainContentY + mainContentHeight) {
            if (rightMaxScroll > 0) {
                rightScrollOffset -= scrollY * 20;
                rightScrollOffset = Math.max(0, Math.min(rightScrollOffset, rightMaxScroll));
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private class ModButton extends Button {
        public ModButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused();

            int bgColor = hovered ? 0xEE2A2A2A : 0xCC1A1A1A;
            int borderColor = hovered ? 0xFFFFAA00 : 0xFF444444;

            gfx.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);
            gfx.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + 1, 0xFF000000);
            gfx.fill(this.getX(), this.getY() + this.getHeight() - 1, this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF000000);
            gfx.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.getHeight(), 0xFF000000);
            gfx.fill(this.getX() + this.getWidth() - 1, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF000000);

            gfx.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.getWidth() - 1, this.getY() + 2, borderColor);
            gfx.fill(this.getX() + 1, this.getY() + this.getHeight() - 2, this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - 1, borderColor);
            gfx.fill(this.getX() + 1, this.getY() + 1, this.getX() + 2, this.getY() + this.getHeight() - 1, borderColor);
            gfx.fill(this.getX() + this.getWidth() - 2, this.getY() + 1, this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - 1, borderColor);

            int textColor = this.active ? (hovered ? 0xFFFFAA00 : 0xFFFFFFFF) : 0xFF888888;
            int tw = font.width(this.getMessage());
            gfx.text(font, this.getMessage(), this.getX() + (this.getWidth() - tw) / 2, this.getY() + (this.getHeight() - 8) / 2, textColor, true);
        }
    }
}