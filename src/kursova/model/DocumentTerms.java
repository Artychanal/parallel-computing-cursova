package kursova.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DocumentTerms {

    private final String documentId;
    private final Map<String, Integer> termCounts;
    private final int totalTerms;

    public DocumentTerms(String documentId, Map<String, Integer> termCounts, int totalTerms) {
        this.documentId = documentId;
        this.termCounts = new HashMap<String, Integer>(termCounts);
        this.totalTerms = totalTerms;
    }

    public String getDocumentId() {
        return documentId;
    }

    public Map<String, Integer> getTermCounts() {
        return Collections.unmodifiableMap(termCounts);
    }

    public Set<String> getUniqueTerms() {
        return termCounts.keySet();
    }

    public int getTotalTerms() {
        return totalTerms;
    }
}
