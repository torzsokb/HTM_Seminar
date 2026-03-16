package column_generation.exact_cluster;

import java.util.*;

/**
 * Heuristic labeling algorithm for RCESPP pricing.
 *
 * Solves:
 *   min reduced cost path from depot to depot
 *   subject to time resource constraint
 *   elementarity (no repeated customers)
 *
 * Designed to generate MANY negative reduced cost columns heuristically.
 */
public class HeuristicLabelingPricing {

    private final double[][] travelTimes;
    private final double[][] reducedCosts;
    private final int depot;
    private final int n;

    private final double maxTime;
    private final double minTime;

    private final int beamWidth;
    private final int maxLabels;
    private final int maxColumns;

    private final double EPS = 1e-6;

    public HeuristicLabelingPricing(
            double[][] travelTimes,
            double[][] reducedCosts,
            int depot,
            double maxTime,
            double minTime,
            int beamWidth,
            int maxLabels,
            int maxColumns
    ) {
        this.travelTimes = travelTimes;
        this.reducedCosts = reducedCosts;
        this.depot = depot;
        this.n = travelTimes.length;
        this.maxTime = maxTime;
        this.minTime = minTime;
        this.beamWidth = beamWidth;
        this.maxLabels = maxLabels;
        this.maxColumns = maxColumns;
    }

    /**
     * Label structure for heuristic ESPPRC.
     */
    private static class Label {
        int node;
        double time;
        double reducedCost;
        BitSet visited;
        Label parent;

        Label(int node, double time, double reducedCost, BitSet visited, Label parent) {
            this.node = node;
            this.time = time;
            this.reducedCost = reducedCost;
            this.visited = visited;
            this.parent = parent;
        }
    }

    /**
     * Main pricing method.
     * Returns list of routes (each route is a list of node indices).
     */
    public List<List<Integer>> generateColumns() {

        List<List<Integer>> columns = new ArrayList<>();

        // Labels stored per node
        List<List<Label>> labelsAtNode = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            labelsAtNode.add(new ArrayList<>());
        }

        // Priority queue sorted by reduced cost (best first)
        PriorityQueue<Label> queue = new PriorityQueue<>(
                Comparator.comparingDouble(l -> l.reducedCost)
        );

        // Initial label at depot
        BitSet visited = new BitSet(n);
        visited.set(depot);

        Label start = new Label(depot, 0.0, 0.0, visited, null);
        queue.add(start);
        labelsAtNode.get(depot).add(start);

        int totalLabels = 1;

        while (!queue.isEmpty()) {

            if (columns.size() >= maxColumns)
                break;

            Label current = queue.poll();

            // Try returning to depot
            if (current.node != depot) {
                double returnTime = current.time + travelTimes[current.node][depot];
                if (returnTime >= minTime && returnTime <= maxTime) {

                    double rc = current.reducedCost + reducedCosts[current.node][depot];

                    if (rc < -EPS) {
                        columns.add(reconstructRoute(current));
                    }
                }
            }

            // Extend to neighbors
            for (int j = 0; j < n; j++) {

                if (j == depot) continue;
                if (current.visited.get(j)) continue;

                double newTime = current.time + travelTimes[current.node][j];
                if (newTime > maxTime) continue;

                double newRC = current.reducedCost + reducedCosts[current.node][j];

                // Simple bound pruning
                if (newRC > 1e6) continue;

                BitSet newVisited = (BitSet) current.visited.clone();
                newVisited.set(j);

                Label extended = new Label(
                        j,
                        newTime,
                        newRC,
                        newVisited,
                        current
                );

                if (!isDominated(extended, labelsAtNode.get(j))) {

                    labelsAtNode.get(j).add(extended);
                    queue.add(extended);
                    totalLabels++;

                    pruneBeam(labelsAtNode.get(j));

                    if (totalLabels > maxLabels)
                        return columns;
                }
            }
        }

        return columns;
    }

    /**
     * Relaxed dominance rule (heuristic).
     */
    private boolean isDominated(Label candidate, List<Label> bucket) {

        for (Label other : bucket) {

            if (other.reducedCost <= candidate.reducedCost + EPS &&
                other.time <= candidate.time + EPS &&
                subset(other.visited, candidate.visited)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Beam width pruning.
     */
    private void pruneBeam(List<Label> bucket) {

        if (bucket.size() <= beamWidth)
            return;

        bucket.sort(Comparator.comparingDouble(l -> l.reducedCost));
        bucket.subList(beamWidth, bucket.size()).clear();
    }

    /**
     * Check if BitSet A is subset of B.
     */
    private boolean subset(BitSet a, BitSet b) {
        BitSet clone = (BitSet) a.clone();
        clone.andNot(b);
        return clone.isEmpty();
    }

    /**
     * Reconstruct route from label.
     */
    private List<Integer> reconstructRoute(Label label) {

        LinkedList<Integer> route = new LinkedList<>();
        route.addFirst(depot);

        Label current = label;

        while (current != null && current.parent != null) {
            route.addFirst(current.node);
            current = current.parent;
        }

        route.addLast(depot);

        return route;
    }
}
