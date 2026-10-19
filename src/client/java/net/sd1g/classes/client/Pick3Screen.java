package net.sd1g.classes.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sd1g.classes.ability.ClassDefinition;
import net.sd1g.classes.ability.ClassRegistry;
import net.sd1g.classes.network.RequestClassChangePayload;

import java.util.List;

public class Pick3Screen extends Screen {
    private final Screen parent;
    private final List<String> options;

    public Pick3Screen(Screen parent, List<String> options) {
        super(Component.literal("Выберите класс"));
        this.parent = parent;
        this.options = options;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int btnWidth = 200;
        int btnHeight = 20;
        int spacing = 10;

        for (int i = 0; i < options.size(); i++) {
            String classId = options.get(i);
            ClassDefinition def = ClassRegistry.get(classId);
            String displayName = def != null ? def.displayName : classId;

            final String finalId = classId;
            this.addRenderableWidget(Button.builder(
                    Component.literal(displayName),
                    b -> {
                        // actionType = 3 означает, что это выбор из уже сгенерированных 3-х (токен не тратится)
                        ClientPlayNetworking.send(new RequestClassChangePayload(3, finalId));
                        Minecraft.getInstance().gui.setScreen(parent);
                    }
            ).bounds(centerX - btnWidth / 2, centerY + i * (btnHeight + spacing) - 30, btnWidth, btnHeight).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xDD000000);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFFFF);
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}