package eu.zerovector.grabble.Data;


// A basic class to hold all data for a player.
public class PlayerData {
    // Email, username and pass
    private String email;
    private String username;
    private String password;
    // Game stuff
    private String createdFactionName; // The name of the faction the player created upon registering
    private String currentFactionName; // The name of the CURRENT faction the player is a member of
    private Alignment alignment; // The player's alignment, never changes.
    private int ash; // Current Ash amount
    private int experience; // Player experience;
    private Inventory inventory; // The player's current inventory
    private Word currentWord; // And the word that the player is currently trying to complete
    private int lettersUntilExtraAsh; // the number of letters that still need to be collected until 1 extra ash is received

    public PlayerData(String email, String username, String password, String createdFactionName, Alignment alignment) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.createdFactionName = createdFactionName;
        this.alignment = alignment;
        // Init all other elements to default
        this.currentFactionName = createdFactionName; // auto-join current faction
        this.ash = 0;
        this.experience = 0;
        this.inventory = new Inventory();
        this.lettersUntilExtraAsh = Experience.BASE_PERKS.getNumLettersForOneAsh();
    }

    // Email, username, alignment and created faction name will NEVER change, therefore...
    // (and also password, because I'm not implementing a password recovery mechanism)
    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCreatedFactionName() {
        return createdFactionName;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    // Current faction names and the other lot can change, however
    public String getCurrentFactionName() {
        return currentFactionName;
    }

    public void setCurrentFactionName(String currentFactionName) {
        this.currentFactionName = currentFactionName;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        // Experience can't be negative
        if (experience < 0) experience = 0;
        this.experience = experience;
    }

    public int getXP() {
        return getExperience();
    }

    public void setXP(int XP) {
        setExperience(XP);
    }

    public void addXP(int amount) {
        setExperience(this.experience += amount);
    }

    public int getAsh() {
        return ash;
    }

    public void setAsh(int ash) {
        // Ash can't be negative.
        if (ash < 0) ash = 0;
        this.ash = ash;
    }

    public void addAsh(int amount) {
        this.ash += amount;
    }

    public void removeAsh(int amount) {
        setAsh(this.ash - amount);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Word getCurrentWord() {
        return currentWord;
    }

    public void setCurrentWord(Word currentWord) {
        this.currentWord = currentWord;
    }

    public int getLettersUntilExtraAsh() {
        return lettersUntilExtraAsh;
    }

    public void setLettersUntilExtraAsh(int lettersUntilExtraAsh) {
        this.lettersUntilExtraAsh = lettersUntilExtraAsh;
    }

    public void decrementLettersUntilExtraAsh(int amount) {
        this.lettersUntilExtraAsh -= amount;
    }

    // Override equals and hashCode to "fix" comparison:
    // Since a user's email address is the only thing we need to identify them, we only need compare it and it alone
    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof PlayerData) && (((PlayerData)obj).getEmail().equals(this.email))) return true;
        else return false;
    }
    @Override
    public int hashCode() {
        return this.email.hashCode();
    }







    // And the inventory class, which can reliably stay in here.
    public class Inventory {
        private int[] letterCounts;

        public Inventory() {
            letterCounts = new int[26];
        }

        public boolean addLetter(Letter letter, int capacity) {
            int index = letter.ordinal();
            if (letterCounts[index] < capacity) {
                letterCounts[index]++;
                return true;
            }
            else return false;
        }

        public boolean removeLetter(Letter letter) {
            return removeLetter(letter, 1); // Auto-assume removal of 1 letter by default
        }

        public boolean removeLetter(Letter letter, int amountToRemove) {
            int index = letter.ordinal();
            if (letterCounts[index] - amountToRemove >= 0) {
                letterCounts[index] -= amountToRemove;
                return true;
            }
            else return false;
        }

        public int[] getLetterCounts() {
            return letterCounts;
        }


    }
}
