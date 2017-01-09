package eu.zerovector.grabble;

import java.util.EnumSet;

// A little observer pattern for all fragments that might have UI that needs updating
public interface UpdateUIListener {
    void onUpdateUIReceived(EnumSet<Code> codes);

    // This... is insane. And absurd. Just mad.
    // It also holds any special behaviours we might want to address
    enum Code {
        EXTRA_ASH_GRANTED,
        WORD_COMPLETED,
        LEVEL_INCREASED, // XP is gained only when a word is completed, so this implies WORD_COMPLETED too
    }
}
