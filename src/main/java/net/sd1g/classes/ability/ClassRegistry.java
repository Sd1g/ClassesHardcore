package net.sd1g.classes.ability;

import java.util.HashMap;
import java.util.Map;

public class ClassRegistry {
    private static final Map<String, ClassDefinition> CLASSES = new HashMap<>();

    public static void register(ClassDefinition def) {
        CLASSES.put(def.id, def);
    }

    public static ClassDefinition get(String id) {
        return CLASSES.get(id);
    }

    public static Map<String, ClassDefinition> getAll() {
        return CLASSES;
    }
}