package eu.zerovector.grabble;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashSet;

import eu.zerovector.grabble.Data.FactionData;
import eu.zerovector.grabble.Data.PlayerData;
import eu.zerovector.grabble.Data.Word;
import eu.zerovector.grabble.Utils.GrabbleAPIException;

// A static class that shall hold all network-related functionality.
public final class Network {
    private static Type playerDataHSType = new TypeToken<HashSet<PlayerData>>(){}.getType();
    private static Type factionDataHSType = new TypeToken<HashSet<FactionData>>(){}.getType();
    private static HashSet<PlayerData> playersThatExist;
    private static HashSet<FactionData> factionsThatExist;
    private static String playerDataFilename = "players.dat";
    private static String factionDataFilename = "factions.dat"; // Factions retained for scalability purposes

    // testing stuff
//    static {
//        playersThatExist.add(new PlayerData("test", "Jimmy", "test", "Jimmy's Angles", Alignment.Openers));
//    }


    // NETWORK FUNCTIONALITY
    public static PlayerData Login(Context caller, String email, String password) throws GrabbleAPIException {
        HashSet<PlayerData> players = GetSavedPlayers(caller);
        for (PlayerData p : players) {
            if (p.getEmail().equals(email)) {
                if (p.getPassword().equals(password)) return p;
                else throw new GrabbleAPIException("Incorrect password.");
            }
        }
        throw new GrabbleAPIException("Details not found.");
    }

    public static void Register(Context caller, PlayerData registrant, String confirmPass) throws GrabbleAPIException {
        // If email is shoddy, throw
        // Inspired by StackOverflow: http://stackoverflow.com/questions/624581/what-is-the-best-java-email-address-validation-method
        if (!registrant.getEmail().matches("^.+@.+(\\.[^\\.]+)+$"))
            throw new GrabbleAPIException("Email address not valid");

        // Bits and bobs lifted from StackOverflow: http://stackoverflow.com/questions/3802192/regexp-java-for-password-validation
        // If passwords don't match, throw
        if (!registrant.getPassword().equals(confirmPass))
            throw new GrabbleAPIException("Passwords don't match");
        // If password less than 6 chars, throw
        int MIN_PASS_LENGTH = 6; // (was 8, but 8 is too much of a hassle when testing)
        if (registrant.getPassword().length() < MIN_PASS_LENGTH)
            throw new GrabbleAPIException("Password must be at least " + MIN_PASS_LENGTH + " chars");
        // If password doesn't contain a digit, throw
        //if (!registrant.getPassword().matches(".*[0-9].*"))
        //    throw new GrabbleAPIException("Password must contain a digit (0-9)");
        // If password doesn't contain at least one uppercase letter, throw
        //if (!registrant.getPassword().matches(".*[A-Z].*"))
        //    throw new GrabbleAPIException("Password must contain an uppercase letter");
        // If password doesn't contain a special character, throw (nah, too much of a hassle)
        //if (!registrant.getPassword().matches(".*[@#$%^&+=*].*"))
        //    throw new GrabbleAPIException("Password must contain a special character (@, #, $, %, ^, &, +, =, *)");

        // Password OK, now check to see if the rest is OK
        // Username must be longer than 3 characters
        int MIN_NAME_LENGTH = 4;
        int MAX_NAME_LENGTH = 25; // and 25 at max, because I say so
        if (registrant.getUsername().length() < MIN_NAME_LENGTH)
            throw new GrabbleAPIException("Username must be at least " + MIN_NAME_LENGTH + " chars");
        if (registrant.getUsername().length() > MAX_NAME_LENGTH)
            throw new GrabbleAPIException("Username must be " + MAX_NAME_LENGTH + " chars at most");

        // Email and stuff mustn't be taken
        HashSet<PlayerData> players = GetSavedPlayers(caller);
        for (PlayerData p : players) {
            if (p.getEmail().equals(registrant.getEmail()))
                throw new GrabbleAPIException( "Email address already registered");
            if (p.getUsername().equals(registrant.getUsername()))
                throw new GrabbleAPIException( "Username already taken");
            if (p.getCreatedFactionName().equals(registrant.getCreatedFactionName()))
                throw new GrabbleAPIException( "Faction name already taken");
        }

        // Faction name
        int MIN_FAC_NAME_LEN = 6;
        if (registrant.getCreatedFactionName().length() < MIN_FAC_NAME_LEN)
            throw new GrabbleAPIException("Faction name must be at least " + MIN_FAC_NAME_LEN + " chars");

        // If all OK, register player:
        players.add(registrant); // No need ot check for duplicates: we've forbidden them beforehand
        SaveAllPlayers(caller, players);

        // Also register the faction:
        HashSet<FactionData> factions = GetSavedFactions(caller);
        factions.add((new FactionData(registrant.getCreatedFactionName(), registrant.getUsername())));
        SaveAllFactions(caller, factions);
    }


    public static void SavePlayerData(Context caller, PlayerData curPlayer) {
        Log.d("NETWORK", "Saving data");
        HashSet<PlayerData> players = GetSavedPlayers(caller);
        players.remove(curPlayer);
        players.add(curPlayer);
        SaveAllPlayers(caller, players);
    }

    public static void SaveFactionData(Context caller, FactionData curFaction) {
        Log.d("NETWORK", "Saving faction data");
        HashSet<FactionData> factions = GetSavedFactions(caller);
        factions.remove(curFaction);
        factions.add(curFaction);
        SaveAllFactions(caller, factions);
    }

    public static void CompleteWord(Word completedWord) {
        // TODO NOTIFY FACTION THAT A WORD HAS BEEN COMPLETED
    }

    public static int[] GetIncompleteWordIndices() {
        return new int[]{};
    }


    // Now - a couple of IO operations to make our lives easier
    // These will basically retain state in memory, with I/O iff necessary
    // That's because there isn't going to be a lot of profiles active at any point in time, so...
    private static HashSet<PlayerData> GetSavedPlayers(final Context caller) {
        if (playersThatExist != null) return playersThatExist;

        // Only read if we don't know have the data on hand
        // Save to file
        FileInputStream inputStream;
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream in = caller.openFileInput(playerDataFilename);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            Toast.makeText(caller, "Error loading player data: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new HashSet<>();
        }
        return new Gson().fromJson(sb.toString(), playerDataHSType);
    }

    private static void SaveAllPlayers(final Context caller, final HashSet<PlayerData> newPlayers) {
        // This is all we need, as we're saving stuff in memory.
        playersThatExist = newPlayers;

        // Writing to file shall be asynchronous to help with stuff.
        new AsyncJob.AsyncJobBuilder<Boolean>().doInBackground(new AsyncJob.AsyncAction<Boolean>() {
            @Override
            public Boolean doAsync() {
                boolean result = true;
                // Save to file
                String data = new Gson().toJson(newPlayers, playerDataHSType);
                FileOutputStream outputStream;
                try {
                    outputStream = caller.openFileOutput(playerDataFilename, Context.MODE_PRIVATE);
                    outputStream.write(data.getBytes());
                    outputStream.close();
                } catch (Exception e) {
                    Toast.makeText(caller, "Error saving progress: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    result = false;
                }
                // DONE
                return result;
            }
        }).create().start();

    }


    // The same, but for factions
    private static HashSet<FactionData> GetSavedFactions(final Context caller) {
        if (factionsThatExist != null) return factionsThatExist;

        // Only read if we don't know have the data on hand
        // Save to file
        FileInputStream inputStream;
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream in = caller.openFileInput(factionDataFilename);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            Toast.makeText(caller, "Error loading faction data: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return new HashSet<>();
        }
        return new Gson().fromJson(sb.toString(), factionDataHSType);
    }

    private static void SaveAllFactions(final Context caller, final HashSet<FactionData> newFactions) {
        // This is all we need, as we're saving stuff in memory.
        factionsThatExist = newFactions;

        // Writing to file shall be asynchronous to help with stuff.
        new AsyncJob.AsyncJobBuilder<Boolean>().doInBackground(new AsyncJob.AsyncAction<Boolean>() {
            @Override
            public Boolean doAsync() {
                boolean result = true;
                // Save to file
                String data = new Gson().toJson(newFactions, playerDataHSType);
                FileOutputStream outputStream;
                try {
                    outputStream = caller.openFileOutput(factionDataFilename, Context.MODE_PRIVATE);
                    outputStream.write(data.getBytes());
                    outputStream.close();
                } catch (Exception e) {
                    Toast.makeText(caller, "Error saving progress: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    result = false;
                }
                // DONE
                return result;
            }
        }).create().start();

    }

}


