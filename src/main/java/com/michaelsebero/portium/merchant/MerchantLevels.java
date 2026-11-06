package com.michaelsebero.portium.merchant;

/** 1.14's {@code VillagerData} experience thresholds for merchant levels 1 (Novice) to 5 (Master). */
public final class MerchantLevels {

    private static final int[] LEVEL_EXPERIENCE = {0, 10, 70, 150, 250};

    public static int getExperiencePrevious(int level) {
        return canLevelUp(level) ? LEVEL_EXPERIENCE[level - 1] : 0;
    }

    public static int getExperienceNext(int level) {
        return canLevelUp(level) ? LEVEL_EXPERIENCE[level] : 0;
    }

    public static boolean canLevelUp(int level) {
        return level >= 1 && level < 5;
    }

    private MerchantLevels() {}
}
