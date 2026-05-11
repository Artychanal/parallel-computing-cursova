package kursova.vectorizer;

import kursova.model.DocumentData;
import kursova.model.DocumentTerms;
import kursova.model.VectorizationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PhaseTimingProfiler extends AbstractTfidfVectorizer {

    public PhaseProfileResult profileSequential(List<DocumentData> documents) {
        long totalStart = System.nanoTime();

        long extractionStart = System.nanoTime();
        ExtractionSnapshot extraction = extractSequential(documents);
        double extractionMillis = toMillis(extractionStart);

        long idfStart = System.nanoTime();
        Map<String, Double> idf = computeIdf(extraction.getDocumentFrequency(), documents.size());
        double idfMillis = toMillis(idfStart);

        long vectorStart = System.nanoTime();
        Map<String, Map<String, Double>> vectors = buildSequentialVectors(extraction.getDocumentTerms(), idf);
        VectorizationResult result = buildResult(
                extraction.getDocumentTerms(),
                extraction.getDocumentFrequency(),
                idf,
                vectors
        );
        double vectorMillis = toMillis(vectorStart);

        return new PhaseProfileResult(result, extractionMillis, idfMillis, vectorMillis, toMillis(totalStart));
    }

    public PhaseProfileResult profileParallel(List<DocumentData> documents, int threadCount) {
        long totalStart = System.nanoTime();

        try (ParallelExecutor executor = new ParallelExecutor(threadCount)) {
            long extractionStart = System.nanoTime();
            ExtractionSnapshot extraction = executor.extractParallel(documents);
            double extractionMillis = toMillis(extractionStart);

            long idfStart = System.nanoTime();
            Map<String, Double> idf = computeIdf(extraction.getDocumentFrequency(), documents.size());
            double idfMillis = toMillis(idfStart);

            long vectorStart = System.nanoTime();
            Map<String, Map<String, Double>> vectors = executor.buildParallelVectors(extraction.getDocumentTerms(), idf);
            VectorizationResult result = buildResult(
                    extraction.getDocumentTerms(),
                    extraction.getDocumentFrequency(),
                    idf,
                    vectors
            );
            double vectorMillis = toMillis(vectorStart);

            return new PhaseProfileResult(result, extractionMillis, idfMillis, vectorMillis, toMillis(totalStart));
        }
    }

    @Override
    public VectorizationResult vectorize(List<DocumentData> documents) {
        throw new UnsupportedOperationException("PhaseTimingProfiler is intended for profiling only.");
    }

    private ExtractionSnapshot extractSequential(List<DocumentData> documents) {
        List<DocumentTerms> documentTerms = new ArrayList<DocumentTerms>(documents.size());
        Map<String, Integer> documentFrequency = new HashMap<String, Integer>(calculateHashCapacity(documents.size()));

        for (DocumentData document : documents) {
            DocumentTerms terms = extractTerms(document);
            documentTerms.add(terms);

            for (String uniqueTerm : terms.getUniqueTerms()) {
                documentFrequency.merge(uniqueTerm, 1, Integer::sum);
            }
        }

        return new ExtractionSnapshot(documentTerms, documentFrequency);
    }

    private Map<String, Map<String, Double>> buildSequentialVectors(
            List<DocumentTerms> documentTerms,
            Map<String, Double> idf
    ) {
        Map<String, Map<String, Double>> vectors = new HashMap<String, Map<String, Double>>(
                calculateHashCapacity(documentTerms.size())
        );

        for (DocumentTerms terms : documentTerms) {
            vectors.put(terms.getDocumentId(), buildDocumentVector(terms, idf));
        }

        return vectors;
    }

    private double toMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    public static class PhaseProfileResult {

        private final VectorizationResult result;
        private final double extractionMillis;
        private final double idfMillis;
        private final double vectorMillis;
        private final double totalMillis;

        public PhaseProfileResult(
                VectorizationResult result,
                double extractionMillis,
                double idfMillis,
                double vectorMillis,
                double totalMillis
        ) {
            this.result = result;
            this.extractionMillis = extractionMillis;
            this.idfMillis = idfMillis;
            this.vectorMillis = vectorMillis;
            this.totalMillis = totalMillis;
        }

        public VectorizationResult getResult() {
            return result;
        }

        public double getExtractionMillis() {
            return extractionMillis;
        }

        public double getIdfMillis() {
            return idfMillis;
        }

        public double getVectorMillis() {
            return vectorMillis;
        }

        public double getTotalMillis() {
            return totalMillis;
        }
    }

    private static class ExtractionSnapshot {

        private final List<DocumentTerms> documentTerms;
        private final Map<String, Integer> documentFrequency;

        private ExtractionSnapshot(List<DocumentTerms> documentTerms, Map<String, Integer> documentFrequency) {
            this.documentTerms = documentTerms;
            this.documentFrequency = documentFrequency;
        }

        private List<DocumentTerms> getDocumentTerms() {
            return documentTerms;
        }

        private Map<String, Integer> getDocumentFrequency() {
            return documentFrequency;
        }
    }

    private class ParallelExecutor implements AutoCloseable {

        private final int threadCount;
        private final ExecutorService executor;

        private ParallelExecutor(int threadCount) {
            this.threadCount = Math.max(1, threadCount);
            this.executor = Executors.newFixedThreadPool(this.threadCount);
        }

        private ExtractionSnapshot extractParallel(List<DocumentData> documents) {
            List<List<DocumentData>> partitions = partition(documents, threadCount);
            List<Future<ExtractionSnapshot>> futures = new ArrayList<Future<ExtractionSnapshot>>(partitions.size());

            for (List<DocumentData> partition : partitions) {
                futures.add(executor.submit(() -> {
                    List<DocumentTerms> partialTerms = new ArrayList<DocumentTerms>(partition.size());
                    Map<String, Integer> localFrequency = new HashMap<String, Integer>(
                            calculateHashCapacity(partition.size())
                    );

                    for (DocumentData document : partition) {
                        DocumentTerms terms = extractTerms(document);
                        partialTerms.add(terms);

                        for (String uniqueTerm : terms.getUniqueTerms()) {
                            localFrequency.merge(uniqueTerm, 1, Integer::sum);
                        }
                    }

                    return new ExtractionSnapshot(partialTerms, localFrequency);
                }));
            }

            List<ExtractionSnapshot> partials = new ArrayList<ExtractionSnapshot>(futures.size());
            int totalDocuments = 0;

            for (Future<ExtractionSnapshot> future : futures) {
                ExtractionSnapshot partial;
                try {
                    partial = future.get();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Parallel execution was interrupted.", exception);
                } catch (ExecutionException exception) {
                    throw new IllegalStateException("Parallel processing failed.", exception);
                }
                partials.add(partial);
                totalDocuments += partial.getDocumentTerms().size();
            }

            List<DocumentTerms> documentTerms = new ArrayList<DocumentTerms>(totalDocuments);
            Map<String, Integer> documentFrequency = new HashMap<String, Integer>(calculateHashCapacity(totalDocuments));

            for (ExtractionSnapshot partial : partials) {
                documentTerms.addAll(partial.getDocumentTerms());
                for (Map.Entry<String, Integer> entry : partial.getDocumentFrequency().entrySet()) {
                    documentFrequency.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }

            return new ExtractionSnapshot(documentTerms, documentFrequency);
        }

        private Map<String, Map<String, Double>> buildParallelVectors(
                List<DocumentTerms> documentTerms,
                Map<String, Double> idf
        ) {
            List<List<DocumentTerms>> partitions = partition(documentTerms, threadCount);
            List<Future<Map<String, Map<String, Double>>>> futures =
                    new ArrayList<Future<Map<String, Map<String, Double>>>>(partitions.size());

            for (List<DocumentTerms> partition : partitions) {
                futures.add(executor.submit(() -> {
                    Map<String, Map<String, Double>> partialVectors = new HashMap<String, Map<String, Double>>(
                            calculateHashCapacity(partition.size())
                    );

                    for (DocumentTerms terms : partition) {
                        partialVectors.put(terms.getDocumentId(), buildDocumentVector(terms, idf));
                    }

                    return partialVectors;
                }));
            }

            Map<String, Map<String, Double>> vectors = new HashMap<String, Map<String, Double>>(
                    calculateHashCapacity(documentTerms.size())
            );

            for (Future<Map<String, Map<String, Double>>> future : futures) {
                try {
                    vectors.putAll(future.get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Parallel execution was interrupted.", exception);
                } catch (ExecutionException exception) {
                    throw new IllegalStateException("Parallel processing failed.", exception);
                }
            }

            return vectors;
        }

        @Override
        public void close() {
            executor.shutdown();
        }
    }

    private <T> List<List<T>> partition(List<T> items, int parts) {
        List<List<T>> partitions = new ArrayList<List<T>>();
        int size = items.size();
        int chunkSize = Math.max(1, (int) Math.ceil((double) size / parts));

        for (int start = 0; start < size; start += chunkSize) {
            int finish = Math.min(size, start + chunkSize);
            partitions.add(items.subList(start, finish));
        }

        return partitions;
    }
}
