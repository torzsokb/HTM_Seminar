package column_generation.exact_cluster;

import core.Stop;

import java.util.BitSet;
import java.util.List;

public class Dominance {

    // Lower bounds (must be initialized once per pricing call)
    private static double[] minReturnTime;     // min time to return to depot
    private static double[] minOutgoingTime;   // min (travel + service) to any next customer
    private static double[] minOutgoingCost;   // min reduced cost of any outgoing arc

    /**
     * Initialize all lower bounds.
     * This MUST be called once before running the labeling algorithm.
     */
    public static void initLowerBounds(double[][] travelTimes,
                                       double[][] reducedCosts,
                                       List<Stop> stops,
                                       int depot,
                                       double breakAndPrep) {

        int n = stops.size();
        minReturnTime = new double[n];
        minOutgoingTime = new double[n];
        minOutgoingCost = new double[n];

        for (int i = 0; i < n; i++) {

            // 1) Minimum time to return to depot (travel + break+prep)
            minReturnTime[i] = travelTimes[i][depot] + breakAndPrep;

            // 2) Minimum outgoing (travel + service)
            double bestTime = Double.POSITIVE_INFINITY;
            double bestCost = Double.POSITIVE_INFINITY;

            for (int j = 0; j < n; j++) {
                if (i == j) continue;

                double t = travelTimes[i][j] + stops.get(j).serviceTime;
                if (t < bestTime) bestTime = t;

                double c = reducedCosts[i][j];
                if (c < bestCost) bestCost = c;
            }

            minOutgoingTime[i] = bestTime;
            minOutgoingCost[i] = bestCost;
        }
    }

    /**
     * Hard feasibility pruning using time and cost lower bounds.
     * 100% SAFE.
     */
    public static boolean infeasibleByBounds(Label L,
                                             double maxShiftDuration,
                                             boolean pricingMode) {

        int i = L.lastNode;

        // 1) Time bound: even returning immediately exceeds max duration
        if (L.time + minReturnTime[i] > maxShiftDuration + 1e-9) {
            return true;
        }

        // 2) Time bound: even adding one more customer + return exceeds max duration
        if (L.time + minOutgoingTime[i] + minReturnTime[i] > maxShiftDuration + 1e-9) {
            return true;
        }

        // 3) Cost bound: even the best outgoing arc cannot make reduced cost negative
        if (pricingMode) {
            if (L.cost + minOutgoingCost[i] >= 0.0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if candidate is dominated by any existing label at this node.
     * SAFE rules only:
     *  - same-visited-set dominance
     *  - classic ESPPRC dominance (subset of visited)
     */
    public static boolean isDominated(Label cand, List<Label> labelsAtNode) {

        for (Label l : labelsAtNode) {

            // 1) SAME-VISITED-SET DOMINANCE (safe)
            if (l.visited.equals(cand.visited)) {
                if (l.time <= cand.time && l.cost <= cand.cost) {
                    return true;
                }
                continue;
            }

            // 2) CLASSIC ESPPRC DOMINANCE (safe)
            if (l.time <= cand.time &&
                l.cost <= cand.cost &&
                subsetOrEqual(l.visited, cand.visited)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Remove labels dominated by candidate (safe rules only).
     */
    public static void removeDominated(Label cand, List<Label> labelsAtNode) {

        labelsAtNode.removeIf(l -> {

            // 1) SAME-VISITED-SET DOMINANCE
            if (cand.visited.equals(l.visited)) {
                return cand.time <= l.time && cand.cost <= l.cost;
            }

            // 2) CLASSIC ESPPRC DOMINANCE
            return cand.time <= l.time &&
                   cand.cost <= l.cost &&
                   subsetOrEqual(cand.visited, l.visited);
        });
    }

    private static boolean subsetOrEqual(BitSet a, BitSet b) {
        BitSet tmp = (BitSet) a.clone();
        tmp.andNot(b);
        return tmp.isEmpty();
    }
}
