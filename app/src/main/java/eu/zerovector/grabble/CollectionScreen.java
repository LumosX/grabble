package eu.zerovector.grabble;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

// Fragment for the real collection screen.
public class CollectionScreen extends Fragment {

    public CollectionScreen() { }
    public static CollectionScreen newInstance() {
        return new CollectionScreen();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Left/Right col adapter references
    private boolean listViewsPopulated = false;

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

        // Populate the sodding listviews. CAN THEY MAKE IT ANY MORE CONVOLUTED?!
        populateListViews(view);

        return view;
    }

    private void populateListViews(View view) {
        if (Game.currentPlayerData() == null && Game.currentPlayerData().getInventory() != null) return;
        // Grab all the things.
        int[] letterCountsInts = Game.currentPlayerData().getInventory().getLetterCounts();
        int letterCapacity = Game.currentPlayerData().getInventory().getCapacity();
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
        // So: Get table
        int NUM_COLS = 3;
        TableLayout table = (TableLayout)view.findViewById(R.id.tblLetters);
        int numRows = (int)Math.ceil(vals.size()/(float)NUM_COLS);
        numRows = 3;
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
                }
                // Otherwise simply set fields to empty text (cell number per row must be preserved)
                else {
                    lblLetterVal.setText("");
                    lblLetterCount.setText("");
                }
            }
            table.addView(row);
        }



        listViewsPopulated = true;
    }


    // This left here for history's sake
    /*
    private class CollectionLetterAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final String[] letterValues;
        private final String[] letterCounts;

        public CollectionLetterAdapter(Context context, String[] letters, String[] counts) {
            super(context, -1, letters);
            this.context = context;
            this.letterValues = letters;
            this.letterCounts = counts;
        }

        @Override @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.collection_layout_letter, parent, false);
            TextView lblName = (TextView)rowView.findViewById(R.id.lblLetterID);
            TextView lblCount = (TextView)rowView.findViewById(R.id.lblLetterCount);
            // VALUES = letter, count => ["A", "4/5"] (capacity pre-calculated)
            lblName.setText(letterValues[position]);
            lblCount.setText(letterCounts[position]);
            return rowView;
        }

    }
    */


}
