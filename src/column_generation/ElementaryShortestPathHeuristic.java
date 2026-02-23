package column_generation;

import java.io.IOException;
import java.util.*;
import core.*;

/**
 * Generates elementary feasible shifts greedily.
 */
public class ElementaryShortestPathHeuristic {
    public static List<Shift> generateShiftPool(List<Stop> stops,
        double[][] travelTimes,
        double[][] reducedCosts,
        double maxShiftDuration,
        double minShiftDuration,
        int maxShifts) throws IOException {

        int n = stops.size();

        List<Integer> startNodes = new ArrayList<>();
        for (int i = 1; i < n; i++) startNodes.add(i);

        Random rnd = new Random(10);
        Collections.shuffle(startNodes, rnd);

        int repetitionsPerStart = 3;   
        double alpha = 0.3;            

        List<Shift> shiftPool = Collections.synchronizedList(new ArrayList<>());

        startNodes.parallelStream().forEach(startNode -> {

        Random localRnd = new Random(startNode);

        for (int r = 0; r < repetitionsPerStart; r++) {

        if (shiftPool.size() >= maxShifts) return;

        try {
        Shift s = generateShiftFromNode(
        stops,
        travelTimes,
        reducedCosts,
        maxShiftDuration,
        minShiftDuration,
        startNode,
        alpha,
        localRnd);

        if (s != null) {
        shiftPool.add(s);
        }

        } catch (IOException e) {
        throw new RuntimeException(e);
        }
        }
        });

        return shiftPool;
        }


    private static Shift generateShiftFromNode(
        List<Stop> stops,
        double[][] travelTimes,
        double[][] reducedCosts,
        double maxShiftDuration,
        double minShiftDuration,
        int startNode,
        double alpha,
        Random rnd) throws IOException {
            int n = stops.size();
            boolean[] visited = new boolean[n];
            List<Integer> route = new ArrayList<>();

            final double FIXED_TIME = 30 + 30; // break + prep (if necessary)

            int depot = 0;

            route.add(depot);
            visited[depot] = true;

            double travelTime = travelTimes[depot][startNode];
            double serviceTime = stops.get(startNode).serviceTime;


            route.add(startNode);
            visited[startNode] = true;

            int current = startNode;

            while (true) {

                List<Integer> feasible = new ArrayList<>();
                double bestCost = 1;
            
                for (int j = 1; j < n; j++) {
            
                    if (visited[j]) continue;
            
                    double newTravel = travelTime + travelTimes[current][j];
                    double newService = serviceTime + stops.get(j).serviceTime;
            
                    double newTotalTime = FIXED_TIME
                            + newTravel
                            + newService
                            + travelTimes[j][depot];
            
                    if (newTotalTime <= maxShiftDuration) {
            
                        double rc = reducedCosts[current][j];
            
                        if (rc < bestCost) {
                            bestCost = rc;
                        }
            
                        feasible.add(j);
                    }
                }
            
                if (feasible.isEmpty())
                    break;
        
                double threshold = bestCost + alpha * Math.abs(bestCost);
            
                List<Integer> rcl = new ArrayList<>();
                for (int j : feasible) {
                    if (reducedCosts[current][j] <= threshold) {
                        rcl.add(j);
                    }
                }
            
                if (rcl.isEmpty())
                    break;
            
                int nextNode = rcl.get(rnd.nextInt(rcl.size()));
            
                travelTime += travelTimes[current][nextNode];
                serviceTime += stops.get(nextNode).serviceTime;
            
                route.add(nextNode);
                visited[nextNode] = true;
            
                current = nextNode;
            }
            
            travelTime += travelTimes[current][depot];
            route.add(depot);

            double totalTime = FIXED_TIME + travelTime + serviceTime;

            String instancePath = "src/core/data_all.txt";

            HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");
            int nightShift = (Utils.containsNightStop(route, instance)) ? 1 : 0;

            if (totalTime < minShiftDuration)
                return null;

            Shift shift = new Shift(route, travelTime, serviceTime, nightShift);
            shift.totalTime = totalTime;

            return shift;
        }

}
