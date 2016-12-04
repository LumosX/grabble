package eu.zerovector.grabble;

import android.content.Context;
import android.widget.Toast;

import com.arasthel.asyncjob.AsyncJob;

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
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

// A static class that shall hold all core gameplay data and helper functions
public final class Game {
    // Again, we're using a singleton and a final class without an interface, because Java is terrible
    private static Game instance;
    // Oh, how I wish C# properties existed here...
    private static Game getInstance() {
        if (instance == null) instance = new Game();
        return instance;
    }
    private Game() {
        // This will trigger only once, upon generating the singleton instance.
        // Ergo, we can make sure the game knows when it's actually ready to do things
        isDictLoaded = false;
        isMapLoaded = false;
    }

    // Game state checking vars - plus public getters, just in case
    private static boolean isDictLoaded = false;
    private static boolean isMapLoaded = false;
    public static boolean isDictLoaded() {
        return isDictLoaded;
    }
    public static boolean isMapLoaded() {
        return isMapLoaded;
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

    // Daily map stuff
    private static List<Placemark> dailyMap = new ArrayList<>();
    // HARDCODED VARIABLES THAT WE DON'T NEED TO EVER CHANGE
    private static final String mapFilePrefix = "http://www.inf.ed.ac.uk/teaching/courses/selp/coursework/";
    private static final String mapFileSuffix = ".kml";

    // This needs to be asynchronous, for obvious reasons
    public static void InitialSetup(final Context appContext) {
        new AsyncJob.AsyncJobBuilder<Boolean>().doInBackground(new AsyncJob.AsyncAction<Boolean>() {
            @Override
            public Boolean doAsync() {
                boolean result = true;

                // READ DICTIONARY FILE
                // File's in the 'raw' folder, so let's get it up
                InputStream dictInput = appContext.getResources().openRawResource(DICT_RES_NAME);
                BufferedReader reader = new BufferedReader(new InputStreamReader(dictInput));
                // We'll set the list of words up, and the list of word values as well

                try {
                    // This is utterly ridiculous, by the way - so convoluted...!

                    // Using our fabulous custom-made class, we can simply add all words to the "global" GrabbleDict.
                    // It uses a HashMap, allegedly guaranteeing that duplicates are removed.
                    grabbleDict = new GrabbleDict();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        grabbleDict.addWord(new Word(line.toUpperCase()));
                    }


                } catch (Exception ex) {
                    result = false; // Make sure we know something's broken
                }

                // DOWNLOAD AND PARSE THE DAILY MAP
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
                    e.printStackTrace();
                    result = false;
                }

                // DONE
                return result;
            }
        }).doWhenFinished(new AsyncJob.AsyncResultAction<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                // The game now knows its dictionary and map have been loaded without issues
                if (result) {
                    isDictLoaded = true;
                    isMapLoaded = true;

                    Toast.makeText(appContext, "game dict size = " + grabbleDict.size() +
                            "\ntotal ash possible = " + grabbleDict.totalAshValue(), Toast.LENGTH_LONG).show();
                }
                // else TODO: Throw massive errors in a dialog box, quit application
            }
        }).create().start();
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
        dailyMap = new ArrayList<>();
        NodeList points = doc.getElementsByTagName("Placemark");
        for (int i = 0; i < points.getLength(); i++) {
            Element point = (Element)points.item(i);
            // Placemarks have only one 'name', 'description', and 'coordinates' sub-tags.
            // If there's more, I don't care.
            String pointID = point.getElementsByTagName("name").item(0).getTextContent(); // "Point X"
            char pointLetter = point.getElementsByTagName("description").item(0).getTextContent().charAt(0); // "X"
            String[] pointCoords = point.getElementsByTagName("coordinates").item(0).getTextContent().split(","); // "LNG,LAT,0"
            // Now we ought to finish the parsing
            int parsedID = Integer.parseInt(pointID.split(" ")[1]);
            // of course the coursework kml files use a format that's not the same as what everyone else uses (LNG,LAT -> LAT,LNG)
            double latitude = Double.parseDouble(pointCoords[1]);
            double longitude = Double.parseDouble(pointCoords[0]);

            dailyMap.add(new Placemark(parsedID, Letter.fromChar(pointLetter), latitude, longitude));
        }
    }

    // Add another one
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String string = "";
        while(s.hasNext()){
            string += s.next();
        }
        return string;
    }


}
