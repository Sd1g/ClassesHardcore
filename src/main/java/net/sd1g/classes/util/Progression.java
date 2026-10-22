package net.sd1g.classes.util;

/**
 * Единая формула прогрессии для ВСЕХ прокачиваемых способностей.
 *
 *   level      = floor(progress / perLevel)
 *   bonus      = min(level * perLevelBonus, maxBonus)
 *   toNext     = сколько ещё нужно до следующей ступени
 *   required   = сколько нужно за одну ступень (для отображения)
 *
 * Применяется: убийства, уровни опыта, ТП, уклонения, полученные удары,
 * блоки лазания, сделки — любой счётчик.
 */
public record Progression(int level, double bonus, int toNext, int required) {

    public static Progression calculate(double progress, double perLevel,
                                        double perLevelBonus, double maxBonus) {
        if (perLevel <= 0) perLevel = 1;
        int level = (int) Math.floor(progress / perLevel);
        double bonus = Math.min(level * perLevelBonus, maxBonus);
        boolean maxed = bonus >= maxBonus;
        int toNext = maxed ? 0 : (int) Math.max(1, Math.ceil(perLevel - (progress % perLevel)));
        return new Progression(level, bonus, toNext, (int) perLevel);
    }
}