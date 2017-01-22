package eu.zerovector.grabble.Activity.Fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.EnumSet;

import eu.zerovector.grabble.Activity.UpdateUIListener;
import eu.zerovector.grabble.Data.Letter;
import eu.zerovector.grabble.Data.Word;
import eu.zerovector.grabble.Data.XPUtils;
import eu.zerovector.grabble.Game;
import eu.zerovector.grabble.R;
import eu.zerovector.grabble.Utils.AnimUtils;
import eu.zerovector.grabble.Utils.GrabbleAPIException;

// Fragment for the Map screen.
public class Ashery extends Fragment implements UpdateUIListener {

    private LetterSelector letterSelector;
    private ImageView imgAsh;
    private TextView lblCurrentAsh;

    private boolean canReceiveUIUpdates = false;

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

        View view = inflater.inflate(R.layout.fragment_ashery, container, false);

        // Link up for UI updates
        Game.addUIListener(this);

        // "Data-bind" the UI elements
        letterSelector = (LetterSelector)view.findViewById(R.id.letterSelector);
        imgAsh = (ImageView)view.findViewById(R.id.imgAsh);
        lblCurrentAsh = (TextView)view.findViewById(R.id.lblCurrentAsh);

        Picasso.with(getActivity()).load(R.drawable.icon_ash).into(imgAsh);

        // Add the "action listener" to the ashery UI
        letterSelector.setMode(LetterSelector.ModusOperandi.Ashery); // just for safety

        letterSelector.setActionListener(new LetterSelector.ActionListener() {
            @Override
            public void onClick(Letter selectedLetter) {
                try {
                    Game.onAsheryRequest(getActivity(), selectedLetter);
                }
                catch (GrabbleAPIException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Also, mustn't forget to update the UI properly - apparently this gets re-called every time we switch the pages.
        canReceiveUIUpdates = true;
        updateUI(false);

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onUpdateUIReceived(EnumSet<Code> codes, Word oldWord) {
        // This is pulled out to allow me to call updateUI without parameters
        updateUI(true);
    }


    private void updateUI(boolean animateAsh) {
        if (!canReceiveUIUpdates || getContext() == null) return;

        // Whenever the inventory and ash amounts change, we need to reflect this
        letterSelector.setInventoryData(Game.currentPlayerData().getInventory(),
                XPUtils.getAllDetailsForXP(Game.currentPlayerData().getXP()).traitSet().getInvCapacity());

        // The ash will be animated // only when we want it to be
        if (animateAsh) AnimUtils.DoGenericAshAnim(lblCurrentAsh, imgAsh, Game.currentPlayerData().getAsh());
        else lblCurrentAsh.setText(String.valueOf(Game.currentPlayerData().getAsh()));
    }
}
