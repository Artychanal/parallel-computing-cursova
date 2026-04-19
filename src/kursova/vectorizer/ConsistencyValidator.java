package kursova.vectorizer;

import kursova.model.DocumentData;
import kursova.model.VectorizationResult;

import java.util.List;
import java.util.Map;

public class ConsistencyValidator {

    private static final double EPSILON = 1.0e-9;

    public boolean matchesParallelResult(
            List<DocumentData> documents,
            VectorizationResult sequentialResult,
            int threadCount
    ) {
        TextVectorizer parallelVectorizer = new ParallelTfidfVectorizer(threadCount);
        VectorizationResult parallelResult = parallelVectorizer.vectorize(documents);
        return areEqual(sequentialResult, parallelResult);
    }

    private boolean areEqual(VectorizationResult left, VectorizationResult right) {
        if (!left.getVocabulary().equals(right.getVocabulary())) {
            return false;
        }

        if (!compareDoubleMaps(left.getInverseDocumentFrequency(), right.getInverseDocumentFrequency())) {
            return false;
        }

        return compareDocumentVectors(left.getDocumentVectors(), right.getDocumentVectors());
    }

    private boolean compareDocumentVectors(
            Map<String, Map<String, Double>> leftVectors,
            Map<String, Map<String, Double>> rightVectors
    ) {
        if (!leftVectors.keySet().equals(rightVectors.keySet())) {
            return false;
        }

        for (Map.Entry<String, Map<String, Double>> entry : leftVectors.entrySet()) {
            Map<String, Double> rightVector = rightVectors.get(entry.getKey());
            if (rightVector == null || !compareDoubleMaps(entry.getValue(), rightVector)) {
                return false;
            }
        }

        return true;
    }

    private boolean compareDoubleMaps(Map<String, Double> left, Map<String, Double> right) {
        if (!left.keySet().equals(right.keySet())) {
            return false;
        }

        for (Map.Entry<String, Double> entry : left.entrySet()) {
            Double rightValue = right.get(entry.getKey());
            if (rightValue == null || Math.abs(entry.getValue() - rightValue.doubleValue()) > EPSILON) {
                return false;
            }
        }

        return true;
    }
}
