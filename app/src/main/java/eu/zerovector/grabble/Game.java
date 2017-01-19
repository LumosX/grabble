package eu.zerovector.grabble;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import eu.zerovector.grabble.Activity.UpdateUIListener;
import eu.zerovector.grabble.Data.Alignment;
import eu.zerovector.grabble.Data.GrabbleDict;
import eu.zerovector.grabble.Data.Letter;
import eu.zerovector.grabble.Data.MapSegments;
import eu.zerovector.grabble.Data.Placemark;
import eu.zerovector.grabble.Data.PlayerData;
import eu.zerovector.grabble.Data.Word;
import eu.zerovector.grabble.Data.XPUtils;
import eu.zerovector.grabble.Utils.GrabbleAPIException;
import eu.zerovector.grabble.Utils.RandomNameGenerator;

// A static class that shall hold all core gameplay data and helper functions
public final class Game {

    // HARDCODED GAME CONSTANTS
    public static final int GLOBAL_ACTIVITY_RESULT_KILL = 0;
    public static final int GLOBAL_ACTIVITY_RESULT_LOGOUT = 1;

    // UI UPDATE LISTENERS
    private static List<UpdateUIListener> uiListeners = new ArrayList<>();

    // And the actual data representation of the player that's currently logged in
    private static PlayerData currentPlayer;
    public static PlayerData currentPlayerData() {
        if (isPlayerLoggedIn) return currentPlayer;
        return null;
    }

    // Game state checking vars - plus public getters, just in case
    private static boolean isDictLoaded = false;
    private static boolean isMapDataLoaded = false;
    public static boolean isDictLoaded() {
        return isDictLoaded;
    }
    public static boolean isMapDataLoaded() {
        return isMapDataLoaded;
    }

    // Now some for the actual player:
    private static boolean isPlayerLoggedIn = false;
    public static boolean isIsPlayerLoggedIn() {
        return isPlayerLoggedIn;
    }


    private static LatLngBounds mapBounds; // The bounds of the map.
    private static LatLng mapCentre; // The centre of the map
    public static LatLngBounds getMapBounds() {
        if (isMapDataLoaded) return mapBounds;
        else return null;
    }

    public static LatLng getMapBoundsCentre() {
        if (isMapDataLoaded) return mapCentre;
        else return null;
    }


    // Grabble dict stuff
    private static final int DICT_RES_NAME = R.raw.grabbledict;
    private static GrabbleDict grabbleDict = new GrabbleDict();
    public static int getDictSize() {
        if (isDictLoaded) return grabbleDict.size();
        // This gets called when instantiating new Factions. In the impossible case that it happens before the
        // dict is loaded, return the total maximum size of the dictionary (including duplicates)
        else return 23869;
    }
    public static GrabbleDict getGameDictionary() {
        if (isDictLoaded) return grabbleDict;
        else return null;
    }

    // Daily map stuff
    private static List<Placemark> dailyMap = new ArrayList<>();
    // HARDCODED VARIABLES THAT WE DON'T NEED TO EVER CHANGE
    private static final String mapFilePrefix = "http://www.inf.ed.ac.uk/teaching/courses/selp/coursework/";
    private static final String mapFileSuffix = ".kml";
    // Map segmentation data. We'll break the placemarks in "chunks" to avoid finding distances to all of them.
    // Again, this is sort of akin to what one does in procedural isosurface generation. I do have a soft spot for that sort of thing.

    // Minimal length in metres of a map segment. Best if it's always greater than the maximum "effective radius" of the player.
    public static final int MAP_SEGMENT_MIN_LENGTH = 25;
    private static MapSegments mapSegments;

    public static List<Placemark> getDailyPlacemarks() {
        if (isMapDataLoaded) return dailyMap;
        else return null;
    }

    public static MapSegments mapSegmentData() {
        if (isMapDataLoaded) return mapSegments;
        else return null;
    }

    // This needs to be asynchronous, for obvious reasons
    private static final String INIT_RESULT_SUCCESS = "SUCCESS";
    public static void InitialSetup(final Context appContext, final int intialDelaySeconds) {
        new AsyncJob.AsyncJobBuilder<InitResult>().doInBackground(new AsyncJob.AsyncAction<InitResult>() {
            private int initialDelay = intialDelaySeconds;

            @Override
            public InitResult doAsync() {
                // Do the delaying if need be.
                if (initialDelay > 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // READ DICTIONARY FILE
                boolean dictOK = true;
                String dictError = "";
                if (!isDictLoaded) {
                    // File's in the 'raw' folder, so let's get it up
                    InputStream dictInput = appContext.getResources().openRawResource(DICT_RES_NAME);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(dictInput));
                    // We'll set the list of words up, and the list of word values as well

                    try {
                        // Using our fabulous custom-made class, we can simply add all words to the "global" GrabbleDict.
                        // It uses a HashMap, allegedly guaranteeing that duplicates are removed.
                        grabbleDict = new GrabbleDict();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            grabbleDict.addWord(new Word(line.toUpperCase()));
                        }
                    } catch (Exception e) {
                        dictOK = false; // Make sure we know something's broken
                        dictError = e.getMessage();
                    }
                }

                // DOWNLOAD AND PARSE THE DAILY MAP
                boolean mapDataOK = true;
                String mapDataError = "";
                if (!isMapDataLoaded) {
                    dailyMap = new ArrayList<>();
                    try {
                        // Must find out what day of the week it is, ergo what file we're looking for
                        URL url = new URL(getDailyMapURL());
                        URLConnection conn = url.openConnection();
                        conn.connect();
                        // Do the actual downloading
                        InputStream mapInput = new BufferedInputStream(url.openStream(), 8192);
                        // Parse the stream into nodes
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(mapInput);
                        // Real XML
                        ParseXML(doc);
                        // Remember to close the stream in the end
                        mapInput.close();
                    } catch (Exception e) {
                        mapDataOK = false;
                        mapDataError = e.getMessage();
                    }
                }

                // DONE
                return new InitResult(dictOK, mapDataOK, dictError, mapDataError);
            }
        }).doWhenFinished(new AsyncJob.AsyncResultAction<InitResult>() {
            @Override
            public void onResult(InitResult result) {
                // Just call the thing that happens when the data loads
                onDataLoaded(result);

                // If something went wrong, try again in a bit.
                if (!result.bothOK()) {
                    int retry_delay_seconds = 10;
                    InitialSetup(appContext, retry_delay_seconds * 1000);

                    if (!result.mapDataOK()) {
                        Toast.makeText(appContext, "Failed to load map data: " + result.mapDataErrorMessage() +
                                                   "\n(Retrying in " + retry_delay_seconds + " sec.)", Toast.LENGTH_LONG).show();
                    }
                    else if (!result.dictOK()) {
                        Toast.makeText(appContext, "Failed to load dictionary: " + result.dictErrorMessage() +
                                                   "\n(Retrying in " + retry_delay_seconds + " sec)", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }).create().start();
    }

    // I'd rather use one of these
    private static class InitResult {
        private boolean dictOK;
        private boolean mapDataOK;
        private String dictErrorMessage;
        private String mapDataErrorMessage;

        public InitResult(boolean dictOK, boolean mapDataOK, String dictErrorMessage, String mapDataErrorMessage) {
            this.dictOK = dictOK;
            this.mapDataOK = mapDataOK;
            this.dictErrorMessage = dictErrorMessage;
            this.mapDataErrorMessage = mapDataErrorMessage;
        }
        public boolean dictOK() {
            return dictOK;
        }
        public boolean mapDataOK() {
            return mapDataOK;
        }

        public String mapDataErrorMessage() {
            return mapDataErrorMessage;
        }

        public String dictErrorMessage() {
            return dictErrorMessage;
        }

        public boolean bothOK() {
            return dictOK && mapDataOK;
        }
        public boolean bothNotOK() {
            return !bothOK();
        }
    }

    // A little helper.
    private static String getDailyMapURL() {
        String infix = "";
        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY: infix = "monday"; break;
            case Calendar.TUESDAY: infix = "tuesday"; break;
            case Calendar.WEDNESDAY: infix = "wednesday"; break;
            case Calendar.THURSDAY: infix = "thursday"; break;
            case Calendar.FRIDAY: infix = "friday"; break;
            case Calendar.SATURDAY: infix = "saturday"; break;
            case Calendar.SUNDAY: infix = "sunday"; break;
            default: infix = "ABORT ABORT NON-EXISTENT DAY!!!"; break;
        }
        return mapFilePrefix + infix + mapFileSuffix;
    }

    // And another little helper.
    private static void ParseXML(Document doc) {
        NodeList points = doc.getElementsByTagName("Placemark");

        // Prepare to segment later
        double minLat = Double.NaN, maxLat = Double.NaN;
        double minLon = Double.NaN, maxLon = Double.NaN;

        // Parse the actual thing, without segmenting it yet.
        // Using a sorted map for this step allows us to order points by ID and remove any potential erroneous duplicates.
        SortedMap<Integer, Placemark> unsegmentedPlacemarks = new TreeMap<>();
        for (int i = 0; i < points.getLength(); i++) {
            Element point = (Element)points.item(i);
            // Placemarks have only one 'name', 'description', and 'coordinates' sub-tags.
            // If there's more, I don't care.
            String pointID = point.getElementsByTagName("name").item(0).getTextContent(); // "Point X"
            char pointLetter = point.getElementsByTagName("description").item(0).getTextContent().charAt(0); // "X"
            String[] pointCoords = point.getElementsByTagName("coordinates").item(0).getTextContent().split(","); // "LNG,LAT,0"
            // Now we ought to finish the parsing
            int parsedID = Integer.parseInt(pointID.split(" ")[1]);
            double latitude = Double.parseDouble(pointCoords[1]);
            double longitude = Double.parseDouble(pointCoords[0]);
            // Whilst we're here, we should calculate the "bounds" of all placemarks, so to speak, so that we know what area the "zone" spans
            if (Double.isNaN(minLat) || minLat > latitude) minLat = latitude;
            if (Double.isNaN(maxLat) || maxLat < latitude) maxLat = latitude;
            if (Double.isNaN(minLon) || minLon > longitude) minLon = longitude;
            if (Double.isNaN(maxLon) || maxLon < longitude) maxLon = longitude;


            //Log.e("tag", "Lon diff = " + SphericalUtil.computeDistanceBetween(new LatLng(minLat, minLon), new LatLng(minLat, maxLon)));
            //Log.e("tag", "Lat diff = " + SphericalUtil.computeDistanceBetween(new LatLng(minLat, minLon), new LatLng(maxLat, minLon)));

            // Unfortunately, we'll have to loop through all placemarks a second time in order to have already calculated the bounds.
            unsegmentedPlacemarks.put(parsedID, new Placemark(parsedID, Letter.fromChar(pointLetter), latitude, longitude, 0));
        }
        // After knowing the bounds of the game area, add a little margin to help with any possible floating point imprecision issues
        // We do this by initialising the map segments variable, and rounding our bounds to 4 decimal places.
        // 4 decimal places ≈ 10 m accuracy (and I'd like a margin of 10 metres).
        mapSegments = new MapSegments(minLat, maxLat, minLon, maxLon, "#.####");

        // We'll also create the bounds here.
        mapBounds = new LatLngBounds(new LatLng(minLat, minLon), new LatLng(maxLat, maxLon));
        mapCentre = new LatLng((maxLat - minLat) / 2, (maxLon - minLon) / 2);

        // Finally, assign segments to all points. Sheeeeesh, we're done.
        dailyMap = new ArrayList<>();
        for(Placemark point : unsegmentedPlacemarks.values()) { // This also does them in order of their IDs now
            // GARBAGE FOR THE GARBAGE (collector) GOD
            dailyMap.add(new Placemark(point.pointID(), point.letter(), point.coords(), mapSegments.computeSegment(point.coords())));
        }

        // DONE AND DONE!

    }

    // And another one, this time for faction name randomisation.
    // Writing stuff like this is quite fun (and also mostly pointless)
    public static String getRandomFactionName(Alignment alignment) {
        // The closers are basically the "good guys", and the openers are the "bad guys".
        // The names should sort of attempt to reflect that.
        // Ended up making a class for this.
        return RandomNameGenerator.getFactionName(alignment);
    }

    // Log-in caretaking
    public static void onLogin(PlayerData player) {
        currentPlayer = player;
        isPlayerLoggedIn = true;

        // If the data managed to load before the log-in process, check if the player needs a new word
        if (isDictLoaded && isMapDataLoaded) checkRequestNewWord();
    }
    public static void onLogout() {
        isPlayerLoggedIn = false;
        currentPlayer = null;
    }

    // To be called once we're sure EVERYTHING is good.
    private static void onDataLoaded(InitResult result) {
        isDictLoaded = result.dictOK;
        isMapDataLoaded = result.mapDataOK;

        // if the player managed to login before the data loads, check whether he/she needs a new word
        if (isPlayerLoggedIn) checkRequestNewWord();
    }

    // We can't have a situation where there isn't a word, so we need to get a new one in case we need it
    public static void checkRequestNewWord() {
        // However, this is only allowed to happen once the dict has been loaded
        if (!isDictLoaded) return;

        // We need to see if the current word has been completed, and reassign a new word if necessary
        Word oldWord = currentPlayer.getCurrentWord();
        if (oldWord == null || oldWord.isComplete()) {
            // TODO CHECK FACTION LIST FOR UNCOMPLETED WORDS
            // Network.GetIncompleteWordIndices();
            Iterator iter = grabbleDict.entrySet().iterator(); // This iterator stuff is bizarre
            List<Word> words = new ArrayList<>();
            while (iter.hasNext()) {
                Map.Entry pair = (Map.Entry)iter.next();
                // Incomplete yet words get added to the list
                if (!(boolean)pair.getValue()) words.add((Word)pair.getKey());
            }
            // Get a random word and assign it as the new one
            currentPlayer.setCurrentWord(words.get(new Random(System.currentTimeMillis()).nextInt(words.size())));
        }


    }

    // This is called whenever we approach a letter within grabbing distance and try to take it.
    public static boolean grabLetter(Letter letter, XPUtils.DataPair extraDetails) {
        boolean result = false;

        XPUtils.LevelDetails levelDetails = extraDetails.levelDetails();
        XPUtils.TraitSet perks = extraDetails.traitSet();

        // Try completing a letter in the current word first
        if (currentPlayer.getCurrentWord().completeLetter(letter)) {
            result = true;
        }
        // Otherwise, add to inventory
        else if (currentPlayer.getInventory().addLetter(letter, perks.getInvCapacity())) {
            result = true;
        }

        if (!result) return false;


        // If something about the player changed, force an UI update, plus other things
        // We're using one of these to notify the listeners of anything specific they need to be doing
        EnumSet<UpdateUIListener.Code> updateCodes = EnumSet.noneOf(UpdateUIListener.Code.class);
        // And we'll need this one too - getting it after any letters have been added
        Word oldWord = currentPlayer.getCurrentWord();

        // First, check to see whether the player requires an extra ash
        int letForAsh = currentPlayer.getLettersUntilExtraAsh() - 1; // decrement automatically
        currentPlayer.decrementLettersUntilExtraAsh(1);
        if (letForAsh == 0) {
            currentPlayer.addAsh(1);
            currentPlayer.setLettersUntilExtraAsh(perks.getNumLettersForOneAsh());
            updateCodes.add(UpdateUIListener.Code.EXTRA_ASH_GRANTED);
        }

        // Get XP with every letter. People like seeing progress, as tiny as it may be.
        int oldXP = currentPlayer.getXP();
        currentPlayer.addXP(letter.getAshCreateValue());

        // If we've completed a word, grant the DESTROY value of the word as an Ash reward
        // (otherwise the player can just keep pumping words with a big enough balance, and break even)
        if (currentPlayer.getCurrentWord().isComplete()) {
            updateCodes.add(UpdateUIListener.Code.WORD_COMPLETED);
            onWordCompleted();
        }

        // And finally, if we've levelled up, grant the extra ash reward (if there is one)
        if (oldXP < levelDetails.nextLevelXP() && oldXP + letter.getAshCreateValue() >= levelDetails.nextLevelXP()) {
            // We need to get the traitSet for the next level in order to find out how much ash it gives us
            int ashReward = XPUtils.getPerksForLevel(levelDetails.level() + 1).getRawAshReward();
            currentPlayer.addAsh(ashReward);
            updateCodes.add(UpdateUIListener.Code.LEVEL_INCREASED);
        }


        // Always force a global update of any interface listeners
        Log.d("GAME", "update codes are" + updateCodes.toString());
        callGlobalUIUpdate(updateCodes, oldWord);
        return true;
    }

    // It had to be done...
    private static void onWordCompleted() {
        currentPlayer.addAsh(currentPlayer.getCurrentWord().ashDestroyValue());
        Network.CompleteWord(currentPlayer.getCurrentWord());
        checkRequestNewWord();
    }

    // The Ashery and Crematorium need to work from here, otherwise we end up tossing the current state all over the place
    private static final int ASHERY_REQUEST_ERROR_TOO_POOR = 0;
    private static final int ASHERY_REQUEST_LETTER_INTO_WORD = 1;
    private static final int ASHERY_REQUEST_LETTER_INTO_INVENTORY = 2;
    private static final int ASHERY_REQUEST_UNIDENTIFIED_ERROR = 3;
    public static void onAsheryRequest(Letter letter) throws GrabbleAPIException {
        // As we did when grabbing a letter, we need to check whether we can fill a spot in the current word first
        int invCap = XPUtils.getAllDetailsForXP(currentPlayer.getXP()).traitSet().getInvCapacity();
        int result = ASHERY_REQUEST_UNIDENTIFIED_ERROR;

        // Before everything, check whether we've got enough ash
        if (currentPlayer.getAsh() < letter.getAshCreateValue()) result = ASHERY_REQUEST_ERROR_TOO_POOR;
        // Try completing a letter in the current word first
        else if (currentPlayer.getCurrentWord().completeLetter(letter)) result = ASHERY_REQUEST_LETTER_INTO_WORD;
        // Otherwise, add to inventory
        else if (currentPlayer.getInventory().addLetter(letter, invCap)) result = ASHERY_REQUEST_LETTER_INTO_INVENTORY;

        // Throw errors if necessary
        if (result == ASHERY_REQUEST_ERROR_TOO_POOR) throw new GrabbleAPIException("Insufficient Ash");
        else if (result == ASHERY_REQUEST_UNIDENTIFIED_ERROR) throw new GrabbleAPIException("Could not complete transaction");

        // If no errors are necessary, DO THE WORK
        Game.currentPlayerData().removeAsh(letter.getAshCreateValue());

        EnumSet<UpdateUIListener.Code> updateCodes = EnumSet.noneOf(UpdateUIListener.Code.class);
        // However, if we DID complete a word (that's why the Ashery is more complicated)...
        // ... we need to do signal this before the UI update
        if (currentPlayer.getCurrentWord().isComplete()) {
            updateCodes.add(UpdateUIListener.Code.WORD_COMPLETED);
            onWordCompleted();
        }

        // MUST UPDATE UI
        callGlobalUIUpdate(EnumSet.noneOf(UpdateUIListener.Code.class), currentPlayer.getCurrentWord());

        // And finally, I'll cheekily take advantage of the fact that this function throws exceptions...
        // Otherwise the player will probably wonder why the Selector "didn't update" its count.
        if (result == ASHERY_REQUEST_LETTER_INTO_WORD) {
            throw new GrabbleAPIException("Added letter into current word!");
        }
    }

    public static void onCrematoriumRequest(Letter letter) throws GrabbleAPIException {
        // The Crematorium is easy - we can't complete words through it, we can only gain Ash.
        if (currentPlayer.getInventory().removeLetter(letter, letter.getNumToDestroy())) {
            currentPlayer.addAsh(letter.getAshDestroyValue());

            // MUST UPDATE UI
            callGlobalUIUpdate(EnumSet.noneOf(UpdateUIListener.Code.class), currentPlayer.getCurrentWord());
        }
        else {
            throw new GrabbleAPIException("Insufficient letters to burn");
        }
    }

    public static void addUIListener(UpdateUIListener listener) {
        uiListeners.add(listener);
    }

    private static void callGlobalUIUpdate(EnumSet<UpdateUIListener.Code> codes, Word oldWord) {
        for (UpdateUIListener listener : uiListeners) {
            listener.onUpdateUIReceived(codes, oldWord);
        }
    }
}
