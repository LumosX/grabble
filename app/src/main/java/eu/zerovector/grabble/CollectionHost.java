package eu.zerovector.grabble;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.castorflex.android.verticalviewpager.VerticalViewPager;

// Fragment for the collection screen - this fragment will host the nested fragments with the real screens.
public class CollectionHost extends Fragment {

    // We'll be using an ad-hoc observer pattern here, in order to notify the game activity of things
    private GameActivity master;

    public CollectionHost() {
        // Required empty public constructor
    }
    public static CollectionHost newInstance(GameActivity gameActivity) {
        // This one actually does something
        CollectionHost host = new CollectionHost();
        host.master = gameActivity;
        return host;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_collection_host, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        VerticalViewPager childPager = (VerticalViewPager)view.findViewById(R.id.verticalViewPager);
        childPager.setAdapter(new ChildPagerAdapter(getChildFragmentManager()));
        childPager.setCurrentItem(1); // again, we're interested in the middle screen
        // Attach pager to master
        if (master != null) master.attachChildPager(childPager);
    }

    // Pager adapter implementation
    private class ChildPagerAdapter extends FragmentPagerAdapter {
        public ChildPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int pos) {
            switch(pos) {
                case 0: return Ashery.newInstance();
                case 1: return CollectionScreen.newInstance();
                default: return Crematorium.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
