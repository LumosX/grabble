package eu.zerovector.grabble.Data;

import java.util.LinkedList;
import java.util.List;


// Small little data structure to hold the data for a given faction
public class FactionData {
    // The name of the faction
    String factionName;
    // The name of the faction's creator (just because)
    String creatorName;
    //

    // The list of members of the faction
    List<String> members;

    public FactionData(String factionName, String creatorName) {
        this.factionName = factionName;
        this.creatorName = creatorName;
        // Init others to default values
        //wordProgress = new BitSet(Game.getDictSize());
        members = new LinkedList<>();
        members.add(creatorName); // The leader auto-joins the faction when it's created (upon him registering)
    }

}
