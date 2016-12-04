package eu.zerovector.grabble;

import com.google.android.gms.maps.model.LatLng;

// And a class for these, because sure, why not...
public class Placemark {
    private int pointID;
    private Letter letter;
    private LatLng coords;

    public Placemark(int pointID, Letter letter, LatLng coords) {
        this.pointID = pointID;
        this.letter = letter;
        this.coords = coords;
    }

    // Ease-of-use constructor that implicitly creates a LatLng (LatLng uses doubles too, I checked)
    public Placemark(int pointID, Letter letter, double latitude, double longitude) {
        this(pointID, letter, new LatLng(latitude, longitude));
    }

    public int getPointID() {
        return pointID;
    }

    public Letter getLetter() {
        return letter;
    }

    public LatLng getCoords() {
        return coords;
    }
}
