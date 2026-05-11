package kursova.vectorizer;

import kursova.model.DocumentData;
import kursova.model.DocumentTerms;
import kursova.model.VectorizationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class ForkJoinTfidfVectorizer extends AbstractTfidfVectorizer implements AutoCloseable {

    private static final int TASKS_PER_WORKER = 8;

    private final int threadCount;
    private final ForkJoinPool pool;

    public ForkJoinTfidfVectorizer(int threadCount) {
        this.threadCount = Math.max(1, threadCount);
        this.pool = new ForkJoinPool(this.threadCount);
    }

    @Override
    public VectorizationResult vectorize(List<DocumentData> documents) {
        int extractionThreshold = calculateLeafThreshold(documents.size());
        PartialTermsResult extractionResult = pool.invoke(
                new ExtractionTask(documents, 0, documents.size(), extractionThreshold)
        );

        List<DocumentTerms> documentTerms = extractionResult.getDocumentTerms();
        Map<String, Integer> documentFrequency = extractionResult.getLocalFrequency();
        Map<String, Double> idf = computeIdf(documentFrequency, documents.size());

        int vectorThreshold = calculateLeafThreshold(documentTerms.size());
        PartialVectorResult vectorResult = pool.invoke(
                new VectorTask(documentTerms, idf, 0, documentTerms.size(), vectorThreshold)
        );

        return buildResult(documentTerms, documentFrequency, idf, vectorResult.getVectors());
    }

    @Override
    public void close() {
        pool.shutdown();
    }

    private int calculateLeafThreshold(int size) {
        int taskCount = Math.max(1, threadCount * TASKS_PER_WORKER);
        return Math.max(1, (int) Math.ceil((double) Math.max(1, size) / taskCount));
    }

    private PartialTermsResult mergeTermsResults(PartialTermsResult left, PartialTermsResult right) {
        List<DocumentTerms> mergedTerms = new ArrayList<>(left.getDocumentTerms().size() + right.getDocumentTerms().size());
        mergedTerms.addAll(left.getDocumentTerms());
        mergedTerms.addAll(right.getDocumentTerms());

        Map<String, Integer> mergedFrequency = new HashMap<>(
                calculateHashCapacity(left.getLocalFrequency().size() + right.getLocalFrequency().size())
        );

        mergeFrequencyMap(mergedFrequency, left.getLocalFrequency());
        mergeFrequencyMap(mergedFrequency, right.getLocalFrequency());

        return new PartialTermsResult(mergedTerms, mergedFrequency);
    }

    private void mergeFrequencyMap(Map<String, Integer> target, Map<String, Integer> source) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    private PartialVectorResult mergeVectorResults(PartialVectorResult left, PartialVectorResult right) {
        Map<String, Map<String, Double>> mergedVectors = new HashMap<>(
                calculateHashCapacity(left.getVectors().size() + right.getVectors().size())
        );
        mergedVectors.putAll(left.getVectors());
        mergedVectors.putAll(right.getVectors());
        return new PartialVectorResult(mergedVectors);
    }

    private class ExtractionTask extends RecursiveTask<PartialTermsResult> {

        private final List<DocumentData> documents;
        private final int start;
        private final int end;
        private final int threshold;

        private ExtractionTask(List<DocumentData> documents, int start, int end, int threshold) {
            this.documents = documents;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
        }

        @Override
        protected PartialTermsResult compute() {
            if (end - start <= threshold) {
                return computeSequentially();
            }

            int middle = start + (end - start) / 2;
            ExtractionTask leftTask = new ExtractionTask(documents, start, middle, threshold);
            ExtractionTask rightTask = new ExtractionTask(documents, middle, end, threshold);

            leftTask.fork();
            PartialTermsResult rightResult = rightTask.compute();
            PartialTermsResult leftResult = leftTask.join();

            return mergeTermsResults(leftResult, rightResult);
        }

        private PartialTermsResult computeSequentially() {
            int size = end - start;
            List<DocumentTerms> partialTerms = new ArrayList<>(size);
            Map<String, Integer> localFrequency = new HashMap<>(
                    calculateHashCapacity(Math.max(1, size * 100))
            );

            for (int index = start; index < end; index++) {
                DocumentTerms terms = extractTerms(documents.get(index));
                partialTerms.add(terms);

                for (String uniqueTerm : terms.getUniqueTerms()) {
                    localFrequency.merge(uniqueTerm, 1, Integer::sum);
                }
            }

            return new PartialTermsResult(partialTerms, localFrequency);
        }
    }

    private class VectorTask extends RecursiveTask<PartialVectorResult> {

        private final List<DocumentTerms> documentTerms;
        private final Map<String, Double> idf;
        private final int start;
        private final int end;
        private final int threshold;

        private VectorTask(List<DocumentTerms> documentTerms, Map<String, Double> idf, int start, int end, int threshold) {
            this.documentTerms = documentTerms;
            this.idf = idf;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
        }

        @Override
        protected PartialVectorResult compute() {
            if (end - start <= threshold) {
                return computeSequentially();
            }

            int middle = start + (end - start) / 2;
            VectorTask leftTask = new VectorTask(documentTerms, idf, start, middle, threshold);
            VectorTask rightTask = new VectorTask(documentTerms, idf, middle, end, threshold);

            leftTask.fork();
            PartialVectorResult rightResult = rightTask.compute();
            PartialVectorResult leftResult = leftTask.join();

            return mergeVectorResults(leftResult, rightResult);
        }

        private PartialVectorResult computeSequentially() {
            Map<String, Map<String, Double>> partialVectors = new HashMap<>(
                    calculateHashCapacity(Math.max(1, end - start))
            );

            for (int index = start; index < end; index++) {
                DocumentTerms terms = documentTerms.get(index);
                partialVectors.put(terms.getDocumentId(), buildDocumentVector(terms, idf));
            }

            return new PartialVectorResult(partialVectors);
        }
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
