package kursova.io;

import kursova.model.DocumentData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SampleCorpusFactory {

    private static final String[] THEMES = {
            "parallel algorithms optimize execution time and improve throughput for complex computations",
            "text vectorization converts raw documents into weighted statistical representations of words",
            "java concurrency uses thread pools futures synchronization and task decomposition",
            "sequential processing is easier to understand but often slower on large collections",
            "term frequency and inverse document frequency highlight important words in documents",
            "benchmarking reveals the scalability efficiency and speedup of the implemented solution"
    };
    private static final long RANDOM_SIZE_SEED = 20260510L;
    private static final long HIGH_VARIANCE_SEED = 20260511L;

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

    public static List<DocumentData> createEqualLengthBenchmarkCorpus(List<DocumentData> sourceDocuments, int size) {
        int actualSize = Math.min(size, sourceDocuments.size());
        List<DocumentData> documents = new ArrayList<>(actualSize);

        for (int index = 0; index < actualSize; index++) {
            documents.add(new DocumentData(
                    "equal-" + (index + 1),
                    sourceDocuments.get(index).getText()
            ));
        }

        return documents;
    }

    public static List<DocumentData> createRandomLengthBenchmarkCorpus(List<DocumentData> sourceDocuments, int size) {
        return createRepartitionedCorpus(sourceDocuments, size, "random", buildRandomWeights(size, RANDOM_SIZE_SEED));
    }

    public static List<DocumentData> createHighVarianceBenchmarkCorpus(List<DocumentData> sourceDocuments, int size) {
        return createRepartitionedCorpus(sourceDocuments, size, "high-variance", buildHighVarianceWeights(size, HIGH_VARIANCE_SEED));
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

    private static List<DocumentData> createRepartitionedCorpus(
            List<DocumentData> sourceDocuments,
            int size,
            String prefix,
            double[] weights
    ) {
        int actualSize = Math.min(size, sourceDocuments.size());
        List<DocumentData> sourceSubset = sourceDocuments.subList(0, actualSize);
        int totalWordCount = countWords(sourceSubset);
        int[] targetLengths = buildTargetLengths(actualSize, totalWordCount, weights);
        WordCursor cursor = new WordCursor(sourceSubset);
        List<DocumentData> documents = new ArrayList<>(actualSize);

        for (int index = 0; index < actualSize; index++) {
            int targetLength = targetLengths[index];
            StringBuilder builder = new StringBuilder(targetLength * 10);

            for (int wordIndex = 0; wordIndex < targetLength; wordIndex++) {
                String word = cursor.nextWord();
                if (word == null) {
                    throw new IllegalStateException("Source corpus does not contain enough words for scenario generation.");
                }
                if (wordIndex > 0) {
                    builder.append(' ');
                }
                builder.append(word);
            }

            documents.add(new DocumentData(prefix + "-" + (index + 1), builder.toString()));
        }

        if (cursor.hasRemainingWords()) {
            throw new IllegalStateException("Scenario generation did not consume all source words.");
        }

        return documents;
    }

    private static int countWords(List<DocumentData> documents) {
        int totalWordCount = 0;

        for (DocumentData document : documents) {
            totalWordCount += splitWords(document.getText()).length;
        }

        return totalWordCount;
    }

    private static int[] buildTargetLengths(int documentCount, int totalWordCount, double[] weights) {
        int[] lengths = new int[documentCount];
        double totalWeight = 0.0;

        for (double weight : weights) {
            totalWeight += weight;
        }

        int remainingWordBudget = totalWordCount - documentCount;
        int assignedExtraWords = 0;
        double[] remainders = new double[documentCount];

        for (int index = 0; index < documentCount; index++) {
            double scaledExtra = remainingWordBudget * (weights[index] / totalWeight);
            int extraWords = (int) Math.floor(scaledExtra);
            lengths[index] = 1 + extraWords;
            remainders[index] = scaledExtra - extraWords;
            assignedExtraWords += extraWords;
        }

        int remainingExtraWords = remainingWordBudget - assignedExtraWords;
        while (remainingExtraWords > 0) {
            int bestIndex = 0;
            for (int index = 1; index < documentCount; index++) {
                if (remainders[index] > remainders[bestIndex]) {
                    bestIndex = index;
                }
            }
            lengths[bestIndex]++;
            remainders[bestIndex] = 0.0;
            remainingExtraWords--;
        }

        return lengths;
    }

    private static double[] buildRandomWeights(int documentCount, long seed) {
        Random random = new Random(seed);
        double[] weights = new double[documentCount];

        for (int index = 0; index < documentCount; index++) {
            weights[index] = 0.5 + random.nextDouble();
        }

        return weights;
    }

    private static double[] buildHighVarianceWeights(int documentCount, long seed) {
        Random random = new Random(seed);
        double[] weights = new double[documentCount];

        for (int index = 0; index < documentCount; index++) {
            double roll = random.nextDouble();
            if (roll < 0.70) {
                weights[index] = 0.10 + random.nextDouble() * 0.30;
            } else if (roll < 0.95) {
                weights[index] = 1.5 + random.nextDouble() * 3.5;
            } else {
                weights[index] = 8.0 + random.nextDouble() * 12.0;
            }
        }

        return weights;
    }

    private static String[] splitWords(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return new String[0];
        }

        return normalized.split("\\s+");
    }

    private static class WordCursor {

        private final List<DocumentData> documents;
        private int documentIndex;
        private String[] currentWords;
        private int wordIndex;

        private WordCursor(List<DocumentData> documents) {
            this.documents = documents;
            this.currentWords = new String[0];
        }

        private String nextWord() {
            while (wordIndex >= currentWords.length) {
                if (documentIndex >= documents.size()) {
                    return null;
                }
                currentWords = splitWords(documents.get(documentIndex).getText());
                documentIndex++;
                wordIndex = 0;
            }

            return currentWords[wordIndex++];
        }

        private boolean hasRemainingWords() {
            if (wordIndex < currentWords.length) {
                return true;
            }

            for (int index = documentIndex; index < documents.size(); index++) {
                if (splitWords(documents.get(index).getText()).length > 0) {
                    return true;
                }
            }

            return false;
        }
    }
}
