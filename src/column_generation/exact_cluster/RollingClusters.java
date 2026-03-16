package column_generation.exact_cluster;

import core.Stop;
import java.util.*;

public class RollingClusters {

    public static List<List<Stop>> build(List<Stop> stops,
                                         int maxInCluster,
                                         int stepSize) {

        Stop depot = stops.get(0);

        // Sort customers by angle around depot
        List<Stop> customers = new ArrayList<>(stops.subList(1, stops.size()));
        customers.sort(Comparator.comparingDouble(s -> angle(depot, s)));

        List<List<Stop>> clusters = new ArrayList<>();

        for (int start = 0; start < customers.size(); start += stepSize) {
            int end = Math.min(start + maxInCluster, customers.size());

            List<Stop> cluster = new ArrayList<>();
            cluster.add(depot); 
            cluster.addAll(customers.subList(start, end));

            clusters.add(cluster);
        }

        return clusters;
    }

    private static double angle(Stop depot, Stop s) {
        double dx = s.longitude - depot.longitude;
        double dy = s.latitude - depot.latitude;
        return Math.atan2(dy, dx);
    }
}
