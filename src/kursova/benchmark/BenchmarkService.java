package kursova.benchmark;

import kursova.model.DocumentData;
import kursova.vectorizer.ParallelTfidfVectorizer;
import kursova.vectorizer.SequentialTfidfVectorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

public class BenchmarkService {

    private static final int WARMUP_RUNS = 3;
    private static volatile long blackhole;

    private final List<DocumentData> documents;
    private final List<Integer> threadCounts;
    private final int iterations;

    public BenchmarkService(List<DocumentData> documents, List<Integer> threadCounts, int iterations) {
        this.documents = new ArrayList<>(documents);
        this.threadCounts = new ArrayList<>(threadCounts);
        this.iterations = iterations;
    }

    public BenchmarkResult run() {
        SequentialTfidfVectorizer sequential = new SequentialTfidfVectorizer();
        List<ParallelMeasurement> measurements = new ArrayList<>();

        for (Integer threadCount : threadCounts) {
            try (ParallelTfidfVectorizer parallel = new ParallelTfidfVectorizer(threadCount)) {
                performWarmup(() -> parallel.vectorize(documents).getDocumentVectors().size(), WARMUP_RUNS);

                double averageMillis = measureAverageMillis(
                        () -> parallel.vectorize(documents).getDocumentVectors().size(),
                        iterations
                );

                measurements.add(new ParallelMeasurement(
                        "Паралельний (" + threadCount + ")",
                        averageMillis,
                        0.0,
                        0.0
                ));
            }
        }

        performWarmup(() -> sequential.vectorize(documents).getDocumentVectors().size(), WARMUP_RUNS);

        double sequentialAverage = measureAverageMillis(
                () -> sequential.vectorize(documents).getDocumentVectors().size(),
                iterations
        );

        List<ParallelMeasurement> finalizedMeasurements = new ArrayList<>(measurements.size());

        for (int index = 0; index < measurements.size(); index++) {
            ParallelMeasurement measurement = measurements.get(index);
            int threadCount = threadCounts.get(index);
            double speedup = sequentialAverage / measurement.getAverageMillis();
            double efficiency = speedup / threadCount;
            finalizedMeasurements.add(new ParallelMeasurement(
                    measurement.getLabel(),
                    measurement.getAverageMillis(),
                    speedup,
                    efficiency
            ));
        }

        return new BenchmarkResult(sequentialAverage, iterations, WARMUP_RUNS, finalizedMeasurements);
    }

    private double measureAverageMillis(LongSupplier computation, int attempts) {
        long totalNanos = 0L;

        for (int attempt = 0; attempt < attempts; attempt++) {
            long start = System.nanoTime();
            blackhole = computation.getAsLong();
            long finish = System.nanoTime();
            totalNanos += (finish - start);
        }

        return (totalNanos / (double) attempts) / 1_000_000.0;
    }

    private void performWarmup(LongSupplier computation, int warmupRuns) {
        for (int run = 0; run < warmupRuns; run++) {
            blackhole = computation.getAsLong();
        }
    }
}
