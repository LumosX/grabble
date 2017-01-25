package eu.zerovector.grabble.Activity.Fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
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

import com.arasthel.asyncjob.AsyncJob;
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
import com.google.android.gms.maps.model.LatLngBounds;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.zerovector.grabble.Activity.UpdateUIListener;
import eu.zerovector.grabble.Data.Alignment;
import eu.zerovector.grabble.Data.Placemark;
import eu.zerovector.grabble.Data.Word;
import eu.zerovector.grabble.Data.XPUtils;
import eu.zerovector.grabble.Game;
import eu.zerovector.grabble.Network;
import eu.zerovector.grabble.R;
import eu.zerovector.grabble.Utils.AnimUtils;
import eu.zerovector.grabble.Utils.MathUtils;

import static eu.zerovector.grabble.Game.currentPlayerData;


// Fragment for the Map screen.
public class CityMap extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener,
                                                 GoogleApiClient.OnConnectionFailedListener, UpdateUIListener {
    private MapView mapView;
    private GoogleMap map;
    private TextView lblTheCity;
    // It ain't worth it to create a wrapper around the GoogleMap class.
    // We do need to link the placemarks to the markers, however.
    // However, I was forced to also include the icon data in there too. Pfffft...
    private Map<Placemark, Pair<Marker, BitmapDescriptor>> mapMarkers = new HashMap<>();

    private boolean mapInitialised = false; // Whether the ACTUAL map is ready.
    private boolean mapDataInitialised = false; // whether OUR DATA for the map is ready.
    private boolean apiClientConnected = false;
    private GoogleApiClient apiClient = null;
    private LocationRequest locationRequest = null;

    public CityMap() { }

    public static CityMap newInstance() {
        return new CityMap();
    }

    private boolean canReceiveUIUpdates = false;
    private ExecutorService locChangedUpdateExec = Executors.newSingleThreadExecutor();

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
    private ImageView imgAsh;
    private ProgressBar pbExperience;
    private ProgressBar pbLettersForAsh;

    // Awkward work-around for something related to activity attaching and detaching or something
    private Context parentContext = null;

    // NB: This works only on API >= 23
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttachMethod(context);
    }

    // And this is for the lower versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        onAttachMethod(activity);
    }

    private void onAttachMethod(Context context) {
        // Setup context
        parentContext = context;

        Log.i(TAG, "ATTACHED TO CONTEXT");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestingLocationUpdates = false;
        lastUpdateTime = "";

        // Make the API client
        apiClient = new GoogleApiClient.Builder(parentContext)
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
        imgAsh = (ImageView)view.findViewById(R.id.imgAsh);
        pbExperience = (ProgressBar)view.findViewById(R.id.pbExperience);
        pbLettersForAsh = (ProgressBar)view.findViewById(R.id.pbLettersForAsh);
        pbLettersForAsh.setProgressDrawable(new ProgressDrawable(Color.WHITE, Color.DKGRAY));

        // Fade out the auxiliary curWord view
        curWordAuxVisible = false;
        wordCompleteAnimRunning = false;
        lblCurrentWordAux.setAlpha(0.0f);

        // Icons didn't seem to load in fragments for me, so I had to force them. The Picasso library appears to do a good job at it.
        Picasso.with(parentContext).load(R.drawable.icon_ash).into(imgAsh);

        // Link up to the Game class for UI updates
        Game.addUIListener(this);

        // Finally, update the UI
        canReceiveUIUpdates = true;
        updateUI(EnumSet.noneOf(Code.class), Game.currentPlayerData().getCurrentWord());

        mapView.onResume();

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
            Toast.makeText(parentContext, errorString, Toast.LENGTH_LONG).show();
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
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "DETACHED FROM CONTEXT");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "ONSTART");
        apiClient.connect();
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "ONSTOP");

        // The map stalls if the activity stops, ergo we need to re-init it
        mapInitialised = false;
        mapDataInitialised = false;

        // I know this isn't elegant, but I'd rather not make it any more complicated
        // Just want to force-save one last time, so no progress is lost
        Network.SavePlayerData(parentContext, Game.currentPlayerData());

        apiClient.disconnect();

        // Try killing any onLocationChanged updates we might still have rolling out
        locChangedUpdateExec.shutdownNow();
    }


    // Overriding all the other map methods
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "ONRESUME");
        if (mapView != null) {
            mapView.onResume();
        }
        // Resume the location updates, assuming they were paused
        if (apiClient.isConnected() && !requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "OnPAUSE");
        if (mapView != null) {
            mapView.onPause();
        }
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (apiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "ONDESTROYVIEW");
        // Just to be safe, disengage from UI updates
        canReceiveUIUpdates = false;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "");
        if (mapView != null) {
            try {
                mapView.onDestroy();
            } catch (NullPointerException e) {
                Log.e(TAG, "Error while attempting MapView.onDestroy(), ignoring exception", e);
            }
        }

        // Disengage from parent context
        parentContext = null;

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
        Log.d(TAG, "saving instance state");
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
        if (!mapDataInitialised) InitMapData();
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
        Toast.makeText(parentContext, "APIclient connection failed + " + result.getErrorMessage(), Toast.LENGTH_LONG);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapInitialised = true;
        this.map = googleMap;
        InitMapData();
        mapView.onResume();
    }

    // To be triggered iff the map is ready to work, and only ONCE.
    private void InitMapData() throws SecurityException {
        if (!mapInitialised) return;
        if (mapDataInitialised) return;
        if (!apiClientConnected) return;
        if (!Game.isMapDataLoaded()) return;
        if (Game.currentPlayerData() == null) return;

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

        // Animate the camera to a target position: ours, if we're within the bounds; or just zoom to the play area
        // We're checking whether the map data has been loaded, so the bounds aren't null.
        if (currentLocation != null && Game.getMapBounds().contains(MathUtils.LocationToLatLng(currentLocation))) {
            map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())));
        }
        else map.moveCamera(CameraUpdateFactory.newLatLngBounds(Game.getMapBounds(), 20));

        // Place markers for ALL points on the map, IF the data has been downloaded.
        List<Placemark> allPlacemarks = Game.getDailyPlacemarks();
        if (allPlacemarks == null) return;
        BitmapDescriptor unknownLetter = BitmapDescriptorFactory.fromResource(R.drawable.marker_letters_unknown);
        for (Placemark point : allPlacemarks) {
            // Don't forget to filter out points we've already taken earlier in the day
            if (Game.currentPlayerData().getPlacemarksCollectedToday().get(point.pointID()))
                continue;

            //// DEBUG: Look at segment values
            //IconGenerator x = new IconGenerator(getActivity());
            //Bitmap b = x.makeIcon(point.segmentID() + "");
            //BitmapDescriptor unknownLetter = BitmapDescriptorFactory.fromBitmap(b);

            Marker marker = map.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(point.coords()).icon(unknownLetter));
            mapMarkers.put(point, new Pair<>(marker, unknownLetter));
        }

        // Also set the circles up - prevent null-pointers with an alternative location if need be
        LatLng targetLocation = Game.getMapBoundsCentre();
        if (currentLocation != null) targetLocation = MathUtils.LocationToLatLng(currentLocation);
        // (a C#-style "var" would be nice right about now, eh?)
        XPUtils.DataPair playerDetails = XPUtils.getAllDetailsForXP(currentPlayerData().getXP());
        // I'm hiding the circles, because at this point curLoc is usually == null.
        circleGrabRadius = map.addCircle(new CircleOptions()
                .center(targetLocation)
                .radius(playerDetails.traitSet().getGrabRange())
                .strokeColor(ContextCompat.getColor(parentContext, R.color.Goldenrod))
                .strokeWidth(2.0f).visible(false));
        circleSightRadius = map.addCircle(new CircleOptions()
                .center(targetLocation)
                .radius(playerDetails.traitSet().getSightRange())
                .strokeColor(ContextCompat.getColor(parentContext, R.color.UI_AshGrey))
                .strokeWidth(3.0f).visible(false));

        // Finally, zoom in for a closer view, enough to wrap the Sight circle (always larger than Grab)
        // Or show the circles if we actually can (activity re-init?)
        if (currentLocation != null) {
            circleSightRadius.setVisible(true);
            circleGrabRadius.setVisible(true);
            zoomToLocation(currentLocation, playerDetails.traitSet().getSightRange());
        }

        mapDataInitialised = true;
    }

    private void zoomToLocation(Location location, int sightRange) {
        if (location == null) {
            Log.e(TAG, "Can't zoom to null!");
            return;
        }

        // Curiously enough, location.getLat and .getLon may apparently throw null pointers
        //  for no reason, even when location isn't null.
        try {
            double offset = sightRange * 1.414214; // Sqrt(2)
            LatLng center = MathUtils.LocationToLatLng(currentLocation);
            LatLng cornerNE = SphericalUtil.computeOffset(center, offset, 45);
            LatLng cornerSW = SphericalUtil.computeOffset(center, offset, 225);
            LatLngBounds zoomBounds = new LatLngBounds(cornerSW, cornerNE);
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(zoomBounds, 20)); // 20px border
        }
        catch (Exception ex) {
            Log.e(TAG, "Couldn't zoom to location: " + ex.getMessage());
        }
    }


    // This fires whenever our location changes.
    // (Tried putting it in a handler, didn't work. Will have to stay on the main thread.)
    @Override
    public void onLocationChanged(final Location location) {
        // Abort if this triggers before we're ready for it.
        if (!canReceiveUIUpdates || parentContext == null) return;
        if (!mapInitialised) return;
        if (!apiClientConnected) return;
        if (!Game.isMapDataLoaded()) return;
        if (Game.currentPlayerData() == null) return;

        Log.d(TAG, "OnLocationChanged");

        // Since the game data (markers from the KML file) are loaded async, TRY initting the map AGAIN, just in case
        if (!mapDataInitialised) {
            InitMapData();
            Log.d(TAG, "OnLocationChanged: map data wasn't ready");
            return;
        }

        // Get the current player's stats here - we'll need them for all sorts of stuff
        XPUtils.DataPair playerDetails = XPUtils.getAllDetailsForXP(currentPlayerData().getExperience());
        XPUtils.TraitSet playerPerks = playerDetails.traitSet();

        // If this is the first legit location we're receiving, enable the circles (they'll've been hidden)
        if (currentLocation == null && location != null) {
            circleSightRadius.setVisible(true);
            circleGrabRadius.setVisible(true);
            zoomToLocation(location, playerDetails.traitSet().getSightRange());
        }
        // Otherwise just interpolate them
        else if (location != null && circleGrabRadius != null && circleSightRadius != null) {
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
                    LatLng curLoc = new LatLng((float)valueAnimator.getAnimatedValue("TRANSLATE_LAT"),
                                               (float)valueAnimator.getAnimatedValue("TRANSLATE_LON"));
                    circleGrabRadius.setCenter(curLoc);
                    circleSightRadius.setCenter(curLoc);
                }
            });
            circleCentreAnim.start();
        }


        // No matter what happens, update our location reference (after we're done moving the circles)
        currentLocation = location;

        // Prepare the playerDetails for usage by the skills system, in particularly the Openers
        XPUtils.TraitSet.Builder modifiedPerks = new XPUtils.TraitSet.Builder(playerPerks);
        Alignment curAlignment = currentPlayerData().getAlignment();
        int curLevel = playerDetails.levelDetails().level();
        int extraSight = 0, extraGrab = 0;
        if (XPUtils.LevelHasSkill(curAlignment, curLevel, XPUtils.Skill.ORACLE)) {
            extraSight = (int)(0.01f * modifiedPerks.getSightRange() * XPUtils.Skill.ORACLE.getCurBonusMagnitude(curLevel));
        }
        if (XPUtils.LevelHasSkill(curAlignment, curLevel, XPUtils.Skill.SACRED_WILL)) {
            extraGrab = (int)(0.01f * modifiedPerks.getGrabRange() * XPUtils.Skill.ORACLE.getCurBonusMagnitude(curLevel));
        }
        if (XPUtils.LevelHasSkill(curAlignment, curLevel, XPUtils.Skill.COMMANDING_PRESENCE)) {
            extraSight *= 2;
            extraGrab *= 2;
        }
        modifiedPerks.addSightRange(extraSight);
        modifiedPerks.addGrabRange(extraGrab);

        // (Whilst we're here, update the size of the circles - no need to bother animating them)
        circleGrabRadius.setRadius(modifiedPerks.getGrabRange());
        circleSightRadius.setRadius(modifiedPerks.getSightRange());

        // get my segment
        // get all points in segment + neighbours
        // get dist to all points
        // if dist < grabradius, add to temp list
        // send network request to grab all points in templist
        // remove templist points from mapmarkers list
        // replace current playerdata with new network-returned object

        final XPUtils.DataPair playerData = new XPUtils.DataPair(playerDetails.levelDetails(), modifiedPerks.build());
        final LatLng currentCoords = MathUtils.LocationToLatLng(location);

        // InitMapData already requires the map data to have loaded, so mapSegmentData() shan't return null ever
        int segmentRadius = modifiedPerks.getSightRange() / Game.MAP_SEGMENT_MIN_LENGTH;
        segmentRadius = (segmentRadius > 0) ? segmentRadius : 0;
        final List<Integer> segmentsOfInterest = Game.mapSegmentData().computeSegmentAndNeighbours(currentCoords, segmentRadius);

        // In order NOT to do all this on the UI thread, we'll queue a background job. Gotta make use of that library, right?
        //===== ASYNCJOB METHOD DEFINITION BEGIN
        new AsyncJob.AsyncJobBuilder<List<Placemark>>().doInBackground(new AsyncJob.AsyncAction<List<Placemark>>() {
            @Override
            public List<Placemark> doAsync() {

                List<Placemark> pointsToGrab = new ArrayList<>();

                // If only we had lambdas and C# (or at least Java 8) features...
                Iterator iter = mapMarkers.entrySet().iterator(); // This iterator stuff is bizarre
                while (iter.hasNext()) {
                    Map.Entry keyVal = (Map.Entry)iter.next();
                    Placemark placemark = (Placemark)keyVal.getKey(); // The data representation
                    Pair<Marker, BitmapDescriptor> pair =
                            (Pair<Marker, BitmapDescriptor>)keyVal.getValue(); // The actual marker on the map, plus its icon
                    // DISREGARD THE WARNING, IT'S PERFECTLY FINE, LINT IS JUST SILLY
                    Marker marker = pair.first;
                    BitmapDescriptor icon = pair.second;
                    // For all points in our segment (or a neighbouring one), compute the distance.
                    if (segmentsOfInterest.contains(placemark.segmentID())) {
                        double dist = SphericalUtil.computeDistanceBetween(placemark.coords(), currentCoords);
                        // If within grabbing range, add this to the network request list and make local grabbing actions
                        if (dist <= playerData.traitSet().getGrabRange()) {
                            // Add point to the list we'll be taking in one fell swoop
                            pointsToGrab.add(placemark);
                            // Notify points that it needs to be removed
                            icon = null;
//                            // Now, kill the marker on the map
//                            marker.remove();
//                            // The point has been taken; remove all references to it, so we can't grab it again
//                            iter.remove();

                        }
                        // If within SEEING distance instead, replace marker icon with correct one.
                        else if (dist <= playerData.traitSet().getSightRange()) {
                            // Grab the icon for the new letter. If it's a letter for OUR word, highlight it in gold!
                            // ... or don't, because the drawable -> bitmap -> descriptor process appears to be quite costly.
                            icon = BitmapDescriptorFactory.fromResource(placemark.letter().getMarkerIconResourceID(parentContext));
//                            marker.setIcon(BitmapDescriptorFactory.fromResource(placemark.letter().getMarkerIconResourceID(parentContext)));
                        }
                        // All other points (too far even to see) should retain their unknown marker icon.
                        else {
                            icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_letters_unknown);
//                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_letters_unknown));
                        }
                    }
                    // Points outside of the relevant segments should also be properly cleared as unknown (just a safeguard)
                    else {
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_letters_unknown);
//                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_letters_unknown));
                    }

                    // Finally, update data
                    mapMarkers.put(placemark, new Pair<>(marker, icon));
                }

                return pointsToGrab;
            }
        }).doWhenFinished(new AsyncJob.AsyncResultAction<List<Placemark>>() {
            @Override
            public void onResult(List<Placemark> grabbedPoints) {
                // However, we DO need to update the markers on the UI thread...
                // And we need to cycle all this shit AGAIN
                Iterator iter = mapMarkers.entrySet().iterator();
                while (iter.hasNext()) {
                    Pair<Marker, BitmapDescriptor> values = (Pair<Marker, BitmapDescriptor>)((Map.Entry)iter.next()).getValue();
                    Marker marker = values.first;
                    BitmapDescriptor icon = values.second;
                    // If the icon is null, we've grabbed the point, so we need to kill it entirely
                    if (icon == null) {
                        marker.remove();
                        iter.remove();
                    }
                    else {
                        marker.setIcon(icon);
                    }
                }
                // Do the actual letter-grabbing
                if (grabbedPoints.size() > 0) {
                    Game.grabPoints(parentContext, grabbedPoints, playerData);
                }
            }
        }).withExecutor(locChangedUpdateExec).create().start();
        //===== ASYNCJOB METHOD DEFINITION END

    }

    @Override
    public void onUpdateUIReceived(EnumSet<Code> codes, Word oldWord) {
        updateUI(codes, oldWord);
    }

    // Updates the UI elements of the fragment based on the current word.
    private void updateUI(EnumSet<Code> updateCodes, Word oldWord) {
        // This is called either upon init, or upon letter collection.
        // AND NOT BEFORE THAT!
        if (!canReceiveUIUpdates || getContext() == null) return;

        // CURRENT WORD
        Word newWord = currentPlayerData().getCurrentWord();
        // If the word hasn't been initialised yet, reflect the loading process that's still taking place.
        Word placeholder = new Word("WAIT");
        if (newWord == null) {
            newWord = placeholder;
            // Also request a new word if we have to. (Though this should be unnecessary.)
            if (!Game.isLoadingNewWord()) {
                Game.checkRequestNewWord(parentContext);
            }
        }

        int curXP = currentPlayerData().getExperience();
        XPUtils.LevelDetails levelDetails = XPUtils.getLevelDetailsForXP(curXP);

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
        XPUtils.TraitSet playerPerks = XPUtils.getPerksForLevel(levelDetails.level());
        ProgressDrawable drawable = (ProgressDrawable)pbLettersForAsh.getProgressDrawable();
        int numLetForAsh = playerPerks.getNumLettersForOneAsh();
        if (XPUtils.LevelHasSkill(currentPlayerData().getAlignment(), levelDetails.level(), XPUtils.Skill.DUSTBECKON) &&
            numLetForAsh > 1) {
            numLetForAsh -= 1;
        }
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
        final Spanned targetCurWordTextSpan = AnimUtils.FromHTML(newWord.toColouredHTML(parentContext));
        // In the general case, just update the textView that holds it.
        final int curWordAnimDuration = 500;
        if (!updateCodes.contains(Code.WORD_COMPLETED) && !wordCompleteAnimRunning) {
            updateCurWordView(targetCurWordTextSpan, curWordAnimDuration);
        }
        // If we did COMPLETE a word, make it fancier. Animate same word to goldenrod first, then return to normal
        else if (!wordCompleteAnimRunning) {
            wordCompleteAnimRunning = true;
            String colour = Integer.toHexString(ContextCompat.getColor(parentContext, R.color.Goldenrod));
            TextView targetView = (curWordAuxVisible) ? lblCurrentWordAux : lblCurrentWord;
            String targetString = "<font color=#" + colour.substring(2) + ">" + targetView.getText().toString() + "</colour>";
            Spanned goldenText = AnimUtils.FromHTML(targetString);
            updateCurWordView(goldenText, (int)(curWordAnimDuration * 1.25f)); // This will animate it to Goldenrod
            // And this bullshit will return it to normal
            // (I'm basically using curWordCompletion as an inline Animator container... WHY NOT?!)
            lblCurrentWordCompletion.animate().setStartDelay((int)(curWordAnimDuration * 1.6f) - 1)
                    .setDuration(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            updateCurWordView(targetCurWordTextSpan, curWordAnimDuration);
                            wordCompleteAnimRunning = false;
                            lblCurrentWordCompletion.animate().setListener(null);
                        }
                    }).start(); // calling Start here turned out to be quite important
        }


        // The rest is easy, though we do need to animate the ash counter (if changed).
        AnimUtils.DoGenericAshAnim(lblCurrentAsh, imgAsh, currentPlayerData().getAsh());
        String newWordCompletion = newWord.numCompletedLetters() + "/" + newWord.length();
        if (newWord.equals(placeholder)) newWordCompletion = "loading";
        lblCurrentWordCompletion.setText(newWordCompletion);

    }

    private void updateCurWordView(final Spanned targetCurWordTextSpan, int fadeDurationMsec) {
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
