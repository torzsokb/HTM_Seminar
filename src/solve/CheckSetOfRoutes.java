package solve;

import core.*;
import neighborhoods.*;
import search.*;
import milp.*;

import java.util.*;
import java.io.File;

public class CheckSetOfRoutes {
    static final double totalShiftLength = 8 * 60;
    static final double overTime = 15.0;
    public static void main(String[] args) throws Exception {

        String instanceDir = "src/core/scenario_instances/txt_files/";
        String initSolutionDir = "src/core/scenario_instances/HTM_scenario_csvs/";

        String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
        String travelDayPath   = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";

        String outputDir = "src/results/postHocScenarios/";
        new File(outputDir).mkdirs();

        // Load travel times once
        double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
        double[][] travelTimesDay   = Utils.readTravelTimes(travelDayPath);

        File folder = new File(instanceDir);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files == null) {
            System.out.println("No scenario files found.");
            return;
        }

        for (File instanceFile : files) {

            String instancePath = instanceFile.getPath();
            String scenarioName = instanceFile.getName().replace(".txt", "");

            System.out.println("\n====================================");
            System.out.println("Running scenario: " + scenarioName);
            System.out.println("====================================");

            HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Type_halte");

            String initSolutionPath = initSolutionDir + "HTM_solution_" + scenarioName + ".csv";
            String outputFile = outputDir + "result_" + scenarioName + ".csv";

            solveScenario(
                    instance,
                    initSolutionPath,
                    travelTimesNight,
                    travelTimesDay,
                    outputFile
            );
        
        }
        String instancePath = "src/core/data_all_feas_typeHalte.txt";

        String initialSolutionPath = "src/results/HTM_data_initRes_typeHalte.csv";
        
        HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Night_shift");
        double[] trafficFlatPenalties = {1.1, 1.2, 1.3, 1.4, 1.5};
        int n = travelTimesNight.length;
        for (double penalty : trafficFlatPenalties) {
            double[][] newDayTT = new double[n][n];
            double[][] newNightTT = new double[n][n];
            for (int i = 0; i <n; i++) {
                for (int j = 0; j <n; j++) {
                newDayTT[i][j] = travelTimesDay[i][j]*penalty;
                newNightTT[i][j] = travelTimesNight[i][j]*penalty;
                }
            }
            System.out.println("\n====================================");
            System.out.println("Running scenario penalty: " + penalty);
            System.out.println("====================================");
            solveScenario(instance, initialSolutionPath, newNightTT, newDayTT,
                outputDir + "/trafficPenalty_" + penalty + ".csv");
        }
    }

    public static void solveScenario(
            HTMInstance instance,
            String initialSolutionPath,
            double[][] travelTimesNight,
            double[][] travelTimesDay,
            String outputFile
    ) throws Exception {

        ObjectiveFunction objectiveBasic = Objective.totalLength();
        ObjectiveFunction objectiveBalanced = Objective.balancedObj(0.003, 0.001);

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

        // ---- Load scenario-specific initial solution ----
        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes(
                initialSolutionPath,
                travelTimesNight,
                travelTimesDay
        );

        TSP.optimizeAllShifts(initial, travelTimesDay, travelTimesNight, instance);
        Utils.makeFeasible(initial, instance, travelTimesNight, travelTimesDay);

        double initialObj = objectiveBasic.shifts(initial) / 60.0;

        System.out.println("Initial shifts: " + initial.size());
        System.out.println("Initial objective: " + initialObj);

        Utils.checkFeasibility(initial, instance, totalShiftLength);

        // ---- Local Search ----
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

        System.out.println("\nRunning local search...");
        List<Shift> improved = ls.runDiffTimes(initial, instance, travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(improved, instance, travelTimesNight, travelTimesDay);

        // ---- Simulated Annealing ----
        int maxIterations = 100000;

        Acceptance.initSimulatedAnnealing(0.5, 0, maxIterations, 5);

        LocalSearch lsSA = new LocalSearch(
                neighborhoods,
                Acceptance.simulatedAnnealing(),
                compatibility,
                ImprovementChoice.FIRST,
                maxIterations,
                totalShiftLength,
                objectiveBasic,
                true
        );

        System.out.println("\nRunning OSA...");
        List<Shift> improvedSA = lsSA.runDiffTimes(improved, instance, travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(improvedSA, instance, travelTimesNight, travelTimesDay);

        // ---- TSP optimization ----
        System.out.println("\nSolving TSP...");
        List<Shift> improvedFinal = Utils.deepCopyShifts(improvedSA);

        TSP.optimizeAllShifts(improvedFinal, travelTimesDay, travelTimesNight, instance);

        // // ---- Balanced Local Search ----
        // System.out.println("\nRunning balanced local search...");

        // LocalSearch lsBalanced = new LocalSearch(
        //         neighborhoods,
        //         acceptGreedy,
        //         compatibility,
        //         ImprovementChoice.FIRST,
        //         1000,
        //         totalShiftLength,
        //         objectiveBalanced,
        //         false
        // );

        // List<Shift> improvedBalanced = lsBalanced.runDiffTimes(
        //         improvedFinal,
        //         instance,
        //         travelTimesNight,
        //         travelTimesDay
        // );

        // Utils.recomputeAllShiftsDiffTimes(improvedBalanced, instance, travelTimesNight, travelTimesDay);
        
        // // ---- Final TSP ----
        // TSP.optimizeAllShifts(improvedBalanced, travelTimesDay, travelTimesNight, instance);

        // double finalObjective = objectiveBasic.shifts(improvedBalanced) / 60.0;

        // System.out.println("\nFinal objective value: " + finalObjective);

        // Utils.checkFeasibility(improvedBalanced, instance, totalShiftLength);
        // Utils.printShiftStatistics(improvedBalanced, instance, totalShiftLength);

        Utils.resultsToCSV(improvedFinal, instance, outputFile);

        System.out.println("Saved result to: " + outputFile);
    }
}