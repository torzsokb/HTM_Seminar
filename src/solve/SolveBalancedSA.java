package solve; 
import core.*; 
import neighborhoods.*; 
import search.*;
import java.util.*;


public class SolveBalancedSA {
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

        ObjectiveFunction objectiveBasic = Objective.totalLength();
        ObjectiveFunction objectiveBalanced = Objective.balancedObj(0.003, 0.002);

        LocalSearch ls_balanced = new LocalSearch(
                neighborhoods,
                acceptGreedy,
                compatibility,
                ImprovementChoice.FIRST,
                1000,       
                totalShiftLength,
                objectiveBalanced,
                false
        );

        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes("src/results/results_final_feasible.csv", travelTimesNight, travelTimesDay);
        double initial_obj_value = objectiveBasic.shifts(initial)/60.0;


        long startBLS = System.currentTimeMillis();
        System.out.println("\nRunning balanced local search...");
        List<Shift> improved_balanced = ls_balanced.runDiffTimes(initial, instance, travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(improved_balanced, instance, travelTimesNight, travelTimesDay);

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


        Utils.resultsToCSV(improved_balanced, instance, "src/results/results_Balanced_0.003_0.002_feasible.csv");

    }
}
