package column_generation;

import search.*;

import java.io.IOException;
import java.util.*;
import core.*;

public class PricingHeuristic {

    private double maxShiftDuration;
    private double minShiftDuration;
    private int maxShifts;
    private List<Neighborhood> neighborhoods;
    private AcceptanceFunction acceptFunction;
    private RouteCompatibility compatibility;

    public PricingHeuristic(double maxShiftDuration, double minShiftDuration, int maxShifts,
                            List<Neighborhood> neighborhoods,
                            AcceptanceFunction acceptFunction,
                            RouteCompatibility compatibility) {
        this.maxShiftDuration = maxShiftDuration;
        this.minShiftDuration = minShiftDuration;
        this.maxShifts = maxShifts;
        this.neighborhoods = neighborhoods;
        this.acceptFunction = acceptFunction;
        this.compatibility = compatibility;
    }
    private static class ShiftWithCost {
        Shift shift;
        double reducedCost;
    
        ShiftWithCost(Shift shift, double reducedCost) {
            this.shift = shift;
            this.reducedCost = reducedCost;
        }
    }    

    public List<Shift> generateShifts(HTMInstance instance, double[][] travelTimes, double[] duals, int nightShift) throws IOException {
        double[][] reducedCosts = ReducedCost.computeReducedCost(travelTimes, duals);

        List<Shift> pool = ElementaryShortestPathHeuristic.generateShiftPool(
                instance.getStops(),
                travelTimes,
                reducedCosts,
                maxShiftDuration,
                minShiftDuration,
                nightShift,
                maxShifts
        );

        for (Shift shift : pool) {
            List<Shift> tmp = new ArrayList<>();
            tmp.add(shift);
            tmp = localSearchOnSingleShift(tmp, travelTimes, neighborhoods);
            shift.route = tmp.get(0).route;
        }

        List<ShiftWithCost> candidates = new ArrayList<>();
        for (Shift s : pool) {
            double rc = ReducedCost.computeShiftReducedCost(s, reducedCosts, duals[0]);

            if (rc < 0 && s.totalTime >= minShiftDuration) {
                candidates.add(new ShiftWithCost(s, rc));
                System.out.printf(
                    "Shift size: %3d | TotalTime: %8.2f | ReducedCost: %10.5f%n",
                    s.route.size(),
                    s.totalTime,
                    rc
                );
            }
        }
        candidates.sort(Comparator.comparingDouble(sw -> sw.reducedCost));

        List<Shift> filtered = filterShiftsWithCost(candidates, 100, 0.75);

        return filtered;
    }

    private List<Shift> filterShiftsWithCost(List<ShiftWithCost> shifts,
        int maxKeep,
        double similarityThreshold) {
            
            List<Shift> filtered = new ArrayList<>();

            for (ShiftWithCost sw : shifts) {
                boolean tooSimilar = false;

                for (Shift f : filtered) {
                if (similarity(sw.shift, f) > similarityThreshold) {
                    tooSimilar = true;
                    break;
                }
            }

            if (!tooSimilar) {
            filtered.add(sw.shift);

            System.out.printf(
            "Shift size: %3d | TotalTime: %8.2f | ReducedCost: %10.5f%n",
            sw.shift.route.size(),
            sw.shift.totalTime,
            sw.reducedCost
            );
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
        String instancePath = "src/core/data_all.txt";
        HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");

        LocalSearch ls = new LocalSearch(
                neighborhoods,
                acceptFunction,
                compatibility,
                ImprovementChoice.BEST,
                100,
                (int) maxShiftDuration
        );
        return ls.run(shifts, instance, travelTimes);
    }
}
