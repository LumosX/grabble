package eu.zerovector.grabble.Data;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


// Small little data structure to hold the data for a given faction
public class FactionData {
    // The name of the faction
    private String factionName;
    // The name of the faction's creator (just so they know who's boss)
    private String creatorName;
    // Completed words. Would rather not use BitSets and indices to avoid any possible confusion
    // We actually only need to check whether the word exists in the set. If yes, it's completed; else it's not.
    private HashSet<Word> completedWords;

    // The list of members of the faction
    private List<String> members;

    public FactionData(String factionName, String creatorName) {
        this.factionName = factionName;
        this.creatorName = creatorName;
        // Init others to default values
        completedWords = new HashSet<>();

        // These two are totally here because of future-proofing reasons
        members = new LinkedList<>();
        members.add(creatorName); // The leader auto-joins the faction when it's created (upon him registering)
    }

    public String getFactionName() {
        return factionName;
    }

    public HashSet<Word> getCompletedWords() {
        return completedWords;
    }

    public int getNumberOfCompletedWords() {
        return completedWords.size();
    }

    public void setCompletedWords(HashSet<Word> completedWords) {
        this.completedWords = completedWords;
    }

    public void addCompletedWord(Word word) {
        completedWords.add(word);
    }
}
