package kursova.io;

import kursova.model.DocumentData;

import java.util.ArrayList;
import java.util.List;

public final class SampleCorpusFactory {

    private static final String[] THEMES = {
            "parallel algorithms optimize execution time and improve throughput for complex computations",
            "text vectorization converts raw documents into weighted statistical representations of words",
            "java concurrency uses thread pools futures synchronization and task decomposition",
            "sequential processing is easier to understand but often slower on large collections",
            "term frequency and inverse document frequency highlight important words in documents",
            "benchmarking reveals the scalability efficiency and speedup of the implemented solution"
    };

    private SampleCorpusFactory() {
    }

    public static List<DocumentData> createSyntheticCorpus(int size) {
        List<DocumentData> documents = new ArrayList<DocumentData>(size);

        for (int i = 0; i < size; i++) {
            String text = buildDocumentText(i);
            documents.add(new DocumentData("generated-" + (i + 1), text));
        }

        return documents;
    }

    public static List<DocumentData> createDemoCorpus() {
        List<DocumentData> documents = new ArrayList<DocumentData>();
        documents.add(new DocumentData(
                "demo-1",
                "Apple apple fruit tree garden harvest fresh fruit and sweet apple."
        ));
        documents.add(new DocumentData(
                "demo-2",
                "Car engine wheel road speed vehicle driver and engine power."
        ));
        documents.add(new DocumentData(
                "demo-3",
                "Apple market fruit price basket fresh market fruit season."
        ));
        return documents;
    }

    private static String buildDocumentText(int index) {
        StringBuilder builder = new StringBuilder();

        for (int block = 0; block < 20; block++) {
            builder.append(THEMES[(index + block) % THEMES.length]).append(' ');
            builder.append(THEMES[(index * 3 + block + 1) % THEMES.length]).append(' ');
            builder.append("document ").append(index).append(" iteration ").append(block).append(' ');
        }

        return builder.toString();
    }
}
