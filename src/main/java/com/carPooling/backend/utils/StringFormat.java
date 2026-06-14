package com.carPooling.backend.utils;

public class StringFormat {

    public static String toTitleCase(String input) {

        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();

        String[] words = input.trim().toLowerCase().split("\\s+");

        for (String word : words) {

            result.append(
                    Character.toUpperCase(word.charAt(0))
            );

            if (word.length() > 1) {
                result.append(word.substring(1));
            }

            result.append(" ");
        }

        return result.toString().trim();
    }
}
