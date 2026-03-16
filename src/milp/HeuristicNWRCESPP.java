package milp;

import core.Stop;
import core.Shift;

import java.util.*;
import java.util.concurrent.*;

public class HeuristicNWRCESPP implements RCESPP {

    private final int numPartitions;
    private final int maxSolutionsPerPartition;
    private final int maxQueueSize;

    public HeuristicNWRCESPP(int numPartitions,
                             int maxSolutionsPerPartition,
                             int maxQueueSize) {
        this.numPartitions = Math.max(1, numPartitions);
        this.maxSolutionsPerPartition = Math.max(1, maxSolutionsPerPartition);
        this.maxQueueSize = Math.max(1000, maxQueueSize);
    }

    private static class Label {
        int node;
        double gCost;      // accumulated reduced cost
        double resource;   // accumulated duration (travel + service)
        double f;          // g + h_c (here h_c = 0)
        Label prev;

        Label(int node, double gCost, double resource, double f, Label prev) {
            this.node = node;
            this.gCost = gCost;
            this.resource = resource;
            this.f = f;
            this.prev = prev;
        }
    }

    @Override
    public List<Shift> getNewShifts(double[][] distances,
                                    List<Stop> stops,
                                    double[] duals,
                                    double maxDuration,
                                    double minDuration) {

        int n = stops.size();
        if (n <= 1) return Collections.emptyList();

        int depot = 0;
        List<List<Integer>> partitions = buildPartitions(n, depot);

        int threads = Math.min(numPartitions, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<List<Shift>>> futures = new ArrayList<>();

        for (List<Integer> part : partitions) {
            futures.add(executor.submit(() ->
                    runNWRCSearchOnPartition(distances, stops, duals,
                                             depot, part, maxDuration, minDuration,
                                             maxSolutionsPerPartition)));
        }

        executor.shutdown();

        List<Shift> all = new ArrayList<>();
        for (Future<List<Shift>> f : futures) {
            try {
                all.addAll(f.get());
                if (all.size() >= 10) break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (all.size() > 10) {
            all = all.subList(0, 10);
        }
        return all;
    }

    private List<List<Integer>> buildPartitions(int n, int depot) {
        List<Integer> nonDepot = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i != depot) nonDepot.add(i);
        }

        List<List<Integer>> partitions = new ArrayList<>();
        int blockSize = Math.max(1, nonDepot.size() / numPartitions);

        for (int i = 0; i < nonDepot.size(); i += blockSize) {
            partitions.add(new ArrayList<>(nonDepot.subList(i, Math.min(nonDepot.size(), i + blockSize))));
        }

        return partitions;
    }

    private List<Shift> runNWRCSearchOnPartition(double[][] distances,
                                                 List<Stop> stops,
                                                 double[] duals,
                                                 int depot,
                                                 List<Integer> partition,
                                                 double maxDuration,
                                                 double minDuration,
                                                 int maxSolutions) {

        int n = stops.size();

        // X(v): list of resource values of non-dominated labels at v
        List<List<Double>> X = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            X.add(new ArrayList<>());
        }

        PriorityQueue<Label> Q = new PriorityQueue<>(Comparator.comparingDouble(l -> l.f));
        double fBar = Double.POSITIVE_INFINITY;

        // initial label at depot
        Label start = new Label(depot, -duals[depot], 0.0, -duals[depot], null);
        Q.add(start);

        List<Shift> solutions = new ArrayList<>();

        while (!Q.isEmpty()) {
            if (Q.size() > maxQueueSize) break;

            Label l = Q.poll();
            if (l.f > fBar) break;

            int v = l.node;

            // lazy dominance check
            if (isDominatedByResources(l.resource, X.get(v))) {
                continue;
            }

            // remove dominated labels in X(v)
            removeDominatedResources(l.resource, X.get(v));
            X.get(v).add(l.resource);

            // capture solution: depot reached with non-trivial path
            if (v == depot && l.prev != null) {
                double totalNoBreak = l.resource;
                if (totalNoBreak >= minDuration && totalNoBreak <= maxDuration && l.gCost < -1e-6) {
                    List<Integer> route = reconstructRoute(l, depot);
                    Shift shift = buildShiftFromRoute(route, distances, stops);
                    solutions.add(shift);
                    fBar = Math.min(fBar, l.f);
                    if (solutions.size() >= maxSolutions) break;
                    continue;
                }
            }

            // expand
            for (int u : partition) {
                if (u == v) continue;

                double travel = distances[v][u];
                double service = (u == depot ? 0.0 : stops.get(u).serviceTime);
                double newRes = l.resource + travel + service;
                if (newRes > maxDuration) continue;

                double newG = l.gCost + travel - duals[u];
                double newF = newG; // h_c(u) = 0

                Label child = new Label(u, newG, newRes, newF, l);
                Q.add(child);
            }

            // also allow returning to depot
            if (v != depot) {
                double travel = distances[v][depot];
                double newRes = l.resource + travel;
                if (newRes <= maxDuration) {
                    double newG = l.gCost + travel - duals[depot];
                    double newF = newG;
                    Label childDepot = new Label(depot, newG, newRes, newF, l);
                    Q.add(childDepot);
                }
            }
        }

        return solutions;
    }

    private boolean isDominatedByResources(double r, List<Double> list) {
        for (double existing : list) {
            if (existing <= r) return true;
        }
        return false;
    }

    private void removeDominatedResources(double r, List<Double> list) {
        Iterator<Double> it = list.iterator();
        while (it.hasNext()) {
            double existing = it.next();
            if (r <= existing) {
                it.remove();
            }
        }
    }

    private List<Integer> reconstructRoute(Label end, int depot) {
        LinkedList<Integer> route = new LinkedList<>();
        Label cur = end;
        while (cur != null) {
            route.addFirst(cur.node);
            cur = cur.prev;
        }
        if (route.getFirst() != depot) {
            route.addFirst(depot);
        }
        if (route.getLast() != depot) {
            route.addLast(depot);
        }
        return route;
    }

    private Shift buildShiftFromRoute(List<Integer> route,
                                      double[][] distances,
                                      List<Stop> stops) {
        double travel = 0.0;
        double service = 0.0;

        for (int i = 0; i < route.size() - 1; i++) {
            int from = route.get(i);
            int to = route.get(i + 1);
            travel += distances[from][to];
        }

        for (int i = 1; i < route.size() - 1; i++) {
            int idx = route.get(i);
            service += stops.get(idx).serviceTime;
        }

        int nightShift = 0;
        for (int i = 1; i < route.size() - 1; i++) {
            int idx = route.get(i);
            if (idx != 0) {
                nightShift = stops.get(idx).nightShift;
                break;
            }
        }

        return new Shift(route, travel, service, nightShift);
    }
}
