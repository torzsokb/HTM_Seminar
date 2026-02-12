import java.util.List;

public class Shift {
    public final List<Integer> route;
    public final double travelTime;
    public final double serviceTime;
    public final double totalTime;
    public int nightShift;

    public final double breakTime = 30.0;
    public final double prepTime = 30.0;


    public Shift(List<Integer> route, double travelTime, double serviceTime, int nightShift) {
        this.route = route;
        this.travelTime = travelTime;
        this.serviceTime = serviceTime;
        this.totalTime = travelTime + serviceTime + breakTime + prepTime;
        this.nightShift = nightShift;
    }
}

