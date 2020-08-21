package com.siempre.siemprewatch;

public class Utilities {
    public static String removeQuotes(String input) {
        if (input.charAt(0) == '\"' && input.charAt(input.length() - 1) == '\"') {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }
}
