package kursova.benchmark;

import kursova.model.DocumentData;
import kursova.vectorizer.BenchmarkTfidfProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

public class BenchmarkService {

    private static final int SEQUENTIAL_WARMUP_RUNS = 1;
    private static volatile double blackhole;

    private final List<DocumentData> documents;
    private final List<Integer> threadCounts;
    private final int iterations;

    public BenchmarkService(List<DocumentData> documents, List<Integer> threadCounts, int iterations) {
        this.documents = new ArrayList<DocumentData>(documents);
        this.threadCounts = new ArrayList<Integer>(threadCounts);
        this.iterations = iterations;
    }

    public BenchmarkResult run() {
        BenchmarkTfidfProcessor processor = new BenchmarkTfidfProcessor();
        List<ParallelMeasurement> measurements = new ArrayList<ParallelMeasurement>();

        for (Integer threadCount : threadCounts) {
            final int currentThreadCount = threadCount.intValue();
            double averageMillis = measureAverageMillis(new DoubleSupplier() {
                @Override
                public double getAsDouble() {
                    return processor.processParallel(documents, currentThreadCount);
                }
            }, iterations);
            measurements.add(new ParallelMeasurement(
                    "Паралельний (" + threadCount + ")",
                    averageMillis,
                    0.0,
                    0.0
            ));
        }

        performWarmup(new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return processor.processSequential(documents);
            }
        }, SEQUENTIAL_WARMUP_RUNS);
        double sequentialAverage = measureAverageMillis(new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return processor.processSequential(documents);
            }
        }, iterations);

        List<ParallelMeasurement> finalizedMeasurements = new ArrayList<ParallelMeasurement>(measurements.size());
        for (int i = 0; i < measurements.size(); i++) {
            ParallelMeasurement measurement = measurements.get(i);
            int threadCount = threadCounts.get(i).intValue();
            double speedup = sequentialAverage / measurement.getAverageMillis();
            double efficiency = speedup / threadCount;

            finalizedMeasurements.add(new ParallelMeasurement(
                    measurement.getLabel(),
                    measurement.getAverageMillis(),
                    speedup,
                    efficiency
            ));
        }

        return new BenchmarkResult(
                sequentialAverage,
                iterations,
                SEQUENTIAL_WARMUP_RUNS,
                finalizedMeasurements
        );
    }

    private double measureAverageMillis(DoubleSupplier computation, int attempts) {
        long totalNanos = 0L;

        for (int i = 0; i < attempts; i++) {
            long start = System.nanoTime();
            blackhole = computation.getAsDouble();
            long finish = System.nanoTime();
            totalNanos += (finish - start);
        }

        double averageNanos = (double) totalNanos / attempts;
        return averageNanos / 1_000_000.0;
    }

    private void performWarmup(DoubleSupplier computation, int warmupRuns) {
        for (int i = 0; i < warmupRuns; i++) {
            blackhole = computation.getAsDouble();
        }
    }
}
