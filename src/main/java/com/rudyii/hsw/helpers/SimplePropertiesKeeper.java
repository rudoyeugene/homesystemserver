package com.rudyii.hsw.helpers;

public class SimplePropertiesKeeper {
    private static boolean homeSystemInitComplete;

    public static boolean isHomeSystemInitComplete() {
        return homeSystemInitComplete;
    }

    public static void homeSystemInitComplete() {
        homeSystemInitComplete = true;
    }
}
