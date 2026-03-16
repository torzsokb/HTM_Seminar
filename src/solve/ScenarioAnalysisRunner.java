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

    static String outputCSV = "src/core/scenario_instances/master_results.csv";

    static String[] solutions = {
            // "BALANCED"
            // "HTM",
            // "TSP",
            "VND"
            // "VND_MIN"
    };

    static double[] probs = {0.25};
    static int[] penalties = {3};

    public static void main(String[] args) throws Exception {

        String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
        String travelDayPath = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";

        double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
        double[][] travelTimesDay = Utils.readTravelTimes(travelDayPath);

        PrintWriter writer = new PrintWriter(new File(outputCSV));

        writer.println("solution,season,prob,penalty,avg_totalOT,numOT,avg_totalOT15,numOT15,avg_numMoves,avg_increaseTT");

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
                    if (pen == 10 && prob == 1.0) continue;
                    int numOT = 0;
                    int numOT15 = 0;
                    double sumOT = 0;
                    double sumOT15 = 0;
                    double sumMoves = 0;
                    double sumIncTT = 0;

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
                        sumIncTT += r.increaseTT;
                    }

                    double avgOT = sumOT / 100;
                    double avgOT15 = sumOT15 / 100;
                    double avgMoves = sumMoves / 100;
                    double avgIncTT = sumIncTT / 100;

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
                        Utils.readInstance(instancePath, "feasible", "Type_halte");

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

        double new_tt = Utils.totalTravelTime(initial);

        return new Result(
                totalOT,
                totalOT15,
                numMoves,
                new_tt - initial_tt,
                OTincurred,
                OT15incurred
        );
    }

    static class Result{

        double totalOT;
        double totalOT15;
        double numMoves;
        double increaseTT;
        int OTincurred;
        int OT15incurred;
        
        Result(double ot, double ot15, double moves, double incTT, int otIn, int ot15In){
            totalOT = ot;
            totalOT15 = ot15;
            numMoves = moves;
            increaseTT = incTT;
            OTincurred = otIn;
            OT15incurred = ot15In;
        }
    }
}