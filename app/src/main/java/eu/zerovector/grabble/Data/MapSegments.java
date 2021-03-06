package eu.zerovector.grabble.Data;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import eu.zerovector.grabble.Game;
import eu.zerovector.grabble.Utils.MathUtils;

// This helps managing the map segmentation code. Relies on the const segment size in Game.java.
// Would've done it as a nested class, but apparently you can't call a non-static nested class from a static class. duck this language.
public class MapSegments {
    private int[][] segmentArray; // Holds segment IDs, just so we're not doing any dark magic in order to retrieve them.
    private double minLatitude, minLongitude, maxLatitude, maxLongitude; // Game area bounds.
    private double totalLatitude, totalLongitude; // Total distances (in coordinate degrees). Just to save computation time.
    private double deltaLatitude, deltaLongitude; // Lat/lon deltas (distance per segment, in degrees). Same reason.
    private int numLatSegments, numLonSegments; // And also remember how many segments there are, because why not.

    public MapSegments(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
        this(minLatitude, maxLatitude, minLongitude, maxLongitude, "");
    }

    public MapSegments(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude, String formatString) {
        // If we aren't going to do any formatting, just init the values.
        if (formatString.equals("")) {
            this.minLatitude = minLatitude;
            this.maxLatitude = maxLatitude;
            this.minLongitude = minLongitude;
            this.maxLongitude = maxLongitude;
        }
        // Otherwise, add a little margin according to the precision of the formatting string.
        else {
            DecimalFormat df = new DecimalFormat(formatString);
            df.setRoundingMode(RoundingMode.FLOOR); // The mins need to be FLOORED, minimising them even more (adding the margin)
            this.minLatitude = Double.parseDouble(df.format(minLatitude));
            this.minLongitude = Double.parseDouble(df.format(minLongitude));
            df.setRoundingMode(RoundingMode.CEILING); // The maxes need to be CEILED, maximising them even more (to add the margin)
            this.maxLatitude = Double.parseDouble(df.format(maxLatitude));
            this.maxLongitude = Double.parseDouble(df.format(maxLongitude));
        }

        // After we're done, we need to find out how many segments we'll actually have in the grid. It needn't be square.
        // Basically, rows = latitude and cols = longitude, right?
        // But we'll do it in a helper method, just to avoid messing up variables and shit.
        InitSegments();
    }

    private void InitSegments() {
        // Find out how many segments we need by converting the total distance to metres first. We can cast the doubles
        // to ints, because we want the segments to be as large as possible, and as few as possible => always FLOOR
        // Actual game area is about 507x400 metres, by the way.
        numLatSegments = (int)(SphericalUtil.computeDistanceBetween(
                new LatLng(minLatitude, minLongitude), new LatLng(maxLatitude, minLongitude)) / Game.MAP_SEGMENT_MIN_LENGTH);
        numLonSegments = (int)(SphericalUtil.computeDistanceBetween(
                new LatLng(minLatitude, minLongitude), new LatLng(minLatitude, maxLongitude)) / Game.MAP_SEGMENT_MIN_LENGTH);
        // Now that we know how many segments we've got, let's also grab the total lat/lon distances
        totalLatitude = Math.abs(maxLatitude - minLatitude);
        totalLongitude = Math.abs(maxLongitude - minLongitude);
        // And the distance deltas (lat/lon per segment). Yeah, this makes the object larger, but I HOPE it saves calculation time.
        deltaLatitude = totalLatitude / numLatSegments;
        deltaLongitude = totalLongitude / numLonSegments;

        // Now actually initialise the array...
        segmentArray = new int[numLatSegments][numLonSegments];
        // Assign ID values to segments. Each is unique, but their order is irrelevant - all this is client-side!
        // Java can't iterate linearly over 2D arrays?! WHO'D'VE THUNK!!! </sarcasm>
        int segID = 0;
        for (int i = 0; i < segmentArray.length; i++) {
            for (int j = 0; j < segmentArray[i].length; j++) {
                segmentArray[i][j] = segID++; // Look at how slick I am!
            }
        }

        // 20x16 segments = 320 in total
        //Log.e("tag", "MAPSEGMENTS: TOTAL segments = " + segID);


        //double a = SphericalUtil.computeDistanceBetween(
        //        new LatLng(minLatitude, minLongitude), new LatLng(maxLatitude, minLongitude)) / numLatSegments;
        //double b = SphericalUtil.computeDistanceBetween(
        //        new LatLng(minLatitude, minLongitude), new LatLng(minLatitude, maxLongitude)) / numLonSegments;
        //Log.e("tag", "DIST PER SEG: LAT = " + a + " === LON = " + b);
    }

    public int computeSegment(LatLng pointCoords) {
        // REMINDER (I'm awful at these global coordinate shenanigans):
        // LATITUDE  = rows;  [-90, 90]  ^ INCREASES --- v DECREASES
        // LONGITUDE = cols; [-180, 180] > INCREASES --- < DECREASES
        // which means that both mins are in the bottom-left, and locationArray[0,0] (top-left) is (maxLat,minLon)...
        // Which isn't strictly true. I'll just invert the array so that [0,0] is in the bottom-left, segment 0. Nobody cares anyways.
        // That's because maths is brilliant - and so am I.

        // We can use some pseudo-"linear algebra" to calculate what we need: Imagine the point as a vector from the origin.
        // We decompose it into its X and Y components, sort of, then centre it (subtracting the "origin point" from it).
        // Dividing the current "vector" by the segment size produces a fraction that tells us where the point lies in that dimension.
        // We don't give a shit about its exact position, so we convert it to an int - which is the segment ID from the origin.
        int latSegment = (int)((pointCoords.latitude - minLatitude) / deltaLatitude);
        int lonSegment = (int)((pointCoords.longitude - minLongitude) / deltaLongitude);
        // Then we need to clamp any potential "outliers" into the array bounds.
        latSegment = MathUtils.ClampInt(latSegment, 0, numLatSegments - 1);
        lonSegment = MathUtils.ClampInt(lonSegment, 0, numLonSegments - 1);
        // And finally, return the segment ID!
        return segmentArray[latSegment][lonSegment];
    }

    // A convenience method for getting the indices of all segments that neighbour another
    // Also, Javadoc is trash and I'm not using it. '<summary>' tags are where it's at.
    public List<Integer> computeNeighbourSegments(int segmentIndex, int radius) {
        List<Integer> neighbours = new ArrayList<>();

        // I can do it the smart way, but I'd rather do it the hard way and avoid ducking it up
        for (int i = 0; i < segmentArray.length; i++) {
            for (int j = 0; j < segmentArray[i].length; j++) {
                if (segmentArray[i][j] == segmentIndex) { // Find the segment
                    for (int radX = -radius; radX <= radius; radX++) {
                        for (int radY = -radius; radY <= radius; radY++) {
                            try{
                                neighbours.add(segmentArray[i+radX][j+radY]);
                            } catch (ArrayIndexOutOfBoundsException e){
                                continue; // were on edge, cuz were edgy kidz
                                // yolo swag JB, taylor swifts my homie

                                // Note, added later: I don't remember what those comments were supposed to mean...
                            }
                        }
                    }
                }
            }
        }
        Log.d("MapSegments", "checked segments: " + neighbours.size() + ", radius = " + radius +
                ", supposed to be:" + Math.pow(radius * 2 + 1, 2));
        // What a terribly inelegant function.
        return neighbours;
    }

    // And one of these, because functions need to do what they say on the tin.
    public List<Integer> computeSegmentAndNeighbours(LatLng pointCoords, int radius) {
        int seg = computeSegment(pointCoords);
        List<Integer> result = computeNeighbourSegments(seg, radius);
        result.add(seg);
        return result;
    }

    public int getNumLonSegments() {
        return numLonSegments;
    }

    public int getNumLatSegments() {
        return numLatSegments;
    }
}