package SmartFeas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.*;
import search.*;
import neighborhoods.*;
import milp.TSP;


public class SmartFeas {

    private static final double penalty = 1000;

    public static void meakFeasibleSmart(List<Shift> shifts, HTMInstance instance, double[][] travelTimesNight, double[][] travelTimesDay, double maxDuration, double maxOvertime, boolean useTSP) {
        System.out.println("\nRunning Smart Make Feasible...");

        long startTime = System.currentTimeMillis();

        List<Neighborhood> neighborhoods = Arrays.asList(
            new Inter2OptStarInfeas(maxDuration, maxOvertime, penalty),
            new InterShiftInfeas(maxDuration, maxOvertime, penalty)
        );

        AcceptanceFunction accept = Acceptance.alwaysTrue();
        RouteCompatibility compatibility = Compatibility.sameNightShift();
        ObjectiveFunction objectiveBasic = Objective.totalLength();

        LocalSearch ls = new LocalSearch(
            neighborhoods, 
            accept, 
            compatibility, 
            ImprovementChoice.BEST, 
            100, 
            maxDuration + maxOvertime, 
            objectiveBasic, 
            false
        );

        double initial_obj_value = objectiveBasic.shifts(shifts)/60.0;

        List<Shift> violatedShifts = new ArrayList<>();
        List<Shift> feasibleShifts = new ArrayList<>();

        for (Shift shift : shifts) {

            if (shift.totalTime <= maxDuration + maxOvertime) {
                feasibleShifts.add(shift);
            } else {
                violatedShifts.add(shift);
            }            
        }

        if (useTSP) {
            TSP.optimizeAllShifts(violatedShifts, travelTimesDay, travelTimesNight, instance);
        }


        List<Shift> improved = ls.runDiffTimes(shifts, instance, travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(improved, instance, travelTimesNight, travelTimesDay);

        double new_obj_value = objectiveBasic.shifts(improved)/60.0;
        double improvement = initial_obj_value - new_obj_value;
        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime-startTime)/1000.0;

        System.out.println("\nLocal search complete.");
        System.out.println("New objective value: " + new_obj_value);
        System.out.println("Improvement: " + improvement);
        System.out.println("Time taken: " + (timeTaken) + " s" );

        Utils.checkFeasibility(improved, instance, 8 * 60);
        Utils.printShiftStatistics(improved, instance, 8 * 60);
        
    }

    public static void main(String[] args) throws IOException {

        String instancePath = "src/core/data_all_feas_typeHalte.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
        String travelDayPath = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
        double[][] travelTimesDay = Utils.readTravelTimes(travelDayPath);
        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes("src/results/HTM_data_initRes_typeHalte.csv", travelTimesNight, travelTimesDay);

        // Utils.makeFeasible(initial, instance, travelTimesNight, travelTimesDay);
        // Utils.checkFeasibility(initial, instance, 8 * 60);
        // Utils.printShiftStatistics(initial, instance, 8 * 60);
        meakFeasibleSmart(initial, instance, travelTimesNight, travelTimesDay, 8 * 60, 0, false);
        // meakFeasibleSmart(initial, instance, travelTimesNight, travelTimesDay, 7.25 * 60, 0, true);
        // meakFeasibleSmart(initial, instance, travelTimesNight, travelTimesDay, 7.25 * 60, 0, true);


    }
    
}
