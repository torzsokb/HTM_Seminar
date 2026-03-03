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
        String instancePath = "src/core/data_all_feas.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        double[][] travelTimesDay = new double[travelTimes.length][travelTimes.length];
        double[][] travelTimesNight = new double[travelTimes.length][travelTimes.length];
        for (int i = 0; i < travelTimes.length; i++) {
            for (int j = 0; j < travelTimes.length; j++) {
                double dayTravel = travelTimes[i][j] * 1.606862669;
                double nightTravel = travelTimes[i][j] * 1.184004072;
                travelTimesDay[i][j] = dayTravel;
                travelTimesNight[i][j] = nightTravel;

            }
        }

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
        List<Shift> improved = ls.runDiffTimes(initial, instance, travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(improved, instance, travelTimesNight, travelTimesDay);

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

    
        //Utils.resultsToCSV(improved, instance, "src/results/results_LS_feasible.csv");
        //Utils.resultsToCSV(improved, instance, "src/results/results_SA_gridsearch_best_Newv2_feasible.csv");

        // Sanity check
        /*
        for (Shift shift : improved) {
             System.out.println(Utils.formatRoute(instance, shift.route));
        }
         */
    }
}


