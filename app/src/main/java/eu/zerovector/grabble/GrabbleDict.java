package eu.zerovector.grabble;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// This class is here to facilitate easier management of the entire dictionary. Also good for factions.
public class GrabbleDict {
    private HashMap<Word, Boolean> wordMap;
    private int size = -1;
    private int totalAshValue = -1;

    private boolean isDirty = false; // hacky hacky hacks

    public GrabbleDict() {
        wordMap = new HashMap<>(); size = -1; totalAshValue = -1;
    }

    public int totalAshValue() {
        // We probably won't need this ever - in fact, I wrote it just out of curiosity
        // (and to decide how much the total XP a player can collect is)
        // ... which is why it'll be evaluated upon request, and not before
        if (!isDirty && totalAshValue >= 0) return totalAshValue;

        int result = 0;
        for (Word w : wordMap.keySet()) {
            result += w.ashValue();
        }
        totalAshValue = result;
        isDirty = false;
        return result;
}

    // I just remembered that the Map doesn't have a 'getSize' method, and has a 'size' method instead.
    // Time to refactor everything (unambiguous) from 'getX' to 'X'.
    // Done.
    public int size() {
        return wordMap.size();
    }

    // Customised setting functions.
    public void addWord(Word element) {
        addWord(element, Boolean.FALSE);
    }

    public void addWord(Word element, Boolean isCompleted) {
        wordMap.put(element, isCompleted);
        // Must also flag the map as dirty, i.e. notifying the "total ash value" method that things have changed
        isDirty = true;
    }

    public Set<Map.Entry<Word, Boolean>> entrySet() {
        return wordMap.entrySet();
    }
}
