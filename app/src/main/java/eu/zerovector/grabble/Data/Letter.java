package eu.zerovector.grabble.Data;


import android.content.Context;
import android.content.res.TypedArray;

// One good thing about Java are the enums.
// This enum represents all possible letters and their 'ash' values (both for creating and destroying)
// (along with some extra flair for each letter. Those are based on a certain alphabet. (Which one is it? Find out on your own!)
// (actually two different alphabets, because why the hell not)
public enum Letter {
    A(3,  1, 1, "Ⰰ", "аз"),
    B(20, 4, 1, "Ⰱ", "буки"),
    C(13, 2, 1, "Ⱍ", "черв"),
    D(10, 2, 1, "Ⰴ", "добро"),
    E(1,  1, 3, "Ⰵ", "ест"),
    F(15, 3, 1, "Ⱇ", "ферт"),
    G(18, 3, 1, "Ⰳ", "глаголи"),
    H(9,  2, 1, "Ⱈ", "ха"),
    I(5,  1, 1, "Ⰻ", "изше"),
    J(25, 5, 1, "Ⰶ", "живете"),
    K(22, 4, 1, "Ⰽ", "како"),
    L(11, 2, 1, "Ⰾ", "люди"),
    M(14, 2, 1, "Ⰿ", "мислете"),
    N(6,  1, 1, "Ⱀ", "наш"),
    O(4,  1, 1, "Ⱁ", "он"),
    P(19, 4, 1, "Ⱂ", "покой"),
    Q(24, 4, 1, "Ⱓ", "ю"),
    R(8,  1, 1, "Ⱃ", "рци"),
    S(7,  1, 1, "Ⱄ", "слово"),
    T(2,  2, 3, "Ⱅ", "твердо"),
    U(12, 2, 1, "Ⱆ", "ук"),
    V(21, 4, 1, "Ⰲ", "веди"),
    W(17, 3, 1, "Ⰷ", "дзело"),
    X(23, 4, 1, "Ⱌ", "ци"),
    Y(16, 3, 1, "Ⰺ", "йота"),
    Z(26, 5, 1, "Ⰸ", "земля");

    public static final Letter[] values = values(); // 1e37 h4x to prevent lots of calculations again and again

    // The letter's "value", as per the coursework handout. Also the Ash price the player needs to pay
    // to create a new letter of that kind.
    private int ashCreateValue;
    // The amount of Ash the player receives upon destroying ("burning") a letter.
    private int ashDestroyValue;
    // The amount of letters the player destroys at once to get the amount of Ash from the previous value.
    // Usually 1, but E and T have an effective price of 0.33 and 0.67 Ash, ergo one needs to burn 3 at once (Ash is an int).
    private int numToDestroy;
    // And just a little bit of something else...
    private String crypticChar;
    private String crypticText;

    public int getAshCreateValue() {
        return ashCreateValue;
    }

    public int getAshDestroyValue() {
        return ashDestroyValue;
    }

    public int getNumToDestroy() {
        return numToDestroy;
    }

    public String getCrypticAlternative() {
        return crypticChar;
    }

    public String getCrypticText() {
        return crypticText;
    }

    public int getMarkerIconResourceID(Context context) {
        TypedArray icons = context.getResources().obtainTypedArray(eu.zerovector.grabble.R.array.marker_letter_icons);
        int offset = this.ordinal();
        // We can use the default value to return the unknown marker! Neat!
        int result = icons.getResourceId(offset, eu.zerovector.grabble.R.drawable.marker_letters_unknown);
        icons.recycle(); // This must be done, and must be the final use of the array
        return result;
    }

    Letter(int ashCreateValue, int ashDestroyValue, int numToDestroy, String crypticChar, String crypticText) {
        this.ashCreateValue = ashCreateValue;
        this.ashDestroyValue = ashDestroyValue;
        this.numToDestroy = numToDestroy;
        this.crypticChar = crypticChar;
        this.crypticText = crypticText;
    }

    // Returns the specific letter instance from a string-wrapped character
    public static Letter fromString(String character) {
        try {
            return Letter.valueOf(character);
        }
        catch (Exception e) {
            return null;
        }
    }
    public static Letter fromChar(char character) {
        return fromString(Character.toString(Character.toUpperCase(character))); // *sigh*
    }


}
