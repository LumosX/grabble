package eu.zerovector.grabble.Activity.Fragment;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.EnumSet;
import java.util.Locale;

import eu.zerovector.grabble.Activity.UpdateUIListener;
import eu.zerovector.grabble.Data.Alignment;
import eu.zerovector.grabble.Data.Word;
import eu.zerovector.grabble.Data.XPUtils;
import eu.zerovector.grabble.Game;
import eu.zerovector.grabble.Network;
import eu.zerovector.grabble.R;
import eu.zerovector.grabble.Utils.AnimUtils;

import static eu.zerovector.grabble.Game.currentPlayerData;

// Fragment for the Map screen.
public class SkillsScreen extends Fragment implements UpdateUIListener {

    private ImageView imgFactionLogo;
    private TextView lblFactionName;
    private TextView lblFactionLeader;
    private TextView lblCompletedWords;
    private Button btnSkill1;
    private Button btnSkill2;
    private Button btnSkill3;
    private Button btnSkill4;
    private Button[] skillButtons;
    private TextView lblSkillName;
    private TextView lblSkillDesc;
    private TextView lblAtCurrentColon;
    private TextView lblCurrentBoon;
    private boolean[] skillsEnabled;
    private int selectedSkill = 1;
    private XPUtils.Skill[] currentSkills;

    public SkillsScreen() { }
    public static SkillsScreen newInstance() {
        return new SkillsScreen();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_skills_screen, container, false);

        // bind
        imgFactionLogo = (ImageView)view.findViewById(R.id.imgFactionLogo);
        lblFactionName = (TextView)view.findViewById(R.id.lblFactionName);
        lblFactionLeader = (TextView)view.findViewById(R.id.lblFactionLeader);
        lblCompletedWords = (TextView)view.findViewById(R.id.lblCompletedWords);
        btnSkill1 = (Button)view.findViewById(R.id.btnSkill1);
        btnSkill2 = (Button)view.findViewById(R.id.btnSkill2);
        btnSkill3 = (Button)view.findViewById(R.id.btnSkill3);
        btnSkill4 = (Button)view.findViewById(R.id.btnSkill4);
        lblSkillName = (TextView)view.findViewById(R.id.lblSkillName);
        lblSkillDesc = (TextView)view.findViewById(R.id.lblSkillDesc);
        lblAtCurrentColon = (TextView)view.findViewById(R.id.lblAtCurrentColon);
        lblCurrentBoon = (TextView)view.findViewById(R.id.lblCurrentBoon);
        skillButtons = new Button[] {btnSkill1, btnSkill2, btnSkill3, btnSkill4};
        skillsEnabled = new boolean[] {false, false, false, false};
        currentSkills = new XPUtils.Skill[4];

        btnSkill1.setOnClickListener(btnSkills_click);
        btnSkill2.setOnClickListener(btnSkills_click);
        btnSkill3.setOnClickListener(btnSkills_click);
        btnSkill4.setOnClickListener(btnSkills_click);

        Alignment alignment = currentPlayerData().getAlignment();
        int curLevel = XPUtils.getLevelDetailsForXP(currentPlayerData().getXP()).level();
        // Load picture based on alignment
        int resourceID = (alignment == Alignment.Closers) ? R.drawable.icon_closers : R.drawable.icon_openers;
        Picasso.with(getActivity()).load(resourceID).into(imgFactionLogo);

        // Load faction name and leader name
        lblFactionName.setText(Game.currentPlayerData().getCurrentFactionName());
        // Whom are we kidding, the leader is always the current player. CHANGE THIS IF YOU ADD MULTIPLAYER!
        lblFactionLeader.setText(Game.currentPlayerData().getUsername());

        // Set the skills we want to use
        int colourIconActiveSkill = 0xffffffff;
        int colourIconInactiveSkill = ContextCompat.getColor(getActivity(), R.color.UI_AshGrey);
        XPUtils.Skill[] allSkills = XPUtils.Skill.values();
        int curBtnIndex = 0, curBtnLimit = skillButtons.length;
        for (XPUtils.Skill skill : allSkills) {
            if (curBtnIndex > curBtnLimit) break;
            if (skill.getAlignment() != alignment) continue;

            // For every fitting skill, set only the icons up.
            // Do it in order and hope that's correct.
            currentSkills[curBtnIndex] = skill;
            skillButtons[curBtnIndex].setText(skill.getIconChar());
            // Switch the icon to inactive if we ain't got the level
            if (skill.getLevelRequired() > curLevel) skillButtons[curBtnIndex].setTextColor(colourIconInactiveSkill);
            else skillButtons[curBtnIndex].setTextColor(colourIconActiveSkill);

            curBtnIndex += 1;
        }

        // Everything else happens in UpdateUI
        updateUI();

        // Now select the first skill
        selectSkill(0);

        return view;
    }

    private void selectSkill(int index) {
        if (index < 0 && index >= 4) return;

        // If valid selection, re-init textboxes
        int curLevel = XPUtils.getLevelDetailsForXP(currentPlayerData().getXP()).level();
        XPUtils.Skill curSkill = currentSkills[index];

        lblSkillName.setText(curSkill.getName());
        lblSkillDesc.setText(curSkill.getDescription());
        boolean curSkillUnlocked = curSkill.getLevelRequired() < curLevel;
        Float curBonus = curSkill.getCurBonusMagnitude(curLevel);
        String extraString = (curSkillUnlocked) ?
                "At current (Rank " + curLevel + "):" : "Skill unlocked at Rank " + curSkill.getLevelRequired();
        // Nested shorthand ifs. Because we're THAT good (and we're running out of time)
        String curBonusString = (curBonus == null) ? "" : ((curSkillUnlocked) ?
                curSkill.getCurrentStatus().replace("!", String.format(Locale.UK, "%.2f", curBonus)) : "");
        // If skill unlocked, investigate
        lblCurrentBoon.setText(curBonusString);
        // No matter what, set the text for the extra info label
        // However, if the skill is unlocked AND passive, clear it out
        if (curSkillUnlocked && curBonus == null) extraString = "";
        lblAtCurrentColon.setText(extraString);

        updateUI();
    }


    // This handles all skill buttons.
    private View.OnClickListener btnSkills_click = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int curLevel = XPUtils.getLevelDetailsForXP(currentPlayerData().getXP()).level();
            int activeBtnColour = ContextCompat.getColor(getActivity(), R.color.UI_WhiteTranslucent);
            int inactiveBtnColour = ContextCompat.getColor(getActivity(), R.color.UI_GreyTranslucent);
            for (int i = 0; i < 4; i++) {
                // Select current skill
                // Animate button backgrounds to colour
                Button curBtn = (Button)skillButtons[i];
                int curBGColour = ((ColorDrawable)curBtn.getBackground()).getColor();
                if (curBtn.getId() == view.getId()) {
                    selectSkill(i);
                    AnimUtils.DoGenericOnClickAnim(curBtn, curBGColour, activeBtnColour);
                }
                else AnimUtils.DoGenericOnClickAnim(curBtn, curBGColour, inactiveBtnColour);
            }
        }
    };

    private void updateUI() {
        // Handle stuff that changes... i.e. the current word count
        int totalWords = Game.getDictSize();
        int curWords = Network.GetFactionData(getActivity(),
                Game.currentPlayerData().getCurrentFactionName()).getNumberOfCompletedWords();
        lblCompletedWords.setText(curWords + "/" + totalWords);

        // And also the colours for the skill icons
        int curLevel = XPUtils.getLevelDetailsForXP(currentPlayerData().getXP()).level();
        for (int i = 0; i < 4; i++) {
            int targetTextColour = 0xffffffff; // White for the unlocked skills
            if (currentSkills[i].getLevelRequired() > curLevel) {
                targetTextColour = ContextCompat.getColor(getActivity(), R.color.UI_DarkGrey);
            }
            skillButtons[i].setTextColor(targetTextColour);
        }

    }


    @Override
    public void onUpdateUIReceived(EnumSet<Code> codes, Word oldWord) {
        updateUI();
    }
}
