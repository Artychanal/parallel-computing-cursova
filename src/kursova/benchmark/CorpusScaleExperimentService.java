package kursova.benchmark;

import kursova.model.DocumentData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CorpusScaleExperimentService {

    private final List<DocumentData> documents;
    private final List<Integer> corpusSizes;
    private final List<Integer> threadCounts;
    private final int iterations;

    public CorpusScaleExperimentService(
            List<DocumentData> documents,
            List<Integer> corpusSizes,
            List<Integer> threadCounts,
            int iterations
    ) {
        this.documents = new ArrayList<DocumentData>(documents);
        this.corpusSizes = new ArrayList<Integer>(corpusSizes);
        this.threadCounts = new ArrayList<Integer>(threadCounts);
        this.iterations = iterations;
    }

    public CorpusScaleExperimentResult run() {
        List<CorpusScaleMeasurement> measurements = new ArrayList<CorpusScaleMeasurement>();
        int warmupRuns = 1;
        Set<Integer> processedSizes = new LinkedHashSet<Integer>();

        for (Integer requestedSize : corpusSizes) {
            int actualSize = Math.min(requestedSize.intValue(), documents.size());
            if (actualSize <= 0 || processedSizes.contains(actualSize)) {
                continue;
            }
            processedSizes.add(actualSize);

            List<DocumentData> subset = new ArrayList<DocumentData>(documents.subList(0, actualSize));
            BenchmarkService benchmarkService = new BenchmarkService(subset, threadCounts, iterations);
            BenchmarkResult benchmarkResult = benchmarkService.run();

            ParallelMeasurement bestParallel = findBestParallel(benchmarkResult.getParallelMeasurements());
            measurements.add(new CorpusScaleMeasurement(
                    actualSize,
                    benchmarkResult.getSequentialAverageMillis(),
                    bestParallel.getAverageMillis(),
                    bestParallel.getLabel(),
                    bestParallel.getSpeedup()
            ));

            warmupRuns = benchmarkResult.getWarmupRuns();
        }

        return new CorpusScaleExperimentResult(iterations, warmupRuns, measurements);
    }

    private ParallelMeasurement findBestParallel(List<ParallelMeasurement> measurements) {
        ParallelMeasurement best = measurements.get(0);

        for (ParallelMeasurement measurement : measurements) {
            if (measurement.getAverageMillis() < best.getAverageMillis()) {
                best = measurement;
            }
        }

        return best;
    }
}
