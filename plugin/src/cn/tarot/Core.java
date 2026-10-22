package cn.tarot;

/** Deterministic rules, independent of the server API. */
public final class Core {
    private Core() {}
    public record Active(int card, int level, long activated, long expires, long unlocks) {
        public boolean expired(long now) { return now >= expires; }
        public boolean locked(long now) { return now < unlocks && !expired(now); }
        public long remaining(long now) { return Math.max(0, expires - now); }
    }
    public static boolean canMerge(int firstCard, int firstLevel, int secondCard, int secondLevel,
                                   int firstCount, int secondCount) {
        return firstCard >= 1 && firstCard <= 22 && firstCard == secondCard && firstLevel >= 1 &&
               firstLevel < 5 && firstLevel == secondLevel && firstCount == 1 && secondCount == 1;
    }
    public static int rarity(int roll, boolean guaranteed) {
        if (roll < 0 || roll >= 10000) throw new IllegalArgumentException("roll");
        int[] weights = guaranteed ? new int[]{0,9400,500,90,10} : new int[]{8000,1800,170,25,5};
        for (int i = 0; i < weights.length; i++) { roll -= weights[i]; if (roll < 0) return i; }
        throw new AssertionError();
    }
}
