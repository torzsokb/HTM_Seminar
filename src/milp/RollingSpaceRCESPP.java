package milp;

import column_generation.*;
import column_generation.exact_cluster.ClusterBuilder;
import column_generation.exact_cluster.ClusterInstance;
import column_generation.exact_cluster.LabelingPricing;
import column_generation.exact_cluster.RollingClusters;
import column_generation.exact_cluster.RouteMapper;
import core.Shift;
import core.Stop;

import java.util.*;

public class RollingSpaceRCESPP implements RCESPP {

    private final int windowSize;
    private final int stepSize;
    private final int maxShifts;

    public RollingSpaceRCESPP(int windowSize,
                              int stepSize,
                              int maxShifts) {
        this.windowSize = windowSize;
        this.stepSize = stepSize;
        this.maxShifts = maxShifts;
    }

    @Override
    public List<Shift> getNewShifts(double[][] distances,
                                    List<Stop> stops,
                                    double[] duals,
                                    double maxDuration,
                                    double minDuration) {

        // Build mapping from objectId → matrix index
        Map<Integer, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < stops.size(); i++) {
            idToIndex.put(stops.get(i).objectId, i);
        }

        // Compute reduced costs
        double[][] reducedCosts = ReducedCost.computeReducedCost(distances, duals);

        // Build rolling clusters
        List<List<Stop>> clusters =
                RollingClusters.build(stops, windowSize, stepSize);

        List<Shift> allCandidates = new ArrayList<>();
        int clusterIndex = 0;

        for (List<Stop> cluster : clusters) {

            long start = System.currentTimeMillis();

            ClusterInstance ci = ClusterBuilder.buildClusterInstance(
                    cluster, stops, distances, reducedCosts, idToIndex);

            // Run exact ESPPRC
            List<Shift> pool = LabelingPricing.generateShiftPool(
                    ci.stops,
                    ci.travelTimes,
                    ci.reducedCosts,
                    maxDuration,
                    minDuration,
                    maxShifts
            );

            long end = System.currentTimeMillis();
            System.out.println("\n=== Pricing Cluster " + clusterIndex +
                    " (" + cluster.size() + " stops) ===");
            System.out.println("Cluster " + clusterIndex +
                    " solved in " + (end - start) / 1000.0 +
                    " sec, " + pool.size() + " raw shifts");

            // Map back to global IDs
            for (Shift s : pool) {
                s.route = RouteMapper.mapBackRoute(s.route, ci.localToGlobal);
                s.recomputeTotalTime();

                double rc = ReducedCost.computeShiftReducedCost(s, reducedCosts, duals[0]);
                if (rc < -1e-4 && s.totalTime >= minDuration) {
                    allCandidates.add(s);
                }
            }

            clusterIndex++;
        }

        // Sort by reduced cost
        allCandidates.sort(Comparator.comparingDouble(
                s -> ReducedCost.computeShiftReducedCost(s, reducedCosts, duals[0])));

        return allCandidates;
    }
}
