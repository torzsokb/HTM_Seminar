package results;

import core.*;
import search.Objective;
import search.ObjectiveFunction;

import java.util.ArrayList;
import java.util.List;

public class EvaluateSolutions {
    static final double totalShiftLength = 8 * 60;

    public static void main(String[] args) throws Exception  {
        String instancePath = "src/core/data_all_feas_typeHalte.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
        String travelDayPath = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
        double[][] travelTimesDay = Utils.readTravelTimes(travelDayPath);

        ObjectiveFunction objectiveBasic = Objective.totalLength();

        // Paths to varying results

        // INIT SOLUTION
        System.out.println("\nInitial solution:");
        // Feasible version
        String initResults = "src/results/HTM_data_initRes_typeHalte.csv";

        // Old version (5 + 8 = 13 & 5 + 15 = 20)
        // String initResults = "src/results/HTM_data_initRes3b.csv";
        
        List<Shift> initShifts = Utils.readShiftsFromCSVDiffTimes(initResults, travelTimesNight, travelTimesDay);

        Utils.printShiftStatistics(initShifts, instance, totalShiftLength);
        Utils.checkFeasibility(initShifts, instance, totalShiftLength);

        double initialObjective = objectiveBasic.shifts(initShifts)/60.0;;
        System.out.println("\nObjective: " + initialObjective + " hours.");

        // Make initial shifts feasible 
        System.out.println("\nInitial feasible solution:");
        // Utils.makeFeasible(initShifts, instance, travelTimesNight, travelTimesDay);

        Utils.printShiftStatistics(initShifts, instance, totalShiftLength);
        Utils.checkFeasibility(initShifts, instance, totalShiftLength);

        double initObj = objectiveBasic.shifts(initShifts)/60.0;;
        System.out.println("\nObjective: " + initObj + " hours.");


        // TSP

        /*
        System.out.println("\nTSP solution:");

        String tspResults = "data/outputs/HTM_CollapsedData_reordered_final.csv";

        List<Shift> tspShifts = Utils.readShiftsFromCSVDiffTimes(tspResults, travelTimesNight, travelTimesDay);

        Utils.printShiftStatistics(tspShifts, instance, totalShiftLength);
        Utils.checkFeasibility(tspShifts, instance, totalShiftLength);

        double tspObj = objectiveBasic.shifts(tspShifts)/60.0;;
        System.out.println("\nObjective: " + tspObj + " hours.");
         */
        

        // LS
        System.out.println("\nLocal search solution:");

        String lsResults = "src/results/results_LS_final.csv";

        List<Shift> lsShifts = Utils.readShiftsFromCSVDiffTimes(lsResults, travelTimesNight, travelTimesDay);

        Utils.printShiftStatistics(lsShifts, instance, totalShiftLength);
        Utils.checkFeasibility(lsShifts, instance, totalShiftLength);

        double lsObj = objectiveBasic.shifts(lsShifts)/60.0;;
        System.out.println("\nObjective: " + lsObj + " hours.");

        // LS WITH SA
        System.out.println("\nLocal search with SA solution:");

        String saResults = "src/results/results_SA_final.csv";

        List<Shift> saShifts = Utils.readShiftsFromCSVDiffTimes(saResults, travelTimesNight, travelTimesDay);

        Utils.printShiftStatistics(saShifts, instance, totalShiftLength);
        Utils.checkFeasibility(saShifts, instance, totalShiftLength);

        double saObj = objectiveBasic.shifts(saShifts)/60.0;;
        System.out.println("\nObjective: " + saObj + " hours.");
        
        System.out.println("\nFinal solution:");

        String finalResults = "src/results/results_final.csv";

        List<Shift> finalShifts = Utils.readShiftsFromCSVDiffTimes(finalResults, travelTimesNight, travelTimesDay);

        Utils.printShiftStatistics(finalShifts, instance, totalShiftLength);
        Utils.checkFeasibility(finalShifts, instance, totalShiftLength);System.out.println("\nFinal solution:");

        double finalObj = objectiveBasic.shifts(finalShifts)/60.0;;
        System.out.println("\nObjective: " + finalObj + " hours.");

        // Balanced LS
        System.out.println("\nBalanced solution:");

        String baLSResults = "src/results/results_Balanced_final_0.002_0.002.csv";

        List<Shift> baLSShifts = Utils.readShiftsFromCSVDiffTimes(baLSResults, travelTimesNight, travelTimesDay);

        Utils.printShiftStatistics(baLSShifts, instance, totalShiftLength);
        Utils.checkFeasibility(baLSShifts, instance, totalShiftLength);

        double baLSObj = objectiveBasic.shifts(baLSShifts)/60.0;;
        System.out.println("\nObjective: " + baLSObj + " hours.");


        // Trams as night stops 
        System.out.println("\nTypehalte solution:");
        List<Shift> typeHalteShifts = Utils.readShiftsFromCSVDiffTimes("src/results/results_typeHalte_final.csv", travelTimesNight, travelTimesDay);
        
        Utils.printShiftStatistics(typeHalteShifts, instance, totalShiftLength);
        Utils.checkFeasibility(typeHalteShifts, instance, totalShiftLength);

        double typeHalteObj = objectiveBasic.shifts(typeHalteShifts) / 60.0;
        System.out.println("\nObjective: " + typeHalteObj + " hours.");

        // Min shifts
        System.out.println("\nMinimum number of shifts solution:");
        List<Shift> minShifts = Utils.readShiftsFromCSVDiffTimes("src/results/results_minShifts_final.csv", travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(minShifts, instance, travelTimesNight, travelTimesDay);

        List<Shift> minimumShiftsVND = new ArrayList<>();
        for (Shift shift : minShifts) {
            if (shift.serviceTime >= 0.001) {
                minimumShiftsVND.add(shift);
            }
        }

        Utils.printShiftStatistics(minimumShiftsVND, instance, totalShiftLength);
        Utils.checkFeasibility(minimumShiftsVND, instance, totalShiftLength);

        double minLSObj = objectiveBasic.shifts(minimumShiftsVND)/60.0;;
        System.out.println("\nObjective: " + minLSObj + " hours.");
        


        // All objectives
        System.out.println("\nAll objectives:");
        System.out.println("Initial objective: " + initObj + " hours.");
        //System.out.println("TSP objective: " + tspObj + " hours.");
        //System.out.println("Improvement: " + (initObj - tspObj) + " hours.");
        System.out.println("LS objective: " + lsObj + " hours.");
        System.out.println("Improvement: " + (initObj - lsObj) + " hours.");
        System.out.println("SA objective: " + saObj + " hours.");
        System.out.println("Improvement: " + (initObj - saObj) + " hours.");
        System.out.println("Final objective: " + finalObj + " hours.");
        System.out.println("Improvement: " + (initObj - finalObj) + " hours.");
        System.out.println("Balanced LS objective: " + baLSObj + " hours.");
        System.out.println("Improvement: " + (initObj - baLSObj) + " hours.");

        System.out.println("Type halte objective: " + typeHalteObj + " hours.");
        System.out.println("Improvement: " + (initObj - typeHalteObj) + " hours.");

        System.out.println("Minimum VND objective: " + minLSObj + " hours.");
        System.out.println("Improvement: " + (initObj - minLSObj) + " hours.");

        // For most efficient and balanced, collect lengths and cleaning times 
        Utils.printCleaningAndLength(finalShifts, "src/results/SA_stats_best.csv");
        Utils.printCleaningAndLength(baLSShifts, "src/results/Balanced_stats_TSP.csv");
        Utils.printCleaningAndLength(initShifts, "src/results/init_stats_feasible.csv");
        Utils.printCleaningAndLength(minShifts, "src/results/MinShifts_stats.csv");
    }
}
