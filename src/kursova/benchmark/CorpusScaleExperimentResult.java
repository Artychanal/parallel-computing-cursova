package kursova.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CorpusScaleExperimentResult {

    private final int iterations;
    private final int warmupRuns;
    private final List<CorpusScaleMeasurement> measurements;

    public CorpusScaleExperimentResult(int iterations, int warmupRuns, List<CorpusScaleMeasurement> measurements) {
        this.iterations = iterations;
        this.warmupRuns = warmupRuns;
        this.measurements = new ArrayList<CorpusScaleMeasurement>(measurements);
    }

    public int getIterations() {
        return iterations;
    }

    public int getWarmupRuns() {
        return warmupRuns;
    }

    public List<CorpusScaleMeasurement> getMeasurements() {
        return new ArrayList<CorpusScaleMeasurement>(measurements);
    }

    public String toTable() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.US, "Warmup runs: %d, measurements: %d%n", warmupRuns, iterations));
        List<String> parallelLabels = collectParallelLabels();

        StringBuilder headerFormat = new StringBuilder("%-18s %-18s");
        for (int index = 0; index < parallelLabels.size(); index++) {
            headerFormat.append(" %-18s");
        }
        headerFormat.append(" %-18s %-18s %-18s%n");

        List<Object> headerValues = new ArrayList<Object>();
        headerValues.add("Documents");
        headerValues.add("Sequential, ms");
        headerValues.addAll(parallelLabels);
        headerValues.add("Best parallel, ms");
        headerValues.add("Best mode");
        headerValues.add("Speedup");

        builder.append(String.format(Locale.US, headerFormat.toString(), headerValues.toArray()));
        builder.append("-".repeat(18 * (5 + parallelLabels.size()))).append(System.lineSeparator());

        for (CorpusScaleMeasurement measurement : measurements) {
            List<Object> rowValues = new ArrayList<Object>();
            rowValues.add(measurement.getDocumentCount());
            rowValues.add(measurement.getSequentialAverageMillis());

            for (String label : parallelLabels) {
                rowValues.add(formatAverageMillis(measurement.getParallelAverageMillis(label)));
            }

            rowValues.add(measurement.getBestParallelAverageMillis());
            rowValues.add(measurement.getBestParallelLabel());
            rowValues.add(measurement.getBestSpeedup());

            StringBuilder rowFormat = new StringBuilder("%-18d %-18.3f");
            for (int index = 0; index < parallelLabels.size(); index++) {
                rowFormat.append(" %-18s");
            }
            rowFormat.append(" %-18.3f %-18s %-18.3f%n");

            builder.append(String.format(Locale.US, rowFormat.toString(), rowValues.toArray()));
        }

        return builder.toString();
    }

    private List<String> collectParallelLabels() {
        if (measurements.isEmpty()) {
            return List.of();
        }

        return measurements.get(0).getParallelMeasurements()
                .stream()
                .map(ParallelMeasurement::getLabel)
                .collect(Collectors.toList());
    }

    private String formatAverageMillis(double value) {
        if (Double.isNaN(value)) {
            return "-";
        }

        return String.format(Locale.US, "%.3f", value);
    }
}
