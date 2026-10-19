package net.sd1g.classes.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.sd1g.classes.ability.Ability;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.network.RequestClassChangePayload;

import java.util.ArrayList;
import java.util.List;

public class ClassesHardcoreMenuScreen extends Screen {
    private final Screen parent;
    private ClassDefinition selectedClass = null;

    // Скроллинг для левой панели
    private double scrollOffset = 0;
    private static final int ITEM_HEIGHT = 40;
    private static final int ITEM_SPACING = 5;
    private static final int COLUMNS = 4;
    private static final int ICON_SIZE = 28;
    private static final int PANEL_LEFT_WIDTH = 280;
    private static final int PADDING = 10;

    public ClassesHardcoreMenuScreen(Screen parent) {
        super(Component.literal("Выбор класса"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int rightPanelX = PANEL_LEFT_WIDTH + PADDING;
        int rightPanelWidth = this.width - rightPanelX - PADDING;
        int buttonY = this.height - 30;

        // Кнопка 1 (Случайный)
        this.addRenderableWidget(Button.builder(
                Component.literal("Смена класса 1 (Случайный)"),
                b -> ClientPlayNetworking.send(new RequestClassChangePayload(0, ""))
        ).bounds(rightPanelX, buttonY, rightPanelWidth / 3 - 5, 20).build());

        // Кнопка 2 (Из 3)
        this.addRenderableWidget(Button.builder(
                Component.literal("Смена класса 2 (Из 3)"),
                b -> ClientPlayNetworking.send(new RequestClassChangePayload(1, ""))
        ).bounds(rightPanelX + rightPanelWidth / 3 + 5, buttonY, rightPanelWidth / 3 - 10, 20).build());

        // Кнопка 3 (Админ)
        this.addRenderableWidget(Button.builder(
                Component.literal("Смена класса 3 (Админ)"),
                b -> {
                    if (selectedClass != null) {
                        ClientPlayNetworking.send(new RequestClassChangePayload(2, selectedClass.id));
                    }
                }
        ).bounds(rightPanelX + (rightPanelWidth / 3) * 2, buttonY, rightPanelWidth / 3 - 5, 20).build());

        if (selectedClass == null) {
            List<ClassDefinition> all = new ArrayList<>(ClassRegistry.getAll().values());
            if (!all.isEmpty()) selectedClass = all.getFirst();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        // Обработка клика по левой панели (сетка классов)
        if (mouseX < PANEL_LEFT_WIDTH && button == 0) {
            List<ClassDefinition> all = new ArrayList<>(ClassRegistry.getAll().values());

            int startY = PADDING + 20;
            int index = (int) ((mouseY - startY + scrollOffset) / (ITEM_HEIGHT + ITEM_SPACING));

            if (index >= 0 && index < all.size()) {
                int col = index % COLUMNS;
                int x = PADDING + col * (PANEL_LEFT_WIDTH / COLUMNS);
                if (mouseX >= x && mouseX <= x + (PANEL_LEFT_WIDTH / COLUMNS) - 5) {
                    selectedClass = all.get(index);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < PANEL_LEFT_WIDTH) {
            scrollOffset -= scrollY * 20;
            if (scrollOffset < 0) scrollOffset = 0;

            int maxScroll = Math.max(0, (ClassRegistry.getAll().size() / COLUMNS) * (ITEM_HEIGHT + ITEM_SPACING) - (this.height - 100));
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // Фон (полупрозрачный)
        gfx.fill(0, 0, this.width, this.height, 0xDD000000);

        // Разделительная линия
        gfx.fill(PANEL_LEFT_WIDTH, 0, PANEL_LEFT_WIDTH + 1, this.height, 0xFFFFFFFF);

        Font font = this.font;
        int startY = PADDING + 20;

        // Рисуем левую панель (сетка)
        List<ClassDefinition> all = new ArrayList<>(ClassRegistry.getAll().values());
        gfx.enableScissor(0, 0, PANEL_LEFT_WIDTH, this.height);

        for (int i = 0; i < all.size(); i++) {
            ClassDefinition def = all.get(i);
            int col = i % COLUMNS;
            int row = i / COLUMNS;

            int x = PADDING + col * (PANEL_LEFT_WIDTH / COLUMNS);
            int y = startY + row * (ITEM_HEIGHT + ITEM_SPACING) - (int)scrollOffset;

            if (y + ITEM_HEIGHT < 0 || y > this.height) continue;

            boolean isSelected = def == selectedClass;
            boolean isHovered = mouseX >= x && mouseX <= x + (PANEL_LEFT_WIDTH / COLUMNS) - 5 && mouseY >= y && mouseY <= y + ITEM_HEIGHT;

            int bgColor = isSelected ? 0x6600AA00 : (isHovered ? 0x66FFFFFF : 0x44FFFFFF);
            gfx.fill(x, y, x + (PANEL_LEFT_WIDTH / COLUMNS) - 5, y + ITEM_HEIGHT, bgColor);

            // Рамка (вместо renderOutline)
            int borderColor = isSelected ? 0xFF00FF00 : 0xFF555555;
            int rightX = x + (PANEL_LEFT_WIDTH / COLUMNS) - 5;
            gfx.fill(x, y, rightX, y + 1, borderColor); // top
            gfx.fill(x, y + ITEM_HEIGHT - 1, rightX, y + ITEM_HEIGHT, borderColor); // bottom
            gfx.fill(x, y, x + 1, y + ITEM_HEIGHT, borderColor); // left
            gfx.fill(rightX - 1, y, rightX, y + ITEM_HEIGHT, borderColor); // right

            // Иконка
            gfx.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, def.icon, x + 4, y + 4, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            // Название
            String name = font.plainSubstrByWidth(def.displayName, (PANEL_LEFT_WIDTH / COLUMNS) - ICON_SIZE - 12);
            gfx.text(font, name, x + ICON_SIZE + 8, y + (ITEM_HEIGHT - 8) / 2, 0xFFFFFFFF, false);
        }
        gfx.disableScissor();

        // Заголовок левой панели
        gfx.text(font, "Все классы", PADDING, PADDING, 0xFFFFFF00, true);

        // Рисуем правую панель (описание)
        int rightPanelX = PANEL_LEFT_WIDTH + PADDING;
        int rightPanelWidth = this.width - rightPanelX - PADDING;

        if (selectedClass != null) {
            gfx.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, selectedClass.icon, rightPanelX, PADDING, 0, 0, 40, 40, 40, 40);
            gfx.text(font, "§6§l" + selectedClass.displayName, rightPanelX + 50, PADDING + 5, 0xFFFFFFFF, true);

            String desc = selectedClass.getDescription();
            List<net.minecraft.util.FormattedCharSequence> wrappedDesc = font.split(Component.literal(desc), rightPanelWidth - 10);
            int y = PADDING + 25;
            for (var line : wrappedDesc) {
                gfx.text(font, line, rightPanelX + 50, y, 0xFFAAAAAA, false);
                y += 10;
            }

            // Способности
            int abilityY = Math.max(y, PADDING + 50) + 10;
            gfx.text(font, "§eСпособности:", rightPanelX, abilityY, 0xFFFFFFFF, true);
            abilityY += 15;

            for (Ability ability : selectedClass.abilities) {
                if (abilityY > this.height - 60) break;

                gfx.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ability.getIcon(), rightPanelX, abilityY, 0, 0, 20, 20, 20, 20);
                gfx.text(font, (ability.isPassive() ? "§7[Пассив] " : "§a[Актив] ") + ability.getDisplayName(), rightPanelX + 25, abilityY, 0xFFFFFFFF, false);

                List<net.minecraft.util.FormattedCharSequence> wrappedAbDesc = font.split(Component.literal(ability.getDescription()), rightPanelWidth - 30);
                int abDescY = abilityY + 11;
                for (var line : wrappedAbDesc) {
                    gfx.text(font, line, rightPanelX + 25, abDescY, 0xFFAAAAAA, false);
                    abDescY += 9;
                }

                abilityY = abDescY + 8;
            }
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}