package kursova.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class NewsCategoryCorpusImporter {

    private static final Pattern HEADLINE_PATTERN = Pattern.compile("\"headline\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("\"short_description\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("\"category\"\\s*:\\s*\"(.*?)\"");

    public int importToCorpus(Path sourcePath, Path corpusDirectory, int limit) {
        try {
            Files.createDirectories(corpusDirectory);

            int importedCount = 0;
            try (Stream<String> lines = Files.lines(sourcePath, StandardCharsets.UTF_8)) {
                for (String line : (Iterable<String>) lines::iterator) {
                    if (importedCount >= limit) {
                        break;
                    }

                    String headline = extractField(line, HEADLINE_PATTERN);
                    String description = extractField(line, DESCRIPTION_PATTERN);
                    String category = extractField(line, CATEGORY_PATTERN);

                    String documentText = buildDocumentText(headline, description);
                    if (documentText.isBlank()) {
                        continue;
                    }

                    String fileName = String.format("%04d_%s.txt", importedCount + 1, sanitizeFileName(category));
                    Path outputPath = corpusDirectory.resolve(fileName);
                    Files.writeString(
                            outputPath,
                            documentText,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    );
                    importedCount++;
                }
            }

            return importedCount;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to import corpus from the news dataset.", exception);
        }
    }

    private String buildDocumentText(String headline, String description) {
        StringBuilder builder = new StringBuilder();

        if (!headline.isBlank()) {
            builder.append(unescapeJson(headline)).append(System.lineSeparator());
        }

        if (!description.isBlank()) {
            builder.append(unescapeJson(description));
        }

        return builder.toString().trim();
    }

    private String extractField(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ");
    }

    private String sanitizeFileName(String value) {
        String normalized = unescapeJson(value).trim().toLowerCase();
        if (normalized.isBlank()) {
            return "unknown";
        }

        return normalized
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
