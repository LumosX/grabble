package eu.zerovector.grabble;

// A static class that shall hold all network-related functionality.
public final class Network {

    // We'll make a singleton, because this stupid language neither allows me to define
    // static methods in interfaces, nor does it allow me to use them here...
    // I wanted to use an interface like a human being, but I guess I won't...
    private static Network instance;
    protected Network() { } // Anti-instantiation measures
    // There also aren't any implicit properties in this language. Really? I need to use a get* method? Pffft.
    private static Network getInstance() {
        if (instance == null) instance = new Network();
        return instance;
    }

    public static boolean Login() {
        return true; // TODO Login if player exists
    }

    public static boolean Register() {
        return true; // TODO actually register player
    }

    public static Object DownloadDailyMap() {
        return null; // TODO Actually download map
    }

    public static boolean GetLetter(int location) {
        return false; // TODO called when getting a letter
    }
}


