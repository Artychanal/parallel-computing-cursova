package kursova.benchmark;

import java.util.ArrayList;
import java.util.List;

public class CorpusScaleMeasurement {

    private final int documentCount;
    private final double sequentialAverageMillis;
    private final List<ParallelMeasurement> parallelMeasurements;
    private final double bestParallelAverageMillis;
    private final String bestParallelLabel;
    private final double bestSpeedup;

    public CorpusScaleMeasurement(
            int documentCount,
            double sequentialAverageMillis,
            List<ParallelMeasurement> parallelMeasurements,
            double bestParallelAverageMillis,
            String bestParallelLabel,
            double bestSpeedup
    ) {
        this.documentCount = documentCount;
        this.sequentialAverageMillis = sequentialAverageMillis;
        this.parallelMeasurements = new ArrayList<ParallelMeasurement>(parallelMeasurements);
        this.bestParallelAverageMillis = bestParallelAverageMillis;
        this.bestParallelLabel = bestParallelLabel;
        this.bestSpeedup = bestSpeedup;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public double getSequentialAverageMillis() {
        return sequentialAverageMillis;
    }

    public List<ParallelMeasurement> getParallelMeasurements() {
        return new ArrayList<ParallelMeasurement>(parallelMeasurements);
    }

    public double getBestParallelAverageMillis() {
        return bestParallelAverageMillis;
    }

    public String getBestParallelLabel() {
        return bestParallelLabel;
    }

    public double getBestSpeedup() {
        return bestSpeedup;
    }

    public double getParallelAverageMillis(String label) {
        for (ParallelMeasurement measurement : parallelMeasurements) {
            if (measurement.getLabel().equals(label)) {
                return measurement.getAverageMillis();
            }
        }

        return Double.NaN;
    }
}
