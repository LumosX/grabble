package eu.zerovector.grabble;

import java.util.ArrayList;
import java.util.List;

// A static class that shall hold all network-related functionality.
public final class Network {
    // TODO: maintain list of network requests (also send System.currentTimeMillis() as request ID)
    // TODO: lots and lots of stuff


    // CRAP TO REMOVE IN THE REAL VERSION
    private static List<PlayerData> playersThatExist = new ArrayList<>();
    private static List<FactionData> factionsThatExist = new ArrayList<>();

    // testing stuff
    static {
        playersThatExist.add(new PlayerData("test", "Jimmy", "test", "Jimmy's Angles", Alignment.Openers));
    }


    // NETWORK FUNCTIONALITY
    public static PlayerData Login(String email, String password) {
        // FIXME: ADD REAL NETWORKED FUNCTIONALITY
        for (PlayerData p : playersThatExist) {
            if (p.getEmail().equals(email) && p.getPassword().equals(password)) return p;
        }
        return null;
    }

    public static final String REGISTER_SUCCESSFUL = "SUCCESS";
    public static String Register(PlayerData registrant, String confirmPass) {
        // We return the error message as a string, because we're hardcore like that

        // If email is shoddy, throw
        // Inspired by StackOverflow: http://stackoverflow.com/questions/624581/what-is-the-best-java-email-address-validation-method
        if (!registrant.getEmail().matches("^.+@.+(\\.[^\\.]+)+$")) return "Email address not valid";

        // Bits and bobs lifted from StackOverflow: http://stackoverflow.com/questions/3802192/regexp-java-for-password-validation
        // If passwords don't match, throw
        if (!registrant.getPassword().equals(confirmPass)) return "Passwords don't match";
        // If password less than 6 chars, throw
        int MIN_PASS_LENGTH = 6; // (was 8, but 8 is too much of a hassle when testing)
        if (registrant.getPassword().length() < MIN_PASS_LENGTH) return "Password must be at least " + MIN_PASS_LENGTH + " chars";
        // If password doesn't contain a digit, throw
        //if (!registrant.getPassword().matches(".*[0-9].*")) return "Password must contain a digit (0-9)";
        // If password doesn't contain at least one uppercase letter, throw
        //if (!registrant.getPassword().matches(".*[A-Z].*")) return "Password must contain an uppercase letter";
        //// If password doesn't contain a special character, throw (nah, too much of a hassle)
        //if (!registrant.getPassword().matches(".*[@#$%^&+=*].*")) return "Password must contain a special character (@, #, $, %, ^, &, +, =, *)";

        // Password OK, now check to see if the email address already exists
        for (PlayerData p : playersThatExist) {
            if (p.getEmail().equals(registrant.getEmail())) return "Email address already registered";
            if (p.getUsername().equals(registrant.getUsername())) return "Username already taken";
            if (p.getCreatedFactionName().equals(registrant.getCreatedFactionName())) return "Faction name already taken";
        }

        // Now the other considerations
        // Username must be longer than 3 characters
        int MIN_NAME_LENGTH = 4;
        int MAX_NAME_LENGTH = 25; // and 25 at max, because I say so
        if (registrant.getUsername().length() < MIN_NAME_LENGTH) return "Username must be at least " + MIN_NAME_LENGTH + " chars";
        if (registrant.getUsername().length() > MAX_NAME_LENGTH) return "Username must be " + MAX_NAME_LENGTH + " chars at most";

        // Faction name
        int MIN_FAC_NAME_LEN = 6;
        if (registrant.getCreatedFactionName().length() < MIN_FAC_NAME_LEN)
            return "Faction name must be at least " + MIN_FAC_NAME_LEN + " chars";

        // If all OK, register player:
        // FIXME: ADD REAL NETWORKED FUNCTIONALITY
        playersThatExist.add(registrant);
        factionsThatExist.add(new FactionData(registrant.getCreatedFactionName(), registrant.getUsername()));
        return REGISTER_SUCCESSFUL;
    }


    // This is called upon taking some letters. We need the server to remove them from the player's list.
    public static void RequestLetterGrab(List<Integer> pointIDs) {
        return;
    }

    public static void CompleteWord(Word completedWord) {
        // TODO NOTIFY FACTION THAT A WORD HAS BEEN COMPLETED
    }

    public static int[] GetIncompleteWordIndices() {
        return new int[]{};
    }

}


