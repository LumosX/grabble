package eu.zerovector.grabble.Activity.Fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import eu.zerovector.grabble.Data.Alignment;
import eu.zerovector.grabble.Data.Inventory;
import eu.zerovector.grabble.Data.Letter;
import eu.zerovector.grabble.Data.XPUtils;
import eu.zerovector.grabble.Game;
import eu.zerovector.grabble.R;
import eu.zerovector.grabble.Utils.AnimUtils;

// A custom view to make setting up the Ashery and Crematorium easier
public class LetterSelector extends RelativeLayout {
    private View view;

    private TableLayout table;
    private TextView[][] letterViews;
    private Letter selectedLetter = Letter.A;
    private Inventory curInventory = null;
    private int curInventoryCap = 5;
    private RowCol markerPos = new RowCol(0,0);
    private ModusOperandi currentMode = ModusOperandi.Ashery;
    private ActionListener listener;
    private boolean actionButtonEnabled = false;

    // Again, I used this: https://www.buzzingandroid.com/tools/android-layout-finder/
    private List<View> allSubViews; // (with some manual additions)
    private ImageView LetSelSelectionMarker;
    private Button LetSelBtnUpwards;
    private Button LetSelBtnLeftwards;
    private Button LetSelBtnDownwards;
    private Button LetSelBtnRightwards;
    private TextView LetSelCurLetterLarge;
    private TextView LetSelCurLetterAuxiliary;
    private TextView LetSelCurLetterDescription;
    private TextView LetSelInvCount;
    private TextView LetSelActionLine1;
    private ImageView imgAsh;
    private TextView LetSelActionAshValue;
    private TextView LetSelActionLine3;
    private Button LetSelBtnDoAction;

    private final int ACTION_BUTTON_ENABLED_BGCOL;
    private final int ACTION_BUTTON_DISABLED_BGCOL;
    private final int DEFAULT_BUTTON_TARGET_COLOUR;

    public LetterSelector(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Really sorry, but I'll do it the garbage way and just inflate a layout.
        // It's cheap, I know, but I've got no desire to write one from scratch right now.
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.layout_letter_selector, this, true);
        table = (TableLayout)view.findViewById(R.id.LetSelParentTable);
        allSubViews = getAllChildrenBFS(table);
        // Link up all the letter-button-views first.
        letterViews = new TextView[3][9]; // I am going in-saaaa-ne
        for (View subView : allSubViews) {
            Object tag = subView.getTag();
            if (tag != null && tag.toString().startsWith("LetterButton")) {
                subView.setClickable(true);
                subView.setOnClickListener(letterButtonClicked);
                RowCol pos = RowCol.parseTagData(tag.toString());
                letterViews[pos.row][pos.col] = (TextView)subView;
            }
            // Also if it's a text view, make it white
            if (subView instanceof TextView) {
                ((TextView)subView).setTextColor(Color.WHITE);
            }
        }

        // Link up all the named views as well
        findNamedViews(view);

        // Load the picture manually - we'll be using this in a fragment
        // However, this throws errors during editing mode, so this should help
        if (!isInEditMode()) {
            Picasso.with(context).load(R.drawable.icon_ash).into(imgAsh);
        }

        // While we're here, we can take advantage of the context and grab some colours
        ACTION_BUTTON_ENABLED_BGCOL = ContextCompat.getColor(context, R.color.UI_GreyTranslucent);
        ACTION_BUTTON_DISABLED_BGCOL = ContextCompat.getColor(context, R.color.UI_DarkGreyTranslucent);
        DEFAULT_BUTTON_TARGET_COLOUR = ContextCompat.getColor(context, R.color.UI_WhiteTranslucent);

        // Then init everything else
        // By default, select letter A in "Ashery" mode
        selectElement(new RowCol(0,0));
        setMode(ModusOperandi.Ashery);
        // Load the current player's inventory... we'll rely on the caller to update it though
        int curInvCap = XPUtils.getAllDetailsForXP(Game.currentPlayerData().getXP()).traitSet().getInvCapacity();
        setInventoryData(Game.currentPlayerData().getInventory(), curInvCap);
    }


    private OnClickListener letterButtonClicked = new OnClickListener() {
        @Override
        public void onClick(View view) {
            String tag = view.getTag().toString();
            selectElement(RowCol.parseTagData(tag));
        }
    };

    private OnClickListener navigationButtonClicked = new OnClickListener() {
        @Override
        public void onClick(View view) {
            // Do an animation, as usual
            AnimUtils.DoGenericOnClickAnim((Button)view, ACTION_BUTTON_ENABLED_BGCOL, DEFAULT_BUTTON_TARGET_COLOUR);

            //RowCol targetPos = markerPos; // I'm *this close* to ducking murdering the guy who invented Java right now
            RowCol targetPos = new RowCol(markerPos);

            int viewId = view.getId();
            if (viewId == R.id.LetSelBtnUpwards) {
                if (targetPos.row > 0) targetPos.row -= 1;
            }
            else if (viewId == R.id.LetSelBtnDownwards) {
                if (targetPos.row < 2) targetPos.row += 1;
            }
            else if (viewId == R.id.LetSelBtnLeftwards) {
                if (targetPos.col > 0) {
                    targetPos.col -= 1;
                } else {
                    targetPos.row--;
                    targetPos.col = 8;
                }
            }
            else if (viewId == R.id.LetSelBtnRightwards) {
                if (targetPos.col < 8) {
                    targetPos.col += 1;
                } else {
                    targetPos.col = 0;
                    targetPos.row++;
                }
            }
            selectElement(targetPos);
        }
    };

    // Select another element and update all the views that need to be updated
    private void selectElement(RowCol targetPos) {
        // By default, DO NOT FORCE ELEMENT RE-SELECTION
        selectElement(targetPos, false);
    }

    private void selectElement(final RowCol targetPos, boolean forceSelect) {
        if (!forceSelect && targetPos == markerPos) return;

        // testing if such an element exists, the lazy way
        try {
            if (letterViews[targetPos.row][targetPos.col] == null) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final View targetView = letterViews[targetPos.row][targetPos.col];
                // set selector size to letter size
                final float y = targetPos.row * targetView.getHeight();
                LetSelSelectionMarker.getLayoutParams().width = targetView.getWidth();
                // The ViewPropertyAnimator is one of the nicest things about this whole sodding environment.
                // ... and naturally, there's something really fishy going on with these things here.
                LetSelSelectionMarker.animate().translationX(targetView.getX()).translationY(y).setDuration(250)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animator) {
                                LetSelSelectionMarker.setX(targetView.getX());
                                LetSelSelectionMarker.setY(y);
                            }

                            @Override
                            public void onAnimationCancel(Animator animator) {
                                LetSelSelectionMarker.setX(targetView.getX());
                                LetSelSelectionMarker.setY(y);
                            }
                        }).start();
                markerPos = targetPos;
                String letterVal = letterViews[targetPos.row][targetPos.col].getText().toString();
                selectedLetter = Letter.fromString(letterVal);

                // Now the eyecandy
                LetSelCurLetterLarge.setText(letterVal);
                LetSelCurLetterAuxiliary.setText(selectedLetter.getCrypticAlternative());
                // unfortunately, the XML textStyle property doesn't quite cut it
                String regText = "<i>" + selectedLetter.getCrypticText() + "</i>";
                Spanned italicText = AnimUtils.FromHTML(regText);
                LetSelCurLetterDescription.setText(italicText);
                LetSelInvCount.setText(getLetSelInvCount());

                // And now set the proper ash value too
                LetSelActionAshValue.setText(String.valueOf(getActionAshValue()));
                LetSelBtnDoAction.setText(setActionButtonStateAndGetItsText());

                // To be sure everything works, I need to update this every single sodding time. This is a farce.
                if (actionButtonEnabled) {
                    ((ColorDrawable)LetSelBtnDoAction.getBackground()).setColor(ACTION_BUTTON_ENABLED_BGCOL);
                    LetSelBtnDoAction.setTextColor(Color.WHITE);
                    LetSelBtnDoAction.setOnClickListener(ActionToOnClickListener(listener, selectedLetter));
                }
                else {
                    // There appears to be a small visual bug with this. Probably the final on-click animation (see AnimUtils)
                    // overrides the stuff I set here. Honestly, I don't care.
                    ((ColorDrawable)LetSelBtnDoAction.getBackground()).setColor(ACTION_BUTTON_DISABLED_BGCOL);
                    LetSelBtnDoAction.setTextColor(ACTION_BUTTON_ENABLED_BGCOL);
                    LetSelBtnDoAction.setOnClickListener(null); // Prevent clicking if we shouldn't be able to click.
                }

            }
        });
    }

    public void setMode(ModusOperandi targetMode) {
        currentMode = targetMode;
        if (targetMode == ModusOperandi.Ashery) {
            LetSelActionLine1.setText("Pay");
            LetSelActionLine3.setText("to");
        } else {
            LetSelActionLine1.setText("Gain");
            LetSelActionLine3.setText("if you");
        }
    }

    // Pretty much this entire component's only link to the outside world
    public void setActionListener(final ActionListener listener) {
        this.listener = listener;
        LetSelBtnDoAction.setOnClickListener(ActionToOnClickListener(listener, selectedLetter));
    }

    // Is this the real life?... / Is this just fantasy?
    // Caught in a JAVA landslide... / No escape from JAVA reality...
    private static OnClickListener ActionToOnClickListener(final ActionListener actionListener,
                                                           final Letter currentlySelectedLetter) {
        // I'm essentially using the same code on both places where I need to dynamically plug onClickListeners.
        // Therefore, let's wrap 'em in a method and make sure the button always gets animated.
        // Not quite sure why I made it static, but hell, who cares.
        // also, old comments from 'setActionListener' moved here:

        // the fact that I can't give custom parameters to a custom onClickListener is idiotic
        // GIVE ME DELEGAAAATES AND LAMBDAAAAS!!!
        return new OnClickListener() {
            @Override
            public void onClick(View view) {
                AnimUtils.DoGenericOnClickAnim((Button)view);
                if (actionListener != null) actionListener.onClick(currentlySelectedLetter);
            }
        };
    }

    // Well, this one is pretty important too
    public void setInventoryData(Inventory inventory, int letterCapacity) {
        curInventory = inventory;
        curInventoryCap = letterCapacity;
        LetSelInvCount.setText(getLetSelInvCount());

        // Now change the colours of the letter-buttons above to match the "fullness" of the inventory
        int colourStart = 0xff3e3e3e; // It pains me to hard-code colours like this, but I don't want to depend on context right now.
        int colourEnd = 0xffffffff; // At least white is white...
        ArgbEvaluator colourEval = new ArgbEvaluator();
        for (View letterButton : allSubViews) {
            Object tag = letterButton.getTag();
            if (tag != null && tag.toString().startsWith("LetterButton")) {
                Letter curLetter = Letter.fromString(((TextView)letterButton).getText().toString());
                float ratio = (float)curInventory.getAmountOfLetter(curLetter)/(float)letterCapacity;
                int curColour = (int)new ArgbEvaluator().evaluate(ratio, colourStart, colourEnd);
                ((TextView) letterButton).setTextColor(curColour);
            }
        }

        // and also, leet hacks
        // force-updating the element will refresh the actionButton's text
        selectElement(markerPos, true);
    }


    // HELPERS
    private void findNamedViews(View parent) {
        LetSelSelectionMarker = (ImageView)parent.findViewById(R.id.LetSelSelectionMarker);
        LetSelBtnUpwards = (Button)parent.findViewById(R.id.LetSelBtnUpwards);
        LetSelBtnLeftwards = (Button)parent.findViewById(R.id.LetSelBtnLeftwards);
        LetSelBtnDownwards = (Button)parent.findViewById(R.id.LetSelBtnDownwards);
        LetSelBtnRightwards = (Button)parent.findViewById(R.id.LetSelBtnRightwards);
        LetSelCurLetterLarge = (TextView)parent.findViewById(R.id.LetSelCurLetterLarge);
        LetSelCurLetterAuxiliary = (TextView)parent.findViewById(R.id.LetSelCurLetterAuxiliary);
        LetSelCurLetterDescription = (TextView)parent.findViewById(R.id.LetSelCurLetterDescription);
        LetSelInvCount = (TextView)parent.findViewById(R.id.LetSelInvCount);
        LetSelActionLine1 = (TextView)parent.findViewById(R.id.LetSelActionLine1);
        imgAsh = (ImageView)parent.findViewById(R.id.imgAsh);
        LetSelActionAshValue = (TextView)parent.findViewById(R.id.LetSelActionAshValue);
        LetSelActionLine3 = (TextView)parent.findViewById(R.id.LetSelActionLine3);
        LetSelBtnDoAction = (Button)parent.findViewById( R.id.LetSelBtnDoAction);

        LetSelBtnUpwards.setOnClickListener(navigationButtonClicked);
        LetSelBtnLeftwards.setOnClickListener(navigationButtonClicked);
        LetSelBtnDownwards.setOnClickListener(navigationButtonClicked);
        LetSelBtnRightwards.setOnClickListener(navigationButtonClicked);
    }

    private String setActionButtonStateAndGetItsText() {
        // Because we're so benevolent, we'll change the button's text accordingly
        // and we'll do it in a slick way
        if (currentMode == ModusOperandi.Ashery) {
            // Ashery mode creates a single letter => fails if inventory full
            actionButtonEnabled = (curInventory.getAmountOfLetter(selectedLetter) < curInventoryCap);
            return (actionButtonEnabled) ? "Create 1x '" + selectedLetter.name() + "'" : "FULL";
        }
        else {
            // Crematorium mode burns a few to create ash => fails if not enough letters in inv.
            int needed = selectedLetter.getNumToDestroy();
            actionButtonEnabled = (curInventory.getAmountOfLetter(selectedLetter) >= needed);
            return ((actionButtonEnabled) ? "Burn " : "NEED ") + needed + "x '" + selectedLetter.name() + "'";
        }
    }

    private int getActionAshValue() {
        // We also need to hook into the various skills.
        XPUtils.LevelDetails det = XPUtils.getLevelDetailsForXP(Game.currentPlayerData().getXP());
        Alignment playerTeam = Game.currentPlayerData().getAlignment();
        if (currentMode == ModusOperandi.Ashery) {
            int amount = selectedLetter.getAshCreateValue();

            if (XPUtils.LevelHasSkill(playerTeam, det.level(), XPUtils.Skill.SPIRE_AGENTS)) {
                float discount = XPUtils.Skill.SPIRE_AGENTS.getCurBonusMagnitude(det.level());
                amount = (int)((1.0f - discount) * (float)amount);
            }
            return amount;
        }
        else {
            int amount = selectedLetter.getAshDestroyValue();
            if (XPUtils.LevelHasSkill(playerTeam, det.level(), XPUtils.Skill.ASHEN_SOUL)) {
                amount += 1;
            }
            return amount;
        }
    }

    private String getLetSelInvCount() {
        return "IN RESERVE: " + curInventory.getAmountOfLetter(selectedLetter) + "/" + curInventoryCap;
    }

    // Again with the helper method, so everything runs a bit smoother
    private List<View> getAllChildrenBFS(View v) {
        List<View> visited = new ArrayList<>();
        List<View> unvisited = new ArrayList<>();
        unvisited.add(v);

        while (!unvisited.isEmpty()) {
            View child = unvisited.remove(0);
            visited.add(child);
            if (!(child instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) child;
            final int childCount = group.getChildCount();
            for (int i = 0; i < childCount; i++) unvisited.add(group.getChildAt(i));
        }

        return visited;
    }


    // Again with the helper method, so everything runs a bit smoother
    private List<View> getChildLetterViews(View v) {
        List<View> children = new ArrayList<>();

        if (v instanceof ViewGroup){
            ViewGroup group = (ViewGroup)v;
            for (int i = 0; i < group.getChildCount(); i++){
                View child = group.getChildAt(i);
                Object tag = child.getTag();
                if (tag.toString().startsWith("LetterButton")) {
                    children.add(child);
                } else {
                    children.addAll(getChildLetterViews(child));
                }
            }
        } else {
            Object tag = v.getTag();
            if (tag.toString().startsWith("LetterButton")) {
                children.add(v);
            }
        }
        return children;
    }

    private static class RowCol {
        public int row;
        public int col;

        public RowCol(int row, int col) {
            this.row = row;
            this.col = col;
        }

        // duck this ducking language and its ducking fanatical by-Ref passing
        public RowCol(RowCol source) {
            this.row = source.row;
            this.col = source.col;
        }

        public static RowCol parseTagData(String tagData) {
            String[] splits = tagData.split("\\|");
            try {
                int row = Integer.parseInt(splits[1].substring(4));
                int col = Integer.parseInt(splits[2].substring(4));
                return new RowCol(row, col);
            }
            catch (Exception ex) {
                String spltzzz = "";
                for (String x : splits) {
                    spltzzz += x + " -- ";
                }

                throw new IllegalArgumentException("ERROR AT SPLITS = " + spltzzz + "\n\n" + ex.toString());
            }

        }

        // auto-generated equals and hashcode
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RowCol rowCol = (RowCol) o;

            if (row != rowCol.row) return false;
            return col == rowCol.col;

        }

        @Override
        public int hashCode() {
            int result = row;
            result = 31 * result + col;
            return result;
        }
    }

    // starting to lose me marbles, I am
    // ha, "starting", what a lie... the marbles are long gone
    public enum ModusOperandi {
        Ashery,
        Crematorium
    }

    public interface ActionListener {
        void onClick(Letter selectedLetter);
    }
}