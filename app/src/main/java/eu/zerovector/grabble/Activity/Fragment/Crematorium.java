package eu.zerovector.grabble.Activity.Fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import eu.zerovector.grabble.R;

// Fragment for the Crematorium screen.
public class Crematorium extends Fragment {

    public Crematorium() { }
    public static Crematorium newInstance() {
        return new Crematorium();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_crematorium, container, false);
    }
}
