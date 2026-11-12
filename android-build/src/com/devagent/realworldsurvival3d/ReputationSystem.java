package com.devagent.realworldsurvival3d;

/**
 * ReputationSystem - Jianghu Hero/Villain Karma, Crime Detection, and Bounty System (Phase 10).
 * Features:
 *   - Hero Karma vs Villain Karma (-100 to +100)
 *   - Crime detection and Bounty accumulation
 *   - Guard hostility and merchant price scaling
 *   - Gathering & Crafting resources: Spirit Ginseng, Star Iron, Crimson Jade
 *   - Pure Java (100% JVM testable)
 */
public class ReputationSystem {

    public int karma = 15;        // -100 (Villain) to +100 (Hero)
    public int bounty = 0;        // Gold bounty on player
    public int herbsGathered = 0;
    public int oresMined = 0;
    public int duelsWon = 0;

    public static final int KARMA_HERO_THRESHOLD = 30;
    public static final int KARMA_VILLAIN_THRESHOLD = -30;

    public boolean isWanted() {
        return bounty > 0;
    }

    public boolean isHero() {
        return karma >= KARMA_HERO_THRESHOLD;
    }

    public boolean isVillain() {
        return karma <= KARMA_VILLAIN_THRESHOLD;
    }

    /** Records a virtuous deed (defeating bandits, completing protector missions, honoring duels). */
    public void recordVirtue(int karmaGain) {
        karma = Math.min(100, karma + karmaGain);
        if (bounty > 0) {
            bounty = Math.max(0, bounty - karmaGain * 20);
        }
    }

    /** Records a crime (assaulting villagers, theft). Incurs villain karma and bounty. */
    public void recordCrime(int karmaLoss, int bountyIncrease) {
        karma = Math.max(-100, karma - karmaLoss);
        bounty += bountyIncrease;
    }

    /** Clears the bounty through payment. */
    public boolean payBounty(Player player) {
        if (player.currency >= bounty) {
            player.currency -= bounty;
            bounty = 0;
            return true;
        }
        return false;
    }

    /** Returns merchant price multiplier based on karma and bounty. */
    public float getMerchantPriceMultiplier() {
        if (isHero()) {
            return 0.80f; // 20% discount for honored hero
        } else if (isVillain()) {
            return 1.40f; // 40% markup for villains
        }
        return 1.0f;
    }

    /** Gathers herbs (Spirit Ginseng). */
    public void gatherHerb() {
        herbsGathered++;
        recordVirtue(2);
    }

    /** Mines ore (Star Iron). */
    public void mineOre() {
        oresMined++;
        recordVirtue(2);
    }

    public String serialize() {
        return karma + ":" + bounty + ":" + herbsGathered + ":" + oresMined + ":" + duelsWon;
    }

    public void deserialize(String str) {
        if (str == null || str.trim().isEmpty()) return;
        try {
            String[] p = str.split(":");
            if (p.length >= 1) karma = Integer.parseInt(p[0]);
            if (p.length >= 2) bounty = Integer.parseInt(p[1]);
            if (p.length >= 3) herbsGathered = Integer.parseInt(p[2]);
            if (p.length >= 4) oresMined = Integer.parseInt(p[3]);
            if (p.length >= 5) duelsWon = Integer.parseInt(p[4]);
        } catch (Exception ignored) {}
    }
}
