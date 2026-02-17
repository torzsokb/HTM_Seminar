package solve;

import core.*;
import neighborhoods.*;
import search.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FindingBestOrderLocalSearch {
    static final double shiftLength = 7*60;
    static final double totalShiftLength = 8*60;

    public static void main(String[] args) throws Exception {
        String instancePath = "src/core/data_all.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        List<Integer> nightIdx = Utils.getAllowedIndices(instance, 1);
        List<Integer> dayIdx   = Utils.getAllowedIndices(instance, 0);

        double shiftLength = 7 * 60; 

        List<Shift> nightShifts = Utils.buildGreedyShifts(instance, travelTimes, nightIdx, 1, shiftLength);
        List<Shift> dayShifts   = Utils.buildGreedyShifts(instance, travelTimes, dayIdx, 0, shiftLength);

        List<Shift> initial = new ArrayList<>();
        initial.addAll(nightShifts);
        initial.addAll(dayShifts);

        double initialObj = Utils.totalObjective(initial);
        System.out.println("Initial solution built:");
        System.out.println("Night shifts: " + nightShifts.size());
        System.out.println("Day shifts:   " + dayShifts.size());
        System.out.println("Total shifts: " + initial.size());
        System.out.println("Initial objective: " + initialObj);

        // ----------------------------
        // Neighborhoods to permute
        // ----------------------------
        List<Neighborhood> neighborhoods = Arrays.asList(
            new IntraSwap(),
            new IntraShift(),
            new InterShift(),
            new InterSwap(),
            new Intra2Opt(),
            new Inter2OptStar()
        );

        RouteCompatibility compatibility = Compatibility.sameNightShift();

        ImprovementChoice[] choices = {ImprovementChoice.FIRST, ImprovementChoice.BEST};

        AcceptanceFunction acceptGreedy = Acceptance.greedy();

        List<List<Neighborhood>> allOrders = new ArrayList<>();
        generatePermutations(neighborhoods, 0, allOrders);

    
        double bestObj = Double.MAX_VALUE;
        List<Shift> bestSolution = null;
        List<Neighborhood> bestOrder = null;
        ImprovementChoice bestChoice = null;

        int runCount = 0;

        for (List<Neighborhood> order : allOrders) {
            for (ImprovementChoice choice : choices) {
                runCount++;

                // Copy initial solution
                List<Shift> initialCopy = Utils.deepCopyShifts(initial);

                LocalSearch ls = new LocalSearch(
                        order,
                        acceptGreedy,
                        compatibility,
                        choice,
                        1000,           // max iterations
                        totalShiftLength
                );

                List<Shift> result = ls.run(initialCopy, instance, travelTimes);
                Utils.recomputeAllShifts(result, instance, travelTimes);
                double obj = Utils.totalObjective(result) + 50.0;

                if (obj < bestObj) {
                    bestObj = obj;
                    bestSolution = result;
                    bestOrder = new ArrayList<>(order);
                    bestChoice = choice;
                    System.out.printf("New best found! Obj = %.6f | Choice = %s | Run %d%n", bestObj, bestChoice, runCount);
                    double bestImprovement = initialObj - bestObj;
                    System.out.println("Best improvement: " + bestImprovement);
                }
            }
        }

        System.out.println("\n=== BEST CONFIGURATION ===");
        System.out.println("Best objective: " + bestObj);
        System.out.println("Best improvement choice: " + bestChoice);
        System.out.println("Best neighborhood order:");
        for (Neighborhood n : bestOrder) {
            System.out.println(" - " + n.getClass().getSimpleName());
        }

        Utils.printShiftStatistics(bestSolution, instance, shiftLength);
        Utils.checkFeasibility(bestSolution, instance, shiftLength);
    }

    public static void generatePermutations(List<Neighborhood> arr, int k, List<List<Neighborhood>> result) {
        if (k == arr.size()) {
            result.add(new ArrayList<>(arr));
        } else {
            for (int i = k; i < arr.size(); i++) {
                Collections.swap(arr, i, k);
                generatePermutations(arr, k + 1, result);
                Collections.swap(arr, i, k);
            }
        }
    }
}
