package eu.zerovector.grabble;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // It's really important to load the dictionary and the daily map here.
        // Even before the UI stuff starts happening
        Game.InitialSetup(getApplicationContext());

        // Remove the title bar.
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

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
        MM_setPage(false);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // Add the Calligraphy wrapper.
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    // Also override the "back" button when we need it
    @Override
    public void onBackPressed() {
        MM_setPage(false); // Return to the "login" "page" if registering
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
        // TODO: LOG IN, SHOW GAME
        if (Network.Login(tbUsername.getText().toString(), tbPassword.getText().toString())) {
            Intent myIntent = new Intent(this, GameActivity.class);
            //myIntent.putExtra("key", value); //Optional parameters
            this.startActivity(myIntent);
        }
        else Toast.makeText(getApplicationContext(), "Couldn't log in: details not recognised", Toast.LENGTH_LONG).show();
    }

    public void btnRegister_click(View v) {
        // Make the expanded registration controls visible.
        MM_setPage(true);
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
        if (regResult.equals(Network.REGISTER_SUCCESSFUL)) btnLogin_click(v);
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
    private void MM_setPage(boolean register) {
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


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
