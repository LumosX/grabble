package eu.zerovector.grabble;

// Since I'm actually abstracting things in classes, I might's well do this
public class Word {
    private String wordValue;
    private int ashValue;

    public Word(String wordValue) {
        this.wordValue = wordValue.toUpperCase();

        // Given that the ash value of a word never changes, we might as well calculate it HERE
        // This creates more overhead upon instantiation, but never again, so it's okay, I guess...
        // The ash value of a word is the sum of the letter (creation) values of all of its letters (see the Letter enum class)
        int val = 0;
        for(Letter l : toLetterArray()) {
            val += l.getAshCreateValue();
        }
        this.ashValue = val;
    }

    // Convert the word to a bunch of letters - no need to store the representaion at all
    public Letter[] toLetterArray() {
        int len = wordValue.length();
        Letter[] result = new Letter[len];
        for(int i = 0; i < len; i++) {
            result[i] = Letter.fromChar(wordValue.charAt(i));
        }
        return result;
    }

    public String toString() {
        return wordValue;
    }

    public int ashValue() {
        return ashValue;
    }

    // Since we're using words in a HashMap (see @GrabbleDict), we ought to overload comparison and hashing
    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof Word) && (((Word)obj).toString().equals(this.wordValue))) return true;
        else return false;
    }
    @Override
    public int hashCode() {
        // The hash code for a word is the same as the hash code of the actual string value of the word
        return this.wordValue.hashCode();
    }
}
