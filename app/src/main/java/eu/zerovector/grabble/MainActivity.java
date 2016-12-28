package eu.zerovector.grabble;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

//import com.google.android.gms.appindexing.Action;
//import com.google.android.gms.appindexing.AppIndex;
//import com.google.android.gms.appindexing.Thing;


// Use the Calligraphy library for custom font support.

// This main activity will inherit Fragment, so as to allow swiping shenanigans
public class MainActivity extends AppCompatActivity {
    // UI binding vars, done caveman-style. Do not modify.
    // Yes, yes, I know that's not the 'correct' way to do data binding. Whatever. It works.
    private EditText tbEmail;
    private EditText tbPassword;
    private EditText tbConfirmPassword;
    private EditText tbUsername;
    private EditText tbFactionName;
    private ImageButton btnOpeners;
    private ImageButton btnClosers;
    private TextView lblAlignment;
    private Alignment registrantAlignment;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    // permissions request code
    private static final int REQUEST_ALL_PERMISSIONS = 1911;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove the title bar.
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Request all permissions we're going to need (valid for API >= 23)
        // The application dies if they're not granted.
        getAllPermissions();

        // It's really important to load the dictionary and the daily map here.
        // Even before the UI stuff starts happening
        Game.InitialSetup(getApplicationContext());

        // Also set up the custom font we'll be using.
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/charter_regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        // Jesus Christ, adding a custom font was more difficult than writing this whole application in C# would've been.

        // Initialise the views
        setContentView(R.layout.activity_main);

        // Now do more stuff.
        // Bind the UI variables to the actual components
        BindVarsToUI();

        //Toast.makeText(getApplicationContext(), (new Word("NARTHEX")).equals(new Word("NARTHEX")) + "", Toast.LENGTH_LONG).show();


        // We're handling new player registration in the same activity, so we'll need to handle a few more things.
        // Assign default alignment to "Closers" - they're the good guys, after all.
        changeAlignment(Alignment.Closers);
        // We also need to hide all registration-related fields.
        setPage(false);
    }

    // Add the Calligraphy wrapper.
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    // Also override the "back" button when we need it
    private long lastBackPressTime = 0;
    private int backTaps = 0;
    private final int QUIT_TAP_INTERVAL_MILLISECONDS = 2000;
    private final int QUIT_TAPS_NEEDED = 5;
    private Toast lastBackMessageShown = null;
    @Override
    public void onBackPressed() {
        // Return to the "login" "page" if registering
        setPage(false);

        // Allow the user to quit the application upon five quick presses
        long currentPressTime = System.currentTimeMillis();
        if (currentPressTime - lastBackPressTime > QUIT_TAP_INTERVAL_MILLISECONDS) {
            backTaps = 0;
        }
        lastBackPressTime = currentPressTime;

        // Kill if the button has been pressed enough times
        if (backTaps >= QUIT_TAPS_NEEDED - 1) {
            this.finishAffinity();
            if (lastBackMessageShown != null) lastBackMessageShown.cancel();
        }
        // Otherwise display a relevant message
        else {
            backTaps += 1;
            int tapsRemaining = QUIT_TAPS_NEEDED - backTaps;
            String message = "Tap the button " + tapsRemaining + " more times to exit.";
            if (tapsRemaining == 1) message = "Tap the button once more to exit.";
            if (lastBackMessageShown != null) lastBackMessageShown.cancel();
            lastBackMessageShown = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
            lastBackMessageShown.show();
        }
    }

    // Functions
    private void BindVarsToUI() { // "data" "binding"
        tbEmail           = (EditText)findViewById(R.id.tbEmail);
        tbPassword        = (EditText)findViewById(R.id.tbPass);
        tbConfirmPassword = (EditText)findViewById(R.id.tbConfirmPass);
        tbUsername        = (EditText)findViewById(R.id.tbUsername);
        tbFactionName     = (EditText)findViewById(R.id.tbFactionName);
        btnOpeners        = (ImageButton)findViewById(R.id.btnOpeners);
        btnClosers        = (ImageButton)findViewById(R.id.btnClosers);
        lblAlignment      = (TextView)findViewById(R.id.lblAlignment);
    }

    public void btnLogin_click(View v) {
        // If the game hasn't loaded yet, display it.
        if (Network.Login(tbEmail.getText().toString(), tbPassword.getText().toString())) {
            Intent game = new Intent(this, GameActivity.class);
            //myIntent.putExtra("key", value); //Optional parameters
            this.startActivityForResult(game, 0);
        }
        else Toast.makeText(getApplicationContext(), "Couldn't log in: details not recognised", Toast.LENGTH_LONG).show();
    }

    public void btnRegister_click(View v) {
        // Make the expanded registration controls visible.
        setPage(true);
    }

    public void btnRandomise_click(View v) {
        tbFactionName.setText(Game.getRandomFactionName(registrantAlignment));
    }

    public void btnClosers_click(View v) {
        changeAlignment(Alignment.Closers);
    }

    public void btnOpeners_click(View v) {
        changeAlignment(Alignment.Openers);
    }

    public void btnConfirmRegister_click(View v) {
        PlayerData registrant = new PlayerData(tbEmail.getText().toString(), tbUsername.getText().toString(),
                tbPassword.getText().toString(), tbFactionName.getText().toString(), registrantAlignment);
        String regResult = Network.Register(registrant, tbConfirmPassword.getText().toString()); // I'm a lazy bastard, I know
        if (regResult.equals(Network.REGISTER_SUCCESSFUL)) {
            // Automatically attempt to log in.
            btnLogin_click(v);
            // In case nothing happens (game not loaded yet), just return to the "login page"
            setPage(false);
        }
        else
            Toast.makeText(getApplicationContext(), "Registration failed: " + regResult, Toast.LENGTH_LONG).show();
    }

    private void changeAlignment(Alignment id) {
        registrantAlignment = id;

        if (id == Alignment.Closers) { // Closers
            // This indexing system is kinda nifty, actually...
            // Though it is ridiculously overcomplicated.
            btnOpeners.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_dark_semitransparent));
            btnClosers.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_semitransparent));
            String text = String.format(getResources().getString(R.string.lblAlignment), "Closers");
            lblAlignment.setText(text);
        } else if (id == Alignment.Openers) { // and Openers, too
            btnOpeners.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_semitransparent));
            btnClosers.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_dark_semitransparent));
            String text = String.format(getResources().getString(R.string.lblAlignment), "Openers");
            lblAlignment.setText(text);
        }
    }


    // Changes the "page", from login to registration, or vice versa, by looking at the tags
    private void setPage(boolean register) {
        List<View> views = getAllChildrenBFS(findViewById(android.R.id.content));
        if (register) {
            for (View v : views) { // What the hell, "in" is apparently in-valid.
                Object t = v.getTag();
                if (t != null && t.equals("MM_REGISTRATION")) v.setVisibility(View.VISIBLE);
                else if (t != null && t.equals("MM_LOGIN")) v.setVisibility(View.INVISIBLE);
            }
        } else {
            for (View v : views) {
                Object t = v.getTag();
                if (t != null && t.equals("MM_REGISTRATION")) v.setVisibility(View.INVISIBLE);
                else if (t != null && t.equals("MM_LOGIN")) v.setVisibility(View.VISIBLE);
            }
        }
    }


    // Helper method, lifted off of StackOverflow, because fuck me, mine didn't work.
    // http://stackoverflow.com/questions/18668897/android-get-all-children-elements-of-a-viewgroup
    private List<View> getAllChildrenBFS(View v) {
        List<View> visited = new ArrayList<View>();
        List<View> unvisited = new ArrayList<View>();
        unvisited.add(v);

        while (!unvisited.isEmpty()) {
            View child = unvisited.remove(0);
            visited.add(child);
            if (!(child instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) child;
            final int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) unvisited.add(group.getChildAt(i));
        }

        return visited;
    }

    // Self-explanatory
    private void getAllPermissions() {
        // Check whether we've already got the permissions
        int ok = PackageManager.PERMISSION_GRANTED; // shortening
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == ok &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == ok &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == ok &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == ok) return;

        // If any of these are missing, request them all.
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_ALL_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            // This sodding language doesn't even have 'goto'!
            allgood: if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_DENIED) break allgood;
                }
                return;
            }
            // These labelled breaks are fun though!
            // ELSE:
            // If at least one of the permissions was denied, kill the app and show the toast for it.
            Toast.makeText(getApplicationContext(), "Grabble can't operate without those permissions.\n" +
                    "Please allow their use to continue.",Toast.LENGTH_LONG).show();
            this.finishAffinity();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Game.GLOBAL_ACTIVITY_RESULT_KILL) { // KILL CODE
            this.finishAffinity();
        }
    }
}
