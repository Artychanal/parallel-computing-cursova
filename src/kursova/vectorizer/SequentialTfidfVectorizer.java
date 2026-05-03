package kursova.vectorizer;

import kursova.model.DocumentData;
import kursova.model.DocumentTerms;
import kursova.model.VectorizationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SequentialTfidfVectorizer extends AbstractTfidfVectorizer {

    @Override
    public VectorizationResult vectorize(List<DocumentData> documents) {
        List<DocumentTerms> documentTerms = new ArrayList<DocumentTerms>(documents.size());
        Map<String, Integer> documentFrequency = new HashMap<String, Integer>(calculateHashCapacity(documents.size()));

        for (DocumentData document : documents) {
            DocumentTerms terms = extractTerms(document);
            documentTerms.add(terms);

            for (String uniqueTerm : terms.getUniqueTerms()) {
                documentFrequency.merge(uniqueTerm, 1, Integer::sum);
            }
        }

        Map<String, Double> idf = computeIdf(documentFrequency, documents.size());
        Map<String, Map<String, Double>> vectors = new HashMap<String, Map<String, Double>>(
                calculateHashCapacity(documentTerms.size())
        );

        for (DocumentTerms terms : documentTerms) {
            vectors.put(terms.getDocumentId(), buildDocumentVector(terms, idf));
        }

        return buildResult(documentTerms, documentFrequency, idf, vectors);
    }
}
