package eu.zerovector.grabble;

// A simple class to make inventory management easier.
public class Inventory {
    private final int DEFAULT_CAPACITY = 5;

    private int capacity = DEFAULT_CAPACITY; // Capacity is usually 5, modified by player level ('rank') in some way
    private int[] letters = new int[26]; // List to hold how many of each letter one has (perfectly matches 'enum.ordinal()')


    // That "generate" IDE functionality is pretty useful, considering there's no properties in this language
    public int getCapacity() {
        return capacity;
    }

    public int[] getLetterCounts() {
        return letters;
    }

    // Upon adding a letter to the inventory, fail if it's full
    public boolean addLetter(Letter letter) {
        int i = letter.ordinal();
        if (letters[i] >= capacity) return false;
        else letters[i] += 1;
        return true;
    }

    // And upon removal, fail if there's no letters left to be removed
    public boolean removeLetter(Letter letter) {
        int i = letter.ordinal();
        if (letters[i] <= 0) return false;
        else letters[i] -= 1;
        return true;
    }

    // TODO: Maybe change this somehow so it's not just public.
    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
    }

    public void resetCapacity() {
        this.capacity = DEFAULT_CAPACITY;
    }
}
