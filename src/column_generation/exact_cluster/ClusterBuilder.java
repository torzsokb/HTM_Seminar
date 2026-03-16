package column_generation.exact_cluster;

import core.Stop;
import java.util.*;

public class ClusterBuilder {

    public static ClusterInstance buildClusterInstance(List<Stop> cluster,
                                                       List<Stop> allStops,
                                                       double[][] travelTimes,
                                                       double[][] reducedCosts,
                                                       Map<Integer,Integer> idToIndex) {

        int n = cluster.size();
        ClusterInstance ci = new ClusterInstance();
        ci.stops = cluster;
        ci.localToGlobal = new int[n];

        // Map cluster-local index → matrix index
        for (int i = 0; i < n; i++) {
            int oid = cluster.get(i).objectId;
            int idx = idToIndex.get(oid);   // SAFE
            ci.localToGlobal[i] = idx;
        }

        ci.travelTimes = new double[n][n];
        ci.reducedCosts = new double[n][n];

        // Build cluster-local matrices
        for (int i = 0; i < n; i++) {
            int gi = ci.localToGlobal[i];
            for (int j = 0; j < n; j++) {
                int gj = ci.localToGlobal[j];
                ci.travelTimes[i][j] = travelTimes[gi][gj];
                ci.reducedCosts[i][j] = reducedCosts[gi][gj];
            }
        }

        return ci;
    }
}
