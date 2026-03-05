package solve; 
import core.*; 
import neighborhoods.*; 
import search.*;
import java.util.*;

public class SolveFullModel {
    //xx
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

        // Choose initial shifts to use 
        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes("src/results/HTM_data_initRes_typeHalte.csv", travelTimesNight, travelTimesDay);

        // Make sure they are feasible 
        Utils.makeFeasible(initial, instance, travelTimesNight, travelTimesDay);

        double initial_obj_value = objectiveBasic.shifts(initial)/60.0;

        System.out.println("Initial solution:");
        System.out.println("Total shifts: " + initial.size());
        System.out.println("Total objective value: " + initial_obj_value);

        Utils.checkFeasibility(initial, instance, totalShiftLength);

        
        
        // NORMAL LOCAL SEARCH 
        List<Neighborhood> neighborhoods = Arrays.asList(
            new Intra2Opt(),
            new InterSwap(),
            new IntraSwap(),
            new IntraShift(),
            new Inter2OptStar(),
            new InterShift()
        );

        AcceptanceFunction acceptGreedy = Acceptance.greedy();

        RouteCompatibility compatibility = Compatibility.sameNightShift();

        LocalSearch ls = new LocalSearch(
                neighborhoods,
                acceptGreedy,
                compatibility,
                ImprovementChoice.BEST,
                1000,       
                totalShiftLength,
                objectiveBasic,
                false
        );
        
        long startTime = System.currentTimeMillis();
        System.out.println("\nRunning local search...");
        List<Shift> improved = ls.runDiffTimes(initial, instance, travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(improved, instance, travelTimesNight, travelTimesDay);

        double new_obj_value = objectiveBasic.shifts(improved)/60.0;

        System.out.println("\nLocal search complete.");

        System.out.println("New objective value: " + new_obj_value);

        double improvement = initial_obj_value - new_obj_value;

        System.out.println("Improvement: " + improvement);

        Utils.checkFeasibility(improved, instance, totalShiftLength);
        Utils.printShiftStatistics(improved, instance, totalShiftLength);

    
        //Utils.resultsToCSV(improved, instance, "src/results/results_LS_feasible.csv");

        // Sanity check
        /*
        for (Shift shift : improved) {
             System.out.println(Utils.formatRoute(instance, shift.route));
        }
         */

        // Now do SA 
        int max_iterations = 30000;
        Acceptance.initSimulatedAnnealing(0.5, 0, max_iterations, 50);
        AcceptanceFunction acceptSA = Acceptance.simulatedAnnealing();
        ObjectiveFunction objectiveTotalLength = Objective.totalLength();

        boolean useSimulatedAnnealing = true;

        LocalSearch ls_SA = new LocalSearch(
                neighborhoods,
                acceptSA,
                compatibility,
                ImprovementChoice.FIRST,
                max_iterations,       
                totalShiftLength,
                objectiveTotalLength,
                useSimulatedAnnealing
        );


        List<Shift> improved_SA = ls_SA.runDiffTimes(improved, instance, travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(improved_SA, instance, travelTimesNight, travelTimesDay);

        double new_obj_value_SA = objectiveTotalLength.shifts(improved_SA)/60.0;

        System.out.println("\nSA complete.");

        System.out.println("New objective value: " + new_obj_value_SA);
        
        double extraImprovement = new_obj_value - new_obj_value_SA;
        System.out.println("Extra improvement " + extraImprovement);

        Utils.printShiftStatistics(improved, instance, totalShiftLength);

        Utils.checkFeasibility(improved, instance, totalShiftLength);
        
        //Utils.resultsToCSV(improved, instance, "src/results/results_SA_feasible.csv");

        // PHASE 3: LS again 
        System.out.println("\nRunning local search (phase 3)...");
        List<Shift> improved_final = ls.runDiffTimes(improved_SA, instance, travelTimesNight, travelTimesDay);

        double final_objective_value = objectiveTotalLength.shifts(improved_final)/60.0;
        System.out.println("Final objective value: " + final_objective_value);

        double improvement_final = initial_obj_value - final_objective_value;

        System.out.println("Improvement: " + improvement_final);

        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime-startTime)/1000.0;
        System.out.println("Total time taken: " + (timeTaken) + " s" );

        Utils.checkFeasibility(improved, instance, totalShiftLength);
        Utils.printShiftStatistics(improved, instance, totalShiftLength);
    
        // Utils.resultsToCSV(improved, instance, "src/results/results_final_feasible.csv");
    }
}



