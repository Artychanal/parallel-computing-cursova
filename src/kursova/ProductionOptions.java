package kursova;

public class ProductionOptions {

    private static final int DEFAULT_PARALLEL_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors());
    private static final int UNLIMITED_DOCUMENTS = -1;

    public enum SourceType {
        BOOKS,
        CORPUS
    }

    private final boolean parallel;
    private final int threadCount;
    private final SourceType sourceType;
    private final int documentLimit;

    private ProductionOptions(boolean parallel, int threadCount, SourceType sourceType, int documentLimit) {
        this.parallel = parallel;
        this.threadCount = threadCount;
        this.sourceType = sourceType;
        this.documentLimit = documentLimit;
    }

    public static ProductionOptions fromArgs(String[] args) {
        if (args.length < 2) {
            return new ProductionOptions(false, 1, SourceType.BOOKS, UNLIMITED_DOCUMENTS);
        }

        String mode = args[1].trim().toLowerCase();
        if ("parallel".equals(mode)) {
            int threads = DEFAULT_PARALLEL_THREADS;
            if (args.length >= 3) {
                try {
                    threads = Math.max(1, Integer.parseInt(args[2]));
                } catch (NumberFormatException ignored) {
                    threads = DEFAULT_PARALLEL_THREADS;
                }
            }
            SourceType sourceType = parseSourceType(args, 3);
            int documentLimit = parseDocumentLimit(args, 4);
            return new ProductionOptions(true, threads, sourceType, documentLimit);
        }

        SourceType sourceType = parseSourceType(args, 2);
        int documentLimit = parseDocumentLimit(args, 3);
        return new ProductionOptions(false, 1, sourceType, documentLimit);
    }

    public boolean isParallel() {
        return parallel;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public int getDocumentLimit() {
        return documentLimit;
    }

    public boolean hasDocumentLimit() {
        return documentLimit > 0;
    }

    public String describe() {
        String sourceDescription = sourceType == SourceType.CORPUS ? "chunked corpus" : "source books";
        String limitDescription = hasDocumentLimit() ? ", limit " + documentLimit + " documents" : ", no document limit";

        if (!parallel) {
            return "sequential, " + sourceDescription + limitDescription;
        }
        return "parallel (" + threadCount + " threads), " + sourceDescription + limitDescription;
    }

    private static SourceType parseSourceType(String[] args, int index) {
        if (args.length <= index) {
            return SourceType.BOOKS;
        }

        String value = args[index].trim().toLowerCase();
        if ("corpus".equals(value) || "chunks".equals(value) || "chunked".equals(value)) {
            return SourceType.CORPUS;
        }
        return SourceType.BOOKS;
    }

    private static int parseDocumentLimit(String[] args, int index) {
        if (args.length <= index) {
            return UNLIMITED_DOCUMENTS;
        }

        try {
            return Math.max(1, Integer.parseInt(args[index]));
        } catch (NumberFormatException ignored) {
            return UNLIMITED_DOCUMENTS;
        }
    }
}
