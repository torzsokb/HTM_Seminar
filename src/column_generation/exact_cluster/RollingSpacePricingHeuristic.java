package column_generation.exact_cluster;

import core.*;
import search.*;

import java.io.IOException;
import java.util.*;

import column_generation.ReducedCost;

public class RollingSpacePricingHeuristic {

    private final double maxShiftDuration;
    private final double minShiftDuration;
    private final int maxShifts;
    private final List<Neighborhood> neighborhoods;
    private final AcceptanceFunction acceptFunction;
    private final RouteCompatibility compatibility;
    private final HTMInstance instance;

    public RollingSpacePricingHeuristic(double maxShiftDuration,
                                        double minShiftDuration,
                                        int maxShifts,
                                        List<Neighborhood> neighborhoods,
                                        AcceptanceFunction acceptFunction,
                                        RouteCompatibility compatibility,
                                        HTMInstance instance) {

        this.maxShiftDuration = maxShiftDuration;
        this.minShiftDuration = minShiftDuration;
        this.maxShifts = maxShifts;
        this.neighborhoods = neighborhoods;
        this.acceptFunction = acceptFunction;
        this.compatibility = compatibility;
        this.instance = instance;
    }
    private static class ShiftWithCost {
        Shift shift;
        double reducedCost;
    
        ShiftWithCost(Shift shift, double reducedCost) {
            this.shift = shift;
            this.reducedCost = reducedCost;
        }
    }   

    public List<Shift> generateShifts(List<Stop> stops,
                                      double[][] travelTimes,
                                      double[] duals) throws IOException {

        double[][] reducedCosts = ReducedCost.computeReducedCost(travelTimes, duals);

        // Build rolling clusters
        List<List<Stop>> clusters =
                RollingClusters.build(stops, 40, 20);

        List<Shift> allCandidates = new ArrayList<>();
        Map<Integer, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < stops.size(); i++) {
            idToIndex.put(stops.get(i).objectId, i);
        }

        for (List<Stop> cluster : clusters) {

            ClusterInstance ci = ClusterBuilder.buildClusterInstance(
                    cluster, stops, travelTimes, reducedCosts, idToIndex);

            // Run your existing pricing heuristic on the cluster
            List<Shift> pool = LabelingPricing.generateShiftPool(
                ci.stops,
                ci.travelTimes,
                ci.reducedCosts,
                maxShiftDuration,
                minShiftDuration,
                maxShifts
        );
        

            pool.parallelStream().forEach(shift -> {
                try {
                    List<Shift> tmp = List.of(shift);
                    List<Shift> improved =
                            localSearchOnSingleShift(tmp, ci.travelTimes, neighborhoods);

                    shift.route = RouteMapper.mapBackRoute(
                            improved.get(0).route, ci.localToGlobal);

                    shift.recomputeTotalTime();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Compute global reduced cost
            for (Shift s : pool) {
                double rc = ReducedCost.computeShiftReducedCost(s, reducedCosts, duals[0]);
                if (rc < -1e-4 && s.totalTime >= minShiftDuration) {
                    allCandidates.add(s);
                }
            }
        }

        // Global filtering
        List<ShiftWithCost> withCost = new ArrayList<>();
        for (Shift s : allCandidates) {
            double rc = ReducedCost.computeShiftReducedCost(s, reducedCosts, duals[0]);
            withCost.add(new ShiftWithCost(s, rc));
        }

        withCost.sort(Comparator.comparingDouble(sw -> sw.reducedCost));

        return filterShiftsWithCost(withCost, 150, 1.0);
    }

    private List<Shift> filterShiftsWithCost(List<ShiftWithCost> withCost,
        int maxKeep,
        double similarityThreshold) {
            
            List<Shift> filtered = new ArrayList<>();

            for (ShiftWithCost sw : withCost) {
                boolean tooSimilar = false;

                for (Shift f : filtered) {
                if (similarity(sw.shift, f) > similarityThreshold) {
                    tooSimilar = true;
                    break;
                }
            }

            if (!tooSimilar) {
            filtered.add(sw.shift);

            // System.out.printf(
            // "Shift size: %3d | TotalTime: %8.2f | ReducedCost: %10.5f%n",
            // sw.shift.route.size(),
            // sw.shift.totalTime,
            // sw.reducedCost
            // );
        }
        if (filtered.size() >= maxKeep)
            break;
         }

            return filtered;
        }
        
        private double similarity(Shift a, Shift b) {
            List<Integer> A = getSortedInternalNodes(a);
            List<Integer> B = getSortedInternalNodes(b);
        
            int i = 0;
            int j = 0;
            int intersection = 0;
        
            while (i < A.size() && j < B.size()) {
        
                int x = A.get(i);
                int y = B.get(j);
        
                if (x == y) {
                    intersection++;
                    i++;
                    j++;
                } else if (x < y) {
                    i++;
                } else {
                    j++;
                }
            }
        
            int union = A.size() + B.size() - intersection;
        
            if (union == 0) return 0.0;
        
            return (double) intersection / union;
        }
        private List<Integer> getSortedInternalNodes(Shift s) {

            List<Integer> nodes = new ArrayList<>();
        
            for (int i = 1; i < s.route.size() - 1; i++) {
                nodes.add(s.route.get(i));
            }
        
            Collections.sort(nodes);
            return nodes;
        }
    
    private List<Shift> localSearchOnSingleShift(List<Shift> shifts, double[][] travelTimes, List<Neighborhood> neighborhoods) throws IOException {
        ObjectiveFunction objectiveFunction = Objective.totalLength();
        LocalSearch ls = new LocalSearch(
                neighborhoods,
                acceptFunction,
                compatibility,
                ImprovementChoice.BEST,
                1000,
                (int) maxShiftDuration -60,
                objectiveFunction
        );
        return ls.run(shifts, instance, travelTimes);
    }
}
