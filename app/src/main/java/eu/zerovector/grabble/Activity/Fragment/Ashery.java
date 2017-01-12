package eu.zerovector.grabble.Activity.Fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import eu.zerovector.grabble.R;

// Fragment for the Map screen.
public class Ashery extends Fragment {

    public Ashery() { }
    public static Ashery newInstance() {
        return new Ashery();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ashery, container, false);
    }
}
