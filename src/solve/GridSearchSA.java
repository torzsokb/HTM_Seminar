package solve;

import core.*;
import neighborhoods.*;
import search.*;

import java.util.*;
import java.io.*;

public class GridSearchSA {

    private static final double totalShiftLength = 8 * 60;

    public static void runGridSearch(String instancePath,
                                     String travelNightPath,
                                     String travelDayPath,
                                     String initialCSVPath,
                                     String resultFolder) throws Exception {

        // Create result folder if it does not exist
        new File(resultFolder).mkdirs();

        // Load instance and travel times
        HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Night_shift");
        double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
        double[][] travelTimesDay = Utils.readTravelTimes(travelDayPath);

        ObjectiveFunction objectiveTotalLength = Objective.totalLength();

        // Load initial solution
        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes(initialCSVPath, travelTimesNight, travelTimesDay);
        double initialObjValue = objectiveTotalLength.shifts(initial) / 60.0;

        // Define neighborhoods
        List<Neighborhood> neighborhoods = Arrays.asList(
                new Inter2OptStar(),
                new InterShift(),
                new IntraShift(),
                new Intra2Opt(),
                new IntraSwap(),
                new InterSwap()
        );

        RouteCompatibility compatibility = Compatibility.sameNightShift();
        boolean useSimulatedAnnealing = true;

        // Grid search ranges
        double[] initialTemps = {0.1, 0.5, 1.0, 2.0};
        int[] maxIterations = {100000};
        int[] oscillations = {1, 5, 10, 50};

        double Tf = 0;

        // Tracking best solution
        double bestObjective = Double.MAX_VALUE;
        double bestT0 = 0;
        int bestMaxIter = 0;
        int bestOsc = 0;

        // Prepare summary CSV
        String summaryPath = resultFolder + "/grid_search_summary_part3.csv";
        PrintWriter summaryWriter = new PrintWriter(new FileWriter(summaryPath));
        summaryWriter.println("T0,maxIter,osc,finalObjective,improvement,runtimeSeconds");

        for (int maxIter : maxIterations) {
            for (double T0 : initialTemps) {
                for (int osc : oscillations) {

                    System.out.println("\n=== Running SA with T0=" + T0 +
                            ", maxIter=" + maxIter +
                            ", osc=" + osc + " ===");

                    Acceptance.initSimulatedAnnealing(T0, Tf, maxIter, osc);
                    AcceptanceFunction acceptSA = Acceptance.simulatedAnnealing();

                    LocalSearch ls = new LocalSearch(
                            neighborhoods,
                            acceptSA,
                            compatibility,
                            ImprovementChoice.FIRST,
                            maxIter,
                            totalShiftLength,
                            objectiveTotalLength,
                            useSimulatedAnnealing
                    );

                    List<Shift> initialCopy = Utils.deepCopyShifts(initial);

                    long startTime = System.currentTimeMillis();

                    List<Shift> improved = ls.runDiffTimes(
                            initialCopy,
                            instance,
                            travelTimesNight,
                            travelTimesDay
                    );

                    long endTime = System.currentTimeMillis();

                    Utils.recomputeAllShiftsDiffTimes(
                            improved,
                            instance,
                            travelTimesNight,
                            travelTimesDay
                    );

                    double runtimeSeconds = (endTime - startTime) / 1000.0;
                    double newObjValue = objectiveTotalLength.shifts(improved) / 60.0;
                    double improvement = initialObjValue - newObjValue;

                    System.out.println("Final objective: " + newObjValue +
                            " | Improvement: " + improvement +
                            " | Time: " + runtimeSeconds + "s");

                    // Save individual result
                    String resultFile = String.format(
                            "%s/results_SA_T0%.2f_maxIter%d_osc%d.csv",
                            resultFolder, T0, maxIter, osc
                    );
                    Utils.resultsToCSV(improved, instance, resultFile);

                    // Write to summary table
                    summaryWriter.println(T0 + "," +
                            maxIter + "," +
                            osc + "," +
                            newObjValue + "," +
                            improvement + "," +
                            runtimeSeconds);

                    // Update best solution
                    if (newObjValue < bestObjective) {
                        bestObjective = newObjValue;
                        bestT0 = T0;
                        bestMaxIter = maxIter;
                        bestOsc = osc;
                    }
                }
            }
        }

        summaryWriter.close();

        System.out.println("\n===== GRID SEARCH COMPLETE =====");
        System.out.println("Best objective: " + bestObjective);
        System.out.println("Best parameters:");
        System.out.println("T0 = " + bestT0);
        System.out.println("maxIter = " + bestMaxIter);
        System.out.println("osc = " + bestOsc);
        System.out.println("Summary saved to: " + summaryPath);
    }

    public static void main(String[] args) throws Exception {
        String instancePath = "src/core/data_all_feas_typeHalte.txt";
        String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
        String travelDayPath = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";
        String initialCSVPath = "src/results/results_LS_feasible.csv";
        String resultFolder = "src/results/grid_search_SA";

        runGridSearch(instancePath,
                travelNightPath,
                travelDayPath,
                initialCSVPath,
                resultFolder);
    }
}