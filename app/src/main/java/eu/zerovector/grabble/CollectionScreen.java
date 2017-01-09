package eu.zerovector.grabble;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

// Fragment for the real collection screen.
public class CollectionScreen extends Fragment implements UpdateUIListener {

    public CollectionScreen() { }
    public static CollectionScreen newInstance() {
        return new CollectionScreen();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Remember all the views and stuff that'll change
    private RelativeLayout rootView;
    private TextView lblPlayerName;
    private TextView lblRankName;
    private ArcProgress prbExperience;
    private TextView lblCurrentRank;
    private TextView lblCurrentAsh;
    private TextView lblCurrentXP;
    private TextView lblCurrentGrabRange;
    private TextView lblCurrentSightRange;
    private TableLayout tblLetters;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_collection_screen, container, false);

        // Load background picture and all the other icons
        Picasso.with(getActivity()).load(R.drawable.background_collection).into((ImageView)view.findViewById(R.id.backgroundImage));
        Picasso.with(getActivity()).load(R.drawable.icon_ash).into((ImageView)view.findViewById(R.id.imgAsh));
        Picasso.with(getActivity()).load(R.drawable.icon_xp).into((ImageView)view.findViewById(R.id.imgXP));
        Picasso.with(getActivity()).load(R.drawable.icon_grab).into((ImageView)view.findViewById(R.id.imgGrab));
        Picasso.with(getActivity()).load(R.drawable.icon_sight).into((ImageView)view.findViewById(R.id.imgSight));

        // Do the "binding" here. I used this very helpful tool: https://www.buzzingandroid.com/tools/android-layout-finder/
        rootView = (RelativeLayout)view;
        lblPlayerName = (TextView)view.findViewById(R.id.lblPlayerName);
        lblRankName = (TextView)view.findViewById(R.id.lblRankName);
        prbExperience = (ArcProgress)view.findViewById(R.id.prbExperience);
        lblCurrentRank = (TextView)view.findViewById(R.id.lblCurrentRank);
        lblCurrentAsh = (TextView)view.findViewById(R.id.lblCurrentAsh);
        lblCurrentXP = (TextView)view.findViewById(R.id.lblCurrentXP);
        lblCurrentGrabRange = (TextView)view.findViewById(R.id.lblCurrentGrabRange);
        lblCurrentSightRange = (TextView)view.findViewById(R.id.lblCurrentSightRange);
        tblLetters = (TableLayout)view.findViewById(R.id.tblLetters);

        // Link up to the Game class
        Game.addUIListener(this);

        // debug
        //Game.currentPlayerData().setXP(300);

        // We can set the name up here - after all, the player's name never changes.
        lblPlayerName.setText(Game.currentPlayerData().getUsername());

        // Populate everything else in the re-usable function.
        updateViews();

        return view;
    }

    private void updateViews() {
        // ALL THE THINGS AT THE TOP
        // Ash
        lblCurrentAsh.setText(String.valueOf(Game.currentPlayerData().getAsh()));
        // XP and level progress
        int curXP = Game.currentPlayerData().getXP();
        Experience.LevelDetails levelStats = Experience.getLevelDetailsForXP(curXP);
        lblCurrentXP.setText(curXP + "/" + levelStats.nextLevelXP());
        lblCurrentRank.setText(String.valueOf(levelStats.level()));
        int progress = (int)((double)(curXP-levelStats.thisLevelXP())/(double)levelStats.nextLevelXP() * 100);
        progress = clampInt(progress, 0, 100);
        prbExperience.setProgress(progress);
        // and rank name
        lblRankName.setText(Experience.getLevelName(levelStats.level(), Game.currentPlayerData().getAlignment()));
        // Sight and Grab ranges
        Experience.TraitSet perks = Experience.getPerksForLevel(levelStats.level());
        lblCurrentGrabRange.setText(perks.getGrabRange() + " m");
        lblCurrentSightRange.setText(perks.getSightRange() + " m");



        // NOW THE TABLE WITH ALL THE LETTERS IN
        // This took me absolute bloody ages to make sort of right...
        // Clear the table first.
        tblLetters.removeAllViews();
        // Grab all the things.
        int[] letterCountsInts = Game.currentPlayerData().getInventory().getLetterCounts();
        int letterCapacity = perks.getInvCapacity();
        // We need to split them in two and pair them up, and this language is complete trash
        List<String> vals = new ArrayList<>();
        List<String> counts = new ArrayList<>();
        for (int i = 0, n = letterCountsInts.length; i < n; i++) {
            String letter = Letter.values[i].toString();
            vals.add(letter);
            counts.add(letterCountsInts[i] + "/" + letterCapacity); // how could you let THE BASIC INT not have a .toString()?!
        }

        // I tried using gridviews, listviews, adapters and God knows what else. NOTHING WORKS.
        // AS USUAL, IF YOU WANT SOMETHING DONE RIGHT, YOU SHOULD DO IT YOURSELF. BY INFLATING TABLE CELLS. BECAUSE WHY THE FUCK NOT.
        // So: Get tblLetters
        int NUM_COLS = 3;
        int numRows = (int)Math.ceil(vals.size()/(float)NUM_COLS);
        //numRows = 3;
        // Inflate rows.
        List<TableRow> rows = new ArrayList<>();
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        for (int i = 0; i < numRows; i++) {
            View rowView = inflater.inflate(R.layout.collection_layout_letter_table_row, null);
            //TableRow row = (TableRow)rowView.findViewById(R.id.rowTableRow);
            TableRow row = (TableRow)rowView;
            // Fill rows with respective cell values
            for (int j = 0; j < NUM_COLS; j++) {
                int itemIndex = i + numRows * j;
                // Yes, I know this is terrible, but I've had enough of this shit
                int valID, countID;
                if (j == 0) {
                    valID = R.id.lblLetterID1; countID = R.id.lblLetterCount1;
                }
                else if (j == 1) {
                    valID = R.id.lblLetterID2; countID = R.id.lblLetterCount2;
                }
                else {
                    valID = R.id.lblLetterID3; countID = R.id.lblLetterCount3;
                }
                TextView lblLetterVal = (TextView)rowView.findViewById(valID);
                TextView lblLetterCount = (TextView)rowView.findViewById(countID);
                // If the element is within our array, get data and fill cells
                if (itemIndex < vals.size()) {
                    lblLetterVal.setText(vals.get(itemIndex));
                    lblLetterCount.setText(counts.get(itemIndex));
                    // Set colours based on count ratio
                    int start = ContextCompat.getColor(getActivity(), R.color.UI_DarkGrey);
                    int end = 0xffffff; // White. No need to call anything if we know the code for 'white', right?
                    float ratio = (float)letterCountsInts[itemIndex] / letterCapacity;
                    int curColour = (int)new ArgbEvaluator().evaluate(ratio, start, end);
                    lblLetterVal.setTextColor(curColour);
                    lblLetterCount.setTextColor(curColour);
                }
                // Otherwise simply set fields to empty text (cell number per row must be preserved)
                else {
                    lblLetterVal.setText("");
                    lblLetterCount.setText("");
                }
            }
            tblLetters.addView(row);
        }
    }

    // I know I shouldn't do it like this, but I will anyway.
    private int clampInt(int val, int min, int max) {
        if (val < min) return min;
        if (val > max) return max;
        else return val;
    }

    @Override
    public void onUpdateUIReceived(EnumSet<Code> codes) {
        updateViews();
    }
}
