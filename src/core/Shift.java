package core;
import java.util.Arrays;
import java.util.List;

public class Shift {
    public List<Integer> route;
    public double travelTime;
    public double serviceTime;
    public double totalTime;
    public int nightShift;
    public final double totalTimeNoBreak;

    public final double breakTime = 30.0;
    public final double prepTime = 30.0;

    private int[] uniqueStopsSorted;
    private int shiftSignature;


    public Shift(List<Integer> route, double travelTime, double serviceTime, int nightShift) {
        this.route = route;
        this.travelTime = travelTime;
        this.serviceTime = serviceTime;
        this.totalTime = travelTime + serviceTime + breakTime + prepTime;
        this.nightShift = nightShift;
        this.totalTimeNoBreak = travelTime + serviceTime;
        this.uniqueStopsSorted = route.stream().mapToInt(Integer::intValue).distinct().sorted().toArray();
        this.shiftSignature = Arrays.hashCode(uniqueStopsSorted);
    }

    public void recomputeTotalTime() {
        totalTime = travelTime + serviceTime + breakTime + prepTime;
    }

    public void updateSignature() {
        totalTime = travelTime + serviceTime + breakTime + prepTime;
        uniqueStopsSorted = route.stream().mapToInt(Integer::intValue).distinct().sorted().toArray();
        shiftSignature = Arrays.hashCode(uniqueStopsSorted);
    }

    public int getSignature() {
        return shiftSignature;
    }

    public int[] getUniqueStops() {
        return uniqueStopsSorted;
    }

    public List<Integer> getRoute() {
        return route;
    }
}

