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

    public static class Data {
        public int iconSize = 24;
    }

    private static Data INSTANCE = new Data();

    public static int getIconSize() {
        return INSTANCE.iconSize;
    }

    public static void setIconSize(int size) {
        int clamped = Math.max(1, Math.min(64, size));
        INSTANCE.iconSize = clamped;
        save();
    }

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