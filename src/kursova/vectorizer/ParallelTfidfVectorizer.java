package kursova.vectorizer;

import kursova.model.DocumentData;
import kursova.model.DocumentTerms;
import kursova.model.VectorizationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelTfidfVectorizer extends AbstractTfidfVectorizer {

    private final int threadCount;

    public ParallelTfidfVectorizer(int threadCount) {
        this.threadCount = Math.max(1, threadCount);
    }

    @Override
    public VectorizationResult vectorize(List<DocumentData> documents) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            List<DocumentTerms> documentTerms = extractTermsParallel(documents, executor);
            Map<String, Integer> documentFrequency = buildDocumentFrequencyParallel(documentTerms, executor);
            Map<String, Double> idf = computeIdf(documentFrequency, documents.size());
            Map<String, Map<String, Double>> vectors = buildVectorsParallel(documentTerms, idf, executor);
            return buildResult(documentTerms, documentFrequency, idf, vectors);
        } finally {
            executor.shutdown();
        }
    }

    private List<DocumentTerms> extractTermsParallel(List<DocumentData> documents, ExecutorService executor) {
        List<Future<DocumentTerms>> futures = new ArrayList<Future<DocumentTerms>>();

        for (DocumentData document : documents) {
            futures.add(executor.submit(new Callable<DocumentTerms>() {
                @Override
                public DocumentTerms call() {
                    return extractTerms(document);
                }
            }));
        }

        List<DocumentTerms> result = new ArrayList<DocumentTerms>(documents.size());
        for (Future<DocumentTerms> future : futures) {
            result.add(getFutureValue(future));
        }

        return result;
    }

    private Map<String, Integer> buildDocumentFrequencyParallel(
            List<DocumentTerms> documentTerms,
            ExecutorService executor
    ) {
        List<List<DocumentTerms>> partitions = partition(documentTerms, threadCount);
        List<Future<Map<String, Integer>>> futures = new ArrayList<Future<Map<String, Integer>>>();

        for (List<DocumentTerms> partition : partitions) {
            futures.add(executor.submit(new Callable<Map<String, Integer>>() {
                @Override
                public Map<String, Integer> call() {
                    Map<String, Integer> localFrequency = new HashMap<String, Integer>();

                    for (DocumentTerms terms : partition) {
                        for (String uniqueTerm : terms.getUniqueTerms()) {
                            localFrequency.merge(uniqueTerm, 1, Integer::sum);
                        }
                    }

                    return localFrequency;
                }
            }));
        }

        Map<String, Integer> documentFrequency = new HashMap<String, Integer>();
        for (Future<Map<String, Integer>> future : futures) {
            Map<String, Integer> localResult = getFutureValue(future);
            for (Map.Entry<String, Integer> entry : localResult.entrySet()) {
                documentFrequency.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return documentFrequency;
    }

    private Map<String, Map<String, Double>> buildVectorsParallel(
            List<DocumentTerms> documentTerms,
            Map<String, Double> idf,
            ExecutorService executor
    ) {
        Map<String, Map<String, Double>> vectors = new ConcurrentHashMap<String, Map<String, Double>>();
        List<Future<?>> futures = new ArrayList<Future<?>>();

        for (DocumentTerms terms : documentTerms) {
            futures.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    vectors.put(terms.getDocumentId(), buildDocumentVector(terms, idf));
                }
            }));
        }

        for (Future<?> future : futures) {
            getFutureValue(future);
        }

        return vectors;
    }

    private <T> T getFutureValue(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Паралельне виконання було перервано.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Помилка під час паралельної обробки.", exception);
        }
    }

    private List<List<DocumentTerms>> partition(List<DocumentTerms> documentTerms, int parts) {
        List<List<DocumentTerms>> partitions = new ArrayList<List<DocumentTerms>>();
        int size = documentTerms.size();
        int chunkSize = Math.max(1, (int) Math.ceil((double) size / parts));

        for (int start = 0; start < size; start += chunkSize) {
            int finish = Math.min(size, start + chunkSize);
            partitions.add(documentTerms.subList(start, finish));
        }

        return partitions;
    }
}
