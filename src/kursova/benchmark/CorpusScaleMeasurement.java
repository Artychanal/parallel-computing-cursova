package kursova.benchmark;

public class CorpusScaleMeasurement {

    private final int documentCount;
    private final double sequentialAverageMillis;
    private final double bestParallelAverageMillis;
    private final String bestParallelLabel;
    private final double bestSpeedup;

    public CorpusScaleMeasurement(
            int documentCount,
            double sequentialAverageMillis,
            double bestParallelAverageMillis,
            String bestParallelLabel,
            double bestSpeedup
    ) {
        this.documentCount = documentCount;
        this.sequentialAverageMillis = sequentialAverageMillis;
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

    public double getBestParallelAverageMillis() {
        return bestParallelAverageMillis;
    }

    public String getBestParallelLabel() {
        return bestParallelLabel;
    }

    public double getBestSpeedup() {
        return bestSpeedup;
    }
}
