package kursova.vectorizer;

import kursova.model.DocumentData;
import kursova.model.DocumentTerms;
import kursova.model.VectorizationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BenchmarkTfidfProcessor extends AbstractTfidfVectorizer {

    @Override
    public VectorizationResult vectorize(List<DocumentData> documents) {
        throw new UnsupportedOperationException("BenchmarkTfidfProcessor does not build persistent vectorization results.");
    }

    public double processSequential(List<DocumentData> documents) {
        List<DocumentTerms> documentTerms = new ArrayList<DocumentTerms>(documents.size());
        Map<String, Integer> documentFrequency = new HashMap<String, Integer>();

        for (DocumentData document : documents) {
            DocumentTerms terms = extractTerms(document);
            documentTerms.add(terms);

            for (String uniqueTerm : terms.getUniqueTerms()) {
                documentFrequency.merge(uniqueTerm, 1, Integer::sum);
            }
        }

        Map<String, Double> idf = computeIdf(documentFrequency, documents.size());
        double checksum = 0.0;

        for (DocumentTerms terms : documentTerms) {
            checksum += accumulateVectorWeight(buildDocumentVector(terms, idf));
        }

        return checksum;
    }

    public double processParallel(List<DocumentData> documents, int threadCount) {
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, threadCount));

        try {
            List<Future<PartialTermsResult>> extractionFutures = submitExtractionTasks(documents, executor, threadCount);
            PartialTermsResult extractionResult = mergeExtractionResults(extractionFutures);

            Map<String, Double> idf = computeIdf(extractionResult.documentFrequency, documents.size());
            List<Future<Double>> vectorFutures = submitVectorTasks(extractionResult.documentTerms, idf, executor, threadCount);

            double checksum = 0.0;
            for (Future<Double> future : vectorFutures) {
                checksum += getFutureValue(future);
            }

            return checksum;
        } finally {
            executor.shutdown();
        }
    }

    private List<Future<PartialTermsResult>> submitExtractionTasks(
            List<DocumentData> documents,
            ExecutorService executor,
            int threadCount
    ) {
        List<List<DocumentData>> partitions = partition(documents, threadCount);
        List<Future<PartialTermsResult>> futures = new ArrayList<Future<PartialTermsResult>>(partitions.size());

        for (List<DocumentData> partition : partitions) {
            futures.add(executor.submit(new Callable<PartialTermsResult>() {
                @Override
                public PartialTermsResult call() {
                    List<DocumentTerms> partialTerms = new ArrayList<DocumentTerms>(partition.size());
                    Map<String, Integer> localFrequency = new HashMap<String, Integer>();

                    for (DocumentData document : partition) {
                        DocumentTerms terms = extractTerms(document);
                        partialTerms.add(terms);

                        for (String uniqueTerm : terms.getUniqueTerms()) {
                            localFrequency.merge(uniqueTerm, 1, Integer::sum);
                        }
                    }

                    return new PartialTermsResult(partialTerms, localFrequency);
                }
            }));
        }

        return futures;
    }

    private PartialTermsResult mergeExtractionResults(List<Future<PartialTermsResult>> futures) {
        List<DocumentTerms> documentTerms = new ArrayList<DocumentTerms>();
        Map<String, Integer> documentFrequency = new HashMap<String, Integer>();

        for (Future<PartialTermsResult> future : futures) {
            PartialTermsResult partialResult = getFutureValue(future);
            documentTerms.addAll(partialResult.documentTerms);

            for (Map.Entry<String, Integer> entry : partialResult.documentFrequency.entrySet()) {
                documentFrequency.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return new PartialTermsResult(documentTerms, documentFrequency);
    }

    private List<Future<Double>> submitVectorTasks(
            List<DocumentTerms> documentTerms,
            Map<String, Double> idf,
            ExecutorService executor,
            int threadCount
    ) {
        List<List<DocumentTerms>> partitions = partition(documentTerms, threadCount);
        List<Future<Double>> futures = new ArrayList<Future<Double>>(partitions.size());

        for (List<DocumentTerms> partition : partitions) {
            futures.add(executor.submit(new Callable<Double>() {
                @Override
                public Double call() {
                    double checksum = 0.0;

                    for (DocumentTerms terms : partition) {
                        checksum += accumulateVectorWeight(buildDocumentVector(terms, idf));
                    }

                    return checksum;
                }
            }));
        }

        return futures;
    }

    private double accumulateVectorWeight(Map<String, Double> vector) {
        double checksum = 0.0;

        for (Double value : vector.values()) {
            checksum += value.doubleValue();
        }

        return checksum;
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
        private final Map<String, Integer> documentFrequency;

        private PartialTermsResult(List<DocumentTerms> documentTerms, Map<String, Integer> documentFrequency) {
            this.documentTerms = documentTerms;
            this.documentFrequency = documentFrequency;
        }
    }
}
