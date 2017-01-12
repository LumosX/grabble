package eu.zerovector.grabble.Utils;

import java.util.TreeMap;

// Wish I could say I made this, but I didn't: http://stackoverflow.com/a/19759564
public class RomanNumber {
    private final static TreeMap<Integer, String> map = new TreeMap<>();
    static {
        map.put(1000, "M");
        map.put(900, "CM");
        map.put(500, "D");
        map.put(400, "CD");
        map.put(100, "C");
        map.put(90, "XC");
        map.put(50, "L");
        map.put(40, "XL");
        map.put(10, "X");
        map.put(9, "IX");
        map.put(5, "V");
        map.put(4, "IV");
        map.put(1, "I");
    }
    public static final String make(int number) {
        int l = map.floorKey(number);
        if (number == l) {
            return map.get(number);
        }
        return map.get(l) + make(number-l);
    }

}