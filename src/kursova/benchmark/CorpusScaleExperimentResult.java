package kursova.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        builder.append(String.format(Locale.US, "%-18s %-18s %-18s %-18s %-18s%n",
                "Documents", "Sequential, ms", "Best parallel, ms", "Best mode", "Speedup"));
        builder.append("-".repeat(94)).append(System.lineSeparator());

        for (CorpusScaleMeasurement measurement : measurements) {
            builder.append(String.format(Locale.US, "%-18d %-18.3f %-18.3f %-18s %-18.3f%n",
                    measurement.getDocumentCount(),
                    measurement.getSequentialAverageMillis(),
                    measurement.getBestParallelAverageMillis(),
                    measurement.getBestParallelLabel(),
                    measurement.getBestSpeedup()));
        }

        return builder.toString();
    }
}
