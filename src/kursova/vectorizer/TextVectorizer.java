package kursova.vectorizer;

import kursova.model.DocumentData;
import kursova.model.VectorizationResult;

import java.util.List;

public interface TextVectorizer {

    VectorizationResult vectorize(List<DocumentData> documents);
}
