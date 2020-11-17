package com.rudyii.hsw.helpers;

public class StringUtils {
    public static boolean stringIsEmptyOrNull(String s) {
        return "".equals(s) || null == s;
    }

    public static boolean stringIsNotEmptyOrNull(String s) {
        return !stringIsEmptyOrNull(s);
    }
}
