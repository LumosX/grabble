package eu.zerovector.grabble;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;


// Fragment for the Map screen.
public class CityMap extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
                                                 GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private MapView mapView;
    private GoogleMap map;
    // It ain't worth it to create a wrapper around the GoogleMap class.
    private List<Marker> mapMarkers = new ArrayList<>();

    private boolean mapInitialised = false;
    private boolean mapDataInitialised = false;
    private boolean apiClientConnected = false;
    private GoogleApiClient apiClient = null;
    private LocationRequest locationRequest = null;

    public CityMap() {
        mapInitialised = false;
        mapDataInitialised = false;
        apiClient = null;
        apiClientConnected = false;
        locationRequest = null;
    }


    public static CityMap newInstance() {
        return new CityMap();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) throws SecurityException {
        super.onCreate(savedInstanceState);

        // Link up the API client and start requesting location updates
        apiClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();



        // TODO: Perhaps pull the hardcoded stuff out in some sort of settings menu?
        locationRequest = new LocationRequest();
        locationRequest.setInterval(7000);
        locationRequest.setFastestInterval(3000);
        //locationRequest.setSmallestDisplacement(1.5f);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_city_map, container, false);

        mapView = (MapView)view.findViewById(R.id.mapViewComponent);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);
        //mapView.onResume();

        return view;
    }

    @Override
    public void onConnected(Bundle bundle) throws SecurityException {
        // Request location updates once the API client has connected
        FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
        apiClientConnected = true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapInitialised = true;
        InitMap(googleMap);
    }

    // To be triggered iff the map is ready to work, and only ONCE.
    private void InitMap(GoogleMap map) throws SecurityException {
        if (!mapInitialised) return;
        if (mapDataInitialised) return;
        if (!apiClientConnected) return;

        // Set up map bounds
        map.setLatLngBoundsForCameraTarget(Game.getMapBounds());

        // Place markers for ALL points on the map, IF the data has been downloaded.
        List<Placemark> allPlacemarks = Game.getDailyPlacemarks();
        if (allPlacemarks == null) return;
        BitmapDescriptor unknownLetter = BitmapDescriptorFactory.fromResource(R.drawable.marker_unknown);
        for (Placemark point : allPlacemarks) {
            Marker newPoint = map.addMarker(new MarkerOptions().position(point.coords()).icon(unknownLetter));
            mapMarkers.add(newPoint);

        }

        this.map = map;


        Location me = LocationServices.FusedLocationApi.getLastLocation(apiClient);
        map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(me.getLatitude(), me.getLongitude())));




        mapDataInitialised = true;
    }

    @Override
    public void onLocationChanged(Location location) {
        // If, for some reason, the map isn't initialised yet, do it ASAP
        if (!mapDataInitialised) InitMap(this.map);

        //
    }



    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }





    // Overriding all the other map methods
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mapView != null) {
            try {
                mapView.onDestroy();
            } catch (NullPointerException e) {
                Log.e(TAG, "Error while attempting MapView.onDestroy(), ignoring exception", e);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

}
