package net.sd1g.classes.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClassesHardcoreConfigScreen extends Screen {
    private final Screen parent;

    public ClassesHardcoreConfigScreen(Screen parent) {
        super(Component.translatable("classesharcore.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Динамический старт, чтобы влезло на любом разрешении
        int startY = Math.max(20, cy - 115);

        // 1. Слайдеры
        addRenderableWidget(new IconSizeSlider(cx - 150, startY, 300, 20, ClientConfig.getIconSize()));
        addRenderableWidget(new SpacingSlider(cx - 150, startY + 22, 300, 20, ClientConfig.getIconSpacing()));
        addRenderableWidget(new PartyScaleSlider(cx - 150, startY + 44, 300, 20, ClientConfig.getPartyScale()));

        // 2. Кнопки позиционирования
        int panelY = startY + 80;
        int btnW = 65;
        int gap = 5;
        int cLeft = cx - 135;
        int pLeft = cx + 10;

        // --- HUD Класса (Левая колонка) ---
        addBtn("Угол ВЛ", "Верхний левый угол", cLeft, panelY, true, ClientConfig.HudAnchor.TOP_LEFT);
        addBtn("Угол ВП", "Верхний правый угол", cLeft + btnW + gap, panelY, true, ClientConfig.HudAnchor.TOP_RIGHT);
        addBtn("Угол НЛ", "Нижний левый угол", cLeft, panelY + 22, true, ClientConfig.HudAnchor.BOTTOM_LEFT);
        addBtn("Угол НП", "Нижний правый угол", cLeft + btnW + gap, panelY + 22, true, ClientConfig.HudAnchor.BOTTOM_RIGHT);

        addBtn("Хотбар ВЛ", "Сверху слева у хотбара", cLeft, panelY + 44, true, ClientConfig.HudAnchor.TOP_LEFT_HOTBAR);
        addBtn("Хотбар ВП", "Сверху справа у хотбара", cLeft + btnW + gap, panelY + 44, true, ClientConfig.HudAnchor.TOP_RIGHT_HOTBAR);
        addBtn("Хотбар НЛ", "Снизу слева у хотбара", cLeft, panelY + 66, true, ClientConfig.HudAnchor.BOTTOM_LEFT_HOTBAR);
        addBtn("Хотбар НП", "Снизу справа у хотбара", cLeft + btnW + gap, panelY + 66, true, ClientConfig.HudAnchor.BOTTOM_RIGHT_HOTBAR);

        // --- HUD Пати (Правая колонка) ---
        addBtn("Угол ВЛ", "Верхний левый угол", pLeft, panelY, false, ClientConfig.HudAnchor.TOP_LEFT);
        addBtn("Угол ВП", "Верхний правый угол", pLeft + btnW + gap, panelY, false, ClientConfig.HudAnchor.TOP_RIGHT);
        addBtn("Угол НЛ", "Нижний левый угол", pLeft, panelY + 22, false, ClientConfig.HudAnchor.BOTTOM_LEFT);
        addBtn("Угол НП", "Нижний правый угол", pLeft + btnW + gap, panelY + 22, false, ClientConfig.HudAnchor.BOTTOM_RIGHT);

        addBtn("Хотбар ВЛ", "Сверху слева у хотбара", pLeft, panelY + 44, false, ClientConfig.HudAnchor.TOP_LEFT_HOTBAR);
        addBtn("Хотбар ВП", "Сверху справа у хотбара", pLeft + btnW + gap, panelY + 44, false, ClientConfig.HudAnchor.TOP_RIGHT_HOTBAR);
        addBtn("Хотбар НЛ", "Снизу слева у хотбара", pLeft, panelY + 66, false, ClientConfig.HudAnchor.BOTTOM_LEFT_HOTBAR);
        addBtn("Хотбар НП", "Снизу справа у хотбара", pLeft + btnW + gap, panelY + 66, false, ClientConfig.HudAnchor.BOTTOM_RIGHT_HOTBAR);

        // 3. Переключатель видимости пати
        addRenderableWidget(Button.builder(
                Component.literal("Пати HUD: " + (ClientConfig.getHudData().showPartyHud ? "§aВКЛ" : "§cВЫКЛ")),
                b -> {
                    ClientConfig.getHudData().showPartyHud = !ClientConfig.getHudData().showPartyHud;
                    b.setMessage(Component.literal("Пати HUD: " + (ClientConfig.getHudData().showPartyHud ? "§aВКЛ" : "§cВЫКЛ")));
                }
        ).bounds(pLeft, panelY + 96, 135, 20).build());

        // 4. Кнопка Готово
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> this.onClose()
        ).bounds(cx - 60, panelY + 130, 120, 20).build());
    }

    private void addBtn(String text, String tooltipText, int x, int y, boolean isClassHud, ClientConfig.HudAnchor anchor) {
        Button btn = Button.builder(Component.literal(text), b -> setAnchor(isClassHud, anchor))
                .bounds(x, y, 65, 20)
                .tooltip(Tooltip.create(Component.literal(tooltipText)))
                .build();
        addRenderableWidget(btn);
    }

    private void setAnchor(boolean isClassHud, ClientConfig.HudAnchor anchor) {
        if (isClassHud) {
            ClientConfig.getHudData().classAnchor = anchor;
            ClientConfig.getHudData().classOffsetX = 0;
            ClientConfig.getHudData().classOffsetY = 0;
        } else {
            ClientConfig.getHudData().partyAnchor = anchor;
            ClientConfig.getHudData().partyOffsetX = 0;
            ClientConfig.getHudData().partyOffsetY = 0;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float a) {
        super.extractRenderState(gfx, mouseX, mouseY, a);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int startY = Math.max(20, cy - 115);

        gfx.centeredText(this.font, this.title, cx, Math.max(5, startY - 25), 0xFFFFFFFF);
        gfx.centeredText(this.font, "§6§lHUD Класса", cx - 68, startY + 67, 0xFFFFFFFF);
        gfx.centeredText(this.font, "§b§lHUD Пати", cx + 78, startY + 67, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        ClientConfig.save();
        this.minecraft.gui.setScreen(parent);
    }

    private static class IconSizeSlider extends AbstractSliderButton {
        IconSizeSlider(int x, int y, int w, int h, int initialValue) {
            super(x, y, w, h, Component.empty(), (initialValue - 8) / 56.0);
            updateMessage();
        }
        private int computeValue() { return (int) Math.round(this.value * 56) + 8; }
        @Override protected void updateMessage() { setMessage(Component.literal("Размер иконок класса: " + computeValue() + " px")); }
        @Override protected void applyValue() { ClientConfig.setIconSize(computeValue()); }
    }

    private static class SpacingSlider extends AbstractSliderButton {
        SpacingSlider(int x, int y, int w, int h, int initialValue) {
            super(x, y, w, h, Component.empty(), initialValue / 20.0);
            updateMessage();
        }
        private int computeValue() { return (int) Math.round(this.value * 20); }
        @Override protected void updateMessage() { setMessage(Component.literal("Отступ между иконками: " + computeValue() + " px")); }
        @Override protected void applyValue() { ClientConfig.setIconSpacing(computeValue()); }
    }

    private static class PartyScaleSlider extends AbstractSliderButton {
        PartyScaleSlider(int x, int y, int w, int h, float initialValue) {
            super(x, y, w, h, Component.empty(), (initialValue - 0.5f) / 1.5f);
            updateMessage();
        }
        private float computeValue() { return 0.5f + (float)this.value * 1.5f; }
        @Override protected void updateMessage() {
            setMessage(Component.literal("Масштаб HUD пати: " + String.format("%.1f", computeValue()) + "x"));
        }
        @Override protected void applyValue() { ClientConfig.setPartyScale(computeValue()); }
    }
}