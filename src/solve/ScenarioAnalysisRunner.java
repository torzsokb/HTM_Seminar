package solve;

import core.*;
import SmartFeas.SmartFeas;

import java.io.*;
import java.util.*;

public class ScenarioAnalysisRunner {

    static final double totalShiftLength = 8 * 60;
    static final double overTime = 0.0;

    static String baseInstance = "src/core/scenario_instances/txt_files/";
    static String baseSolutions = "src/core/scenario_instances/";

    static String outputCSV = "src/core/scenario_instances/scenario_results_interShift.csv";

    static String[] solutions = {
            "BALANCED"
            // "HTM",
            // "TSP",
            // "VND"
            // "VND_MIN"
    };

    static double[] probs = {0.25, 0.5, 1.0};
    static int[] penalties = {3, 5};

    public static void main(String[] args) throws Exception {

        String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
        String travelDayPath = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";

        double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
        double[][] travelTimesDay = Utils.readTravelTimes(travelDayPath);

        PrintWriter writer = new PrintWriter(new File(outputCSV));

        writer.println("solution,season,prob,penalty,avg_totalOT,numOT,avg_totalOT15,numOT15,avg_numMoves,avg_numMoves15,avg_numStopMoved,avg_numStopMoved15,stillInfeas,stillInfeas15,avg_increaseTT");

        runAutumnExperiments(writer, travelTimesNight, travelTimesDay);
        runSummerExperiments(writer, travelTimesNight, travelTimesDay);

        writer.close();

        System.out.println("All experiments finished.");
    }

    static void runAutumnExperiments(
            PrintWriter writer,
            double[][] night,
            double[][] day
    ) throws Exception {

        for(String sol : solutions){

            for(double prob : probs){

                String probStr;
            
                if(prob == 1.0)
                    probStr = "1";
                else
                    probStr = String.valueOf(prob);

                for(int pen : penalties){
                    if (pen == 5 && prob == 1.0) continue;
                    int numOT = 0;
                    int numOT15 = 0;
                    double sumOT = 0;
                    double sumOT15 = 0;
                    double sumMoves = 0;
                    double sumMoves15 = 0;
                    double sumIncTT = 0;
                    double sumStopsmoved = 0;
                    double sumStopsmoved15 = 0;
                    int stillInfeas = 0;
                    int stillInfeas15 = 0;

                    for(int i=1;i<=100;i++){
                        
                        String instancePath =
                                baseInstance +
                                "autumn/prob_" + probStr +
                                "/penalty_" + pen +
                                "/autumn_" + i + ".txt";
                        
                        String solutionPath =
                                baseSolutions + sol +
                                "/Autumn_cleaning_" + i +
                                "_prob_" + probStr +
                                "_pen_" + pen + ".csv";

                        HTMInstance instance =
                                Utils.readInstance(instancePath, "feasible", "Type_halte");

                        Result r = checkSensitivity(
                                instance,
                                solutionPath,
                                night,
                                day
                        );

                        sumOT += r.totalOT;
                        sumOT15 += r.totalOT15;
                        numOT += r.OTincurred;
                        numOT15 += r.OT15incurred;
                        sumMoves += r.numMoves;
                        sumMoves15 += r.numMoves15;
                        sumIncTT += r.increaseTT;
                        sumStopsmoved += r.stopsMoved;
                        sumStopsmoved15 += r.stopsMoved15;
                        stillInfeas += r.stillInfeasible;
                        stillInfeas15 += r.stillInfeasible15;
                    }

                    double avgOT = sumOT / 100;
                    double avgOT15 = sumOT15 / 100;
                    double avgMoves = sumMoves / 100;
                    double avgMoves15 = sumMoves15 / 100;
                    double avgIncTT = sumIncTT / 100;
                    double avgNumStopMoved = sumStopsmoved /100;
                    double avgNumStopMoved15 = sumStopsmoved15 /100;

                    writer.println(
                            sol + "," +
                            "autumn," +
                            prob + "," +
                            pen + "," +
                            avgOT + "," +
                            numOT + "," +
                            avgOT15 + "," +
                            numOT15 + "," +
                            avgMoves + "," +
                            avgMoves15 + "," +
                            avgNumStopMoved + "," +
                            avgNumStopMoved15 + "," +
                            stillInfeas + "," +
                            stillInfeas15 + "," +
                            avgIncTT
                    );

                    System.out.println("Finished: " + sol +
                            " prob=" + prob +
                            " pen=" + pen);
                }
            }
        }
    }

    static void runSummerExperiments(
            PrintWriter writer,
            double[][] night,
            double[][] day
    ) throws Exception {

        int[] summerPenalties = {3,5,10};

        for(String sol : solutions){

            for(int pen : summerPenalties){

                String instancePath =
                        baseInstance +
                        "summer/summer_" + pen + ".txt";

                String solutionPath =
                        baseSolutions + sol +
                        "/Summer_cleaning_time_fixed_" + pen + ".csv";

                HTMInstance instance =
                        Utils.readInstance(instancePath, "feasible", "Night_shift");

                Result r = checkSensitivity(
                        instance,
                        solutionPath,
                        night,
                        day
                );
                
                writer.println(
                        sol + "," +
                        "summer," +
                        "-" + "," +
                        pen + "," +
                        r.totalOT + "," +
                        r.OTincurred + "," +
                        r.totalOT15 + "," +
                        r.OT15incurred + "," +  
                        r.numMoves + "," +
                        r.numMoves15 + "," +
                        r.stopsMoved + "," +
                        r.stopsMoved15 + "," +
                        r.stillInfeasible + "," +
                        r.stillInfeasible15 + "," +
                        r.increaseTT
                );
            }
        }
    }

    static Result checkSensitivity(
            HTMInstance instance,
            String solution_path,
            double[][] travelTimesNight,
            double[][] travelTimesDay
    ) throws Exception {

        List<Shift> initial =
                Utils.readShiftsFromCSVDiffTimes(solution_path, travelTimesNight, travelTimesDay);

        List<Shift> old = Utils.deepCopyShifts(initial);
        List<Shift> initial2 = Utils.deepCopyShifts(initial);
        double totalOT = Utils.totalOverTime(initial, totalShiftLength);
        int OTincurred = (totalOT > 0) ? 1 : 0;
        double totalOT15 = Utils.totalOverTime(initial, totalShiftLength + 15);
        int OT15incurred = (totalOT15 > 0) ? 1 : 0;

        double initial_tt = Utils.totalTravelTime(initial);

        int numMoves = SmartFeas.meakFeasibleSmartNumMoves(
                initial,
                instance,
                travelTimesNight,
                travelTimesDay,
                totalShiftLength,
                overTime,
                false
        );

        int stillInfeas = (!Utils.feasibleTime(initial, instance, totalShiftLength)) ? 1 : 0;

        int numMoves15 = SmartFeas.meakFeasibleSmartNumMoves(
            initial2,
            instance,
            travelTimesNight,
            travelTimesDay,
            totalShiftLength + 15,
            overTime,
            false
        );
        int stillInfeas15 = (!Utils.feasibleTime(initial2, instance, totalShiftLength + 15)) ? 1 : 0;

        double new_tt = Utils.totalTravelTime(initial);
        double numStopsMoved = SmartFeas.countMoves(old, initial);
        double numStopsMoved15 = SmartFeas.countMoves(old, initial2);

        return new Result(
                totalOT,
                totalOT15,
                numMoves,
                numMoves15,
                new_tt - initial_tt,
                OTincurred,
                OT15incurred,
                numStopsMoved,
                numStopsMoved15,
                stillInfeas,
                stillInfeas15
        );
    }

    static class Result{

        double totalOT;
        double totalOT15;
        double numMoves;
        double numMoves15;
        double increaseTT;
        int OTincurred;
        int OT15incurred;
        double stopsMoved;
        double stopsMoved15;
        int stillInfeasible;
        int stillInfeasible15;
        
        Result(double ot, double ot15, double moves, double moves15, double incTT, int otIn, int ot15In, double stopsM, double stopsM15, int stillInfeas, int stillInfeas15){
            totalOT = ot;
            totalOT15 = ot15;
            numMoves = moves;
            numMoves15 = moves15;
            increaseTT = incTT;
            OTincurred = otIn;
            OT15incurred = ot15In;
            stopsMoved = stopsM;
            stopsMoved15 = stopsM15;
            stillInfeasible = stillInfeas;
            stillInfeasible15 = stillInfeas15;
        }
    }
}