package eu.zerovector.grabble.Utils;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

// Well, it's *sort of* math-related, right?
public class MathUtils {

    // Java doesn't even have a 'Math.clamp' function. And no extension methods, either. BOOO!
    public static int ClampInt(int val, int min, int max) {
        if (val < min) return min;
        if (val > max) return max;
        else return val;
    }


    // AGAIN, no extension methods in Java... *sigh*
    public static LatLng LocationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }


    // We need to handle the "word-inventory" distance somehow.
    // For that, we'll implement a "bag-of-letters" model, use both words and inventories as
    // "frequency vectors", and we'll measure similarity between them.
    // And, if Euclid and Jaccard and Tanimoto can all plaster their names all over things...
    // ... THEN SO CAN I!
    public static int ComputeKirilchevCoefficient(int[] baseFreq, int[] wordFreq) {
        // The Kirilchev coefficient is essentially a bounded-magnitude distance measure.
        // It measures how many letters can be completed by directly sourcing the player's Invantory.
        // The measures range from 0 (none) to <word-length> (all).
        // Pretty simple, actually. I initially intended to overcomplicate it a lot, but this is better.

        // Now, on to the real work...
        // In essence, the Inventory itself is a frequency vector of some word of arbitrary length.
        // Therefore, we're passing the pre-processed variables to this method
        int ALPHABET_LENGTH = baseFreq.length; // We're assuming that the Inventory class handles this correctly
        int total = 0;
        // To make everything faster, we'll ad-hoc kernelise the distance function.
        for (int i = 0; i < ALPHABET_LENGTH; i++) {
            if (wordFreq[i] == 0 || baseFreq[i] == 0) continue;
            total += (wordFreq[i] < baseFreq[i]) ? wordFreq[i] : baseFreq[i]; // pretty much Math.min() on them
        }
        return total;
    }

}
