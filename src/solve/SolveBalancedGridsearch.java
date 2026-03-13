package solve; 
import core.*; 
import neighborhoods.*; 
import search.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;
import milp.*;


public class SolveBalancedGridsearch {
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
            new Intra2Opt(),
            new InterSwap(),
            new IntraSwap(),
            new IntraShift(),
            new Inter2OptStar(),
            new InterShift()
        );

        AcceptanceFunction acceptGreedy = Acceptance.greedy();

        RouteCompatibility compatibility = Compatibility.sameNightShift();


        // BALANCED LOCAL SEARCH 

        ObjectiveFunction objectiveBasic = Objective.totalLength(); 

        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes("src/results/results_final.csv", travelTimesNight, travelTimesDay);
        double initial_obj_value = objectiveBasic.shifts(initial)/60.0;

        Utils.printShiftStatistics(initial, instance, totalShiftLength);


        // Make grid
         double[] lambdaLs = {0.0, 0.0005, 0.001, 0.0015, 0.002, 0.0025, 0.003, 0.0035, 0.004, 0.0045, 0.005, 0.0055, 0.006, 0.0065, 0.007, 0.0075, 0.008, 0.0085, 0.009, 0.0095, 0.01};
         double[] ratios = {1.0, 0.5, 1.0/3.0, 0.25, 0.2};


        // To save results 
        String outputFile = "src/results/GridSearchBalanced.csv";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("LambdaL,LambdaC,ratio,objective,travelTime,minShiftLength,maxShiftLength,varShiftLength,minCleaning,maxCleaning");
            writer.newLine();

            for (double lambdaL : lambdaLs) {
            for (double ratio : ratios) {
                double lambdaC = ratio * lambdaL;
                System.out.println("\nLambdaL = " + lambdaL + ", lambdaC = " + lambdaC + " (ratio " + ratio + ")");
                ObjectiveFunction objectiveBalanced = Objective.balancedObj(lambdaL, lambdaC);

                List<Shift> start = Utils.deepCopyShifts(initial);

                LocalSearch ls_balanced = new LocalSearch(
                neighborhoods,
                acceptGreedy,
                compatibility,
                ImprovementChoice.BEST,
                1000,       
                totalShiftLength,
                objectiveBalanced,
                false
                );

                List<Shift> improved_balanced = ls_balanced.runDiffTimes(start, instance, travelTimesNight, travelTimesDay);

                Utils.recomputeAllShiftsDiffTimes(improved_balanced, instance, travelTimesNight, travelTimesDay);

                // Now do a TSP on the Balanced 
                TSP.optimizeAllShifts(improved_balanced, travelTimesDay, travelTimesNight, instance);

                double obj_TSP = objectiveBasic.shifts(improved_balanced) / 60.0;

                double totalImprovement = initial_obj_value - obj_TSP;

                System.out.println("\nComplete.");
                System.out.println("Total Improvement: " + totalImprovement);

                // Get results 
                double travelTime = 0.0;
                double minShiftLength = Double.POSITIVE_INFINITY;;
                double maxShiftLength = 0.0;
                double minCleaning = Double.POSITIVE_INFINITY;;
                double maxCleaning = 0.0;
                for (Shift shift : improved_balanced) {
                    travelTime += shift.travelTime;
                    if (shift.totalTime < minShiftLength) {
                        minShiftLength = shift.totalTime;
                    }

                    if (shift.totalTime > maxShiftLength ) {
                        maxShiftLength = shift.totalTime;
                    }

                    if (shift.serviceTime < minCleaning) {
                        minCleaning = shift.serviceTime;
                    }

                    if (shift.serviceTime > maxCleaning) {
                        maxCleaning = shift.serviceTime;
                    }
                }

                double varLengths = getVarShiftLengths(improved_balanced);

                // Write to file 
                writer.write(lambdaL + "," + lambdaC + "," + ratio + "," + obj_TSP + "," + (travelTime / 60.0) + "," + (minShiftLength / 60.0) + "," + (maxShiftLength / 60.0) + "," + varLengths + "," + (minCleaning / 60.0) + "," + (maxCleaning / 60.0));
                writer.newLine();
                writer.flush();
                }
            }
        }
    }  

    public static double getVarShiftLengths(List<Shift> shifts) {
        if (shifts == null || shifts.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (Shift s : shifts) {
           sum += s.totalTime;
        }

        double mean = sum / shifts.size();

        double squaredDiffSum = 0.0;
        for (Shift s : shifts) {
          double diff = s.totalTime - mean;
         squaredDiffSum += diff * diff;
     }

        // Variance
        return squaredDiffSum / shifts.size();
    }
}

