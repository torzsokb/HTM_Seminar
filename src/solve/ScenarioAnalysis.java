package solve;

import core.*;
import neighborhoods.*;
import search.*;
import milp.*;

import java.util.*;
import java.io.File;
import SmartFeas.SmartFeas;

public class ScenarioAnalysis {
    static final double totalShiftLength = 8 * 60;
    static final double overTime = 0.0;
    public static void main(String[] args) throws Exception {
        String instancePath = "src/core/data_all_feas_typeHalte.txt";

        String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
        String travelDayPath = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Type_halte");

        double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
        double[][] travelTimesDay = Utils.readTravelTimes(travelDayPath);

        String solution_path = "data/results/HTM.csv";

        checkSensitivity(instance, solution_path, travelTimesNight, travelTimesDay);
        
    }
    public static void checkSensitivity(
            HTMInstance instance,
            String solution_path,
            double[][] travelTimesNight,
            double[][] travelTimesDay
    ) throws Exception {
        
        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes(solution_path, travelTimesNight, travelTimesDay);
        double totalOT = Utils.totalOverTime(initial, totalShiftLength);
        double totalOT15 = Utils.totalOverTime(initial, totalShiftLength + 15);
        double initial_tt = Utils.totalTravelTime(initial);

        int numMoves = SmartFeas.meakFeasibleSmartNumMoves(initial, instance, travelTimesNight, travelTimesDay, totalShiftLength, overTime, false);

        double new_tt = Utils.totalTravelTime(initial);
        double increaseTT = new_tt-initial_tt;

        System.out.println("Total OT " + totalOT);
        System.out.println("Total OT + 15+ " + totalOT15);
        System.out.println("NumMoves makeFeasible " + numMoves);
        System.out.println("IncreaseTT " + increaseTT);
        
    }
}
