package solve; 
import core.*; 
import neighborhoods.*; 
import search.*;
import milp.*;

import java.util.*;
import java.util.List;

import search.Objective;
import search.ObjectiveFunction;

//optimizeAllShifts(List<Shift> shifts, double[][] travelTimesDay, double[][] travelTimesNight, HTMInstance instance)
public class SolveTSP {
    // Solving TSP after SA 
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


        ObjectiveFunction objective = Objective.balancedObj(0.01, 0.01);
        ObjectiveFunction objectiveTotalLength = Objective.totalLength();

        // Load initial shifts 

        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes("src/results/HTM_data_initRes_typeHalte.csv", travelTimesNight, travelTimesDay);

        double initial_obj_value = objectiveTotalLength.shifts(initial)/60.0;

        // Load SA 
        List<Shift> saShifts = Utils.readShiftsFromCSVDiffTimes("src/results/bestGridSearchResults.csv", travelTimesNight, travelTimesDay);

        double sa_obj = objectiveTotalLength.shifts(saShifts) / 60.0;


        // Now run the TSP (phase 3) 
        long startTime = System.currentTimeMillis();

        TSP.optimizeAllShifts(saShifts, travelTimesDay, travelTimesNight, instance);

        long endTime = System.currentTimeMillis();

        double new_obj_value = objectiveTotalLength.shifts(saShifts)/60.0;

        System.out.println("\nTSP complete.");

        System.out.println("New objective value: " + new_obj_value);
        
        double improvement = initial_obj_value - new_obj_value;
        double extraImprovement = sa_obj - new_obj_value;
        System.out.println("Extra improvement " + extraImprovement);

        System.out.println("Improvement: " + improvement);
        
        double timeTaken = (endTime-startTime)/1000.0;
        System.out.println("Time taken: " + (timeTaken) + " s" );


        Utils.printShiftStatistics(saShifts, instance, totalShiftLength);

        Utils.checkFeasibility(saShifts, instance, totalShiftLength);

        Utils.resultsToCSV(saShifts, instance, "src/results/results_final.csv");
    }

}
