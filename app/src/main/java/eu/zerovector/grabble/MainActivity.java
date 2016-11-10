package eu.zerovector.grabble;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


// Use the Calligraphy library for custom font support.

// This main activity will inherit Fragment, so as to allow swiping shenanigans
public class MainActivity extends AppCompatActivity {
    private PlayerData registrant = new PlayerData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        // We're handling new player registration in the same activity, so we'll need to handle a few more things.
        // Assign default alignment to "Closers" - they're the good guys, after all.
        changeAlignment(Alignment.Closers);
        // We also need to hide all registration-related fields.
        MM_setPage(false);





    }

    // Add the Calligraphy wrapper.
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    // Also override the "back" button when we need it
    //@Override
    //public void onBackPressed() {
    //    MM_setPage(false); // Return to the "login" "page" if registering
    //}


    // Functions
    public void btnLogin_click(View v) {
        // TODO: LOG IN, SHOW GAME
    }

    public void btnRegister_click(View v) {
        MM_setPage(true);
    }

    public void btnClosers_click(View v) {
        changeAlignment(Alignment.Closers);
    }

    public void btnOpeners_click(View v) {
        changeAlignment(Alignment.Openers);
    }

    public void btnConfirmRegister_click(View v) {
        // TODO: DO REGISTRATION
        // Log player in the game
        //btnLogin_click(v);
    }

    private void changeAlignment(Alignment id) {
        ImageButton o = (ImageButton)findViewById(R.id.btnOpeners);
        ImageButton c = (ImageButton)findViewById(R.id.btnClosers);
        TextView lbl = (TextView)findViewById(R.id.lblAlignment);

        if (id == Alignment.Closers) { // Closers
            // This indexing system is kinda nifty, actually...
            // Though it is ridiculously overcomplicated.
            o.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_dark_semitransparent));
            c.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_semitransparent));
            String text = String.format(getResources().getString(R.string.lblAlignment), "Closers");
            lbl.setText(text);
        }
        else if (id == Alignment.Openers) { // and Openers, too
            o.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_semitransparent));
            c.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_dark_semitransparent));
            String text = String.format(getResources().getString(R.string.lblAlignment), "Openers");
            lbl.setText(text);
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
        }
        else {
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
            for (int i=0; i<childCount; i++) unvisited.add(group.getChildAt(i));
        }

        return visited;
    }


}
