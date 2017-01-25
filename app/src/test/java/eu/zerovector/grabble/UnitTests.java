package eu.zerovector.grabble;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import org.junit.Assert;
import org.junit.Test;

import eu.zerovector.grabble.Data.MapSegments;


public class UnitTests {

    @Test
    public void testBasicSegmentation() throws Exception {
        // A test for the segmentation system.
        // We'll set up some faux bounds first
        LatLng cornerSW = new LatLng(0, 0);
        // Making a rectangular area that sort of mimics the regular play area.
        LatLng cornerNW = SphericalUtil.computeOffset(cornerSW, Game.MAP_SEGMENT_MIN_LENGTH * 8.4, 0);
        LatLng cornerSE = SphericalUtil.computeOffset(cornerSW, Game.MAP_SEGMENT_MIN_LENGTH * 15.10, 90);
        MapSegments seg = new MapSegments(cornerSW.latitude, cornerNW.latitude, cornerSW.longitude, cornerSE.longitude);
        // Check if the segments are properly expanding from their minimum size
        Assert.assertTrue("MapSegments generated less latitude segments than expected.", seg.getNumLatSegments() == 8);
        Assert.assertTrue("MapSegments generated less longitude segments than expected.", seg.getNumLonSegments() == 15);

        // Yeah... testing the other part of the segmentation would require an Instrumented test.
        // Besides, I can't come up with something sensible to test it against.
    }
}