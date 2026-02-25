package solve; 
import core.*; 
import neighborhoods.*; 
import search.*;
import java.util.*;


public class SolveBalancedSA {
    static final double shiftLength = 7*60;
    static final double totalShiftLength = 8*60;

    public static void main(String[] args) throws Exception {
        String instancePath = "src/core/data_all.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        long startTotalTime = System.currentTimeMillis();

        long startGreedy = System.currentTimeMillis();

        List<Integer> nightIdx = Utils.getAllowedIndices(instance, 1);
        List<Integer> dayIdx   = Utils.getAllowedIndices(instance, 0);

        List<Shift> nightShifts = Utils.buildGreedyShifts(instance, travelTimes, nightIdx, 1, shiftLength);
        List<Shift> dayShifts   = Utils.buildGreedyShifts(instance, travelTimes, dayIdx, 0, shiftLength);

        List<Shift> initial = new ArrayList<>();
        initial.addAll(nightShifts);
        initial.addAll(dayShifts);

        //ObjectiveFunction objectiveBalanced = Objective.balancedObj(0.05, 0.05);
        ObjectiveFunction objectiveBasic = Objective.totalLength();

        double initial_obj_value = objectiveBasic.shifts(initial)/60.0;

        long endGreedy = System.currentTimeMillis();

        double totalTimeGreedy = (endGreedy - startGreedy) / 1000.0;
        System.out.println("Initial solution built:");
        System.out.println("Night shifts: " + nightShifts.size());
        System.out.println("Day shifts:   " + dayShifts.size());
        System.out.println("Total shifts: " + initial.size());
        System.out.println("Total objective value: " + initial_obj_value);
        System.out.println("Time taken: " + totalTimeGreedy + " s.");

        //Utils.resultsToCSV(initial, instance, "src/results/results_Greedy_abri.csv");


        // NORMAL LOCAL SEARCH 
        List<Neighborhood> neighborhoods = Arrays.asList(
            new Inter2OptStar(),
            new InterShift(),
            new IntraShift(),
            new Intra2Opt(),
            new IntraSwap(),
            new InterSwap()
        );

        AcceptanceFunction acceptGreedy = Acceptance.greedy();

        RouteCompatibility compatibility = Compatibility.sameNightShift();

        // BALANCED LOCAL SEARCH 

        ObjectiveFunction objectiveBalanced = Objective.balancedObj(0.02, 0.02);
        LocalSearch ls_balanced = new LocalSearch(
                neighborhoods,
                acceptGreedy,
                compatibility,
                ImprovementChoice.FIRST,
                1000,       
                totalShiftLength,
                objectiveBalanced
        );
        long startBLS = System.currentTimeMillis();
        System.out.println("\nRunning balanced local search...");
        List<Shift> improved_balanced = ls_balanced.run(initial, instance, travelTimes);

        Utils.recomputeAllShifts(improved_balanced, instance, travelTimes);

        double new_obj_value_balanced = objectiveBasic.shifts(improved_balanced)/60.0;

        System.out.println("\nBalanced local search complete.");

        System.out.println("New objective value: " + new_obj_value_balanced);

        double balanced_improvement = initial_obj_value - new_obj_value_balanced;

        System.out.println("Improvement: " + balanced_improvement);
        long endBLS = System.currentTimeMillis();
        double timeTakenBLS = (endBLS-startBLS)/1000.0;
        System.out.println("Time taken: " + (timeTakenBLS) + " s" );

        Utils.checkFeasibility(improved_balanced, instance, totalShiftLength);
        Utils.printShiftStatistics(improved_balanced, instance, totalShiftLength);

    
        double totalTimeTakenBLS = totalTimeGreedy + timeTakenBLS;
        System.out.println("Total time taken: " + totalTimeTakenBLS + " s.");

        Utils.resultsToCSV(improved_balanced, instance, "src/results/results_BalancedSA_gridsearch.csv");


        // Now do the grid search SA with basic objective 
        List<Shift> currentBest = improved_balanced;
    
        // for (Shift shift : currentBest) {
        //     System.out.println(Utils.formatRoute(instance, shift.route));
        // }
        double bestValue = Utils.totalObjective(currentBest);
        System.out.println("Best value: " + bestValue);
        System.out.println("Best improvement: " + (initial_obj_value-bestValue));

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
        double maxValue = 386.9;

        while (improvementFound && bestValue >= maxValue) {

            improvementFound = false;

            for (double temp : temperatures) {
                for (double rate : coolingRates) {
                    for (ImprovementChoice choice : improvementChoices) {
                        currentBest = Utils.readShiftsFromCSV("src/results/results_BalancedSA_gridsearch.csv", travelTimes);
                        // System.out.println("\nRunning SA with Temp=" + temp + ", Rate=" + rate + ", Choice=" + choice);

                        Acceptance.initSimulatedAnnealing(temp, rate);
                        AcceptanceFunction acceptSA = Acceptance.simulatedAnnealing();

                        LocalSearch ls_SA = new LocalSearch(
                                neighborhoods,
                                acceptSA,
                                compatibility,
                                choice,
                                400,
                                totalShiftLength,
                                objectiveBasic
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

                            System.out.println("Best new objective value: " + objValue);
                            System.out.println("Best improvement: " + (initial_obj_value - objValue));
                            Utils.resultsToCSV(bestSolution, instance, "src/results/results_BalancedSA_gridsearch.csv");

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
        Utils.resultsToCSV(bestSolution, instance, "src/results/results_BalancedSA_gridsearch.csv");
    }
}
