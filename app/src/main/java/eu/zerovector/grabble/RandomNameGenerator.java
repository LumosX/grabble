package eu.zerovector.grabble;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A silly thing that will waste more of my time than I have to waste.
// Going to be pretty fun to write though.
public final class RandomNameGenerator {
    private RandomNameGenerator() { }

    // It'll be like an LL-1 grammar, sort of. Fun stuff.

    // Closers first. The icon_closers are the good guys, so we're placing lots of "goody" references for them.
    private static final String[] closersPrefixes = {"The Most", "Utmost"}; // optional, determined by RNG
    private static final String[] closersTier1 = {"[p] Sacred [2]", "[p] Hallowed [2]", "[p] Honoured [2]", "[p] Holy [2]",
                                                  "[p] Blessed [2]", "[p] Exalted [2]", "Consecrated [2]", "Royal [2]",
                                                  "[p] Regal [2]", "[p] Honourable [2]", "The Queen's Own [3]", "[2]"};
    private static final String[] closersTier2 = {"Order of [4]", "Fellowship of [4]", "Kinship of [4]", "Creed of [4]",
                                                  "Brotherhood of [4]", "Cult of [4]", "Brigade of [4]"};
    // Using tier 3 like this allows us to achieve some flexibility.
    private static final String[] closersTier3 = {"Order", "Fellowship", "Kinship", "Creed", "Brotherhood", "Cult", "Brigade"};
    // references to my own book, Christianity, Dungeons&Dragons, the Malazan Book of the Fallen, Warcraft, and the DR Congo(?).
    private static final String[] closersTier4 = {"the Father", "the Saviour", "the Holy Ghost", "the Holy Light", "Orohim Fireheart",
                                                  "Aora Elderborn", "the Dawn Goddess", "Nycta", "the Queen", "the Queen of Dreams",
                                                  "the Firebrand", "Lord Mercer", "Pelor", "Anomander Rake", "Osserc", "House Paran",
                                                  "Kruppe", "Silverfox", "the Whirlwind Goddess", "Starvald Demelain", "Kurald Thyrllan",
                                                  "Jon Snow", "Lady Liadrin", "Uther Lightbringer", "Lordaeron", "Teldrassil", "Kinshasa"};
    private static final String[][] closersTiers = {closersPrefixes, closersTier1, closersTier2, closersTier3, closersTier4};

    // Now the icon_openers. They're the baddies, so we'll put in lots of bad stuff.
    private static final String[] openersPrefixes = {"Unholy", "Dark", "Eternal", "Illuminated", "Elucidated", "Ancient"}; // optional
    // These guys are biased towards cults.
    private static final String[] openersTier1 = {"[p] Cult of [2]", "[p] Cult of [2]", "[p] Cult of [2]", "[p] Order of [2]",
                                                  "[p] Brotherhood of [2]", "[p] Creed of [2]", "[p] Brethren of [3]"};
    private static final String[] openersTier2 = {"the Damned", "[3]", "the Servants of [3]"};
    // references to my own book, the Cthulhu mythos, Warcraft, the Malazan Book of the Fallen, and Terry Pratchett
    private static final String[] openersTier3 = {"Halflight", "the Unseen God", "Unending Night", "Cthulhu", "Yog-Sothoth", "Shub-Niggurath",
                                                  "Yogg-Saron", "Y'Shaarj", "Blackwing", "Deathwing", "Nefarian", "the Oathbreaker", "the Rope",
                                                  "the Blight", "the Betrayer", "Arthas", "the Lich King", "the Crippled God", "Ammanas",
                                                  "the Ebony Night", "the Ebon Night"};
    private static final String[][] openersTiers = {openersPrefixes, openersTier1, openersTier2, openersTier3};


    private static final int TIER_START = 1; // 0 is the prefix array
    private static final int TIER_PREFIX = 0;
    private static final Random RNG = new Random();
    private static final int PERCENT_CHANCE_FOR_PREFIX = 35;
    private static final String PATTERN_PREFIX = "\\[";
    private static final String PATTERN_SUFFIX = "\\]";
    private static final String PATTERN_TAG_PREFIX = "p";


    // Generates a random faction name for a given alignment
    public static String getFactionName(Alignment targetAlignment) {
        // Seed the RNG (Random Number Generator) we'll be using
        RNG.setSeed(System.currentTimeMillis());
        // Select alignment and prepare result string
        String[][] arrayToUse = closersTiers;
        if (targetAlignment == Alignment.Openers) arrayToUse = openersTiers;
        return tierStringIterator(arrayToUse, TIER_START);
}

    // We're pulling this in a helper, as I'd like to do it recursively
    private static String tierStringIterator(String[][] tierList, int tier) {
        int totalTiers = tierList.length; // excluding tier 0 (prefixes)
        String item = getRandInArray(tierList[tier]);
        // Now try to parse any visible tags - prefixes are special
        String prefixReplacement = "";
        if (System.currentTimeMillis() % 100 < PERCENT_CHANCE_FOR_PREFIX) {
            prefixReplacement = getRandInArray(tierList[TIER_PREFIX]);
        }
        item = item.replaceAll(PATTERN_PREFIX + PATTERN_TAG_PREFIX + PATTERN_SUFFIX, prefixReplacement);
        // After prefixes, we parse ANY level tags we might find
        for (int i = 1; i < totalTiers; i++) {
            Matcher matcher = Pattern.compile(PATTERN_PREFIX + i + PATTERN_SUFFIX).matcher(item);
            // BOW DOWN BEFORE THE GOD OF RECURSION
            if (matcher.find()) {
                item = matcher.replaceAll(tierStringIterator(tierList, i));
            }
        }
        // Finally, don't forget to trim the results - there'll be whitespaces all over the place.
        return item.trim();
    }

    private static String getRandInArray(String[] array) {
        return array[RNG.nextInt(array.length)];
    }

}
