package kursova.preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TextPreprocessor {

    public List<String> tokenize(String text) {
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        List<String> tokens = new ArrayList<String>();
        if (normalized.isEmpty()) {
            return tokens;
        }

        String[] parts = normalized.split(" ");
        for (String part : parts) {
            if (part.length() > 1) {
                tokens.add(part);
            }
        }

        return tokens;
    }
}
