package eu.zerovector.grabble.Data;

import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collections;

import eu.zerovector.grabble.Utils.RomanNumber;

// A utility class revolving around the concept of experience and progression.
public final class Experience {
    // ============== XP STUFF
    // The absolute total maximum XP that can be gathered from the entire dictionary is about 1.1M.
    private static final int XP_FOR_FIRST_LEVEL = 100;
    private static final int XP_FOR_MAX_LEVEL = 1000000;
    // This one is personal preference.
    private static final int MAX_LEVEL = 100;
    private static ArrayList<Integer> XP_REQUIREMENTS_FOR_LEVEL = new ArrayList<>(MAX_LEVEL); // level 0 exclusive

    // First static block: Init XP-by-level list using l337 maths
    static { // I never thought I'd have to legit-use one of these!
        // We've got 100 levels, which should ideally fit some sort of divergent series, or ever-increasing equation.
        // We know that y(1) = XP_FOR_FIRST_LEVEL && y(MAX_LEVEL) = XP_FOR_MAX_LEVEL. How do we get a curve from this?
        // Simple! Imagine an ellipse, "centred" (you know what I mean) at the origin, with a = 100 and b = 1 million.
        // The curve that gives us our XP requirements is therefore the ellipse arc in quadrant IV, shited up to quadrant I.
        // (Plus the XP_FOR_FIRST_LEVEL, of course).
        // We'll then "pre-load" it into an array, so we don't need to re-calculate all this whenever we want to update the UI.

        // Ellipse equation: (x^2)/(a^2) + (y^2)/(b^2) = 1;
        //          where a = semiminor axis, b = semimajor axis (as b > a); a || x axis; b || y axis;
        // We need it solved for Y:
        int a = MAX_LEVEL - 1;
        long b = XP_FOR_MAX_LEVEL - XP_FOR_FIRST_LEVEL;
        int prevXP = 0;
        for (int i = 0; i < MAX_LEVEL; i++) {
            // the XP for level 0 is 0, so the 0-th element in the array corresponds to level 1 (i.e. level 0 exclusive)
            // this means the ellipse's X-axis (the semiminor one) must be equal to (MAX_LEVEL - 1)
            // Rearranging the equation:
            int x = i + 1;
            long y = b * b; // Account for massive values here
            y *= (1-(x*x)/(double)(a*a));
            y = (long)Math.sqrt(y);
            // However, we need to take the negative solution (quadrant IV) and shift it into quadrant I
            y = -y + XP_FOR_MAX_LEVEL;
            // Finally, we round it to the nearest 100, just to make it cleaner...
            int xp = ((int)y/100) * 100; // always floor down
            // ... make sure it's not the same as the previous one (happens near the end of the curve, when it's (almost?) straight)...
            if (xp == prevXP && i > 1) {
                // if that's the case, force prevXP [i-1] to be at 75% of the way between [i-2] and [i].
                int prevPrevXP = XP_REQUIREMENTS_FOR_LEVEL.get(i-2);
                double diff = (xp - prevPrevXP) * 0.75;
                int fixedPrevXP = ((int)(prevPrevXP + diff)/100) * 100; // round down the new value too, so it fits
                XP_REQUIREMENTS_FOR_LEVEL.set(i-1, fixedPrevXP);
            }
            // ... and add it to the list.
            XP_REQUIREMENTS_FOR_LEVEL.add(i, xp);
            prevXP = xp;
        }

        Log.d("TAG", "FINE1");
    }

    public static LevelDetails getLevelDetailsForXP(int curXP) {
        // We can use Arrays.binarySearch to find either a value from the array, or the "insertion point".
        // See the docs for info.
        if (curXP < XP_FOR_FIRST_LEVEL) return new LevelDetails(0, 0, XP_FOR_FIRST_LEVEL);
        if (curXP >= XP_FOR_MAX_LEVEL) return new LevelDetails(MAX_LEVEL, 0, XP_FOR_MAX_LEVEL); // make sure the rank100 counter is always full
        int result = Collections.binarySearch(XP_REQUIREMENTS_FOR_LEVEL, curXP); // we're using an ArrayList, so it's all good
        // If we're exactly at the XP for some level, we'll get that value. We need the next one.
        if (result >= 0) return new LevelDetails(result + 1, XP_REQUIREMENTS_FOR_LEVEL.get(result), XP_REQUIREMENTS_FOR_LEVEL.get(result + 1));
        // Otherwise, we've got: -(insertionPoint)-1, which also needs to be additionally offset by 1
        else {
            result = -result - 1;
            return new LevelDetails(result, XP_REQUIREMENTS_FOR_LEVEL.get(result-1), XP_REQUIREMENTS_FOR_LEVEL.get(result));
        }
    }

    // And a little helper to help me return 2 ints together
    // As it turns out, Java's "static" keyword means something completely different to C#'s. AS FUCKING USUAL.
    // This language sucks massive dongs, y'know.
    public static class LevelDetails {
        private int level;
        private int thisLevelXP;
        private int nextLevelXP;

        public LevelDetails(int level, int thisLevelXP, int nextLevelXP) {
            this.level = level;
            this.thisLevelXP = thisLevelXP;
            this.nextLevelXP = nextLevelXP;
        }

        public int level() {
            return level;
        }
        public int thisLevelXP() {
            return thisLevelXP;
        }
        public int nextLevelXP() {
            return nextLevelXP;
        }
    }




    // ============== LEVEL-UP BOONS
    private static final ArrayList<TraitSet> LEVEL_PERKS;
    public static final TraitSet BASE_PERKS = new TraitSet(6, 15, 5, 6, 0);
    // Second static block: Init XP-by-level list using l337 maths
    static {
        // So. We need to have a traitSet for every level, but we're too smart to declare them by hand.
        // How? We'll implement a map, then we'll look it up, and generate an array full of traitSets.
        // This array will act as the pre-load, to speed up the lookup element.
        // However, something similar applies to that map itself - only suckers do things by hand.
        // ARITHMETIC SERIES TO THE RESCUE!
        // Did some looking using the might of MS Excel, and here is the summary.
        // - SightRange ought to be increased the most, and it will be increased by 1 at every even level (EXCEPT when there's another bonus).
        //      This will maintain until level 50, at which point it'll start increasing by 2 every even level (except if other bonus).
        //      As a result, it shall rise from 15 to 75
        // - GrabRange should be a rarer boon, increased only every third and fifth level (EXCEPT when there's another bonus).
        //      I.e. at level 3, 5, 13, 15, 23, 25, etc.; after level 25 it'll follow a 1-2 pattern, and after 50 - a 2-2 pattern.
        //      As a result, it shall rise from 6 to 40 - we place an extra one at level 9, just so that earlier levels are better.
        // - Inventory space increases by 1 every odd-ten levels. I.e. every level 10, 30, 50, etc.
        //      It's an amazingly powerful effect, so having a max of 10 is good enough.
        // - Letters for 1 ash start at 6 and decrease by 1 every 20 levels, up to 1 at level 100.
        //      It's an absurdly strong effect too, so that's that.
        //
        // At level 100, to celebrate max rank, we further increase Sight by 25 metres, Grab by 10 metres, and Inv by 10 slots.
        // Final totals at rank 100: Sight 100 metres, Grab 50 metres, Inventory 20 slots, and every letter = 1 extra Ash.
        // The lookup table needs to be implemented in reverse, with the strongest bonuses first.
        LEVEL_PERKS = new ArrayList<>(MAX_LEVEL+1);
        // Level 0 and level 100 are easy.
        LEVEL_PERKS.add(0, BASE_PERKS);
        TraitSet curPerks = new TraitSet(BASE_PERKS);
        for (int i = 1; i <= MAX_LEVEL; i++) {
            curPerks.setRawAshReward(0);
            // If at max level, add 25 Sight, 10 Grab, 10 inventory slots and reduce letters for ash to 1
            if (i == MAX_LEVEL) {
                // We're doing rounding UP to nearest 10 as well, so things are cleaner.
                // This is overkill, but it's here so it works with any other amount of levels as well
                curPerks.setSightRange((curPerks.getSightRange() + 25 + 9)/10 * 10);
                curPerks.setGrabRange((curPerks.getGrabRange() + 10 + 9)/10 * 10);
                curPerks.setInvCapacity((curPerks.getInvCapacity() + 10 + 9)/10 * 10);
                curPerks.setNumLettersForOneAsh(1);
                curPerks.setRawAshReward(5000); // why the hell not
            }
            // If not at max level...
            // Increase super-perks every ten levels.
            else if (i % 10 == 0) {
                // Even levels (20, 40, 60...) improve "letters for 1 ash", UP TO 1. Zero letters per 1 ash makes no sense.
                if ((i / 10) % 2 == 0) {
                    if (curPerks.getNumLettersForOneAsh() > 1) {
                        curPerks.reduceNumLettersForOneAsh(1);
                    }
                }
                // The odd levels (10, 30, 50) improve inventory space
                else {
                    curPerks.addInvCapacity(1);
                }
            }
            // Grab range, as specified. This one looks terrible due to the special patterns it needs
            else if (i == 1) {
                curPerks.addGrabRange(1); // at level 1, give one extra
            }
            else if (i % 10 == 3) {
                if (i >= 50) curPerks.addGrabRange(2); // double gainz at _3 levels from 50 onwards (2-2 pattern)
                else curPerks.addGrabRange(1);
            }
            else if (i % 10 == 5){
                if (i >= 25) curPerks.addGrabRange(2); // double gainz at _5 levels from 25 onwards (1-2 or 2-2 pattern)
                else curPerks.addGrabRange(1);
            }
            // And finally, improve sight range if nothing else happened and the level is even.
            else if (i % 2 == 0) {
                if (i >= 50) curPerks.addSightRange(2); // double gainz from 50 onwards
                else curPerks.addSightRange(1);
            }

            // If there are no other rewards at all - usually on every _1st, _7th, and _9th level...
            // ... why not give them a little ash? It's nice to have some reward on every single level anyways.
            else {
                curPerks.setRawAshReward(i * 10); // tenfold the level should be enough
            }

            LEVEL_PERKS.add(i, new TraitSet(curPerks));
        }

        Log.d("TAG", "FINE2");
    }

    public static TraitSet getPerksForLevel(int level) {
        return LEVEL_PERKS.get(level);
    }

    public static DataPair getAllDetailsForXP(int curXP) {
        // we're getting the LevelDetails anyways, so...
        LevelDetails det = getLevelDetailsForXP(curXP);
        return new DataPair(det, LEVEL_PERKS.get(det.level));
    }

    // The alternative was to use "Pair<Experience.LevelDetails, Experience.TraitSet>", which was way too sodding long and shitty
    // I'm starting to get really annoyed at everything
    public static class DataPair {
        private LevelDetails det;
        private TraitSet tra;
        public DataPair(LevelDetails details, TraitSet perks) {
            this.det = details;
            this.tra = perks;
        }
        public LevelDetails levelDetails() {
            return det;
        }
        public TraitSet traitSet() {
            return tra;
        }
    }

    // This class holds auxiliary values that may change with player level
    // I wanted to shoehorn the "Builder" pattern somewhere, but that ended up not happening
    public static class TraitSet {
        private int grabRange; // RANGES ARE RADII, NOT DIAMETERS!
        private int sightRange;
        private int invCapacity;
        private int numLettersForOneAsh;
        private int rawAshReward;
        public TraitSet(int grabRange, int sightRange, int invCapacity, int lettersForOneAsh, int ashReward) {
            this.grabRange = grabRange;
            this.sightRange = sightRange;
            this.invCapacity = invCapacity;
            this.numLettersForOneAsh = lettersForOneAsh;
            this.rawAshReward = ashReward;
        }
        // This is necessary because, apparently, passing "by reference" is being taken too literally...
        public TraitSet(TraitSet copySource) {
            this.grabRange = copySource.getGrabRange();
            this.sightRange = copySource.getSightRange();
            this.invCapacity = copySource.getInvCapacity();
            this.numLettersForOneAsh = copySource.getNumLettersForOneAsh();
            this.rawAshReward = copySource.getRawAshReward();
        }

        public int getGrabRange() {
            return grabRange;
        }

        // Setters shall be private, given that all the setting is done in the master-class
        private void addGrabRange(int range) {
            this.grabRange += range;
        }

        private void setGrabRange(int grabRange) {
            this.grabRange = grabRange;
        }

        public int getSightRange() {
            return sightRange;
        }

        private void addSightRange(int range) {
            this.sightRange += range;
        }

        private void setSightRange(int sightRange) {
            this.sightRange = sightRange;
        }

        public int getInvCapacity() {
            return invCapacity;
        }

        private void addInvCapacity(int extraCapacity) {
            this.invCapacity += extraCapacity;
        }

        private void setInvCapacity(int invCapacity) {
            this.invCapacity = invCapacity;
        }


        public int getNumLettersForOneAsh() {
            return numLettersForOneAsh;
        }

        private void reduceNumLettersForOneAsh(int delta) {
            this.numLettersForOneAsh -= delta;
        }

        private void setNumLettersForOneAsh(int amount) {
            this.numLettersForOneAsh = amount;
        }

        public int getRawAshReward() {
            return rawAshReward;
        }

        private void setRawAshReward(int amount) {
            this.rawAshReward = amount;
        }
    }


    // ============== LEVEL NAMES
    private static final ArrayList<LevelTierNamePair> LEVEL_NAMES;
    // FINAL static block: level names. because I've apparently got time to fucking waste on pointless things
    static {
        // Every five levels, players get a new name. If they don't, every level, their current name gets another suffix.
        // Again, we're pre-generating a list to help UI updates.
        LEVEL_NAMES = new ArrayList<>(MAX_LEVEL+1);
        SparseArray<LevelTierNamePair> specialNames = new SparseArray<>(21);
        //                                          CLOSERS                    OPENERS
        specialNames.put(0,  new LevelTierNamePair("Novice",                 "Acolyte"));
        specialNames.put(5,  new LevelTierNamePair("Accepted",               "Grunt"));
        specialNames.put(10, new LevelTierNamePair("Hunter",                 "Cultist"));
        specialNames.put(15, new LevelTierNamePair("Soldier",                "Zealot"));
        specialNames.put(20, new LevelTierNamePair("Watcher",                "Spireling"));
        specialNames.put(25, new LevelTierNamePair("Keeper",                 "Defiler"));
        specialNames.put(30, new LevelTierNamePair("Sergeant",               "Reaver"));
        specialNames.put(35, new LevelTierNamePair("Guardian",               "Ravager"));
        specialNames.put(40, new LevelTierNamePair("Captain",                "Ringleader"));
        specialNames.put(45, new LevelTierNamePair("Elite",                  "Dedicated"));
        specialNames.put(50, new LevelTierNamePair("Chosen",                 "Chosen")); // These are the same. Why? BECAUSE!
        specialNames.put(55, new LevelTierNamePair("Sentinel",               "Forsworn"));
        specialNames.put(60, new LevelTierNamePair("Stormcrow",              "Chief"));
        specialNames.put(65, new LevelTierNamePair("Blademaster",            "Destroyer"));
        specialNames.put(70, new LevelTierNamePair("Commander",              "Reaper"));
        specialNames.put(75, new LevelTierNamePair("Centurion",              "Spire Lord"));
        specialNames.put(80, new LevelTierNamePair("Prefect",                "Harbinger"));
        specialNames.put(85, new LevelTierNamePair("High General",           "Greater Chief"));
        specialNames.put(90, new LevelTierNamePair("Mortal Sword",           "Immortal"));
        specialNames.put(95, new LevelTierNamePair("Exalted",                "Revered"));
        specialNames.put(MAX_LEVEL, new LevelTierNamePair("Eternal Prophet", "Dark Messiah"));
        // Now generate all the other names, using THESE as a basis
        int curLevelTier = 0;
        for (int i = 0; i <= MAX_LEVEL; i++) {
            LevelTierNamePair curName = specialNames.get(i);
            // Add name as-is if we're at a level where it is
            if (curName != null) {
                LEVEL_NAMES.add(i, new LevelTierNamePair(curName));
                curLevelTier = i;
            }
            // In all other cases, find nearest match and create a roman numeral suffix for it
            else { // Just in case
                String suffix = RomanNumber.make(i - curLevelTier + 1); // The first one should be "II", implying the tier to be "I"
                curName = specialNames.get(curLevelTier); // We need to get the current one without a suffix first
                LEVEL_NAMES.add(i, new LevelTierNamePair(curName).append(" " + suffix));
            }
            // Basically, the output is as follows:
            // level 0 = "Novice" or "Acolyte"
            // level 1 = "Novice II" or "Acolyte II"
            // level 4 = "Novice V" or "Acolyte V"
            // level 58 = "Sentinel IV" or "Forsworn IV"
            // ... etc.
        }

        Log.d("TAG", "FIN3");
    }

    public static String getLevelName(int level, Alignment alignment) {
        LevelTierNamePair namePair = LEVEL_NAMES.get(level);
        if (alignment == Alignment.Closers) return namePair.closersName();
        else return namePair.openersName();
    }

    // I'm getting tired now...
    private static class LevelTierNamePair {
        private String openers;
        private String closers;

        public LevelTierNamePair(String closers, String openers) {
            this.closers = closers;
            this.openers = openers;
        }
        public LevelTierNamePair(LevelTierNamePair copySource) {
            this.closers = copySource.closersName();
            this.openers = copySource.openersName();
        }

        public String openersName() {
            return openers;
        }

        public String closersName() {
            return closers;
        }

        // Well, this sort of shoehorns the builder pattern into everything... lol.
        public LevelTierNamePair append(String suffix) {
            this.openers += suffix;
            this.closers += suffix;
            return this;
        }
    }

}
