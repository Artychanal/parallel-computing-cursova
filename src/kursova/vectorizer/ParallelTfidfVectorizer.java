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

public class ParallelTfidfVectorizer extends AbstractTfidfVectorizer implements AutoCloseable {

    private final int threadCount;
    private final ExecutorService executor;

    public ParallelTfidfVectorizer(int threadCount) {
        this.threadCount = Math.max(1, threadCount);
        this.executor = Executors.newFixedThreadPool(this.threadCount);
    }

    @Override
    public VectorizationResult vectorize(List<DocumentData> documents) {
        List<Future<PartialTermsResult>> extractionFutures = submitExtractionTasks(documents);
        PartialTermsResult extractionResult = mergeExtractionResults(extractionFutures, documents.size());

        List<DocumentTerms> documentTerms = extractionResult.getDocumentTerms();
        Map<String, Integer> documentFrequency = extractionResult.getLocalFrequency();
        Map<String, Double> idf = computeIdf(documentFrequency, documents.size());

        List<Future<PartialVectorResult>> vectorFutures = submitVectorTasks(documentTerms, idf);
        Map<String, Map<String, Double>> vectors = mergeVectorResults(vectorFutures, documentTerms.size());

        return buildResult(documentTerms, documentFrequency, idf, vectors);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private List<Future<PartialTermsResult>> submitExtractionTasks(List<DocumentData> documents) {
        List<List<DocumentData>> partitions = partition(documents, threadCount);
        List<Future<PartialTermsResult>> futures = new ArrayList<>(partitions.size());

        for (List<DocumentData> partition : partitions) {
            futures.add(executor.submit(() -> {
                List<DocumentTerms> partialTerms = new ArrayList<>(partition.size());
                Map<String, Integer> localFrequency = new HashMap<>(
                        calculateHashCapacity(partition.size() * 100)
                );

                for (DocumentData document : partition) {
                    DocumentTerms terms = extractTerms(document);
                    partialTerms.add(terms);

                    for (String uniqueTerm : terms.getUniqueTerms()) {
                        localFrequency.merge(uniqueTerm, 1, Integer::sum);
                    }
                }

                return new PartialTermsResult(partialTerms, localFrequency);
            }));
        }

        return futures;
    }

    private PartialTermsResult mergeExtractionResults(
            List<Future<PartialTermsResult>> futures,
            int expectedSize
    ) {
        List<DocumentTerms> documentTerms = new ArrayList<>(expectedSize);
        Map<String, Integer> documentFrequency = new HashMap<>(calculateHashCapacity(expectedSize * 100));

        for (Future<PartialTermsResult> future : futures) {
            PartialTermsResult partialResult = getFutureValue(future);
            documentTerms.addAll(partialResult.getDocumentTerms());

            for (Map.Entry<String, Integer> entry : partialResult.getLocalFrequency().entrySet()) {
                documentFrequency.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return new PartialTermsResult(documentTerms, documentFrequency);
    }

    private List<Future<PartialVectorResult>> submitVectorTasks(
            List<DocumentTerms> documentTerms,
            Map<String, Double> idf
    ) {
        List<List<DocumentTerms>> partitions = partition(documentTerms, threadCount);
        List<Future<PartialVectorResult>> futures = new ArrayList<>(partitions.size());

        for (List<DocumentTerms> partition : partitions) {
            futures.add(executor.submit(() -> {
                Map<String, Map<String, Double>> partialVectors = new HashMap<>(calculateHashCapacity(partition.size()));

                for (DocumentTerms terms : partition) {
                    partialVectors.put(terms.getDocumentId(), buildDocumentVector(terms, idf));
                }
                return new PartialVectorResult(partialVectors);
            }));
        }

        return futures;
    }

    private Map<String, Map<String, Double>> mergeVectorResults(
            List<Future<PartialVectorResult>> futures,
            int expectedSize
    ) {
        Map<String, Map<String, Double>> vectors = new HashMap<>(calculateHashCapacity(expectedSize));

        for (Future<PartialVectorResult> future : futures) {
            vectors.putAll(getFutureValue(future).getVectors());
        }

        return vectors;
    }

    private <T> T getFutureValue(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Parallel execution was interrupted.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Parallel processing failed.", exception);
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

    private static class PartialTermsResult {

        private final List<DocumentTerms> documentTerms;
        private final Map<String, Integer> localFrequency;

        private PartialTermsResult(List<DocumentTerms> documentTerms, Map<String, Integer> localFrequency) {
            this.documentTerms = documentTerms;
            this.localFrequency = localFrequency;
        }

        private List<DocumentTerms> getDocumentTerms() {
            return documentTerms;
        }

        private Map<String, Integer> getLocalFrequency() {
            return localFrequency;
        }
    }

    private static class PartialVectorResult {

        private final Map<String, Map<String, Double>> vectors;

        private PartialVectorResult(Map<String, Map<String, Double>> vectors) {
            this.vectors = vectors;
        }

        private Map<String, Map<String, Double>> getVectors() {
            return vectors;
        }
    }
}
