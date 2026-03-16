package column_generation.exact_cluster;

import core.Shift;
import core.Stop;

import java.util.*;

public class LabelingPricing {

    // break + prep (same as your rollout heuristic)
    private static final double BREAK_AND_PREP = 60.0;

    /**
     * Main pricing routine: generate all negative reduced-cost elementary shifts.
     */
    public static List<Shift> generateShiftPool(List<Stop> stops,
                                                double[][] travelTimes,
                                                double[][] reducedCosts,
                                                double maxShiftDuration,
                                                double minShiftDuration,
                                                int maxShifts) {

        int n = stops.size(); // depot = 0

        // Initialize lower bounds for dominance (safe)
        Dominance.initLowerBounds(travelTimes, reducedCosts, stops, 0, BREAK_AND_PREP);

        // Labels stored per node
        @SuppressWarnings("unchecked")
        List<Label>[] labelsAtNode = new List[n];
        for (int i = 0; i < n; i++) labelsAtNode[i] = new ArrayList<>();

        // BFS/label-setting queue
        Queue<Label> queue = new ArrayDeque<>();

        // Start label at depot
        Label start = Label.initial();
        labelsAtNode[0].add(start);
        queue.add(start);

        List<Shift> shifts = new ArrayList<>();

        while (!queue.isEmpty()) {

            Label L = queue.poll();

            // Try to close the route at depot
            if (L.lastNode != 0) {

                double backTravel = travelTimes[L.lastNode][0];
                double totalTimeNoBreak = L.time + backTravel;
                double totalTime = totalTimeNoBreak + BREAK_AND_PREP;

                if (totalTime <= maxShiftDuration && totalTime >= minShiftDuration) {

                    List<Integer> route = new ArrayList<>(L.path);
                    route.add(0);

                    double travel = computeTravelTime(route, travelTimes);
                    double service = computeServiceTime(route, stops);

                    Shift s = new Shift(route, travel, service, 0);
                    shifts.add(s);

                    if (shifts.size() >= maxShifts) break;
                }
            }

            // Extend to next customers
            for (int j = 1; j < n; j++) {

                if (L.visited.get(j)) continue; // elementary

                double travel = travelTimes[L.lastNode][j];
                double service = stops.get(j).serviceTime;
                double newTime = L.time + travel + service;

                // Hard time feasibility (no break yet)
                if (newTime > maxShiftDuration) continue;

                double newCost = L.cost + reducedCosts[L.lastNode][j];

                BitSet newVisited = (BitSet) L.visited.clone();
                newVisited.set(j);

                List<Integer> newPath = new ArrayList<>(L.path);
                newPath.add(j);

                Label L2 = new Label(j, newTime, newCost, newVisited, newPath);

                // SAFE lower-bound pruning
                if (Dominance.infeasibleByBounds(L2, maxShiftDuration, true)) {
                    continue;
                }

                List<Label> nodeLabels = labelsAtNode[j];

                // SAFE dominance check
                if (Dominance.isDominated(L2, nodeLabels)) {
                    continue;
                }

                // Remove dominated labels (safe)
                Dominance.removeDominated(L2, nodeLabels);

                nodeLabels.add(L2);
                queue.add(L2);
            }
        }

        return shifts;
    }

    private static double computeTravelTime(List<Integer> route, double[][] travelTimes) {
        double tt = 0.0;
        for (int i = 0; i < route.size() - 1; i++) {
            tt += travelTimes[route.get(i)][route.get(i + 1)];
        }
        return tt;
    }

    private static double computeServiceTime(List<Integer> route, List<Stop> stops) {
        double st = 0.0;
        for (int i = 1; i < route.size() - 1; i++) {
            st += stops.get(route.get(i)).serviceTime;
        }
        return st;
    }
}
