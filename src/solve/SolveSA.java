package solve; 
import core.*; 
import neighborhoods.*; 
import search.*;

import java.util.*;


public class SolveSA {
    //xx
    static final double shiftLength = 7*60;
    static final double totalShiftLength = 8*60;

    public static void main(String[] args) throws Exception {
        String instancePath = "src/core/data_all_feas_typeHalte.txt";
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


        ObjectiveFunction objective = Objective.balancedObj(0.01, 0.01);
        ObjectiveFunction objectiveTotalLength = Objective.totalLength();

        // Choose initial shifts to use 
        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes("src/results/HTM_data_initRes_typeHalte.csv", travelTimesNight, travelTimesDay);

        // Make sure they are feasible 
        Utils.makeFeasible(initial, instance, travelTimesNight, travelTimesDay);

        double initial_obj_value = objectiveTotalLength.shifts(initial)/60.0;

        // NORMAL LOCAL SEARCH 
        List<Neighborhood> neighborhoods = Arrays.asList(
            new Inter2OptStar(),
            new InterShift(),
            new IntraShift(),
            new Intra2Opt(),
            new IntraSwap(),
            new InterSwap()
        );
        
        
        int max_iterations = 30000;
        Acceptance.initSimulatedAnnealing(0.5, 0, max_iterations, 50);
        AcceptanceFunction acceptSA = Acceptance.simulatedAnnealing();

        AcceptanceFunction acceptGreedy = Acceptance.greedy();

        RouteCompatibility compatibility = Compatibility.sameNightShift();
        boolean useSimulatedAnnealing = true;

        LocalSearch ls = new LocalSearch(
                neighborhoods,
                acceptSA,
                compatibility,
                ImprovementChoice.FIRST,
                max_iterations,       
                totalShiftLength,
                objectiveTotalLength,
                useSimulatedAnnealing
        );
        long startTime = System.currentTimeMillis();
        
        initial = Utils.readShiftsFromCSVDiffTimes("src/results/results_LS_feasible.csv", travelTimesNight, travelTimesDay);
        
        double intital_obj = objectiveTotalLength.shifts(initial)/60.0;

        System.out.println("Initial solution:");
        System.out.println("Total shifts: " + initial.size());
        System.out.println("Total objective value: " + intital_obj);

        Utils.printShiftStatistics(initial, instance, 480);

        System.out.println("Running SA...");
        List<Shift> improved = ls.runDiffTimes(initial, instance, travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(improved, instance, travelTimesNight, travelTimesDay);

        double new_obj_value = objectiveTotalLength.shifts(improved)/60.0;

        System.out.println("\nSA complete.");

        System.out.println("New objective value: " + new_obj_value);
        
        double improvement = initial_obj_value - new_obj_value;
        double extraImprovement = intital_obj - new_obj_value;
        System.out.println("Extra improvement " + extraImprovement);

        System.out.println("Improvement: " + improvement);
        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime-startTime)/1000.0;
        System.out.println("Time taken: " + (timeTaken) + " s" );
        Utils.printShiftStatistics(improved, instance, totalShiftLength);

        Utils.checkFeasibility(improved, instance, totalShiftLength);
        
        Utils.resultsToCSV(improved, instance, "src/results/SA_feasible");
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
