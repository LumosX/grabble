package eu.zerovector.grabble;

import com.google.android.gms.maps.model.LatLng;

// And a class for these, because sure, why not...
public class Placemark {
    private int pointID;
    private Letter letter;
    private LatLng coords;
    private int segmentID; // We're segmenting the map into "chunks" for easier state checking

    public Placemark(int pointID, Letter letter, LatLng coords, int segmentID) {
        this.pointID = pointID;
        this.letter = letter;
        this.coords = coords;
        this.segmentID = segmentID;
    }

    // Ease-of-use constructor that implicitly creates a LatLng (LatLng uses doubles too, I checked)
    public Placemark(int pointID, Letter letter, double latitude, double longitude, int segmentID) {
        this(pointID, letter, new LatLng(latitude, longitude), segmentID);
    }

    public int pointID() {
        return pointID;
    }

    public Letter letter() {
        return letter;
    }

    public LatLng coords() {
        return coords;
    }

    public int segmentID() { return segmentID; }
}
