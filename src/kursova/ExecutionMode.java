package kursova;

public enum ExecutionMode {
    BENCHMARK,
    QUICK_BENCHMARK,
    DEMO,
    PRODUCTION;

    public static ExecutionMode fromArgs(String[] args) {
        if (args.length == 0) {
            return BENCHMARK;
        }

        String normalized = args[0].trim().toLowerCase();
        if ("quick".equals(normalized) || "quick-benchmark".equals(normalized) || "fast".equals(normalized)) {
            return QUICK_BENCHMARK;
        }
        if ("demo".equals(normalized) || "test".equals(normalized)) {
            return DEMO;
        }
        if ("production".equals(normalized) || "prod".equals(normalized) || "real".equals(normalized)) {
            return PRODUCTION;
        }

        return BENCHMARK;
    }
}
