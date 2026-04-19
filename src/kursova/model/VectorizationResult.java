package kursova.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class VectorizationResult {

    private final List<String> vocabulary;
    private final Map<String, Map<String, Double>> documentVectors;
    private final Map<String, Double> inverseDocumentFrequency;

    public VectorizationResult(
            List<String> vocabulary,
            Map<String, Map<String, Double>> documentVectors,
            Map<String, Double> inverseDocumentFrequency
    ) {
        this.vocabulary = List.copyOf(vocabulary);
        this.documentVectors = Map.copyOf(documentVectors);
        this.inverseDocumentFrequency = Map.copyOf(inverseDocumentFrequency);
    }

    public List<String> getVocabulary() {
        return Collections.unmodifiableList(vocabulary);
    }

    public Map<String, Map<String, Double>> getDocumentVectors() {
        return Collections.unmodifiableMap(documentVectors);
    }

    public Map<String, Double> getInverseDocumentFrequency() {
        return Collections.unmodifiableMap(inverseDocumentFrequency);
    }
}
