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
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.network.RequestClassChangePayload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pick3Screen extends Screen {
    private final Screen parent;
    private final List<String> options;
    private ClassDefinition selectedClass = null;
    private boolean viewingCurrentClass = false;

    // Для предупреждения
    private boolean showCancelWarning = false;
    private Button selectButton;
    private Button cancelButton;
    private Button warningYesButton;
    private Button warningNoButton;

    private int leftPanelWidth;
    private int rightPanelWidth;
    private int rightPanelX;
    private final int topPanelY = 5;
    private final int topPanelHeight = 30;
    private final int mainContentY = 40;
    private int mainContentHeight;

    private static final int PADDING = 10;
    private static final int ITEM_HEIGHT = 50;
    private static final int ITEM_SPACING = 8;
    private static final int ICON_SIZE = 32;
    private int itemWidth;

    private static final Map<Identifier, int[]> TEXTURE_SIZE_CACHE = new HashMap<>();

    public Pick3Screen(Screen parent, List<String> options) {
        super(Component.literal("Выбор из 3"));
        this.parent = parent;
        this.options = options;
    }

    @Override
    protected void init() {
        this.leftPanelWidth = (int) (this.width * 0.6);
        this.rightPanelWidth = this.width - leftPanelWidth - PADDING * 3;
        this.rightPanelX = leftPanelWidth + PADDING * 2;
        this.mainContentHeight = this.height - mainContentY - 40;

        this.itemWidth = leftPanelWidth - PADDING * 4;

        if (selectedClass == null && !options.isEmpty()) {
            // ИСПРАВЛЕНИЕ: options.getFirst() вместо options.get(0)
            selectedClass = ClassRegistry.get(options.getFirst());
            viewingCurrentClass = false;
        }

        int buttonY = this.height - 30;
        int buttonWidth = (rightPanelWidth - PADDING) / 2;

        this.selectButton = Button.builder(
                Component.literal("§aПодтвердить выбор"),
                _ -> {
                    if (selectedClass != null && !viewingCurrentClass) {
                        ClientPlayNetworking.send(new RequestClassChangePayload(3, selectedClass.id));
                        this.onClose();
                    }
                }
        ).bounds(rightPanelX + buttonWidth + PADDING, buttonY, buttonWidth, 24).build();
        this.addRenderableWidget(this.selectButton);

        this.cancelButton = Button.builder(
                Component.literal("§cВернуться назад"),
                _ -> {
                    this.showCancelWarning = true;
                    updateWarningButtons();
                }
        ).bounds(rightPanelX, buttonY, buttonWidth, 24).build();
        this.addRenderableWidget(this.cancelButton);

        // Кнопки для всплывающего окна
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int warnBtnWidth = 120;

        this.warningYesButton = Button.builder(
                Component.literal("§cДа, выйти"),
                _ -> this.onClose()
        ).bounds(centerX - warnBtnWidth - 10, centerY + 30, warnBtnWidth, 20).build();

        this.warningNoButton = Button.builder(
                Component.literal("§aНет, остаться"),
                _ -> {
                    this.showCancelWarning = false;
                    updateWarningButtons();
                }
        ).bounds(centerX + 10, centerY + 30, warnBtnWidth, 20).build();

        this.addRenderableWidget(this.warningYesButton);
        this.addRenderableWidget(this.warningNoButton);

        updateWarningButtons();
    }

    private void updateWarningButtons() {
        if (warningYesButton == null || warningNoButton == null) return;

        warningYesButton.visible = showCancelWarning;
        warningNoButton.visible = showCancelWarning;

        if (selectButton != null) {
            selectButton.active = !viewingCurrentClass && !showCancelWarning;
        }
        if (cancelButton != null) {
            cancelButton.active = !showCancelWarning;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xEE0A0A0A);

        renderTopPanel(gfx);
        renderLeftPanel(gfx, mouseX, mouseY);
        renderRightPanel(gfx);

        if (this.selectButton != null) {
            this.selectButton.active = !viewingCurrentClass && !showCancelWarning;
        }

        // Отрисовка предупреждения
        if (showCancelWarning) {
            gfx.fill(0, 0, this.width, this.height, 0xBB000000);

            int w = 320;
            int h = 120;
            int x = (this.width - w) / 2;
            int y = (this.height - h) / 2;
            drawBeveledPanel(gfx, x, y, w, h, 0xEE1A1A1A);

            // ИСПРАВЛЕНИЕ: Убран параметр boolean (true) из centeredText
            gfx.centeredText(this.font, "§l§cВнимание!", this.width / 2, y + 15, 0xFFFFFFFF);
            gfx.centeredText(this.font, "Вы уверены, что хотите выйти?", this.width / 2, y + 35, 0xFFCCCCCC);
            gfx.centeredText(this.font, "Ваш токен будет потрачен впустую.", this.width / 2, y + 50, 0xFFFF5555);
            gfx.centeredText(this.font, "Вы не сможете выбрать класс заново.", this.width / 2, y + 65, 0xFFAAAAAA);
        }

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

        int startY = mainContentY + 15;
        int startX = PADDING + 15;

        for (int i = 0; i < options.size(); i++) {
            ClassDefinition def = ClassRegistry.get(options.get(i));
            if (def == null) continue;

            int y = startY + i * (ITEM_HEIGHT + ITEM_SPACING);

            boolean isHovered = mouseX >= startX && mouseX <= startX + itemWidth && mouseY >= y && mouseY <= y + ITEM_HEIGHT;

            if (isHovered && (selectedClass != def || viewingCurrentClass) && !showCancelWarning) {
                gfx.fill(startX, y, startX + itemWidth, y + ITEM_HEIGHT, 0x22FFFFFF);
            }
            if (def == selectedClass && !viewingCurrentClass) {
                gfx.fill(startX, y, startX + itemWidth, y + ITEM_HEIGHT, 0x44FFAA00);
                gfx.fill(startX, y, startX + 3, y + ITEM_HEIGHT, 0xFFFFAA00);
            }

            drawIcon(gfx, def.icon, startX + 10, y + 9, ICON_SIZE);
            gfx.text(this.font, "§l" + def.displayName, startX + ICON_SIZE + 20, y + 20, 0xFFFFFFFF, true);
        }

        int currentY = startY + options.size() * (ITEM_HEIGHT + ITEM_SPACING) + 20;
        gfx.fill(startX, currentY - 10, startX + itemWidth, currentY - 9, 0xFF444444);

        ClassDefinition currentDef = ClassRegistry.get(ClassesHardcoreClient.currentClass);
        if (currentDef != null) {
            boolean isHovered = mouseX >= startX && mouseX <= startX + itemWidth && mouseY >= currentY && mouseY <= currentY + ITEM_HEIGHT;

            if (isHovered && (!viewingCurrentClass || selectedClass != currentDef) && !showCancelWarning) {
                gfx.fill(startX, currentY, startX + itemWidth, currentY + ITEM_HEIGHT, 0x22FFFFFF);
            }
            if (viewingCurrentClass) {
                gfx.fill(startX, currentY, startX + itemWidth, currentY + ITEM_HEIGHT, 0x4444FF44);
                gfx.fill(startX, currentY, startX + 3, currentY + ITEM_HEIGHT, 0xFF44FF44);
            }

            drawIcon(gfx, currentDef.icon, startX + 10, currentY + 9, ICON_SIZE);
            gfx.text(this.font, "§7Ваш класс: §f" + currentDef.displayName, startX + ICON_SIZE + 20, currentY + 20, 0xFFFFFFFF, true);
        }
    }

    private void renderRightPanel(GuiGraphicsExtractor gfx) {
        drawBeveledPanel(gfx, rightPanelX, mainContentY, rightPanelWidth, mainContentHeight, 0xCC151515);

        if (selectedClass == null) return;

        int contentX = rightPanelX + PADDING + 5;
        int contentY = mainContentY + PADDING + 5;

        drawIcon(gfx, selectedClass.icon, contentX, contentY, ICON_SIZE);
        String title = viewingCurrentClass ? "§7[Текущий] §l" + selectedClass.displayName : "§l" + selectedClass.displayName;
        gfx.text(this.font, title, contentX + ICON_SIZE + 10, contentY + 12, 0xFFFFAA00, true);

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
        if (showCancelWarning) {
            return super.mouseClicked(event, isDoubleClick);
        }

        double mouseX = event.x();
        double mouseY = event.y();

        if (mouseX < leftPanelWidth && event.button() == 0) {
            int startY = mainContentY + 15;
            int startX = PADDING + 15;

            for (int i = 0; i < options.size(); i++) {
                int y = startY + i * (ITEM_HEIGHT + ITEM_SPACING);
                if (mouseX >= startX && mouseX <= startX + itemWidth && mouseY >= y && mouseY <= y + ITEM_HEIGHT) {
                    selectedClass = ClassRegistry.get(options.get(i));
                    viewingCurrentClass = false;
                    return true;
                }
            }

            int currentY = startY + options.size() * (ITEM_HEIGHT + ITEM_SPACING) + 20;
            ClassDefinition currentDef = ClassRegistry.get(ClassesHardcoreClient.currentClass);
            if (currentDef != null && mouseX >= startX && mouseX <= startX + itemWidth && mouseY >= currentY && mouseY <= currentY + ITEM_HEIGHT) {
                selectedClass = currentDef;
                viewingCurrentClass = true;
                return true;
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}