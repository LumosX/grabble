package eu.zerovector.grabble;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// Fragment for the Map screen.
public class CollectionScreen extends Fragment {

    public CollectionScreen() {
        // Required empty public constructor
    }


    public static CollectionScreen newInstance(String param1, String param2) {
        CollectionScreen f = new CollectionScreen();
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_collection_screen, container, false);
    }
}
