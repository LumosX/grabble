package eu.zerovector.grabble.Activity;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
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
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

import eu.zerovector.grabble.Data.Alignment;
import eu.zerovector.grabble.Data.PlayerData;
import eu.zerovector.grabble.Game;
import eu.zerovector.grabble.Network;
import eu.zerovector.grabble.R;
import eu.zerovector.grabble.Utils.AnimUtils;
import eu.zerovector.grabble.Utils.GrabbleAPIException;
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
    private Button btnOpeners;
    private Button btnClosers;
    private TextView lblOpenersInfo;
    private TextView lblClosersInfo;
    private ImageView imgOpeners;
    private ImageView imgClosers;
    private Alignment registrantAlignment;
    private List<View> allChildViews;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    // permissions request code
    private static final int REQUEST_ALL_PERMISSIONS = 1911;

    private boolean registrationActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove the title bar, if any
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Request all permissions we're going to need (valid for API >= 23)
        // The application dies if they're not granted.
        getAllPermissions();

        // It's really important to load the dictionary and the daily map here.
        // Even before the UI stuff starts happening
        Game.InitialSetup(getApplicationContext(), 0);

        // Also set up the custom font we'll be using.
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Khartiya-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        // Jesus Christ, adding a custom font was more difficult than writing this whole application in C# would've been.

        // Hack the background image in again - done here so it works properly with a scrollview and soft keyboard
        getWindow().setBackgroundDrawableResource(R.drawable.background_main_menu);

        // Initialise the views
        setContentView(R.layout.activity_main);

        // Now do more stuff.
        // Bind the UI variables to the actual components
        BindVarsToUI();

        //Toast.makeText(this, (new Word("NARTHEX")).equals(new Word("NARTHEX")) + "", Toast.LENGTH_LONG).show();

        // We're handling new player registration in the same activity, so we'll need to handle a few more things.
        // Assign default alignment to "Closers" - they're the good guys, after all.
        changeAlignment(Alignment.Closers);
        // We also need to hide all registration-related fields.
        setPage(false);
    }

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
        if (registrationActive) {
            setPage(false);
            return;
        }

        // If not registering...
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
            lastBackMessageShown = Toast.makeText(this, message, Toast.LENGTH_SHORT);
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
        btnOpeners        = (Button)findViewById(R.id.btnOpeners);
        btnClosers        = (Button)findViewById(R.id.btnClosers);
        lblClosersInfo    = (TextView)findViewById(R.id.lblClosersInfo);
        lblOpenersInfo    = (TextView)findViewById(R.id.lblOpenersInfo);
        imgOpeners        = (ImageView)findViewById(R.id.imgOpeners);
        imgClosers        = (ImageView)findViewById(R.id.imgClosers);
        allChildViews = getAllChildrenDFS(findViewById(android.R.id.content));
    }

    public void btnLogin_click(View v) {
        AnimUtils.DoGenericOnClickAnim((Button)v);
        // Log in, or at least try to.
        try {
            PlayerData player = Network.Login(this, tbEmail.getText().toString(), tbPassword.getText().toString());
            Intent game = new Intent(this, GameActivity.class);
            this.startActivityForResult(game, 0);
            Game.onLogin(this, player);
        }
        catch (GrabbleAPIException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void btnRegister_click(View v) {
        AnimUtils.DoGenericOnClickAnim((Button)v);
        // Make the expanded registration controls visible.
        setPage(true);
    }

    public void btnRandomise_click(View v) {
        AnimUtils.DoGenericOnClickAnim((Button)v);
        tbFactionName.setText(Game.getRandomFactionName(registrantAlignment));
    }

    public void btnClosers_click(View v) {
        AnimUtils.DoGenericOnClickAnim((Button)v);
        changeAlignment(Alignment.Closers);
    }

    public void btnOpeners_click(View v) {
        AnimUtils.DoGenericOnClickAnim((Button)v);
        changeAlignment(Alignment.Openers);
    }

    public void btnConfirmRegister_click(View v) {
        AnimUtils.DoGenericOnClickAnim((Button)v);

        PlayerData registrant = new PlayerData(
                tbEmail.getText().toString(),
                tbUsername.getText().toString(),
                tbPassword.getText().toString(),
                tbFactionName.getText().toString(),
                registrantAlignment);
        try {
            Network.Register(this, registrant, tbConfirmPassword.getText().toString());
            // Automatically attempt to log in if all went well.
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
            btnLogin_click(v);
            // In case nothing happens (game not loaded yet), just return to the "login page"
            setPage(false);
        }
        catch (GrabbleAPIException e) {
            Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void changeAlignment(final Alignment id) {
        // Do nothing if the alignment is this one (otherwise the anims glitch out)
        if (registrantAlignment == id) return;

        // Set the alignment first to make sure we don't screw anything up
        registrantAlignment = id;

        // REVISITING MUCH LATER ON: Time for fancy animations.
        ValueAnimator animator = new ValueAnimator();
        animator.setInterpolator(new LinearInterpolator());
        animator.setFloatValues(0.0f, 1.0f);
        animator.setDuration(500);
        final int brightButtonBackground = 0xff000000 | ContextCompat.getColor(this, R.color.UI_GreyTranslucent);
        final int darkButtonBackground = 0xff000000 | ContextCompat.getColor(this, R.color.UI_DarkGreyTranslucent);
        final ArgbEvaluator colourEvaluator = new ArgbEvaluator();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float curProgress = valueAnimator.getAnimatedFraction();
                float invProgress = 1.0f - curProgress; // 1.0 -> 0.0, respectively
                // Note that we don't want the images to reach full brightness
                float clampedProgress = 0.75f * curProgress;
                float invClampedProg = 0.75f * invProgress;
                // This indexing system is kinda nifty, actually...
                // Though it is ridiculously overcomplicated.
                if (id == Alignment.Closers) { // Closers
                    // Fade out the Opener stuff, fade in the Closer stuff
                    int curColour = (int)colourEvaluator.evaluate(curProgress, darkButtonBackground, brightButtonBackground);
                    int invColour = (int)colourEvaluator.evaluate(curProgress, brightButtonBackground, darkButtonBackground);
                    btnClosers.setBackgroundColor(curColour); btnOpeners.setBackgroundColor(invColour);
                    imgClosers.setAlpha(clampedProgress); imgOpeners.setAlpha(invClampedProg);
                    lblClosersInfo.setAlpha(curProgress); lblOpenersInfo.setAlpha(invProgress);
                }
                else { // and Openers, too
                    // Do the opposite of the above
                    int curColour = (int)colourEvaluator.evaluate(curProgress, darkButtonBackground, brightButtonBackground);
                    int invColour = (int)colourEvaluator.evaluate(curProgress, brightButtonBackground, darkButtonBackground);
                    btnOpeners.setBackgroundColor(curColour); btnClosers.setBackgroundColor(invColour);
                    imgOpeners.setAlpha(clampedProgress); imgClosers.setAlpha(invClampedProg);
                    lblOpenersInfo.setAlpha(curProgress); lblClosersInfo.setAlpha(invProgress);
                }
            }
        });
        animator.start();


    }


    // Changes the "page", from login to registration, or vice versa, by looking at the tags
    private void setPage(boolean register) {
        registrationActive = register;
        for (View v : allChildViews) { // What the hell, "in" is apparently in-valid.
            Object t = v.getTag();
            if (t != null && t.equals("MM_REGISTRATION")) v.setVisibility((register) ? View.VISIBLE : View.INVISIBLE);
            else if (t != null && t.equals("MM_LOGIN")) v.setVisibility((register) ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private List<View> getAllChildrenDFS(View v) {
        List<View> children = new ArrayList<>();

        if (!(v instanceof ViewGroup))
            return new ArrayList<>();

        ViewGroup group = (ViewGroup) v;

        for (int i = 0; i < group.getChildCount(); i++){
            View child = group.getChildAt(i);
            children.add(child);
            children.addAll(getAllChildrenDFS(child));
        }

        return children;
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
            Toast.makeText(this, "Grabble can't operate without those permissions.\n" +
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
