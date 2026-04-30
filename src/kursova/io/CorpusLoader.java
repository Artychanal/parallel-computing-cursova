package kursova.io;

import kursova.model.DocumentData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CorpusLoader {

    private static final int PROGRESS_STEP = 1000;

    public List<DocumentData> loadFromDirectory(Path directory) {
        return loadFromDirectory(directory, Integer.MAX_VALUE);
    }

    public List<DocumentData> loadFromDirectory(Path directory, int maxDocuments) {
        List<DocumentData> documents = new ArrayList<DocumentData>();

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return documents;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            Stream<Path> documentStream = paths.filter(Files::isRegularFile)
                    .filter(this::isTextFile)
                    .sorted();

            if (maxDocuments < Integer.MAX_VALUE) {
                documentStream = documentStream.limit(maxDocuments);
            }

            documentStream
                    .forEach(path -> {
                        documents.add(new DocumentData(
                                path.getFileName().toString(),
                                readText(path)
                        ));

                        if (documents.size() % PROGRESS_STEP == 0) {
                            System.out.println("Loaded " + documents.size() + " documents...");
                        }
                    });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read document directory: " + directory, exception);
        }

        return documents;
    }

    private boolean isTextFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".txt");
    }

    private String readText(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read file: " + path, exception);
        }
    }
}
