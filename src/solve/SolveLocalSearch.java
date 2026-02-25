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

        List<Integer> nightIdx = Utils.getAllowedIndices(instance, 1);
        List<Integer> dayIdx   = Utils.getAllowedIndices(instance, 0);

        List<Shift> nightShifts = Utils.buildGreedyShifts(instance, travelTimes, nightIdx, 1, shiftLength);
        List<Shift> dayShifts   = Utils.buildGreedyShifts(instance, travelTimes, dayIdx, 0, shiftLength);

        List<Shift> initial = new ArrayList<>();
        initial.addAll(nightShifts);
        initial.addAll(dayShifts);

        ObjectiveFunction objective = Objective.balancedObj(0.001, 0);
        ObjectiveFunction objectiveBalanced = Objective.totalLength();

        double initial_obj_value = objectiveBalanced.shifts(initial)/60.0;

        System.out.println("Initial solution built:");
        System.out.println("Night shifts: " + nightShifts.size());
        System.out.println("Day shifts:   " + dayShifts.size());
        System.out.println("Total shifts: " + initial.size());
        System.out.println("Total objective value: " + initial_obj_value);

        initial = Utils.readShiftsFromCSV("src/results/results_SA_gridsearch_best_Newv2.csv", travelTimes);

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
                objective
        );
        long startTime = System.currentTimeMillis();
        System.out.println("Running local search...");
        
        List<Shift> improved = ls.run(initial, instance, travelTimes);

        Utils.recomputeAllShifts(improved, instance, travelTimes);

        double new_obj_value = objectiveBalanced.shifts(improved)/60.0;

        System.out.println("\nLocal search complete.");

        System.out.println("New objective value: " + new_obj_value);

        double improvement = initial_obj_value - new_obj_value;

        System.out.println("Improvement: " + improvement);
        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime-startTime)/1000.0;
        System.out.println("Time taken: " + (timeTaken) + " s" );

        Utils.checkFeasibility(improved, instance, totalShiftLength);
        Utils.printShiftStatistics(improved, instance, totalShiftLength);

        Utils.resultsToCSV(improved, instance, "src/results/results_LS_abri_balanced_001_0.csv");
        // for (Shift shift : improved) {
        //     System.out.println(Utils.formatRoute(instance, shift.route));
        // }

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
