package kursova.vectorizer;

import kursova.model.DocumentData;
import kursova.model.DocumentTerms;
import kursova.model.VectorizationResult;
import kursova.preprocessing.TextPreprocessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractTfidfVectorizer implements TextVectorizer {

    protected final TextPreprocessor preprocessor;

    protected AbstractTfidfVectorizer() {
        this.preprocessor = new TextPreprocessor();
    }

    protected DocumentTerms extractTerms(DocumentData document) {
        List<String> tokens = preprocessor.tokenize(document.getText());
        Map<String, Integer> termCounts = new HashMap<String, Integer>(calculateHashCapacity(tokens.size()));

        for (String token : tokens) {
            termCounts.merge(token, 1, Integer::sum);
        }

        return new DocumentTerms(document.getId(), termCounts, tokens.size());
    }

    protected Map<String, Double> computeIdf(Map<String, Integer> documentFrequency, int documentCount) {
        Map<String, Double> idf = new HashMap<String, Double>(calculateHashCapacity(documentFrequency.size()));

        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            double value = Math.log((1.0 + documentCount) / (1.0 + entry.getValue())) + 1.0;
            idf.put(entry.getKey(), value);
        }

        return idf;
    }

    protected Map<String, Double> buildDocumentVector(DocumentTerms documentTerms, Map<String, Double> idf) {
        Map<String, Double> vector = new HashMap<String, Double>(
                calculateHashCapacity(documentTerms.getTermCounts().size())
        );

        if (documentTerms.getTotalTerms() == 0) {
            return vector;
        }

        for (Map.Entry<String, Integer> entry : documentTerms.getTermCounts().entrySet()) {
            double tf = (double) entry.getValue() / documentTerms.getTotalTerms();
            double weight = tf * idf.getOrDefault(entry.getKey(), 0.0);
            vector.put(entry.getKey(), weight);
        }

        return vector;
    }

    protected VectorizationResult buildResult(
            List<DocumentTerms> documentTerms,
            Map<String, Integer> documentFrequency,
            Map<String, Double> idf,
            Map<String, Map<String, Double>> vectors
    ) {
        List<String> vocabulary = new ArrayList<String>(documentFrequency.keySet());
        Collections.sort(vocabulary);

        Map<String, Map<String, Double>> orderedVectors = new LinkedHashMap<String, Map<String, Double>>(
                calculateHashCapacity(documentTerms.size())
        );
        for (DocumentTerms terms : documentTerms) {
            orderedVectors.put(terms.getDocumentId(), vectors.getOrDefault(terms.getDocumentId(), Map.of()));
        }

        return new VectorizationResult(vocabulary, orderedVectors, idf);
    }

    protected int calculateHashCapacity(int expectedSize) {
        return Math.max(16, (int) (expectedSize / 0.75f) + 1);
    }
}
