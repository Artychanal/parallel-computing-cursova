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

    public List<DocumentData> loadFromDirectory(Path directory) {
        List<DocumentData> documents = new ArrayList<DocumentData>();

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return documents;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isTextFile)
                    .sorted()
                    .forEach(path -> documents.add(new DocumentData(
                            path.getFileName().toString(),
                            readText(path)
                    )));
        } catch (IOException exception) {
            throw new IllegalStateException("Не вдалося зчитати директорію з документами: " + directory, exception);
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
            throw new IllegalStateException("Не вдалося зчитати файл: " + path, exception);
        }
    }
}
