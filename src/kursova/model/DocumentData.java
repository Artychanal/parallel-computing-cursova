package kursova.model;

public class DocumentData {

    private final String id;
    private final String text;

    public DocumentData(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
