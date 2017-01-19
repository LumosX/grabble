package eu.zerovector.grabble.Activity.Fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
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

import eu.zerovector.grabble.Activity.UpdateUIListener;
import eu.zerovector.grabble.Data.Letter;
import eu.zerovector.grabble.Data.Word;
import eu.zerovector.grabble.Data.XPUtils;
import eu.zerovector.grabble.Game;
import eu.zerovector.grabble.R;
import eu.zerovector.grabble.Utils.AnimUtils;

import static eu.zerovector.grabble.Game.currentPlayerData;

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
    private ImageView imgAsh;
    private TextView lblCurrentAsh;
    private TextView lblCurrentXP;
    private TextView lblCurrentGrabRange;
    private TextView lblCurrentSightRange;
    private TableLayout tblLetters;
    private boolean levelUpAnimRunning = false;


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
        imgAsh = (ImageView)view.findViewById(R.id.imgAsh);
        lblCurrentXP = (TextView)view.findViewById(R.id.lblCurrentXP);
        lblCurrentGrabRange = (TextView)view.findViewById(R.id.lblCurrentGrabRange);
        lblCurrentSightRange = (TextView)view.findViewById(R.id.lblCurrentSightRange);
        tblLetters = (TableLayout)view.findViewById(R.id.tblLetters);
        levelUpAnimRunning = false;

        // debug
        Game.currentPlayerData().setXP(1000000);

        // Link up to the Game class
        Game.addUIListener(this);

        // We can set the name up here - after all, the player's name never changes.
        lblPlayerName.setText(Game.currentPlayerData().getUsername());

        // Populate everything else in the re-usable function.
        updateViews(EnumSet.noneOf(Code.class));

        return view;
    }

    private void updateViews(EnumSet<Code> updateCodes) {
        // ALL THE THINGS AT THE TOP
        // Ash
        lblCurrentAsh.setText(String.valueOf(Game.currentPlayerData().getAsh()));
        // XP and level progress
        // Unfortunately, the only thing which remembers what the old XP was is the actual counter
        int oldXP = Integer.parseInt(lblCurrentXP.getText().toString().split("/")[0]);
        int curXP = Game.currentPlayerData().getXP();
        final XPUtils.LevelDetails levelStats = XPUtils.getLevelDetailsForXP(curXP);
        lblCurrentRank.setText(String.valueOf(levelStats.level()));
            ValueAnimator animatorXP = new ValueAnimator();
            animatorXP.setInterpolator(new LinearInterpolator());
            animatorXP.setIntValues(oldXP, curXP);
            animatorXP.setDuration(1250);
            final int t = levelStats.thisLevelXP();
            animatorXP.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    int intermediateXP = (int) animation.getAnimatedValue();
                    int progress = (int)((float)(intermediateXP - t)/(float)(levelStats.nextLevelXP() - t) * 100);
                    progress = clampInt(progress, 0, 100);
                    prbExperience.setProgress(progress);
                    lblCurrentXP.setText(intermediateXP + "/" + levelStats.nextLevelXP());
                }
            });
            animatorXP.start();
        // However, if we levelled up, do some fancy-schmancy animating
        if (updateCodes.contains(Code.LEVEL_INCREASED)) {
            levelUpAnimRunning = true;
            final int startColour = 0xff000000 | ContextCompat.getColor(getActivity(), R.color.White);
            final int endColour = 0xff000000 | ContextCompat.getColor(getActivity(), R.color.Goldenrod);
            final ArgbEvaluator colorEvaluator = new ArgbEvaluator();
            ValueAnimator animator = new ValueAnimator();
            animator.setInterpolator(new LinearInterpolator());
            animator.setFloatValues(0.0f, 1.0f);
            animator.setDuration(625);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(1);
            animator.setStartDelay(125); // Give a little time to the XP counter to get running with its own animation
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float curFraction = valueAnimator.getAnimatedFraction();
                    int curColour = (int)colorEvaluator.evaluate(curFraction, startColour, endColour);
                    prbExperience.setFinishedStrokeColor(curColour);
                    lblCurrentRank.setTextColor(curColour);
                    // And also, a trick: after all, we're animating from 0 to 1 and back...
                    lblRankName.setAlpha(1.0f - curFraction); // make this fade out, then back again
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    lblRankName.setText(XPUtils.getLevelName(levelStats.level(), Game.currentPlayerData().getAlignment()));
                    levelUpAnimRunning = false;
                }
            });
            animator.start();
        }

        // Update rank name iff level-up animation not running
        if (!levelUpAnimRunning) {
            lblRankName.setText(XPUtils.getLevelName(levelStats.level(), Game.currentPlayerData().getAlignment()));
        }

        // Now do the same thing with the ash that we did in CityMap
        // I think Ash looks better when it's not using a linear interpolator.
        AnimUtils.DoGenericAshAnim(lblCurrentAsh, imgAsh, currentPlayerData().getAsh());
        // Sight and Grab ranges
        XPUtils.TraitSet perks = XPUtils.getPerksForLevel(levelStats.level());
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
        // AS USUAL, IF YOU WANT SOMETHING DONE RIGHT, YOU SHOULD DO IT YOURSELF. BY INFLATING TABLE CELLS. BECAUSE WHY THE DUCK NOT??!
        // also, I ain't animating this crap. it's not impossible as-is, but I'd want to kill myself afterwards.
        // So: Get tblLetters
        int NUM_COLS = 3;
        int numRows = (int)Math.ceil(vals.size()/(float)NUM_COLS);
        //numRows = 3;
        // Inflate rows.
        List<TableRow> rows = new ArrayList<>();
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Pre-set colours based on count ratio
        int colourStart = 0xff000000 | ContextCompat.getColor(getActivity(), R.color.UI_DarkGrey);
        int colourEnd = 0xffffffff; // White. No need to call anything if we know the code for 'white', right?
        for (int i = 0; i < numRows; i++) {
            View rowView = inflater.inflate(R.layout.collection_layout_letter_table_row, null);
            //TableRow x = (TableRow)rowView.findViewById(R.id.rowTableRow);
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

                    float ratio = (float)letterCountsInts[itemIndex]/(float)letterCapacity;
                    int curColour = (int)new ArgbEvaluator().evaluate(ratio, colourStart, colourEnd);
                    lblLetterVal.setTextColor(curColour);
                    lblLetterCount.setTextColor(curColour);
                }
                // Otherwise simply set fields to empty text (cell number per x must be preserved)
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
    public void onUpdateUIReceived(EnumSet<Code> codes, Word oldWord) {
        updateViews(codes);
    }
}
