package milp;

import core.*;

import search.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RolloutHeur implements RCESPP {

    private final int runs;                  // how many routes to construct per pricing call
    private final int rolloutCandidatePool;  // how many next-stops we evaluate at each rollout step (top-K)
    private final int baseCandidatePool;     // how many candidates base greedy considers (top-K)
    private final long seed;
    

    // break + prep
    private static final double FIXED_OVERHEAD = 60.0;

    public RolloutHeur() {
        this(20, 10, 10, 10);
    }

    public RolloutHeur(int runs, int rolloutCandidatePool, int baseCandidatePool, long seed) {
        this.runs = runs;
        this.rolloutCandidatePool = rolloutCandidatePool;
        this.baseCandidatePool = baseCandidatePool;
        this.seed = seed;
    }

    @Override
    public List<Shift> getNewShifts(
            double[][] distances,
            List<Stop> stops,
            double[] duals,
            double maxDuration,
            double minDuration
    ) {
        int n = stops.size();
        if (n <= 1) return Collections.emptyList();

        final int depot = 0; // stops.get(0)
        Random rng = new Random(seed);

        Set<String> seen = Collections.synchronizedSet(new HashSet<>());

        List<Shift> shifts = IntStream.range(0, runs)
        .parallel() 
        .mapToObj(r -> {
            // Each thread gets its own Random to avoid contention
            Random threadRng = new Random(seed + r);

            RouteCandidate cand = rolloutSingle(distances, stops, duals, depot, maxDuration, minDuration, threadRng);
            if (cand == null || cand.reducedCost >= -1e-6) return null;
            // System.out.println("Reduced cost: " + cand.reducedCost);

            String sig = signature(cand.routeIdx, depot);
            if (!seen.add(sig)) return null; // synchronized set

            // convert indices -> objectIds excluding depot at start/end
            List<Integer> routeObjectIds = new ArrayList<>();
            for (int k = 1; k < cand.routeIdx.size() - 1; k++) {
                routeObjectIds.add(stops.get(cand.routeIdx.get(k)).objectId);
            }

            return new Shift(routeObjectIds, cand.travel, cand.service, 0);
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());


        shifts = filterShifts(shifts, runs, 1);
        System.out.println("Final number of shifts returned " + shifts.size());
        // analyzeCoverage(shifts, stops);
        return shifts;
    }

    public static void analyzeCoverage(List<Shift> shifts, List<Stop> stops) {

        Map<Integer, Integer> visitCount = new HashMap<>();
    
        // Initialize all stops except depot if needed
        for (Stop stop : stops) {
            visitCount.put(stop.objectId, 0);
        }
    
        // Count visits
        for (Shift shift : shifts) {
            for (int stopId : shift.getRoute()) {
                visitCount.merge(stopId, 1, Integer::sum);
            }
        }

        int unused = 0;
    
        System.out.println("=== Coverage Report ===");
    
        for (Stop stop : stops) {
            int count = visitCount.get(stop.objectId);
    
            if (count == 0) {
                unused++;
            }

            if (count > 0) {
                System.out.println(
                    "Stop " + stop.objectId +
                    " visited " +
                    count + " times"
                );
            }
            
        }
    
        System.out.println("Stops not visited at all: " + unused + " out of " + (stops.size() - 1));
        System.out.println("Stops used: " + (stops.size() - unused));
    }

    // Rollout (partial route)

    private RouteCandidate rolloutSingle(
        double[][] d,
        List<Stop> stops,
        double[] duals,
        int depot,
        double maxDuration,
        double minDuration,
        Random rng
) {
            int n = stops.size();

            List<Integer> partial = new ArrayList<>();
            partial.add(depot);

            boolean[] visited = new boolean[n];
            visited[depot] = true;

            int current = depot;
            double travel = 0.0;
            double service = 0.0;

            Integer shiftNight = null;

            while (true) {

                List<ScoredNode> candidates = new ArrayList<>();

                for (int j = 1; j < n; j++) {

                    if (visited[j]) continue;


                    if (!feasibleToAdd(current, j, travel, service, d, stops, depot, maxDuration))
                        continue;

                    double dualJ = (j < duals.length) ? duals[j] : 0.0;
                    double score = d[current][j] + stops.get(j).serviceTime - dualJ;

                    candidates.add(new ScoredNode(j, score));
                }

                if (candidates.isEmpty()) break;

                candidates.sort(Comparator.comparingDouble(a -> a.score));

                int k = Math.min(rolloutCandidatePool, candidates.size());
                List<ScoredNode> pool = candidates.subList(0, k);

                double bestRC = Double.POSITIVE_INFINITY;

                List<Integer> poolNodes = new ArrayList<>();
                List<RouteCandidate> poolCompletions = new ArrayList<>();

                for (ScoredNode sn : pool) {

                    int j = sn.node;

                    boolean[] visited2 = Arrays.copyOf(visited, n);
                    visited2[j] = true;

                    List<Integer> startRoute = new ArrayList<>(partial);
                    startRoute.add(j);

                    double travel2 = travel + d[current][j];
                    double service2 = service + stops.get(j).serviceTime;

                    RouteCandidate completed = greedyCompleteFrom(
                            startRoute,
                            visited2,
                            j,
                            travel2,
                            service2,
                            d,
                            stops,
                            duals,
                            depot,
                            maxDuration
                    );

                    if (completed == null) continue;

                    double totalTime = completed.travel + completed.service + FIXED_OVERHEAD;
                    if (totalTime > maxDuration + 1e-9) continue;
                    if (totalTime + 1e-9 < minDuration) continue;

                    poolNodes.add(j);
                    poolCompletions.add(completed);

                    if (completed.reducedCost < bestRC) {
                        bestRC = completed.reducedCost;
                    }
                }

                if (poolNodes.isEmpty()) break;

                double alpha = 0.30;
                double threshold = bestRC + alpha * Math.abs(bestRC);

                List<Integer> rclIndices = new ArrayList<>();

                for (int i = 0; i < poolNodes.size(); i++) {
                    if (poolCompletions.get(i).reducedCost <= threshold) {
                        rclIndices.add(i);
                    }
                }

                if (rclIndices.isEmpty()) break;

                int chosenIndex = rclIndices.get(rng.nextInt(rclIndices.size()));
                int chosenNext = poolNodes.get(chosenIndex);

                partial.add(chosenNext);
                visited[chosenNext] = true;

                travel += d[current][chosenNext];
                service += stops.get(chosenNext).serviceTime;
                current = chosenNext;

                if (shiftNight == null)
                    shiftNight = stops.get(chosenNext).nightShift;

                double closeTravel = travel + d[current][depot];
                double closeService = service;
                double closeTotal = closeTravel + closeService + FIXED_OVERHEAD;

                if (closeTotal >= minDuration - 1e-9 &&
                    closeTotal <= maxDuration + 1e-9) {

                    List<Integer> closed = new ArrayList<>(partial);
                    closed.add(depot);

                    double rc = reducedCost(
                            closed,
                            stops,
                            d,
                            duals,
                            depot,
                            closeTravel,
                            closeService
                    );

                    if (rc < 0) {
                        return new RouteCandidate(closed, closeTravel, closeService, rc);
                    }
                }
            }

            if (partial.size() == 1) return null;

            List<Integer> closed = new ArrayList<>(partial);
            closed.add(depot);

            double travelClosed = travel + d[current][depot];
            double serviceClosed = service;
            double totalClosed = travelClosed + serviceClosed + FIXED_OVERHEAD;

            if (totalClosed > maxDuration + 1e-9) return null;
            if (totalClosed + 1e-9 < minDuration) return null;

            double rc = reducedCost(
                    closed,
                    stops,
                    d,
                    duals,
                    depot,
                    travelClosed,
                    serviceClosed
            );

            if (rc >= -1e-6) return null;

            return new RouteCandidate(closed, travelClosed, serviceClosed, rc);
        }

    // Greedy heuristic

    private RouteCandidate greedyCompleteFrom(
            List<Integer> startRoute,
            boolean[] visited,
            int current,
            double travel,
            double service,
            double[][] d,
            List<Stop> stops,
            double[] duals,
            int depot,
            double maxDuration
    ) {
        int n = stops.size();
        List<Integer> route = new ArrayList<>(startRoute);

        while (true) {
            List<ScoredNode> candidates = new ArrayList<>();

            for (int j = 1; j < n; j++) {
                if (visited[j]) continue;
                //if (stops.get(j).nightShift != shiftNight) continue;

                if (!feasibleToAdd(current, j, travel, service, d, stops, depot, maxDuration)) continue;

                double dualJ = (j < duals.length) ? duals[j] : 0.0;
                double score = d[current][j] + stops.get(j).serviceTime - dualJ;
                candidates.add(new ScoredNode(j, score));
            }

            if (candidates.isEmpty()) break;

            candidates.sort(Comparator.comparingDouble(a -> a.score));
            int k = Math.min(baseCandidatePool, candidates.size());
            // deterministic best among top-k (fast + stable)
            int chosen = candidates.get(0).node;

            route.add(chosen);
            visited[chosen] = true;

            travel += d[current][chosen];
            service += stops.get(chosen).serviceTime;

            current = chosen;
        }

        // close to depot
        route.add(depot);
        travel += d[current][depot];

        // feasibility including overhead
        double totalTime = travel + service + FIXED_OVERHEAD;
        if (totalTime > maxDuration + 1e-9) return null;

        double rc = reducedCost(route, stops, d, duals, depot, travel, service);
        return new RouteCandidate(route, travel, service, rc);
    }

    // =========================
    // Helpers
    // =========================

    private boolean feasibleToAdd(
            int current,
            int next,
            double travel,
            double service,
            double[][] d,
            List<Stop> stops,
            int depot,
            double maxDuration
    ) {
        double newTravel = travel + d[current][next];
        double newService = service + stops.get(next).serviceTime;

        // must still be able to return to depot
        double travelWithReturn = newTravel + d[next][depot];
        double totalWithReturn = travelWithReturn + newService + FIXED_OVERHEAD;

        return totalWithReturn <= maxDuration + 1e-9;
    }

    /**
     * Reduced cost = (travel + service) - sum(duals[visited customers]) - duals[0].
     * Here "visited customers" are indices in route excluding depot at ends.
     */
    private double reducedCost(
            List<Integer> routeIdx,
            List<Stop> stops,
            double[][] d,
            double[] duals,
            int depot,
            double travel,
            double service
    ) {

        double rc = -duals[0];

        for (int k = 0; k < routeIdx.size() - 1; k++) {
            int i = routeIdx.get(k);
            int j = routeIdx.get(k + 1);

            if (i == depot) {
                continue;
            }

            rc += d[i][j];
            if (i < duals.length) {
                rc -= duals[i];
            }
        }
        return rc;
    }

    private String signature(List<Integer> routeIdx, int depot) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < routeIdx.size(); k++) {
            int v = routeIdx.get(k);
            if (v == depot) continue;
            sb.append(v).append("-");
        }
        return sb.toString();
    }

    private static class ScoredNode {
        final int node;
        final double score;
        ScoredNode(int node, double score) { this.node = node; this.score = score; }
    }

    private static class RouteCandidate {
        final List<Integer> routeIdx; // includes depot at start and end (indices into stops)
        final double travel;
        final double service;
        final double reducedCost;

        RouteCandidate(List<Integer> routeIdx, double travel, double service, double reducedCost) {
            this.routeIdx = routeIdx;
            this.travel = travel;
            this.service = service;
            this.reducedCost = reducedCost;
        }
    }

    private double similarity(Shift a, Shift b) {

        int[] A = a.getUniqueStops();

        int[] B = b.getUniqueStops();

    
        int i = 0;
        int j = 0;
        int intersection = 0;
    
        while (i < A.length && j < B.length) {
    
            if (A[i] == B[j]) {
                intersection++;
                i++;
                j++;
            } else if (A[i] < B[j]) {
                i++;
            } else {
                j++;
            }
        }
    
        int union = A.length + B.length - intersection;
    
        if (union == 0) return 0.0;
    
        return (double) intersection / union;
    }

    public List<Shift> filterShifts(
        List<Shift> shifts,
        int maxKeep,
        double similarityThreshold) {

    List<Shift> filtered = new ArrayList<>();

    for (Shift s : shifts) {

        boolean tooSimilar = false;

        for (Shift f : filtered) {
            if (similarity(s, f) > similarityThreshold) {
                tooSimilar = true;
                break;
            }
        }

        if (!tooSimilar) {
            filtered.add(s);
        }

        if (filtered.size() >= maxKeep)
            break;
    }

    return filtered;
}

}


