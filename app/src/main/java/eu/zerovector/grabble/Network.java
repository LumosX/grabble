package eu.zerovector.grabble;

import java.util.ArrayList;
import java.util.List;

// A static class that shall hold all network-related functionality.
public final class Network {
    // We'll make a singleton, because this stupid language neither allows me to define
    // static methods in interfaces, nor does it allow me to use them here...
    // I wanted to use an interface like a human being, but I guess I won't...
    private static Network instance;
    private Network() { } // Anti-instantiation measures
    // There also aren't any implicit properties in this language. Really? I need to use a get* method? Pffft.
    private static Network getInstance() {
        if (instance == null) instance = new Network();
        return instance;
    }



    // CRAP TO REMOVE IN THE REAL VERSION
    static List<PlayerData> playersThatExist = new ArrayList<>();
    static List<FactionData> factionsThatExist = new ArrayList<>();




    // NETWORK FUNCTIONALITY
    public static boolean Login(String username, String password) {
        boolean result = false;
        for (PlayerData p : playersThatExist) {
            if (p.getUsername().equals(username) && p.getPassword().equals(password)) result = true;
        }
        return result;
    }

    public static final String REGISTER_SUCCESSFUL = "SUCCESS";
    public static String Register(PlayerData registrant, String confirmPass) {
        // We return the error message as a string, because we're hardcore like that

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

        // If all OK, register player:
        playersThatExist.add(registrant);
        factionsThatExist.add(new FactionData(registrant.getCreatedFactionName(), registrant.getUsername()));
        return REGISTER_SUCCESSFUL;
    }


    public static boolean GetLetter(int location) {
        return false; // TODO called when collecting a letter
    }
}


