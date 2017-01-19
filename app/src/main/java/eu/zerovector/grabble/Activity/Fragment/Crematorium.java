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

// Fragment for the Crematorium screen.
public class Crematorium extends Fragment implements UpdateUIListener {

    private LetterSelector letterSelector;
    private ImageView imgAsh;
    private TextView lblCurrentAsh;


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
        View view = inflater.inflate(R.layout.fragment_crematorium, container, false);

        // Link up for UI updates
        Game.addUIListener(this);

        // "Data-bind" the UI elements
        letterSelector = (LetterSelector)view.findViewById(R.id.letterSelector);
        imgAsh = (ImageView)view.findViewById(R.id.imgAsh);
        lblCurrentAsh = (TextView)view.findViewById(R.id.lblCurrentAsh);

        Picasso.with(getActivity()).load(R.drawable.icon_ash).into(imgAsh);

        // Set correct mode
        letterSelector.setMode(LetterSelector.ModusOperandi.Crematorium);

        // And, most importantly, link up to the letterSelector's button
        final Crematorium linkingInstance = this;
        letterSelector.setActionListener(new LetterSelector.ActionListener() {
            @Override
            public void onClick(Letter selectedLetter) {
                try {
                    Game.onCrematoriumRequest(selectedLetter);
                }
                catch (GrabbleAPIException e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Also, mustn't forget to update the UI properly - apparently this gets re-called every time we switch the pages.
        updateUI(false);

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onUpdateUIReceived(EnumSet<UpdateUIListener.Code> codes, Word oldWord) {
        // This is pulled out to allow me to call updateUI without parameters
        updateUI(true);
    }


    private void updateUI(boolean animateAsh) {
        // Whenever the inventory and ash amounts change, we need to reflect this
        letterSelector.setInventoryData(Game.currentPlayerData().getInventory(),
                XPUtils.getAllDetailsForXP(Game.currentPlayerData().getXP()).traitSet().getInvCapacity());

//        // Seriously, whoever came up with this idea should be hanged, shot, ran over by a train, quartered, and fed to pigs.
//        letterSelector.setActionListener(new LetterSelector.ActionListener() {
//            @Override
//            public void onClick(Letter selectedLetter) {
//                // The LetterSelector class verifies the amounts needed; however, in the case of the Ashery,
//                // we need to manually verify whether we've got enough Ash for the transaction.
//                XPUtils.DataPair playerData = XPUtils.getAllDetailsForXP(Game.currentPlayerData().getXP());
//                int invCap = playerData.traitSet().getInvCapacity();
//                PlayerData.Inventory curInv = Game.currentPlayerData().getInventory();
//                if (Game.currentPlayerData().getAsh() >= selectedLetter.getAshCreateValue()) {
//                    // If all conditions were met (addLetter returns true iff letter already added successfully)
//                    // we can take as much ash as we need and just update the UI.
//                    Game.currentPlayerData().removeAsh(selectedLetter.getAshCreateValue());
//                    Game.grabLetter(selectedLetter, playerData); // YES, YES IT IS MENTAL
//                }
//                else {
//                    Toast.makeText(getActivity(), "Insufficient Ash.", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

        // The ash will be animated // only when we want it to be
        if (animateAsh) AnimUtils.DoGenericAshAnim(lblCurrentAsh, imgAsh, Game.currentPlayerData().getAsh());
        else lblCurrentAsh.setText(String.valueOf(Game.currentPlayerData().getAsh()));

    }
}
