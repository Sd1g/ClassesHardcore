package net.sd1g.classes.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClassesHardcoreConfigScreen extends Screen {
    private final Screen parent;

    public ClassesHardcoreConfigScreen(Screen parent) {
        super(Component.translatable("classeshardcore.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        addRenderableWidget(new IconSizeSlider(cx - 110, cy - 10, 220, 20, ClientConfig.getIconSize()));

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> this.minecraft.gui.setScreen(parent)
        ).bounds(cx - 60, cy + 30, 120, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float a) {
        // Заголовок по центру
        String title = this.title.getString();
        int tw = this.font.width(title);
        gfx.text(this.font, title, this.width / 2 - tw / 2, 20, 0xFFFFFFFF, true);

        // Отрисовка виджетов
        super.extractRenderState(gfx, mouseX, mouseY, a);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    private static class IconSizeSlider extends AbstractSliderButton {
        IconSizeSlider(int x, int y, int w, int h, int initialValue) {
            super(x, y, w, h, Component.empty(), (initialValue - 1) / 63.0);
            updateMessage();
        }

        private int computeValue() {
            return (int) Math.round(this.value * 63) + 1;
        }

        @Override
        protected void updateMessage() {
            int v = computeValue();
            setMessage(Component.translatable("classeshardcore.config.icon_size", v));
        }

        @Override
        protected void applyValue() {
            int v = computeValue();
            ClientConfig.setIconSize(v);
            ClassesHardcoreClient.setIconSize(v);
        }
    }
}