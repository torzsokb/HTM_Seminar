package column_generation.exact_cluster;

import core.Stop;

import java.util.List;

public class ClusterInstance {
    public List<Stop> stops;
    public double[][] travelTimes;
    public double[][] reducedCosts;
    public int[] localToGlobal;
}
