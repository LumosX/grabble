package eu.zerovector.grabble.Activity.Fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
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
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
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

import eu.zerovector.grabble.Data.Experience;
import eu.zerovector.grabble.Game;
import eu.zerovector.grabble.Network;
import eu.zerovector.grabble.Data.Placemark;
import eu.zerovector.grabble.R;
import eu.zerovector.grabble.Activity.UpdateUIListener;
import eu.zerovector.grabble.Data.Word;

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
    private TextView lblCurrentWordAux; // We need this to allow for a (hacky) smooth fading when updating the textView
    private boolean curWordAuxVisible = false; // this one tracks the state
    private boolean wordCompleteAnimRunning = false;
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
        lblCurrentWordAux = (TextView)view.findViewById(R.id.lblCurrentWordAux);
        lblCurrentWordCompletion = (TextView)view.findViewById(R.id.lblCurrentWordCompletion);
        lblCurrentAsh = (TextView)view.findViewById(R.id.lblCurrentAsh);
        pbExperience = (ProgressBar)view.findViewById(R.id.pbExperience);
        pbLettersForAsh = (ProgressBar)view.findViewById(R.id.pbLettersForAsh);
        pbLettersForAsh.setProgressDrawable(new ProgressDrawable(Color.WHITE, Color.DKGRAY));

        // Fade out the auxiliary curWord view
        curWordAuxVisible = false;
        wordCompleteAnimRunning = false;
        lblCurrentWordAux.setAlpha(0.0f);

        // Link up to the Game class
        Game.addUIListener(this);

        // Icons didn't seem to load in fragments for me, so I had to force them. The Picasso library appears to do a good job at it.
        ImageView imgAshIcon = (ImageView)view.findViewById(R.id.imgAsh);
        Picasso.with(getActivity()).load(R.drawable.icon_ash).into(imgAshIcon);

        // Finally, update the UI
        updateUI(EnumSet.noneOf(Code.class), Game.currentPlayerData().getCurrentWord());

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
        LocationServices.FusedLocationApi
                .requestLocationUpdates(apiClient, locationRequest, this)
                .setResultCallback(new ResultCallback<Status>() {
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

        if (location != null && circleGrabRadius != null && circleSightRadius != null) {
            // Smoothly interpolate the position of the circles (the radius will remain as-is)
            // We can't make a PVH from doubles, and I'm not making it out of objects, so the lat-lons need to get casted to floats.
            PropertyValuesHolder pvhLon = PropertyValuesHolder.ofFloat("TRANSLATE_LON",
                    (float)currentLocation.getLongitude(), (float)location.getLongitude());
            PropertyValuesHolder pvhLat = PropertyValuesHolder.ofFloat("TRANSLATE_LAT",
                    (float)currentLocation.getLatitude(), (float)location.getLatitude());
            ValueAnimator circleCentreAnim = ValueAnimator.ofPropertyValuesHolder(pvhLon, pvhLat);
            circleCentreAnim.setInterpolator(new LinearInterpolator());
            circleCentreAnim.setDuration(450);
            circleCentreAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    // But since casting floats to doubles isn't that straight-forward... this is a sodding mess.
                    LatLng curLoc = new LatLng(Double.parseDouble(Float.toString((float)valueAnimator.getAnimatedValue("TRANSLATE_LAT"))),
                                               Double.parseDouble(Float.toString((float)valueAnimator.getAnimatedValue("TRANSLATE_LON"))));
                    circleGrabRadius.setCenter(curLoc);
                    circleSightRadius.setCenter(curLoc);
                }
            });
            circleCentreAnim.start();
        }

        // No matter what happens, update our location reference (after we're done moving the circles)
        currentLocation = location;


        Log.d(TAG, "OnLocationChanged");

        // Since the game data (markers from the KML file) are loaded async, TRY initting the map AGAIN, just in case
        if (!mapDataInitialised) {
            InitMap();
            Log.d(TAG, "OnLocationChanged, map initted");
            return;
        }



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
        // FIXME: Get as many neighbours as we need to fully "wrap" the Sight radius of the player
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
            // Points outside of the relevant segments should also be properly cleared as unknown (just a safeguard)
            else {
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_letters_unknown));
            }
        }
        // Send the notification that letters have been collected.
        // Note that we're doing all the stuff locally, and we're using the network request as confirmation.
        Network.RequestLetterGrab(pointIDsToSend);

    }

    @Override
    public void onUpdateUIReceived(EnumSet<Code> codes, Word oldWord) {
        updateUI(codes, oldWord);
    }

    // Updates the UI elements of the fragment based on the current word.
    private void updateUI(EnumSet<Code> updateCodes, Word oldWord) {
        // This is called either upon init, or upon letter collection.

        // CURRENT WORD
        Word newWord = currentPlayerData().getCurrentWord();
        // If the word hasn't been initialised yet, reflect the loading process that's still taking place.
        Word placeholder = new Word("--WAIT--");
        if (newWord == null) {
            newWord = placeholder;
            // Also request a new word.
            checkRequestNewWord();
        }

        int curXP = currentPlayerData().getExperience();
        Experience.LevelDetails levelDetails = Experience.getLevelDetailsForXP(curXP);

        // No matter what happened, update the word label, completion count, and ash count
        // Update progress bars: XP first
        int t = levelDetails.thisLevelXP();
        final int curXPProgress = (int)((float)(curXP - t)/(float)(levelDetails.nextLevelXP() - t) * 100);
        // Animating the progress is easy, if there hasn't been a level-up.
        // If that DID happen, animate to 100%, set to %, continue to whatever was necessary.
        final ValueAnimator xpAnimator = new ValueAnimator();
        int animXPTarget = curXPProgress;
        // We'll be recycling this
        final ValueAnimator.AnimatorUpdateListener animXPFunc = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                pbExperience.setProgress((int)animation.getAnimatedValue());
            }
        };
        if (updateCodes.contains(Code.LEVEL_INCREASED)) {
            animXPTarget = 100; // animate to end first
            // then reset the animator and animate to current level
            xpAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // And these two are to prevent infinite loops
                    xpAnimator.removeAllListeners();
                    xpAnimator.addUpdateListener(animXPFunc);
                    xpAnimator.end();
                    xpAnimator.setIntValues(0, curXPProgress);
                    xpAnimator.setStartDelay(600);
                    xpAnimator.start();
                }
            });
        }
        xpAnimator.setInterpolator(new LinearInterpolator());
        xpAnimator.setIntValues(pbExperience.getProgress(), animXPTarget);
        xpAnimator.setDuration(1000);
        xpAnimator.addUpdateListener(animXPFunc);
        xpAnimator.start();

        // And now we need to do the same thing for the letters for ash (because things look better when they're smoother)
        Experience.TraitSet playerPerks = Experience.getPerksForLevel(levelDetails.level());
        ProgressDrawable drawable = (ProgressDrawable)pbLettersForAsh.getProgressDrawable();
        int numLetForAsh = playerPerks.getNumLettersForOneAsh();
        if (drawable.getNumSegments() != numLetForAsh) {
            drawable.setNumSegments(numLetForAsh);
        }
        float a = currentPlayerData().getLettersUntilExtraAsh();
        float b = (float)numLetForAsh;
        //final int curLetAshVal = (int)(100 * (1.0f - (a/(float)numLetForAsh))); // round UP
        final int curLetAshVal = (int)(((b-a)/b) * 100);
        int letAshTarget = curLetAshVal;
        final ValueAnimator curLetForAshAnim = new ValueAnimator();
        // We'll be recycling this
        final ValueAnimator.AnimatorUpdateListener animLetAshFunc = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                pbLettersForAsh.setProgress((int)animation.getAnimatedValue());
            }
        };
        if (updateCodes.contains(Code.EXTRA_ASH_GRANTED)) {
            letAshTarget = 100; // animate to end first
            // then reset the animator and animate to current level
            curLetForAshAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // And these two are to prevent infinite loops
                    curLetForAshAnim.removeAllListeners();
                    curLetForAshAnim.addUpdateListener(animLetAshFunc);
                    curLetForAshAnim.end();
                    curLetForAshAnim.setIntValues(0, curLetAshVal);
                    curLetForAshAnim.setStartDelay(600);
                    curLetForAshAnim.start();
                }
            });
        }
        curLetForAshAnim.setInterpolator(new LinearInterpolator());
        curLetForAshAnim.setIntValues(pbLettersForAsh.getProgress(), letAshTarget);
        curLetForAshAnim.setDuration(1000);
        curLetForAshAnim.addUpdateListener(animLetAshFunc);
        curLetForAshAnim.start();


        // Current word: we want some animation here as well.
        // HTML.fromHTML is deprecated in API 24, so we need to check for that
        final Spanned targetCurWordTextSpan;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            targetCurWordTextSpan = Html.fromHtml(newWord.toColouredHTML(getActivity()), Html.FROM_HTML_MODE_LEGACY);
        } else {
            targetCurWordTextSpan = Html.fromHtml(newWord.toColouredHTML(getActivity()));
        }
        // In the general case, just update the textView that holds it.
        final int curWordAnimDuration = 500;
        if (!updateCodes.contains(Code.WORD_COMPLETED) && !wordCompleteAnimRunning) {
            updateCurWordView(targetCurWordTextSpan, curWordAnimDuration);
        }
        // If we did COMPLETE a word, make it fancier. Animate same word to goldenrod first, then return to normal
        else {
            wordCompleteAnimRunning = true;
            Spanned goldenText;
            String colour = Integer.toHexString(ContextCompat.getColor(getActivity(), R.color.Goldenrod));
            String targetString = "<font color=#" + colour.substring(2) + ">" + lblCurrentWord.getText().toString() + "</colour>";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                goldenText = Html.fromHtml(targetString, Html.FROM_HTML_MODE_LEGACY);
            } else {
                goldenText = Html.fromHtml(targetString);
            }
            updateCurWordView(goldenText, (int)(curWordAnimDuration * 1.25f)); // This will animate it to Goldenrod
            // And this bullshit will return it to normal
            lblCurrentWord.animate().setStartDelay((int)(curWordAnimDuration * 1.6f) - 1).setDuration(1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    lblCurrentWord.animate().setListener(null);
                    updateCurWordView(targetCurWordTextSpan, curWordAnimDuration);
                    wordCompleteAnimRunning = false;
                }
            });
        }


        // The rest is easy, though we do need to animate the ash counter (if changed).
        int currentAsh = currentPlayerData().getAsh();
        int oldAsh = Integer.valueOf(lblCurrentAsh.getText().toString());
        if (oldAsh != currentAsh) {
            ValueAnimator animator = new ValueAnimator();
            animator.setIntValues(oldAsh, currentAsh);
            animator.setDuration(1250);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    lblCurrentAsh.setText(String.valueOf((int)animation.getAnimatedValue()));
                }
            });
            animator.start();
        }
        String newWordCompletion = newWord.numCompletedLetters() + "/" + newWord.length();
        if (newWord == placeholder) newWordCompletion = "loading";
        lblCurrentWordCompletion.setText(newWordCompletion);
    }

    private void updateCurWordView(Spanned targetCurWordTextSpan, int fadeDurationMsec) {
        // Fade between the current and the auxiliary textView.
        // if Aux is "on top", fade it out, update the real one and fade it in (else do the opposite)
        if (curWordAuxVisible) {
            lblCurrentWord.setText(targetCurWordTextSpan);
            lblCurrentWordAux.animate().alpha(0.0f).setDuration(fadeDurationMsec);
            lblCurrentWord.animate().alpha(1.0f).setDuration(fadeDurationMsec);
        }
        else {
            lblCurrentWordAux.setText(targetCurWordTextSpan);
            lblCurrentWord.animate().alpha(0.0f).setDuration(fadeDurationMsec);
            lblCurrentWordAux.animate().alpha(1.0f).setDuration(fadeDurationMsec);
        }
        curWordAuxVisible ^= true; // toggle the boolean var by using clever XOR-ing.
    }

}
