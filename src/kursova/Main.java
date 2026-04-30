package kursova;

import kursova.benchmark.BenchmarkResult;
import kursova.benchmark.BenchmarkService;
import kursova.benchmark.CorpusScaleExperimentResult;
import kursova.benchmark.CorpusScaleExperimentService;
import kursova.io.CorpusLoader;
import kursova.io.GutenbergCorpusImporter;
import kursova.io.ResultExporter;
import kursova.io.SampleCorpusFactory;
import kursova.model.DocumentData;
import kursova.model.VectorizationResult;
import kursova.vectorizer.ConsistencyValidator;
import kursova.vectorizer.ParallelTfidfVectorizer;
import kursova.vectorizer.SequentialTfidfVectorizer;
import kursova.vectorizer.TextVectorizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Main {

    private static final Path CORPUS_DIRECTORY = Path.of("data", "corpus_fulltext");
    private static final Path GUTENBERG_SOURCE_DIRECTORY = Path.of("data", "source", "gutenberg");
    private static final int IMPORT_DOCUMENT_LIMIT = 60000;
    private static final int CHUNK_WORD_COUNT = 2000;
    private static final int CHUNK_STRIDE = 100;
    private static final int BENCHMARK_ITERATIONS = 20;
    private static final int CONSISTENCY_CHECK_LIMIT = 1000;
    private static final List<Integer> THREAD_COUNTS = Arrays.asList(2, 4, 8);
    private static final List<Integer> CORPUS_SIZES = Arrays.asList(500, 1000, 2000, 3000, 5000, 10000, 15000, 20000);

    public static void main(String[] args) {
        ExecutionMode mode = ExecutionMode.fromArgs(args);
        ProductionOptions productionOptions = mode == ExecutionMode.PRODUCTION
                ? ProductionOptions.fromArgs(args)
                : null;
        System.out.println("Application started. Mode: " + mode + ".");
        if (mode == ExecutionMode.PRODUCTION) {
            System.out.println("Production arguments: " + String.join(" ", args));
        }
        List<DocumentData> documents = loadDocuments(mode, productionOptions);

        if (documents.isEmpty()) {
            System.out.println("Document corpus is empty. Application will exit.");
            return;
        }

        System.out.println("Document count: " + documents.size());
        System.out.println();

        if (mode == ExecutionMode.PRODUCTION) {
            runProductionMode(documents, productionOptions);
            return;
        }

        runBenchmarkMode(documents);
    }

    private static void runBenchmarkMode(List<DocumentData> documents) {
        TextVectorizer sequentialVectorizer = new SequentialTfidfVectorizer();
        VectorizationResult sequentialResult = sequentialVectorizer.vectorize(documents);
        printPreview(sequentialResult);
        validateConsistency(documents);

        BenchmarkService benchmarkService = new BenchmarkService(documents, THREAD_COUNTS, BENCHMARK_ITERATIONS);
        BenchmarkResult benchmarkResult = benchmarkService.run();
        CorpusScaleExperimentService scaleExperimentService = new CorpusScaleExperimentService(
                documents,
                CORPUS_SIZES,
                THREAD_COUNTS,
                BENCHMARK_ITERATIONS
        );
        CorpusScaleExperimentResult scaleExperimentResult = scaleExperimentService.run();

        ResultExporter exporter = new ResultExporter();
        Path outputDirectory = exporter.createRunDirectory("benchmark");
        exporter.exportBenchmarkResults(outputDirectory, benchmarkResult, scaleExperimentResult);

        System.out.println();
        System.out.println("Performance benchmark results:");
        System.out.println(benchmarkResult.toTable());
        System.out.println();
        System.out.println("Scalability by corpus size:");
        System.out.println(scaleExperimentResult.toTable());
        System.out.println();
        System.out.println("Benchmark results were saved to: " + outputDirectory.toAbsolutePath());
    }

    private static void runProductionMode(List<DocumentData> documents, ProductionOptions options) {
        System.out.println("Production mode configuration: " + options.describe() + ".");
        System.out.println("Production mode: starting TF-IDF computation for the entire corpus...");

        VectorizationResult result;
        if (options.isParallel()) {
            try (ParallelTfidfVectorizer parallelVectorizer = new ParallelTfidfVectorizer(options.getThreadCount())) {
                result = parallelVectorizer.vectorize(documents);
            }
        } else {
            TextVectorizer sequentialVectorizer = new SequentialTfidfVectorizer();
            result = sequentialVectorizer.vectorize(documents);
        }

        System.out.println("Production mode: TF-IDF computation completed.");
        printPreview(result);

        ResultExporter exporter = new ResultExporter();
        System.out.println("Production mode: preparing output directory...");
        Path outputDirectory = exporter.createRunDirectory("production");
        System.out.println("Production mode: saving results to files...");
        exporter.exportProductionResults(outputDirectory, result, documents.size());
        System.out.println("Production mode: result export completed.");

        System.out.println();
        System.out.println("Production run results were saved to: " + outputDirectory.toAbsolutePath());
    }

    private static List<DocumentData> loadDocuments(ExecutionMode mode, ProductionOptions productionOptions) {
        if (mode == ExecutionMode.PRODUCTION) {
            return loadProductionDocuments(productionOptions);
        }

        return loadBenchmarkDocuments();
    }

    private static List<DocumentData> loadProductionDocuments(ProductionOptions options) {
        long start = System.nanoTime();
        System.out.println("Starting production corpus loading...");
        CorpusLoader loader = new CorpusLoader();
        Path sourceDirectory = options.getSourceType() == ProductionOptions.SourceType.CORPUS
                ? CORPUS_DIRECTORY
                : GUTENBERG_SOURCE_DIRECTORY;

        if (options.getSourceType() == ProductionOptions.SourceType.CORPUS) {
            ensureCorpusSettings();
            if (countCorpusDocuments() == 0) {
                importGutenbergCorpusIfAvailable();
            }
        }

        int documentLimit = options.hasDocumentLimit() ? options.getDocumentLimit() : Integer.MAX_VALUE;
        List<DocumentData> documents = loader.loadFromDirectory(sourceDirectory, documentLimit);

        if (!documents.isEmpty()) {
            System.out.println("Production documents were loaded from directory: "
                    + sourceDirectory.toAbsolutePath());
            printElapsed("Production corpus loading completed", start);
            return documents;
        }

        System.out.println("Production source files were not found. Falling back to the generated corpus.");
        printElapsed("Generated production corpus loading completed", start);
        return SampleCorpusFactory.createSyntheticCorpus(14);
    }

    private static List<DocumentData> loadBenchmarkDocuments() {
        long start = System.nanoTime();
        System.out.println("Starting corpus loading...");
        CorpusLoader loader = new CorpusLoader();
        ensureCorpusCapacity();
        List<DocumentData> documents = loader.loadFromDirectory(CORPUS_DIRECTORY);

        if (!documents.isEmpty()) {
            System.out.println("Documents were loaded from directory: " + CORPUS_DIRECTORY.toAbsolutePath());
            printElapsed("Corpus loading completed", start);
            return documents;
        }

        if (importGutenbergCorpusIfAvailable()) {
            documents = loader.loadFromDirectory(CORPUS_DIRECTORY);
            if (!documents.isEmpty()) {
                System.out.println("Imported full-text corpus was loaded from directory: " + CORPUS_DIRECTORY.toAbsolutePath());
                printElapsed("Corpus loading completed", start);
                return documents;
            }
        }

        System.out.println("Text directory was not found or is empty. Falling back to the generated corpus.");
        printElapsed("Generated corpus loading completed", start);
        return SampleCorpusFactory.createSyntheticCorpus(2000);
    }

    private static void ensureCorpusCapacity() {
        int requiredDocuments = CORPUS_SIZES.get(CORPUS_SIZES.size() - 1).intValue();
        ensureCorpusSettings();
        int currentDocuments = countCorpusDocuments();

        if (currentDocuments >= requiredDocuments) {
            return;
        }

        boolean imported = importGutenbergCorpusIfAvailable();
        if (!imported && currentDocuments > 0) {
            System.out.println("The corpus contains fewer documents than required for all planned experiments: "
                    + currentDocuments + " out of " + requiredDocuments + ".");
        }
    }

    private static void ensureCorpusSettings() {
        Path manifestPath = CORPUS_DIRECTORY.resolve(GutenbergCorpusImporter.MANIFEST_FILE_NAME);
        String expectedManifest = buildExpectedManifest();

        try {
            if (Files.exists(manifestPath)) {
                String actualManifest = Files.readString(manifestPath);
                if (expectedManifest.equals(actualManifest)) {
                    return;
                }

                clearCorpusDirectory();
                return;
            }

            if (Files.exists(CORPUS_DIRECTORY) && countCorpusDocuments() > 0) {
                clearCorpusDirectory();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to validate corpus import settings.", exception);
        }
    }

    private static boolean importGutenbergCorpusIfAvailable() {
        if (!Files.exists(GUTENBERG_SOURCE_DIRECTORY)) {
            return false;
        }

        GutenbergCorpusImporter importer = new GutenbergCorpusImporter();
        int importedCount = importer.importChunkedCorpus(
                GUTENBERG_SOURCE_DIRECTORY,
                CORPUS_DIRECTORY,
                IMPORT_DOCUMENT_LIMIT,
                CHUNK_WORD_COUNT,
                CHUNK_STRIDE
        );
        System.out.println("Imported full-text documents from Project Gutenberg: " + importedCount);
        return importedCount > 0;
    }

    private static String buildExpectedManifest() {
        return "maxDocuments=" + IMPORT_DOCUMENT_LIMIT + System.lineSeparator()
                + "chunkWordCount=" + CHUNK_WORD_COUNT + System.lineSeparator()
                + "chunkStride=" + CHUNK_STRIDE + System.lineSeparator();
    }

    private static void clearCorpusDirectory() throws IOException {
        if (!Files.exists(CORPUS_DIRECTORY)) {
            return;
        }

        try (Stream<Path> files = Files.walk(CORPUS_DIRECTORY)) {
            List<Path> paths = files
                    .sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .toList();

            for (Path path : paths) {
                if (!path.equals(CORPUS_DIRECTORY)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static int countCorpusDocuments() {
        if (!Files.exists(CORPUS_DIRECTORY)) {
            return 0;
        }

        try (Stream<Path> files = Files.walk(CORPUS_DIRECTORY)) {
            return (int) files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".txt"))
                    .count();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to count documents in the corpus.", exception);
        }
    }

    private static void validateConsistency(List<DocumentData> documents) {
        int checkSize = Math.min(CONSISTENCY_CHECK_LIMIT, documents.size());
        List<DocumentData> subset = documents.subList(0, checkSize);
        VectorizationResult sequentialSubsetResult = new SequentialTfidfVectorizer().vectorize(subset);

        ConsistencyValidator validator = new ConsistencyValidator();
        boolean consistent = validator.matchesParallelResult(subset, sequentialSubsetResult, 4);

        System.out.println();
        System.out.println(
                consistent
                        ? "Correctness check: sequential and parallel results match"
                        + " on a subset of " + checkSize + " documents."
                        : "Correctness check: differences were found between sequential and parallel results"
                        + " on a subset of " + checkSize + " documents."
        );
    }

    private static void printPreview(VectorizationResult result) {
        System.out.println("Vocabulary size: " + result.getVocabulary().size());

        if (result.getDocumentVectors().isEmpty()) {
            return;
        }

        String firstDocumentId = result.getDocumentVectors().keySet().iterator().next();
        Map<String, Double> vector = result.getDocumentVectors().get(firstDocumentId);

        System.out.println("Sample TF-IDF vector for document \"" + firstDocumentId + "\":");
        vector.entrySet()
                .stream()
                .sorted((left, right) -> Double.compare(right.getValue(), left.getValue()))
                .limit(10)
                .forEach(entry -> System.out.printf("  %-20s %.6f%n", entry.getKey(), entry.getValue()));
    }

    private static void printElapsed(String label, long startNanos) {
        double elapsedSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        System.out.printf("%s: %.3f s%n", label, elapsedSeconds);
    }
}
