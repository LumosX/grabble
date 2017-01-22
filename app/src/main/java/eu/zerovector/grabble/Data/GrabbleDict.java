package eu.zerovector.grabble.Data;

import java.util.HashSet;

// This class is here to facilitate easier management of the entire dictionary. Also good for factions.
public class GrabbleDict {
    private HashSet<Word> wordSet;
    private int size = -1;
    private int totalAshValue = -1;

    private boolean isDirty = false; // hacky hacky hacks

    public GrabbleDict() {
        wordSet = new HashSet<>();
        size = -1;
        totalAshValue = -1;
    }

    public int totalAshValue() {
        // We probably won't need this ever - in fact, I wrote it just out of curiosity
        // (and to decide how much the total ash create value for all words is)
        // ... which is why it'll be evaluated upon request, and not before
        if (!isDirty && totalAshValue >= 0) return totalAshValue;

        int result = 0;
        for (Word w : wordSet) {
            result += w.ashCreateValue();
        }
        totalAshValue = result;
        isDirty = false;
        return result;
}

    // I just remembered that the Map doesn't have a 'getSize' method, and has a 'size' method instead.
    // Time to refactor everything (unambiguous) from 'getX' to 'X'.
    // Done.
    public int size() {
        return wordSet.size();
    }

    // Customised setting functions.
    public void addWord(Word element) {
        wordSet.add(element);
        // Must also flag the map as dirty, i.e. notifying the "total ash value" method that things have changed
        isDirty = true;
    }

    public HashSet<Word> wordSet() {
        return wordSet;
    }
}
