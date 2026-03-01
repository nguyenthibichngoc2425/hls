package raven.modal.demo.utils;

public class LogoutStateManager {
    private static boolean isLoggedOut = false;

    public static boolean isLoggedOut() {
        return isLoggedOut;
    }

    public static void setLoggedOut(boolean loggedOut) {
        isLoggedOut = loggedOut;
    }

    public static void reset() {
        isLoggedOut = false;
    }
}
