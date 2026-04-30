package kursova.io;

import kursova.benchmark.BenchmarkResult;
import kursova.benchmark.CorpusScaleExperimentResult;
import kursova.benchmark.CorpusScaleMeasurement;
import kursova.benchmark.ParallelMeasurement;
import kursova.model.VectorizationResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class ResultExporter {

    private static final DateTimeFormatter DIRECTORY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public Path createRunDirectory(String modeName) {
        Path outputDirectory = Path.of("results", modeName + "_" + LocalDateTime.now().format(DIRECTORY_FORMATTER));

        try {
            Files.createDirectories(outputDirectory);
            return outputDirectory;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create output directory: " + outputDirectory, exception);
        }
    }

    public void exportBenchmarkResults(
            Path outputDirectory,
            BenchmarkResult benchmarkResult,
            CorpusScaleExperimentResult scaleResult
    ) {
        writeText(outputDirectory.resolve("benchmark_summary.txt"), benchmarkResult.toTable());
        writeText(outputDirectory.resolve("scale_summary.txt"), scaleResult.toTable());
        writeText(outputDirectory.resolve("benchmark_summary.csv"), buildBenchmarkCsv(benchmarkResult));
        writeText(outputDirectory.resolve("scale_experiment.csv"), buildScaleCsv(scaleResult));
    }

    public void exportProductionResults(
            Path outputDirectory,
            VectorizationResult result,
            int documentCount
    ) {
        writeText(outputDirectory.resolve("run_summary.txt"), buildProductionSummary(result, documentCount));
        writeVocabulary(outputDirectory.resolve("vocabulary.txt"), result);
        writeVectorsCsv(outputDirectory.resolve("vectors.csv"), result);
    }

    private String buildBenchmarkCsv(BenchmarkResult benchmarkResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("mode,average_millis,speedup,efficiency").append(System.lineSeparator());
        builder.append(String.format(Locale.US, "sequential,%.3f,,%n", benchmarkResult.getSequentialAverageMillis()));

        for (ParallelMeasurement measurement : benchmarkResult.getParallelMeasurements()) {
            builder.append(String.format(
                    Locale.US,
                    "\"%s\",%.3f,%.3f,%.3f%n",
                    measurement.getLabel(),
                    measurement.getAverageMillis(),
                    measurement.getSpeedup(),
                    measurement.getEfficiency()
            ));
        }

        return builder.toString();
    }

    private String buildScaleCsv(CorpusScaleExperimentResult scaleResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("documents,sequential_millis,best_parallel_millis,best_parallel_mode,speedup").append(System.lineSeparator());

        for (CorpusScaleMeasurement measurement : scaleResult.getMeasurements()) {
            builder.append(String.format(
                    Locale.US,
                    "%d,%.3f,%.3f,\"%s\",%.3f%n",
                    measurement.getDocumentCount(),
                    measurement.getSequentialAverageMillis(),
                    measurement.getBestParallelAverageMillis(),
                    measurement.getBestParallelLabel(),
                    measurement.getBestSpeedup()
            ));
        }

        return builder.toString();
    }

    private String buildProductionSummary(VectorizationResult result, int documentCount) {
        return "documents=" + documentCount + System.lineSeparator()
                + "vocabulary=" + result.getVocabulary().size() + System.lineSeparator();
    }

    private void writeVocabulary(Path path, VectorizationResult result) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (String term : result.getVocabulary()) {
                writer.write(term);
                writer.newLine();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write result file: " + path, exception);
        }
    }

    private void writeVectorsCsv(Path path, VectorizationResult result) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("document_id,term,weight");
            writer.newLine();

            for (Map.Entry<String, Map<String, Double>> entry : result.getDocumentVectors().entrySet()) {
                for (Map.Entry<String, Double> termEntry : entry.getValue().entrySet()) {
                    writer.write(String.format(
                            Locale.US,
                            "\"%s\",\"%s\",%.8f%n",
                            entry.getKey(),
                            termEntry.getKey(),
                            termEntry.getValue()
                    ));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write result file: " + path, exception);
        }
    }

    private void writeText(Path path, String content) {
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write result file: " + path, exception);
        }
    }
}
