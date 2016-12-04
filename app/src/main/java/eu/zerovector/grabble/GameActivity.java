package eu.zerovector.grabble;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Window;

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

        // Also don't forget to set the actual game up



        ViewPager pager = (ViewPager)findViewById(R.id.viewPager);
        pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
        pager.setCurrentItem(1); // Set the page to the city map

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }




    // Pager adapter implementation
    private class MyPagerAdapter extends FragmentPagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int pos) {
            switch(pos) {
                case 0: return CollectionScreen.newInstance("a","B");
                case 1: return CityMap.newInstance("a","B");
                default: return FactionScreen.newInstance("a","B");
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}