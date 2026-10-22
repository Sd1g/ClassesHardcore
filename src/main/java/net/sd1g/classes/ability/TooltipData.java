package net.sd1g.classes.ability;

import java.util.List;

public record TooltipData(
        String description,
        String currentBonus,
        String toNextLevel,
        String perLevelBonus,
        String requiredForLevel,
        Integer cooldownTicks,
        Integer durationTicks,
        Integer castTimeTicks, // НОВОЕ ПОЛЕ
        Double chance,
        List<String> extra
) {
    public static Builder builder(String description) { return new Builder(description); }

    public static final class Builder {
        private final String description;
        private String currentBonus, toNextLevel, perLevelBonus, requiredForLevel;
        private Integer cooldownTicks, durationTicks, castTimeTicks;
        private Double chance;
        private final List<String> extra = new java.util.ArrayList<>();

        private Builder(String description) { this.description = description; }

        public Builder currentBonus(String v) { this.currentBonus = v; return this; }
        public Builder toNext(String v) { this.toNextLevel = v; return this; }
        public Builder perLevel(String v) { this.perLevelBonus = v; return this; }
        public Builder required(String v) { this.requiredForLevel = v; return this; }
        public Builder cooldown(int ticks) { this.cooldownTicks = ticks; return this; }
        public Builder duration(int ticks) { this.durationTicks = ticks; return this; }
        public Builder castTime(int ticks) { this.castTimeTicks = ticks; return this; } // НОВЫЙ МЕТОД
        public Builder chance(double c) { this.chance = c; return this; }
        public Builder extra(String line) { this.extra.add(line); return this; }

        public TooltipData build() {
            return new TooltipData(description, currentBonus, toNextLevel, perLevelBonus,
                    requiredForLevel, cooldownTicks, durationTicks, castTimeTicks, chance, List.copyOf(extra));
        }
    }
}