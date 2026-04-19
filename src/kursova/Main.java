package kursova;

import kursova.benchmark.BenchmarkResult;
import kursova.benchmark.BenchmarkService;
import kursova.io.CorpusLoader;
import kursova.io.SampleCorpusFactory;
import kursova.model.DocumentData;
import kursova.model.VectorizationResult;
import kursova.vectorizer.ConsistencyValidator;
import kursova.vectorizer.SequentialTfidfVectorizer;
import kursova.vectorizer.TextVectorizer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        List<DocumentData> documents = loadDocuments();

        if (documents.isEmpty()) {
            System.out.println("Корпус документів порожній. Завершення роботи.");
            return;
        }

        System.out.println("Кількість документів: " + documents.size());
        System.out.println();

        TextVectorizer sequentialVectorizer = new SequentialTfidfVectorizer();
        VectorizationResult sequentialResult = sequentialVectorizer.vectorize(documents);
        printPreview(sequentialResult);
        validateConsistency(documents, sequentialResult);

        List<Integer> threadCounts = Arrays.asList(2, 4, 8);
        BenchmarkService benchmarkService = new BenchmarkService(documents, threadCounts, 3);
        BenchmarkResult benchmarkResult = benchmarkService.run();

        System.out.println();
        System.out.println("Результати вимірювання продуктивності:");
        System.out.println(benchmarkResult.toTable());
    }

    private static List<DocumentData> loadDocuments() {
        Path corpusDirectory = Path.of("data", "corpus");
        CorpusLoader loader = new CorpusLoader();
        List<DocumentData> documents = loader.loadFromDirectory(corpusDirectory);

        if (!documents.isEmpty()) {
            System.out.println("Завантажено документи з директорії: " + corpusDirectory.toAbsolutePath());
            return documents;
        }

        System.out.println("Директорію з текстами не знайдено або вона порожня. Використовую згенерований корпус.");
        return SampleCorpusFactory.createSyntheticCorpus(2000);
    }

    private static void validateConsistency(List<DocumentData> documents, VectorizationResult sequentialResult) {
        ConsistencyValidator validator = new ConsistencyValidator();
        boolean consistent = validator.matchesParallelResult(documents, sequentialResult, 4);

        System.out.println();
        System.out.println(
                consistent
                        ? "Перевірка коректності: результати послідовної і паралельної реалізації збігаються."
                        : "Перевірка коректності: виявлено відмінності між послідовною і паралельною реалізаціями."
        );
    }

    private static void printPreview(VectorizationResult result) {
        System.out.println("Розмір словника: " + result.getVocabulary().size());

        if (result.getDocumentVectors().isEmpty()) {
            return;
        }

        String firstDocumentId = result.getDocumentVectors().keySet().iterator().next();
        Map<String, Double> vector = result.getDocumentVectors().get(firstDocumentId);

        System.out.println("Приклад TF-IDF вектора для документа \"" + firstDocumentId + "\":");
        vector.entrySet()
                .stream()
                .sorted((left, right) -> Double.compare(right.getValue(), left.getValue()))
                .limit(10)
                .forEach(entry -> System.out.printf("  %-20s %.6f%n", entry.getKey(), entry.getValue()));
    }
}
