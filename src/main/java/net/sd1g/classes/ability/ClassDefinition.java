package net.sd1g.classes.ability;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Supplier;

public class ClassDefinition {
    public final String id;
    public final String displayName;
    public final List<Ability> abilities;
    public final Identifier icon;

    /** Лямбда, возвращающая описание. Читает ModConfig при каждом вызове. */
    private final Supplier<String> descriptionSupplier;

    /** Конструктор со статическим описанием (для классов без чисел). */
    public ClassDefinition(String id, String displayName, String description, List<Ability> abilities) {
        this(id, displayName, () -> description, abilities);
    }

    /** Конструктор с динамическим описанием. */
    public ClassDefinition(String id, String displayName, Supplier<String> descriptionSupplier, List<Ability> abilities) {
        this.id = id;
        this.displayName = displayName;
        this.descriptionSupplier = descriptionSupplier;
        this.abilities = abilities;
        this.icon = Identifier.fromNamespaceAndPath("classeshardcore",
                "textures/gui/sprites/class/" + id + ".png");
    }

    /** Динамическое описание — пересчитывается при каждом вызове тултипа. */
    public String getDescription() {
        return descriptionSupplier.get();
    }
}