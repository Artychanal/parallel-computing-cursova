package kursova.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BenchmarkResult {

    private final double sequentialAverageMillis;
    private final int iterations;
    private final int warmupRuns;
    private final List<ParallelMeasurement> parallelMeasurements;

    public BenchmarkResult(
            double sequentialAverageMillis,
            int iterations,
            int warmupRuns,
            List<ParallelMeasurement> parallelMeasurements
    ) {
        this.sequentialAverageMillis = sequentialAverageMillis;
        this.iterations = iterations;
        this.warmupRuns = warmupRuns;
        this.parallelMeasurements = new ArrayList<ParallelMeasurement>(parallelMeasurements);
    }

    public double getSequentialAverageMillis() {
        return sequentialAverageMillis;
    }

    public int getIterations() {
        return iterations;
    }

    public int getWarmupRuns() {
        return warmupRuns;
    }

    public List<ParallelMeasurement> getParallelMeasurements() {
        return new ArrayList<ParallelMeasurement>(parallelMeasurements);
    }

    public String toTable() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.US, "Warmup runs: %d, measurements: %d%n", warmupRuns, iterations));
        builder.append(String.format(Locale.US, "%-18s %-18s %-18s %-18s%n",
                "Mode", "Average time, ms", "Speedup", "Efficiency"));
        builder.append("-".repeat(74)).append(System.lineSeparator());

        for (ParallelMeasurement measurement : parallelMeasurements) {
            builder.append(String.format(Locale.US, "%-18s %-18.3f %-18.3f %-18.3f%n",
                    measurement.getLabel(),
                    measurement.getAverageMillis(),
                    measurement.getSpeedup(),
                    measurement.getEfficiency()));
        }

        builder.append(String.format(Locale.US, "%-18s %-18.3f %-18s %-18s%n",
                "Sequential", sequentialAverageMillis, "-", "-"));

        return builder.toString();
    }
}
