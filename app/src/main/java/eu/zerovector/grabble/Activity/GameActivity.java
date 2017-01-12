package eu.zerovector.grabble.Activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Window;
import android.widget.Toast;

import eu.zerovector.grabble.Activity.Fragment.CityMap;
import eu.zerovector.grabble.Activity.Fragment.CollectionHost;
import eu.zerovector.grabble.Activity.Fragment.FactionScreen;
import eu.zerovector.grabble.Game;
import eu.zerovector.grabble.R;
import fr.castorflex.android.verticalviewpager.VerticalViewPager;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

// We'll be using lateral navigation inside this activity for the entire game content.
// Done with the help of StackOverflow (because it's too convoluted otherwise)
// Link: http://stackoverflow.com/questions/18413309/how-to-implement-a-viewpager-with-different-fragments-layouts
// I read more stuff too, but who remembers all the links
public class GameActivity extends FragmentActivity {

    private ViewPager masterPager;
    private VerticalViewPager nestedFragmentPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Again, remove any potential title bar and set the custom font up
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/charter_regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        // Do everything else
        setContentView(R.layout.activity_game);

        // Fix pager
        masterPager = (ViewPager)findViewById(R.id.viewPager);
        masterPager.setAdapter(new GamePagerAdapter(this, getSupportFragmentManager()));
        masterPager.setCurrentItem(1); // Set the page to the city map
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    // Here, we also override the back button, but to allow the user to log out instead.
    private long lastBackPressTime = 0;
    private int backTaps = 0;
    private final int LOGOUT_TAP_INTERVAL_MILLISECONDS = 2000;
    private final int LOGOUT_TAPS_NEEDED = 5;
    private Toast lastBackMessageShown = null;
    @Override
    public void onBackPressed() {
        // Allow the user to exit to main menu
        long currentPressTime = System.currentTimeMillis();
        if (currentPressTime - lastBackPressTime > LOGOUT_TAP_INTERVAL_MILLISECONDS) {
            backTaps = 0;
        }
        lastBackPressTime = currentPressTime;

        // Kill if the button has been pressed enough times
        if (backTaps >= LOGOUT_TAPS_NEEDED - 1) {
            this.setResult(Game.GLOBAL_ACTIVITY_RESULT_LOGOUT);
            this.finish();
            // Notify the game that we're logging out
            Game.onLogout();
            if (lastBackMessageShown != null) lastBackMessageShown.cancel();
        }
        // Otherwise display a relevant message
        else {
            backTaps += 1;
            int tapsRemaining = LOGOUT_TAPS_NEEDED - backTaps;
            String message = "Tap the button " + tapsRemaining + " more times to log out.";
            if (tapsRemaining == 1) message = "Tap the button once more to log out.";
            if (lastBackMessageShown != null) lastBackMessageShown.cancel();
            lastBackMessageShown = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            lastBackMessageShown.show();
        }
    }

    public void attachChildPager(VerticalViewPager childPager) {
        if (childPager == null) throw new IllegalArgumentException("Pager not initialised yet!");
        nestedFragmentPager = childPager;

        // Attach new listeners: upon returning to the "City Map" fragment on the master, force-set the child to the "Collection" view
        // Look at how much pointless code Java is forcing me to add. SHAME!
        masterPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int page; // I shouldn't be allowed to do this. This is INSANE.

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
            @Override
            public void onPageScrollStateChanged(int state) {
                // Force the page to 1 ("Collection") when the animation stops
                if (state == ViewPager.SCROLL_STATE_IDLE && page == 1) {
                    nestedFragmentPager.setCurrentItem(1); // the "Collection" screen
                }
            }
            @Override
            public void onPageSelected(int position) {
                this.page = position;
            }
        });
    }

    // Pager adapter implementation. This also needs to remember who created in, in order to be the "middle-man"
    private class GamePagerAdapter extends FragmentPagerAdapter {
        private GameActivity master;

        public GamePagerAdapter(GameActivity master, FragmentManager fm) {
            super(fm);
            this.master = master;
        }

        @Override
        public Fragment getItem(int pos) {
            switch(pos) {
                case 0: return CollectionHost.newInstance(master);
                case 1: return CityMap.newInstance();
                default: return FactionScreen.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}