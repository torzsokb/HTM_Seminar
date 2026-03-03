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

<<<<<<< HEAD
        double initial_obj_value = objectiveBasic.shifts(initial)/60.0;

        System.out.println("Initial solution:");
=======
        ObjectiveFunction objective = Objective.balancedObj(0.01, 0.01);
        ObjectiveFunction objectiveTotalLength = Objective.totalLength();

        double initial_obj_value = objectiveTotalLength.shifts(initial)/60.0;

        long endGreedy = System.currentTimeMillis();

        double totalTimeGreedy = (endGreedy - startGreedy) / 1000.0;
        System.out.println("Initial solution built:");
        System.out.println("Night shifts: " + nightShifts.size());
        System.out.println("Day shifts:   " + dayShifts.size());
>>>>>>> 330afabfcb1fb50c960bd45016bd690b759bc6e7
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
<<<<<<< HEAD
                objectiveBasic
=======
                objectiveTotalLength,
                useSimulatedAnnealing
>>>>>>> 330afabfcb1fb50c960bd45016bd690b759bc6e7
        );
        
        long startTime = System.currentTimeMillis();
<<<<<<< HEAD
        System.out.println("\nRunning local search...");
        List<Shift> improved = ls.runDiffTimes(initial, instance, travelTimesNight, travelTimesDay);
=======
        System.out.println("Running local search...");
        initial = Utils.readShiftsFromCSV("src/results/results_LS_abri.csv", travelTimes);
        double intital_obj = Utils.totalObjective(initial);
        System.out.println("Improvement: " + (initial_obj_value - intital_obj));
        Utils.printShiftStatistics(initial, instance, 480);
        List<Shift> improved = ls.run(initial, instance, travelTimes);
>>>>>>> 330afabfcb1fb50c960bd45016bd690b759bc6e7

        Utils.recomputeAllShiftsDiffTimes(improved, instance, travelTimesNight, travelTimesDay);

<<<<<<< HEAD
        double new_obj_value = objectiveBasic.shifts(improved)/60.0;
=======
        double new_obj_value = objectiveTotalLength.shifts(improved)/60.0;
>>>>>>> 330afabfcb1fb50c960bd45016bd690b759bc6e7

        System.out.println("\nLocal search complete.");

        System.out.println("New objective value: " + new_obj_value);
        
        double improvement = initial_obj_value - new_obj_value;
        double extraImprovement = intital_obj - new_obj_value;
        System.out.println("Extra improvement " + extraImprovement);

        System.out.println("Improvement: " + improvement);
        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime-startTime)/1000.0;
        System.out.println("Time taken: " + (timeTaken) + " s" );
        Utils.printShiftStatistics(improved, instance, totalShiftLength);
<<<<<<< HEAD
=======

        // Utils.checkFeasibility(improved, instance, totalShiftLength);
        
        // Utils.resultsToCSV(improved, instance, "src/results/Test");
        // for (Shift shift : improved) {
        //     System.out.println(Utils.formatRoute(instance, shift.route));
        // }
>>>>>>> 330afabfcb1fb50c960bd45016bd690b759bc6e7

    
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


