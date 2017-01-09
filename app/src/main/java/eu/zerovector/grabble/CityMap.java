package eu.zerovector.grabble;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static eu.zerovector.grabble.Game.checkRequestNewWord;
import static eu.zerovector.grabble.Game.currentPlayerData;

// the IDE keeps auto-importing this shit. Can't help it (and don't want to exclude it), so here it stays.


// Fragment for the Map screen.
public class CityMap extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener,
                                                 GoogleApiClient.OnConnectionFailedListener, UpdateUIListener {
    private MapView mapView;
    private GoogleMap map;
    private TextView lblTheCity;
    // It ain't worth it to create a wrapper around the GoogleMap class.
    // We do need to link the placemarks to the markers, however.
    private Map<Placemark, Marker> mapMarkers = new HashMap<>();

    private boolean mapInitialised = false;
    private boolean mapDataInitialised = false;
    private boolean apiClientConnected = false;
    private GoogleApiClient apiClient = null;
    private LocationRequest locationRequest = null;

    public CityMap() { }

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
    protected Circle circleGrabRadius;
    protected Circle circleSightRadius;
    protected boolean requestingLocationUpdates;
    protected String lastUpdateTime;

    // UI "data-binding"
    private TextView lblCurrentWord;
    private TextView lblCurrentWordCompletion;
    private TextView lblCurrentAsh;
    private ProgressBar pbExperience;
    private ProgressBar pbLettersForAsh;

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
        // Inflate the layout for this fragment - using the Calligraphy-wrapped inflater from the parent activity
        View view = inflater.inflate(R.layout.fragment_city_map, container, false);

        mapView = (MapView)view.findViewById(R.id.mapViewComponent);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        // Make map invisible until it's ready; make "The City" label visible until map's ready.
        mapView.setAlpha(0.0f);
        lblTheCity = (TextView)view.findViewById(R.id.lblTheCity);
        lblTheCity.setAlpha(1.0f);

        // Link up all other components
        lblCurrentWord = (TextView)view.findViewById(R.id.lblCurrentWord);
        lblCurrentWordCompletion = (TextView)view.findViewById(R.id.lblCurrentWordCompletion);
        lblCurrentAsh = (TextView)view.findViewById(R.id.lblCurrentAsh);
        pbExperience = (ProgressBar)view.findViewById(R.id.pbExperience);
        pbLettersForAsh = (ProgressBar)view.findViewById(R.id.pbLettersForAsh);
        pbLettersForAsh.setProgressDrawable(new ProgressDrawable(Color.WHITE, Color.TRANSPARENT));

        // Link up to the Game class
        Game.addUIListener(this);

        // Icons didn't seem to load in fragments for me, so I had to force them. The Picasso library appears to do a good job at it.
        ImageView imgAshIcon = (ImageView)view.findViewById(R.id.imgAsh);
        Picasso.with(getActivity()).load(R.drawable.icon_ash).into(imgAshIcon);

        // Finally, update the UI
        updateUI(EnumSet.noneOf(Code.class));


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

            // Finally, remove the keys from the bundle, because the idiotic mapView doesn't like it otherwise
            //savedInstanceState.remove(KEY_REQUESTING_LOCATION_UPDATES);
            //savedInstanceState.remove(KEY_LOCATION);
            //savedInstanceState.remove(KEY_LAST_UPDATED_TIME_STRING);
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
            Toast.makeText(getActivity(), errorString, Toast.LENGTH_LONG).show();
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
        //if (mapView != null) {
        //    mapView.onSaveInstanceState(outState);
        //}
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


        if (currentLocation == null) {
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(apiClient);
            lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        }

        // Make sure to init the map if we've not already done so
        if (!mapDataInitialised) InitMap();
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
        Toast.makeText(getActivity(), "APIclient connection failed + " + result.getErrorMessage(), Toast.LENGTH_LONG);
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

        // Set up map bounds and other settings
        map.setLatLngBoundsForCameraTarget(Game.getMapBounds());
        map.setMinZoomPreference(15.0f);
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setIndoorLevelPickerEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);

        // Fade mapView in, fade the "The City" label out.
        mapView.animate().alpha(1.0f).setDuration(1500);
        lblTheCity.animate().alpha(0.0f).setDuration(1500);

        // Animate the camera to a target position: ours, if we're within the bounds; or just the centre of the play area.
        // We're checking whether the map data has been loaded, so the bounds aren't null.
        if (currentLocation != null && Game.getMapBounds().contains(locationToLatLng(currentLocation))) {
            map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())));
        }
        else map.moveCamera(CameraUpdateFactory.newLatLng(Game.getMapBoundsCentre()));

        // Finally, zoom in for a closer view
        map.animateCamera(CameraUpdateFactory.zoomTo(19.0f));

        // Place markers for ALL points on the map, IF the data has been downloaded.
        List<Placemark> allPlacemarks = Game.getDailyPlacemarks();
        if (allPlacemarks == null) return;
        BitmapDescriptor unknownLetter = BitmapDescriptorFactory.fromResource(R.drawable.marker_letters_unknown);
        for (Placemark point : allPlacemarks) {
            // DEBUG: Look at segment values
            //IconGenerator x = new IconGenerator(getActivity());
            //Bitmap b = x.makeIcon(point.segmentID() + "");
            //BitmapDescriptor unknownLetter = BitmapDescriptorFactory.fromBitmap(b);

            Marker marker = map.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(point.coords()).icon(unknownLetter));
            mapMarkers.put(point, marker);
        }

        // Also set the circles up - prevent null-pointers with a mock location, if necessary
        if (currentLocation == null) currentLocation = new Location("me");
        // (a C#-style "var" would be nice right about now, eh?)
        Experience.DataPair playerDetails = Experience.getAllDetailsForXP(currentPlayerData().getXP());
        circleGrabRadius = map.addCircle(new CircleOptions()
                .center(locationToLatLng(currentLocation))
                .radius(playerDetails.traitSet().getGrabRange())
                .strokeColor(ContextCompat.getColor(getActivity(), R.color.Goldenrod))
                .strokeWidth(2.0f));
        circleSightRadius = map.addCircle(new CircleOptions()
                .center(locationToLatLng(currentLocation))
                .radius(playerDetails.traitSet().getSightRange())
                .strokeColor(ContextCompat.getColor(getActivity(), R.color.UI_AshGrey))
                .strokeWidth(3.0f));


        mapDataInitialised = true;
    }


    // No extension methods in Java... *sigh*
    private LatLng locationToLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    // This fires whenever our location changes.
    @Override
    public void onLocationChanged(Location location) {
        // No matter what happens, update our location reference.
        currentLocation = location;
        circleGrabRadius.setCenter(locationToLatLng(currentLocation));
        circleSightRadius.setCenter(locationToLatLng(currentLocation));


        Log.d(TAG, "OnLocationChanged");

        // Since the game data (markers from the KML file) are loaded async, TRY initting the map AGAIN, just in case
        if (!mapDataInitialised) {
            InitMap();
            return;
        }

        Log.d(TAG, "OnLocationChanged, map initted");

        // get my segment
        // get all points in segment + neighbours
        // get dist to all points
        // if dist < grabradius, add to temp list
        // send network request to grab all points in templist
        // remove templist points from mapmarkers list
        // replace current playerdata with new network-returned object

        List<Integer> pointIDsToSend = new ArrayList<>();

        LatLng currentCoords = locationToLatLng(location);
        // InitMap already requires the map data to have loaded, so mapSegmentData() shan't return null ever
        List<Integer> segmentsOfInterest = Game.mapSegmentData().computeSegmentAndNeighbours(currentCoords);
        // If only we had lambdas and C# (or at least Java 8) features...


        // Get the current player's stats here
        Experience.DataPair playerDetails = Experience.getAllDetailsForXP(currentPlayerData().getExperience());
        Experience.TraitSet playerPerks = playerDetails.traitSet();
        // (Whilst we're here, update the positions of the circles
        circleGrabRadius.setRadius(playerPerks.getGrabRange());
        circleSightRadius.setRadius(playerPerks.getSightRange());

        Iterator iter = mapMarkers.entrySet().iterator(); // This iterator stuff is bizarre
        while (iter.hasNext()) {
            Map.Entry pair = (Map.Entry)iter.next();
            Placemark placemark = (Placemark)pair.getKey(); // The data representation
            Marker marker = (Marker)pair.getValue(); // The actual marker on the map.
            // For all points in our segment (or a neighbouring one), compute the distance.
            if (segmentsOfInterest.contains(placemark.segmentID())) {
                double dist = SphericalUtil.computeDistanceBetween(placemark.coords(), currentCoords);
                // If within grabbing range, add this to the network request list and make local grabbing actions
                if (dist <= playerPerks.getGrabRange()) {




                    // TAKE POINT (and watch the flanks), and if we did take it, queue it for network verification
                    if (Game.grabLetter(placemark.letter(), playerDetails)) {
                        // Actually, only send the thing if the client wants it; otherwise - nae bovver, m8
                        pointIDsToSend.add(placemark.pointID());
                        // Now, kill the marker on the map
                        marker.remove();
                        // The point has been taken; remove all references to it, so we can't grab it again (separate serverside checks)
                        iter.remove();
                    }




                }
                // If within SEEING distance instead, replace marker icon with correct one.
                else if (dist <= playerPerks.getSightRange()) {
                    // Grab the icon for the new letter. If it's a letter for OUR word, highlight it in gold!
                    // ... or don't, because the drawable -> bitmap -> descriptor process appears to be quite costly.
                    marker.setIcon(BitmapDescriptorFactory.fromResource(placemark.letter().getMarkerIconResourceID(getActivity())));
                }
                // All other points (too far even to see) should retain their unknown marker icon.
                else {
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_letters_unknown));
                }
            }
        }
        // Send the notification that letters have been collected.
        // Note that we're doing all the stuff locally, and we're using the network request as confirmation.
        Network.RequestLetterGrab(pointIDsToSend);

    }

    // Updates the UI elements of the fragment based on the current word.
    private void updateUI(EnumSet<Code> updateCodes) {
        // This is called either upon init, or upon letter collection.

        // CURRENT WORD
        // Whatever happened, update the word label, completion count, and ash count
        Word newWord = currentPlayerData().getCurrentWord();



        // If the word hasn't been initialised yet, reflect the loading process that's still taking place.
        Word placeholder = new Word("--WAIT--");
        if (newWord == null) {
            newWord = placeholder;
            // Also request a new word.
            checkRequestNewWord();
        }


        // If the word's been completed, a new one has already been assigned (by the call to Game.grabLetter()).
        // We need to make a fancy animation and replace the thing above with a new word.
        if (oldWord != null && oldWord.isComplete()) {
            // FIXME: REWORK THIS SO IT TAKES ORDERS INSTEAD
        }


        // Update progress bars
        int curXP = currentPlayerData().getExperience();
        Experience.LevelDetails levelDetails = Experience.getLevelDetailsForXP(curXP);
        int progress = (int)((float)(curXP - levelDetails.thisLevelXP())/(float)levelDetails.nextLevelXP());
        pbExperience.setProgress(progress);

        Experience.TraitSet playerPerks = Experience.getPerksForLevel(levelDetails.level());
        ProgressDrawable drawable = (ProgressDrawable)pbLettersForAsh.getProgressDrawable();
        int numLetForAsh = playerPerks.getNumLettersForOneAsh();
        if (drawable.getNumSegments() != numLetForAsh) {
            drawable.setNumSegments(numLetForAsh);
        }
        pbLettersForAsh.setProgress((int)(1.0f - (float)currentPlayerData().getLettersUntilExtraAsh()/(float)numLetForAsh));


        // HTML.fromHTML is deprecated in API 24, so we need to check for that
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            lblCurrentWord.setText(Html.fromHtml(newWord.toColouredHTML(getActivity()), Html.FROM_HTML_MODE_LEGACY));
        } else {
            lblCurrentWord.setText(Html.fromHtml(newWord.toColouredHTML(getActivity())));
        }

        // The rest is easy, though we do need to animate the ash counter (if changed).
        int currentAsh = currentPlayerData().getAsh();
        int oldAsh = Integer.valueOf(lblCurrentAsh.getText().toString());
        if (oldAsh != currentAsh) {
            ValueAnimator animator = new ValueAnimator();
            animator.setObjectValues(oldAsh, currentAsh);
            animator.setDuration(1250);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    lblCurrentAsh.setText((int)animation.getAnimatedValue());
                }
            });
            animator.start();
        }
        String newWordCompletion = newWord.numCompletedLetters() + "/" + newWord.length();
        if (newWord == placeholder) newWordCompletion = "-/-";
        lblCurrentWordCompletion.setText(newWordCompletion);
    }


    @Override
    public void onUpdateUIReceived(EnumSet<Code> codes) {
        // TODO LINKING UP
        updateUI(codes);
    }
}
