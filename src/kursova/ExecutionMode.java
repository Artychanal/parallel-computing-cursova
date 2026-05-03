package kursova;

public enum ExecutionMode {
    BENCHMARK,
    DEMO,
    PRODUCTION;

    public static ExecutionMode fromArgs(String[] args) {
        if (args.length == 0) {
            return BENCHMARK;
        }

        String normalized = args[0].trim().toLowerCase();
        if ("demo".equals(normalized) || "test".equals(normalized)) {
            return DEMO;
        }
        if ("production".equals(normalized) || "prod".equals(normalized) || "real".equals(normalized)) {
            return PRODUCTION;
        }

        return BENCHMARK;
    }
}
