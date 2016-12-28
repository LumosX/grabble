package eu.zerovector.grabble;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


// Fragment for the Map screen.
public class CityMap extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
                                                 GoogleApiClient.OnConnectionFailedListener, LocationListener,
                                                 GoogleMap.OnCameraMoveListener {
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

    protected static final String TAG = "GameAcivity -- CityMap";
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 7000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    // Keys for storing activity state in the Bundle.
    protected final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    protected final static String KEY_LOCATION = "location";
    protected final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

    protected LocationSettingsRequest locationSettingsRequest;
    protected Location currentLocation;
    protected boolean requestingLocationUpdates;
    protected String lastUpdateTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestingLocationUpdates = false;
        lastUpdateTime = "";

        // Make the API client
        apiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        apiClient.connect();

        // And the location request
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // And the location settings request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();

        // Also make sure location services are ENABLED
        checkLocationSettings();

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_city_map, container, false);

        mapView = (MapView)view.findViewById(R.id.mapViewComponent);
        mapView.onCreate(savedInstanceState);
        mapView.setVisibility(View.INVISIBLE);

        // Ash icon doesn't show, will try to force it
        ImageView ashIcon = (ImageView)view.findViewById(R.id.imgAsh);
        ashIcon.setAdjustViewBounds(true);

        mapView.getMapAsync(this);
        //mapView.onResume();

        return view;
    }

    // Loading data, if any was saved beforehand.
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                requestingLocationUpdates = savedInstanceState.getBoolean(KEY_REQUESTING_LOCATION_UPDATES);
            }
            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                currentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }
            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                lastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
            }
        }
    }


    // Check if the location settings have been turned on. If not, try to resolve the issue.
    protected void checkLocationSettings() {
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(apiClient, locationSettingsRequest);
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    // Everything's fine
                    case LocationSettingsStatusCodes.SUCCESS:
                        startLocationUpdates();
                        break;
                    // Action needed
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                            status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            showErrorKillApp("Unable to execute request for location data enabling.\n" +
                                             "The game cannot work without location data.");
                        }
                        break;
                    //
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        showErrorKillApp("The game cannot work without location data enabled.");
                        break;
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check for the integer request code originally supplied to startResolutionForResult().
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            switch (resultCode) {
                // 'Result_OK' should mean that the user agreed to GIVE US POWER
                case Activity.RESULT_OK:
                    startLocationUpdates();
                    break;
                // The user declined. Kill application.
                case Activity.RESULT_CANCELED:
                    showErrorKillApp("The game cannot work without location data enabled.");
                    break;
            }
        }
    }

    public void showErrorKillApp(String errorString) {
        if (!errorString.equals("")) {
            Toast.makeText(getActivity().getApplicationContext(), errorString, Toast.LENGTH_LONG).show();
        }
        // King of the KILL
        getActivity().setResult(Game.GLOBAL_ACTIVITY_RESULT_KILL);
        getActivity().finish();
    }

    // Start the location updates from the FusedAPI
    protected void startLocationUpdates() throws SecurityException {
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this).setResultCallback(new ResultCallback<Status>() {
            @Override
                public void onResult(Status status) {
                    requestingLocationUpdates = true;
                }
            });
        Log.i(TAG, "starting location updates");
    }

    // ... and stop the location updates from the FusedAPI
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                requestingLocationUpdates = false;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        apiClient.connect();
    }


    @Override
    public void onStop() {
        super.onStop();
        apiClient.disconnect();
    }


    // Overriding all the other map methods
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        // Resume the location updates, assuming they were paused
        if (apiClient.isConnected() && requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (apiClient.isConnected()) {
            stopLocationUpdates();
        }
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
        outState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates);
        outState.putParcelable(KEY_LOCATION, currentLocation);
        outState.putString(KEY_LAST_UPDATED_TIME_STRING, lastUpdateTime);
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) throws SecurityException {
        Log.i(TAG, "Connected to GoogleApiClient");
        apiClientConnected = true;

        // Make sure to init the map if we've not already done so
        if (!mapDataInitialised) InitMap();

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (currentLocation == null) {
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(apiClient);
            lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        // TODO: HANDLE ON CONNECTION FAILED
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
        Toast.makeText(getActivity(), "APIclient connection failed + " + result.getErrorCode(), Toast.LENGTH_LONG);
    }


    // This fires whenever our location changes.
    @Override
    public void onLocationChanged(Location location) {
        // Since the game data (markers from the KML file) are loaded async, TRY AGAIN just in case
        if (!mapDataInitialised) InitMap();


        // TODO: HANDLE THING
        currentLocation = location;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapInitialised = true;
        this.map = googleMap;
        InitMap();
        mapView.onResume();
    }

    // To be triggered iff the map is ready to work, and only ONCE.
    private void InitMap() throws SecurityException {
        if (!mapInitialised) return;
        if (mapDataInitialised) return;
        if (!apiClientConnected) return;
        if (!Game.isMapDataLoaded()) return;

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


        if (currentLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())));
        }




        mapDataInitialised = true;
    }


    @Override
    public void onCameraMove() {

    }
}
