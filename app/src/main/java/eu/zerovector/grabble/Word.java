package eu.zerovector.grabble;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

// Since I'm actually abstracting things in classes, I might's well do this
// We'll also be using this class to represent the current word that needs completion, so we'll
// simply disregard state in all words.
public class Word {
    private String wordValue;
    private int ashValue;
    private boolean[] completionState;

    public Word(String wordValue) {
        this.wordValue = wordValue.toUpperCase();

        // Given that the ash value of a word never changes, we might as well calculate it HERE
        // This creates more overhead upon instantiation, but never again, so it's okay, I guess...
        // The ash value of a word is the sum of the letter (creation) values of all of its letters (see the Letter enum class)
        int val = 0;
        List<Letter> letters = toLetterList();
        for(Letter l : letters) {
            val += l.getAshCreateValue();
        }
        this.ashValue = val;
        // We ought to also set the completion status array up as well - set all to FALSE (uncollected)
        this.completionState = new boolean[letters.size()];
        for(int i = 0; i < completionState.length; i++) {
            completionState[i] = false;
        }
    }

    // Convert the word to a bunch of letters - no need to store the representation at all
    public List<Letter> toLetterList() {
        int len = wordValue.length();
        List<Letter> result = new ArrayList<>(len);
        for(int i = 0; i < len; i++) {
            Letter l = Letter.fromChar(wordValue.charAt(i));
            if (l != null) result.add(l); // This allows us to use other symbols which don't count
        }
        return result;
    }

    public boolean[] completionState() {
        return completionState;
    }

    public int numCompletedLetters() {
        int result = 0;
        for (boolean letter : completionState) {
            if (letter) result++;
        }
        return result;
    }

    public boolean isComplete() {
        boolean result = true;
        for (boolean letter : completionState) {
            if (!letter) return false;
        }
        return result;
    }

    public String toString() {
        return wordValue;
    }

    public String toColouredHTML(Context context) {
        String result = "";
        List<Letter> letters = toLetterList();
        int colourToUse;
        for (int i = 0, n = letters.size(); i < n; i++) {
            if (completionState[i]) colourToUse = R.color.White;
            else colourToUse = R.color.UI_AshGrey;
            String colour = Integer.toHexString(ContextCompat.getColor(context, colourToUse));
            colour = "#" + colour.substring(2); // this is sodding GARBAGE
            //Log.d("FUCK OFF", "colour = " + colour);
            result += "<font color=" + colour + ">" + letters.get(i).toString() + "</font>";
        }
        return result;
    }

    public int ashValue() {
        return ashValue;
    }

    public int length() { return wordValue.length(); }

    // This is to be called upon attempting to GRAB a letter and add it to the current word
    public boolean completeLetter(Letter newLetter) {
        List<Letter> letters = toLetterList();
        for (int i = 0, n = letters.size(); i < n; i++) {
            // We're simply "filling in" the first empty spot we can find.
            if (!completionState[i] && letters.get(i) == newLetter) {
                completionState[i] = true;
                return true;
            }
        }
        return false;
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
