package net.sd1g.classes.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("classeshardcore-client.json").toFile();

    public enum HudAnchor {
        TOP_LEFT, TOP_RIGHT,
        BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP_LEFT_HOTBAR, TOP_RIGHT_HOTBAR,
        BOTTOM_LEFT_HOTBAR, BOTTOM_RIGHT_HOTBAR,
        FREE
    }

    public static class Data {
        public int iconSize = 24;
        public int iconSpacing = 3;

        // HUD Класса
        public HudAnchor classAnchor = HudAnchor.BOTTOM_RIGHT_HOTBAR;
        public int classOffsetX = 0;
        public int classOffsetY = 0;

        // HUD Пати
        public boolean showPartyHud = true;
        public float partyHudScale = 1.0f;
        public HudAnchor partyAnchor = HudAnchor.TOP_LEFT;
        public int partyOffsetX = 0;
        public int partyOffsetY = 0;
    }

    private static Data INSTANCE = new Data();

    public static int getIconSize() { return INSTANCE.iconSize; }
    public static void setIconSize(int size) { INSTANCE.iconSize = Math.max(1, Math.min(64, size)); }

    public static int getIconSpacing() { return INSTANCE.iconSpacing; }
    public static void setIconSpacing(int spacing) { INSTANCE.iconSpacing = Math.max(0, Math.min(20, spacing)); }

    public static float getPartyScale() { return INSTANCE.partyHudScale; }
    public static void setPartyScale(float scale) { INSTANCE.partyHudScale = Math.max(0.5f, Math.min(2.0f, scale)); }

    public static Data getHudData() { return INSTANCE; }

    public static void load() {
        try {
            if (FILE.exists()) {
                try (FileReader r = new FileReader(FILE)) {
                    Data d = GSON.fromJson(r, Data.class);
                    if (d != null) INSTANCE = d;
                }
            }
            INSTANCE.iconSize = Math.max(1, Math.min(64, INSTANCE.iconSize));
        } catch (Exception e) {
            INSTANCE = new Data();
        }
    }

    public static void save() {
        try {
            FILE.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(FILE)) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (Exception ignored) {}
    }
}