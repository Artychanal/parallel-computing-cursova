package kursova.benchmark;

public class BenchmarkScenarioResult {

    private final String scenarioLabel;
    private final int documentCount;
    private final BenchmarkResult benchmarkResult;

    public BenchmarkScenarioResult(String scenarioLabel, int documentCount, BenchmarkResult benchmarkResult) {
        this.scenarioLabel = scenarioLabel;
        this.documentCount = documentCount;
        this.benchmarkResult = benchmarkResult;
    }

    public String getScenarioLabel() {
        return scenarioLabel;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public BenchmarkResult getBenchmarkResult() {
        return benchmarkResult;
    }
}
