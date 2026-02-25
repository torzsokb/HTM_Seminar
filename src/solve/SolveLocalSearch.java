package solve; 
import core.*; 
import neighborhoods.*; 
import search.*;
import java.util.*;


public class SolveLocalSearch {
    //xx
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

        LocalSearch ls = new LocalSearch(
                neighborhoods,
                acceptGreedy,
                compatibility,
                ImprovementChoice.FIRST,
                1000,       
                totalShiftLength,
                objectiveBasic
        );
        long startTime = System.currentTimeMillis();
        System.out.println("\nRunning local search...");
        List<Shift> improved = ls.run(initial, instance, travelTimes);

        Utils.recomputeAllShifts(improved, instance, travelTimes);

        double new_obj_value = objectiveBasic.shifts(improved)/60.0;

        System.out.println("\nLocal search complete.");

        System.out.println("New objective value: " + new_obj_value);

        double improvement = initial_obj_value - new_obj_value;

        System.out.println("Improvement: " + improvement);
        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime-startTime)/1000.0;
        System.out.println("Time taken: " + (timeTaken) + " s" );

        Utils.checkFeasibility(improved, instance, totalShiftLength);
        Utils.printShiftStatistics(improved, instance, totalShiftLength);

        double totalTimeTaken = totalTimeGreedy + timeTaken;
        System.out.println("Total time taken: " + totalTimeTaken + " s.");
    
        //Utils.resultsToCSV(improved, instance, "src/results/results_LS_abri.csv");

        // Sanity check
        /*
        for (Shift shift : improved) {
             System.out.println(Utils.formatRoute(instance, shift.route));
        }
         */


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

    
        //Utils.resultsToCSV(improved_balanced, instance, "src/results/results_BalancedLS_abri.csv");

        // Sanity check
        /*
        for (Shift shift : improved) {
             System.out.println(Utils.formatRoute(instance, shift.route));
        }
         */
        


        // Acceptance.initSimulatedAnnealing(100.0, 0.98);
        // AcceptanceFunction acceptSA = Acceptance.simulatedAnnealing();

        // LocalSearch ls_SA = new LocalSearch(
        //     neighborhoods,
        //     acceptSA,
        //     compatibility,
        //     ImprovementChoice.FIRST,
        //     1000,       
        //     totalShiftLength,
        //         objectiveBasic
        // );
        // List<Shift> improved_SA = ls_SA.run(improved, instance, travelTimes);
        
        // Utils.recomputeAllShifts(improved_SA, instance, travelTimes);
        // double new_obj_value_SA = objectiveBasic.shifts(improved_SA)/60.0;
        // double improvement_SA = new_obj_value - new_obj_value_SA;
        // System.out.println("\nSA obj value: " + new_obj_value_SA);
        // System.out.println("Improvement: " + improvement_SA);
        // double total_improvement_SA = initial_obj_value - new_obj_value_SA;
        // System.out.println("Total improvement: " + total_improvement_SA);

        // long endTotalTime = System.currentTimeMillis();
        // double totalTimeTaken = (endTotalTime-startTotalTime)/1000.0;

        // System.out.println("Total time taken: " + totalTimeTaken + " s");

        // Utils.checkFeasibility(improved_SA, instance, totalShiftLength);
        // Utils.printShiftStatistics(improved_SA, instance, totalShiftLength);
        // Utils.resultsToCSV(improved_SA, instance, "src/results/results_SA_abri.csv");
        
    

        // for (int r = 0; r < improved.size(); r++) {
        //     Shift s = improved.get(r);
        //     System.out.println("Route " + r + ": " + s.route + " | total=" + s.totalTime);
        // }
    }
}
