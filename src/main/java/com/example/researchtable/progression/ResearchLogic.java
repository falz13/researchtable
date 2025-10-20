package com.example.researchtable.progression;

import net.minecraft.enchantment.Enchantment;

public final class ResearchLogic {
    public static final int[] MULTI_LEVEL_THRESHOLDS = {20, 50, 150, 500, 2000};
    public static final int SINGLE_LEVEL_THRESHOLD = 250;

    private ResearchLogic() {
    }

    public static int getProgressThreshold(Enchantment enchantment, int level) {
        if (enchantment.getMaxLevel() <= 1) {
            return SINGLE_LEVEL_THRESHOLD;
        }
        int index = Math.min(level, MULTI_LEVEL_THRESHOLDS.length) - 1;
        return MULTI_LEVEL_THRESHOLDS[index];
    }

    public static int getMaxProgress(Enchantment enchantment) {
        if (enchantment.getMaxLevel() <= 1) {
            return SINGLE_LEVEL_THRESHOLD;
        }
        int maxLevel = Math.min(enchantment.getMaxLevel(), MULTI_LEVEL_THRESHOLDS.length);
        return MULTI_LEVEL_THRESHOLDS[maxLevel - 1];
    }

    public static int getProgressReward(Enchantment enchantment, int level) {
        if (enchantment.getMaxLevel() <= 1) {
            return 30;
        }
        return level * 10;
    }

    public static int getUnlockedLevel(int progress, Enchantment enchantment) {
        if (enchantment.getMaxLevel() <= 1) {
            return progress >= SINGLE_LEVEL_THRESHOLD ? 1 : 0;
        }
        int maxLevel = Math.min(enchantment.getMaxLevel(), MULTI_LEVEL_THRESHOLDS.length);
        for (int level = maxLevel; level >= 1; level--) {
            if (progress >= MULTI_LEVEL_THRESHOLDS[level - 1]) {
                return level;
            }
        }
        return 0;
    }

    public static int getXpCost(Enchantment enchantment, int level) {
        if (level <= 0) {
            return 0;
        }
        int base = Math.max(1, level * 5);
        if (enchantment.getMaxLevel() <= 1) {
            return 8;
        }
        return base;
    }

    public static int getLapisCost(Enchantment enchantment, int level) {
        if (level <= 0) {
            return 0;
        }
        if (enchantment.getMaxLevel() <= 1) {
            return 3;
        }
        return Math.max(1, level);
    }
}
