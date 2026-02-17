package column_generation;

import java.util.*;
import core.*;

/**
 * Generates elementary feasible shifts greedily.
 */
public class ElementaryShortestPathHeuristic {

    public static List<Shift> generateShiftPool(List<Stop> stops, double[][] travelTimes,
                                                double[][] reducedCosts,
                                                double maxShiftDuration,
                                                double minShiftDuration,
                                                int nightShift,
                                                int maxShifts) {
        List<Shift> shiftPool = new ArrayList<>();
        int n = stops.size();

        // Randomize start nodes (skip depot)
        List<Integer> startNodes = new ArrayList<>();
        for (int i = 1; i < n; i++) startNodes.add(i);
        Collections.shuffle(startNodes);

        for (int startNode : startNodes) {
            Shift s = generateShiftFromNode(stops, travelTimes, reducedCosts,
                                            maxShiftDuration, minShiftDuration, nightShift, startNode);
            if (s != null) {
                shiftPool.add(s);
                if (shiftPool.size() >= maxShifts) break;
            }
        }

        return shiftPool;
    }

    private static Shift generateShiftFromNode(
        List<Stop> stops,
        double[][] travelTimes,
        double[][] reducedCosts,
        double maxShiftDuration,
        double minShiftDuration,
        int nightShift,
        int startNode) {
            
            int n = stops.size();
            boolean[] visited = new boolean[n];
            List<Integer> route = new ArrayList<>();

            final double FIXED_TIME = 30 + 30; // break + prep (if required)

            int depot = 0;

            route.add(depot);
            visited[depot] = true;

            double travelTime = travelTimes[depot][startNode];
            double serviceTime = stops.get(startNode).serviceTime;


            route.add(startNode);
            visited[startNode] = true;

            int current = startNode;

            while (true) {

                int nextNode = -1;
                double bestCost = Double.POSITIVE_INFINITY;

                for (int j = 1; j < n; j++) {

                    if (visited[j]) continue;

                    double newTravel = travelTime + travelTimes[current][j];
                    double newService = serviceTime + stops.get(j).serviceTime;

                    double newTotalTime = FIXED_TIME
                            + newTravel
                            + newService
                            + travelTimes[j][depot];

                    if (newTotalTime <= maxShiftDuration
                            && reducedCosts[current][j] < bestCost) {

                        bestCost = reducedCosts[current][j];
                        nextNode = j;
                    }
                }

                if (nextNode == -1)
                    break;

                travelTime += travelTimes[current][nextNode];
                serviceTime += stops.get(nextNode).serviceTime;

                route.add(nextNode);
                visited[nextNode] = true;

                current = nextNode;
            }

            travelTime += travelTimes[current][depot];
            route.add(depot);

            double totalTime = FIXED_TIME + travelTime + serviceTime;

            if (totalTime < minShiftDuration)
                return null;

            Shift shift = new Shift(route, travelTime, serviceTime, nightShift);
            shift.totalTime = totalTime;

            return shift;
        }

}
