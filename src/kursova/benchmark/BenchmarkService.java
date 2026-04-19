package kursova.benchmark;

import kursova.model.DocumentData;
import kursova.vectorizer.ParallelTfidfVectorizer;
import kursova.vectorizer.SequentialTfidfVectorizer;
import kursova.vectorizer.TextVectorizer;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkService {

    private final List<DocumentData> documents;
    private final List<Integer> threadCounts;
    private final int iterations;

    public BenchmarkService(List<DocumentData> documents, List<Integer> threadCounts, int iterations) {
        this.documents = new ArrayList<DocumentData>(documents);
        this.threadCounts = new ArrayList<Integer>(threadCounts);
        this.iterations = iterations;
    }

    public BenchmarkResult run() {
        double sequentialAverage = measureAverageMillis(new SequentialTfidfVectorizer(), iterations);
        List<ParallelMeasurement> measurements = new ArrayList<ParallelMeasurement>();

        for (Integer threadCount : threadCounts) {
            TextVectorizer parallelVectorizer = new ParallelTfidfVectorizer(threadCount.intValue());
            double averageMillis = measureAverageMillis(parallelVectorizer, iterations);
            double speedup = sequentialAverage / averageMillis;
            double efficiency = speedup / threadCount.intValue();

            measurements.add(new ParallelMeasurement(
                    "Паралельний (" + threadCount + ")",
                    averageMillis,
                    speedup,
                    efficiency
            ));
        }

        return new BenchmarkResult(sequentialAverage, measurements);
    }

    private double measureAverageMillis(TextVectorizer vectorizer, int attempts) {
        long totalNanos = 0L;

        for (int i = 0; i < attempts; i++) {
            long start = System.nanoTime();
            vectorizer.vectorize(documents);
            long finish = System.nanoTime();
            totalNanos += (finish - start);
        }

        double averageNanos = (double) totalNanos / attempts;
        return averageNanos / 1_000_000.0;
    }
}
