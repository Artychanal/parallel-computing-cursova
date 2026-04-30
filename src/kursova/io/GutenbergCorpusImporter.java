package kursova.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GutenbergCorpusImporter {

    public static final String MANIFEST_FILE_NAME = ".import-settings";

    private static final String[] START_MARKERS = {
            "*** START OF THE PROJECT GUTENBERG EBOOK",
            "*** START OF THIS PROJECT GUTENBERG EBOOK",
            "***START OF THE PROJECT GUTENBERG EBOOK"
    };

    private static final String[] END_MARKERS = {
            "*** END OF THE PROJECT GUTENBERG EBOOK",
            "*** END OF THIS PROJECT GUTENBERG EBOOK",
            "***END OF THE PROJECT GUTENBERG EBOOK"
    };

    public int importChunkedCorpus(
            Path sourceDirectory,
            Path corpusDirectory,
            int maxDocuments,
            int chunkWordCount,
            int chunkStride
    ) {
        try {
            Files.createDirectories(corpusDirectory);

            List<Path> sourceFiles;
            try (Stream<Path> files = Files.walk(sourceDirectory)) {
                sourceFiles = files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                        .sorted()
                        .collect(Collectors.toList());
            }

            int documentIndex = 1;
            for (Path sourceFile : sourceFiles) {
                if (documentIndex > maxDocuments) {
                    break;
                }

                String rawText = Files.readString(sourceFile, StandardCharsets.UTF_8);
                String cleanedText = stripProjectGutenbergBoilerplate(rawText);
                List<String> words = splitWords(cleanedText);

                if (words.size() < chunkWordCount) {
                    continue;
                }

                String baseName = sourceFile.getFileName().toString().replace(".txt", "");
                for (int start = 0; start + chunkWordCount <= words.size() && documentIndex <= maxDocuments; start += chunkStride) {
                    int finish = Math.min(words.size(), start + chunkWordCount);
                    String chunkText = String.join(" ", words.subList(start, finish));
                    String outputFileName = String.format("%05d_%s.txt", documentIndex, sanitizeFileName(baseName));
                    Path outputPath = corpusDirectory.resolve(outputFileName);

                    Files.writeString(
                            outputPath,
                            chunkText,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    );
                    documentIndex++;
                }
            }

            writeManifest(corpusDirectory, maxDocuments, chunkWordCount, chunkStride);
            return documentIndex - 1;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to import corpus from Project Gutenberg.", exception);
        }
    }

    private void writeManifest(Path corpusDirectory, int maxDocuments, int chunkWordCount, int chunkStride)
            throws IOException {
        String manifest = "maxDocuments=" + maxDocuments + System.lineSeparator()
                + "chunkWordCount=" + chunkWordCount + System.lineSeparator()
                + "chunkStride=" + chunkStride + System.lineSeparator();

        Files.writeString(
                corpusDirectory.resolve(MANIFEST_FILE_NAME),
                manifest,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private String stripProjectGutenbergBoilerplate(String rawText) {
        String normalized = rawText.replace("\r\n", "\n");
        String upper = normalized.toUpperCase(Locale.ROOT);

        int startIndex = 0;
        for (String marker : START_MARKERS) {
            int markerIndex = upper.indexOf(marker);
            if (markerIndex >= 0) {
                int lineEnd = normalized.indexOf('\n', markerIndex);
                startIndex = lineEnd >= 0 ? lineEnd + 1 : markerIndex;
                break;
            }
        }

        int endIndex = normalized.length();
        for (String marker : END_MARKERS) {
            int markerIndex = upper.indexOf(marker);
            if (markerIndex >= 0) {
                endIndex = markerIndex;
                break;
            }
        }

        return normalized.substring(startIndex, endIndex).trim();
    }

    private List<String> splitWords(String text) {
        String normalized = text
                .replaceAll("[^\\p{L}\\p{Nd}\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return List.of();
        }

        String[] parts = normalized.split(" ");
        List<String> words = new ArrayList<String>(parts.length);
        for (String part : parts) {
            if (!part.isBlank()) {
                words.add(part);
            }
        }

        return words;
    }

    private String sanitizeFileName(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
