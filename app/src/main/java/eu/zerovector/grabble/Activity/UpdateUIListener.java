package eu.zerovector.grabble.Activity;

import java.util.EnumSet;

import eu.zerovector.grabble.Data.Word;

// A little observer pattern for all fragments that might have UI that needs updating
public interface UpdateUIListener {
    void onUpdateUIReceived(EnumSet<Code> codes, Word oldWord);

    // This... is insane. And absurd. Just mad.
    // It also holds any special behaviours we might want to address
    enum Code {
        EXTRA_ASH_GRANTED,
        WORD_COMPLETED,
        LEVEL_INCREASED,
    }
}
