package eu.zerovector.grabble;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Window;
import android.widget.Toast;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

// We'll be using lateral navigation inside this activity for the entire game content.
// Done with the help of StackOverflow (because it's too convoluted otherwise)
// Link: http://stackoverflow.com/questions/18413309/how-to-implement-a-viewpager-with-different-fragments-layouts
public class GameActivity extends FragmentActivity {

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
        ViewPager pager = (ViewPager)findViewById(R.id.viewPager);
        pager.setAdapter(new gamePagerAdapter(getSupportFragmentManager()));
        pager.setCurrentItem(1); // Set the page to the city map

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
            if (lastBackMessageShown != null) lastBackMessageShown.cancel();
        }
        // Otherwise display a relevant message
        else {
            backTaps += 1;
            int tapsRemaining = LOGOUT_TAPS_NEEDED - backTaps;
            String message = "Tap the button " + tapsRemaining + " more times to log out.";
            if (tapsRemaining == 1) message = "Tap the button once more to log out.";
            if (lastBackMessageShown != null) lastBackMessageShown.cancel();
            lastBackMessageShown = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
            lastBackMessageShown.show();
        }
    }



    // Pager adapter implementation
    private class gamePagerAdapter extends FragmentPagerAdapter {
        public gamePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int pos) {
            switch(pos) {
                case 0: return CollectionScreen.newInstance("a","B");
                case 1: return CityMap.newInstance();
                default: return FactionScreen.newInstance("a","B");
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}