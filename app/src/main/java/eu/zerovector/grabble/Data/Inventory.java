package eu.zerovector.grabble.Data;

// This is basically a wrapper around an int array.
// Used to be stuck inside the PlayerData class, but now it's not.
public class Inventory {
    private int[] letterCounts;

    public Inventory() {
        letterCounts = new int[26];
    }

    public boolean addLetter(Letter letter, int capacity) {
        int index = letter.ordinal();
        if (letterCounts[index] < capacity) {
            letterCounts[index]++;
            return true;
        }
        else return false;
    }

    public boolean removeLetter(Letter letter) {
        return removeLetter(letter, 1); // Auto-assume removal of 1 letter by default
    }

    public boolean removeLetter(Letter letter, int amountToRemove) {
        int index = letter.ordinal();
        if (letterCounts[index] - amountToRemove >= 0) {
            letterCounts[index] -= amountToRemove;
            return true;
        }
        else return false;
    }

    public int[] getLetterCounts() {
        return letterCounts;
    }

    public int getAmountOfLetter(Letter letter) {
        int index = letter.ordinal();
        return letterCounts[index];
    }

}