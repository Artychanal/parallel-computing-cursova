package kursova.benchmark;

public class ParallelMeasurement {

    private final String label;
    private final double averageMillis;
    private final double speedup;
    private final double efficiency;

    public ParallelMeasurement(String label, double averageMillis, double speedup, double efficiency) {
        this.label = label;
        this.averageMillis = averageMillis;
        this.speedup = speedup;
        this.efficiency = efficiency;
    }

    public String getLabel() {
        return label;
    }

    public double getAverageMillis() {
        return averageMillis;
    }

    public double getSpeedup() {
        return speedup;
    }

    public double getEfficiency() {
        return efficiency;
    }
}
