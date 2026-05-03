package kursova.preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class TextPreprocessor {
    
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{Nd}\\s]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at",
            "to", "for", "of", "with", "by", "from", "is", "it",
            "as", "be", "was", "are", "were", "been", "has", "have",
            "had", "he", "she", "they", "we", "you", "i", "his",
            "her", "their", "its", "this", "that"
    );

    public List<String> tokenize(String text) {
        String normalized = NON_WORD.matcher(text.toLowerCase(Locale.ROOT))
                .replaceAll(" ");
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ").trim();

        List<String> tokens = new ArrayList<>();
        if (normalized.isEmpty()) {
            return tokens;
        }

        for (String part : normalized.split(" ")) {
            if (part.length() > 1 && !STOP_WORDS.contains(part)) {
                tokens.add(part);
            }
        }

        return tokens;
    }
}
