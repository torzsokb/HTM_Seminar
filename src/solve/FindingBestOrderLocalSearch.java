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
        String instancePath = "src/core/data_all_feas_typeHalte.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";
        
        String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
        String travelDayPath = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
        double[][] travelTimesDay = Utils.readTravelTimes(travelDayPath);

        //ObjectiveFunction objectiveBalanced = Objective.balancedObj(0.05, 0.05);
        ObjectiveFunction objectiveBasic = Objective.totalLength();

        System.out.println("Get initial solution: ");

        // Choose initial shifts to use 
        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes("src/results/HTM_data_initRes_typeHalte.csv", travelTimesNight, travelTimesDay);

        // Make sure they are feasible 
        Utils.makeFeasible(initial, instance, travelTimesNight, travelTimesDay);

        double initial_obj_value = objectiveBasic.shifts(initial)/60.0;

        System.out.println("Initial solution:");
        System.out.println("Total shifts: " + initial.size());
        System.out.println("Total objective value: " + initial_obj_value);

        Utils.checkFeasibility(initial, instance, totalShiftLength);

        double initialObj = objectiveBasic.shifts(initial)/60.0;

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

    
        double bestObj = initialObj;
        List<Shift> bestSolution = null;
        List<Neighborhood> bestOrder = null;
        ImprovementChoice bestChoice = null;

        int runCount = 0;

        for (List<Neighborhood> order : allOrders) {
            for (ImprovementChoice choice : choices) {
                runCount++;

                // Copy initial solution
                List<Shift> initialCopy = Utils.deepCopyShifts(initial);

                ObjectiveFunction objectiveFunction = Objective.totalLength();

                LocalSearch ls = new LocalSearch(
                        order,
                        acceptGreedy,
                        compatibility,
                        choice,
                        1000,           // max iterations
                        totalShiftLength,
                        objectiveFunction,
                        false
                );

                List<Shift> result = ls.runDiffTimes(initialCopy, instance, travelTimesNight, travelTimesDay);
                Utils.recomputeAllShiftsDiffTimes(result, instance, travelTimesNight, travelTimesDay);

                double obj = objectiveBasic.shifts(result)/60.0;

                if (obj < bestObj) {
                    bestObj = obj;
                    bestSolution = result;
                    bestOrder = new ArrayList<>(order);
                    bestChoice = choice;
                    System.out.printf("New best found! Obj = %.6f | Choice = %s | Run %d%n", bestObj, bestChoice, runCount);
                    double bestImprovement = initialObj - bestObj;
                    System.out.println("Best improvement: " + bestImprovement);
                    System.out.println("Best neighborhood order:");
                    for (Neighborhood n : bestOrder) {
                        System.out.println(" - " + n.getClass().getSimpleName());
                    }
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

        Utils.printShiftStatistics(bestSolution, instance, totalShiftLength);
        Utils.checkFeasibility(bestSolution, instance, totalShiftLength);
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
