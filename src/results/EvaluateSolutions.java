package results;

import core.*;
import search.Objective;
import search.ObjectiveFunction;

import java.util.List;

public class EvaluateSolutions {
    static final double totalShiftLength = 8 * 60;

    public static void main(String[] args) throws Exception  {
        String instancePath = "src/core/data_all.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        // Paths to varying results

        // INIT SOLUTION
        System.out.println("\nInitial solution:");
        String initResults = "src/results/HTM_data_initRes.csv";

        List<Shift> initShifts = Utils.readShiftsFromCSV(initResults, travelTimes);

        Utils.printShiftStatistics(initShifts, instance, totalShiftLength);
        Utils.checkFeasibility(initShifts, instance, totalShiftLength);

        double initObj = Utils.totalObjective(initShifts);
        System.out.println("\nObjective: " + initObj + " hours.");

        // GREEDY
        System.out.println("\nGreedy solution:");
        String greedyResults = "src/results/results_Greedy_abri.csv";

        List<Shift> greedyShifts = Utils.readShiftsFromCSV(greedyResults, travelTimes);

        Utils.printShiftStatistics(greedyShifts, instance, totalShiftLength);
        //Utils.checkFeasibility(greedyShifts, instance, totalShiftLength);

        double greedyObj = Utils.totalObjective(greedyShifts);

        ObjectiveFunction objectiveBasic = Objective.totalLength();
        double greedyValue = objectiveBasic.shifts(greedyShifts);
        System.out.println("\nObjective: " + greedyObj + " hours.");
        System.out.println("Value: " + greedyValue);

        // LS
        System.out.println("\nLocal search solution:");

        String lsResults = "src/results/results_LS_abri.csv";

        List<Shift> lsShifts = Utils.readShiftsFromCSV(lsResults, travelTimes);

        Utils.printShiftStatistics(lsShifts, instance, totalShiftLength);
        //Utils.checkFeasibility(lsShifts, instance, totalShiftLength);

        double lsObj = Utils.totalObjective(lsShifts);
        System.out.println("\nObjective: " + lsObj + " hours.");

        // LS WITH SA
        System.out.println("\nLocal search with SA solution:");

        String saResults = "src/results/results_SA_gridsearch_best_Newv2.csv";

        List<Shift> saShifts = Utils.readShiftsFromCSV(saResults, travelTimes);

        Utils.printShiftStatistics(saShifts, instance, totalShiftLength);
        //Utils.checkFeasibility(saShifts, instance, totalShiftLength);

        double saObj = Utils.totalObjective(saShifts);
        System.out.println("\nObjective: " + saObj + " hours.");

        // Balanced LS
        System.out.println("\nBalanced solution:");

        String baLSResults = "src/results/results_Balanced_0.002_0.001.csv";

        List<Shift> baLSShifts = Utils.readShiftsFromCSV(baLSResults, travelTimes);

        Utils.printShiftStatistics(baLSShifts, instance, totalShiftLength);
        //Utils.checkFeasibility(baLSShifts, instance, totalShiftLength);

        double baLSObj = Utils.totalObjective(baLSShifts);
        System.out.println("\nObjective: " + baLSObj + " hours.");



        // All objectives
        System.out.println("\nAll objectives:");
        System.out.println("Initial objective: " + initObj + " hours.");
        System.out.println("Greedy objective: " + greedyObj + " hours.");
        System.out.println("LS objective: " + lsObj + " hours.");
        System.out.println("SA objective: " + saObj + " hours.");
        System.out.println("Balanced LS objective: " + baLSObj + " hours.");
    }
}
