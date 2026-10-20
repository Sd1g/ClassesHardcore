package net.sd1g.classes.ability;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Supplier;

public class ClassDefinition {
    public final String id;
    public final String displayName;
    public final List<Ability> abilities;
    public final Identifier icon;

    private final Supplier<String> descriptionSupplier;

    public ClassDefinition(String id, String displayName, String description, List<Ability> abilities) {
        this(id, displayName, () -> description, abilities);
    }

    public ClassDefinition(String id, String displayName, Supplier<String> descriptionSupplier, List<Ability> abilities) {
        this.id = id;
        this.displayName = displayName;
        this.descriptionSupplier = descriptionSupplier;
        this.abilities = abilities;
        // ИСПРАВЛЕНИЕ: Убран префикс "textures/" и расширение ".png"
        this.icon = Identifier.fromNamespaceAndPath("classeshardcore", "gui/sprites/class/" + id);
    }

    public String getDescription() {
        return descriptionSupplier.get();
    }
}