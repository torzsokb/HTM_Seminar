package solve;

import core.*;
import neighborhoods.*;
import search.*;
import java.util.*;

public class GridSearchSA {

    static final double shiftLength = 7*60;
    static final double totalShiftLength = 8*60;

    public static void main(String[] args) throws Exception {
        String instancePath = "src/core/data_all_feas_typeHalte.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);
       
        List<Neighborhood> neighborhoods = Arrays.asList(
            new Intra2Opt(),
            new Inter2OptStar(),
            new IntraSwap(),
            new IntraShift(),
            new InterSwap(),
            new InterShift()
        );

        RouteCompatibility compatibility = Compatibility.sameNightShift();

        List<Shift> currentBest = Utils.readShiftsFromCSV("src/results/results_SA_gridsearch_best_Newv2_feasible.csv", travelTimes);
    
        ObjectiveFunction objectiveBasic = Objective.totalLength();

        double initial_obj_value = objectiveBasic.shifts(currentBest);
        // for (Shift shift : currentBest) {
        //     System.out.println(Utils.formatRoute(instance, shift.route));
        // }
        double bestValue = Utils.totalObjective(currentBest);
        System.out.println("Best value: " + bestValue);


        // Utils.checkFeasibility(currentBest, instance, 60*8);


        // Grid of SA parameters
        double[] temperatures = {50.0, 75.0, 100.0, 125.0, 150.0, 175.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0};
        double[] coolingRates  = {0.90, 0.91, 0.92, 0.93, 0.94, 0.95, 0.96, 0.97, 0.98, 0.99};      
        ImprovementChoice[] improvementChoices = {ImprovementChoice.BEST, ImprovementChoice.FIRST};

        double bestTemp = 0;
        double bestRate = 0;
        ImprovementChoice bestChoice = ImprovementChoice.FIRST;
        List<Shift> bestSolution = null;

        // Intensifying
        boolean improvementFound = true;
        long startingTime = System.currentTimeMillis();

        while (improvementFound) {

            improvementFound = false;

            for (double temp : temperatures) {
                for (double rate : coolingRates) {
                    for (ImprovementChoice choice : improvementChoices) {
                        currentBest = Utils.readShiftsFromCSV("src/results/results_SA_gridsearch_best_Newv2_feasible.csv", travelTimes);
                        // System.out.println("\nRunning SA with Temp=" + temp + ", Rate=" + rate + ", Choice=" + choice);

                        Acceptance.initSimulatedAnnealing(0.5, 0, 1000, 50);
                        AcceptanceFunction acceptSA = Acceptance.simulatedAnnealing();

                        LocalSearch ls_SA = new LocalSearch(
                                neighborhoods,
                                acceptSA,
                                compatibility,
                                choice,
                                400,
                                totalShiftLength,
                                objectiveBasic,
                                true
                        );

                        List<Shift> improved_SA = ls_SA.run(currentBest, instance, travelTimes);

                        Utils.recomputeAllShifts(improved_SA, instance, travelTimes);

                        double objValue = objectiveBasic.shifts(improved_SA)/60.0;

                        // System.out.println("SA objective value: " + objValue);

                        if (objValue < bestValue) {

                            bestValue = objValue;
                            bestTemp = temp;
                            bestRate = rate;
                            bestChoice = choice;
                            bestSolution = improved_SA;
                            currentBest = improved_SA;

                            long totalTimeTaken = System.currentTimeMillis() - startingTime;
                            System.out.println("Best new objective value: " + objValue);
                            System.out.println("Best improvement: " + (initial_obj_value - objValue));
                            System.out.println("Time taken: " + totalTimeTaken/1000.0 + " (s)");
                            Utils.resultsToCSV(bestSolution, instance, "src/results/results_SA_gridsearch_best_Newv2_feasible.csv");

                            improvementFound = true;

                            // break all loops immediately
                            break;
                        }
                    }
                    if (improvementFound) break;
                }
                if (improvementFound) break;
            }
            
        }
        

        System.out.println("\n=== Grid Search Complete ===");
        System.out.println("Best SA objective value: " + bestValue);
        System.out.println("Best parameters: Temp=" + bestTemp + ", Rate=" + bestRate + ", Choice=" + bestChoice);

        Utils.checkFeasibility(bestSolution, instance, totalShiftLength);
        Utils.printShiftStatistics(bestSolution, instance, totalShiftLength);
        Utils.resultsToCSV(bestSolution, instance, "src/results/results_SA_gridsearch_best_Newv2_feasible.csv");

        // for (Shift shift : bestSolution) {
        //     System.out.println(Utils.formatRoute(instance, shift.route));
        // }
    }
}
