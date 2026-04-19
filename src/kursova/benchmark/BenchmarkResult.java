package kursova.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BenchmarkResult {

    private final double sequentialAverageMillis;
    private final List<ParallelMeasurement> parallelMeasurements;

    public BenchmarkResult(double sequentialAverageMillis, List<ParallelMeasurement> parallelMeasurements) {
        this.sequentialAverageMillis = sequentialAverageMillis;
        this.parallelMeasurements = new ArrayList<ParallelMeasurement>(parallelMeasurements);
    }

    public double getSequentialAverageMillis() {
        return sequentialAverageMillis;
    }

    public List<ParallelMeasurement> getParallelMeasurements() {
        return new ArrayList<ParallelMeasurement>(parallelMeasurements);
    }

    public String toTable() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.US, "%-18s %-18s %-18s %-18s%n",
                "Режим", "Середній час, мс", "Прискорення", "Ефективність"));
        builder.append("-".repeat(74)).append(System.lineSeparator());
        builder.append(String.format(Locale.US, "%-18s %-18.3f %-18s %-18s%n",
                "Послідовний", sequentialAverageMillis, "-", "-"));

        for (ParallelMeasurement measurement : parallelMeasurements) {
            builder.append(String.format(Locale.US, "%-18s %-18.3f %-18.3f %-18.3f%n",
                    measurement.getLabel(),
                    measurement.getAverageMillis(),
                    measurement.getSpeedup(),
                    measurement.getEfficiency()));
        }

        return builder.toString();
    }
}
