package eu.zerovector.grabble.Data;


import android.content.Context;
import android.content.res.TypedArray;

// One good thing about Java are the enums.
// This enum represents all possible letters and their 'ash' values (both for creating and destroying)
public enum Letter {
    A(3,  1, 1),
    B(20, 4, 1),
    C(13, 2, 1),
    D(10, 2, 1),
    E(1,  1, 3),
    F(15, 3, 1),
    G(18, 3, 1),
    H(9,  2, 1),
    I(5,  1, 1),
    J(25, 5, 1),
    K(22, 4, 1),
    L(11, 2, 1),
    M(14, 2, 1),
    N(6,  1, 1),
    O(4,  1, 1),
    P(19, 4, 1),
    Q(24, 4, 1),
    R(8,  1, 1),
    S(7,  1, 1),
    T(2,  2, 3),
    U(12, 2, 1),
    V(21, 4, 1),
    W(17, 3, 1),
    X(23, 4, 1),
    Y(16, 3, 1),
    Z(26, 5, 1);

    public static final Letter[] values = values(); // 1e37 h4x

    // The letter's "value", as per the coursework handout. Also the Ash price the player needs to pay
    // to create a new letter of that kind.
    private int ashCreateValue;
    // The amount of Ash the player receives upon destroying ("burning") a letter.
    private int ashDestroyValue;
    // The amount of letters the player destroys at once to get the amount of Ash from the previous value.
    // Usually 1, but E and T have an effective price of 0.33 and 0.67 Ash, ergo one needs to burn 3 at once (Ash is an int).
    private int numToDestroy;

    public int getAshCreateValue() {
        return ashCreateValue;
    }

    public int getAshDestroyValue() {
        return ashDestroyValue;
    }

    public int getNumToDestroy() {
        return numToDestroy;
    }

    public int getMarkerIconResourceID(Context context) {
        TypedArray icons = context.getResources().obtainTypedArray(eu.zerovector.grabble.R.array.marker_letter_icons);
        int offset = this.ordinal();
        // We can use the default value to return the unknown marker! Neat!
        int result = icons.getResourceId(offset, eu.zerovector.grabble.R.drawable.marker_letters_unknown);
        icons.recycle(); // This must be done, and must be the final use of the array
        return result;
    }

    Letter(int ashCreateValue, int ashDestroyValue, int numToDestroy) {
        this.ashCreateValue = ashCreateValue;
        this.ashDestroyValue = ashDestroyValue;
        this.numToDestroy = numToDestroy;
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
