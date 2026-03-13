package solve;

import core.HTMInstance;
import core.Shift;
import core.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import neighborhoods.Inter2OptStar;
import neighborhoods.InterShift;
import neighborhoods.InterSwap;
import neighborhoods.Intra2Opt;
import neighborhoods.IntraShift;
import neighborhoods.IntraSwap;
import search.Compatibility;
import search.Evaluation;
import search.Move;
import search.Neighborhood;
import search.Objective;
import search.ObjectiveFunction;
import search.RouteCompatibility;

public class BestKMovesSearchFeas {

    static final double REGULAR_SHIFT_LENGTH = 8 * 60;       // 480
    static final double TOTAL_SHIFT_LENGTH   = 8 * 60 + 15;  // 495
    static final double EPS = 1e-9;
    public static void main(String[] args) throws Exception {
        String instancePath = "src/core/data_all_feas_typeHalte.txt";
        String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
        String travelDayPath   = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";

        String initialShiftsPath = "src/results/HTM_data_initRes_typeHalte.csv";
        String outputPath        = "src/results/best_k_moves_result.csv";

        // -------------------------------------------------
        // CHOOSE HERE
        // -------------------------------------------------
        int k = 5;

        List<String> selectedNeighborhoodNames = Arrays.asList(
                "inter_shift",
                "intra_shift",
                "inter_swap",
                "intra_swap",
                "intra_2opt",
                "inter_2opt_star"
        );
        // -------------------------------------------------

        HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Night_shift");
        double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
        double[][] travelTimesDay   = Utils.readTravelTimes(travelDayPath);

        List<Shift> shifts = Utils.readShiftsFromCSVDiffTimes(
                initialShiftsPath, travelTimesNight, travelTimesDay
        );

        // Keep this removed if you want to evaluate the actual input routes:
        // Utils.makeFeasible(shifts, instance, travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(shifts, instance, travelTimesNight, travelTimesDay);

        System.out.println("Initial solution:");
        System.out.println("Total shifts: " + shifts.size());
        System.out.println("Total objective value (hours): " + totalLengthHours(shifts));
        Utils.checkFeasibility(shifts, instance, TOTAL_SHIFT_LENGTH);

        List<Neighborhood> neighborhoods = buildNeighborhoods(selectedNeighborhoodNames);
        RouteCompatibility compatibility = Compatibility.sameNightShift();

        double initialObjectiveMinutes = totalLengthMinutes(shifts);
        double cumulativeImprovement = 0.0;

        List<MoveSummary> summaries = new ArrayList<>();

        for (int iter = 1; iter <= k; iter++) {
            BestMoveResult best = findBestMove(
                    shifts,
                    neighborhoods,
                    compatibility,
                    instance,
                    travelTimesNight,
                    travelTimesDay,
                    TOTAL_SHIFT_LENGTH
            );

            if (best == null || !best.evaluation.feasible || best.evaluation.improvement <= EPS) {
                System.out.println("\nNo further improving feasible move found.");
                break;
            }

            double oldObjectiveMinutes = totalLengthMinutes(shifts);

            System.out.println("\n===== MOVE " + iter + " =====");
            printMoveDetails(best, shifts);

            shifts = best.neighborhood.applyMove(
                    best.move,
                    shifts,
                    instance,
                    travelTimesNight,
                    travelTimesDay
            );

            Utils.recomputeAllShiftsDiffTimes(shifts, instance, travelTimesNight, travelTimesDay);

            double newObjectiveMinutes = totalLengthMinutes(shifts);
            double actualImprovementMinutes = oldObjectiveMinutes - newObjectiveMinutes;
            cumulativeImprovement += actualImprovementMinutes;

            summaries.add(new MoveSummary(
                    iter,
                    best.neighborhood.getClass().getSimpleName(),
                    actualImprovementMinutes,
                    cumulativeImprovement,
                    newObjectiveMinutes
            ));

            System.out.println("Actual improvement this move: " + actualImprovementMinutes + " min");
            System.out.println("Actual improvement this move: " + (actualImprovementMinutes / 60.0) + " h");
            System.out.println("Cumulative improvement: " + cumulativeImprovement + " min");
            System.out.println("Cumulative improvement: " + (cumulativeImprovement / 60.0) + " h");
            System.out.println("New total objective: " + newObjectiveMinutes + " min");
            System.out.println("New total objective: " + (newObjectiveMinutes / 60.0) + " h");
        }

        System.out.println("\n===== FINAL SOLUTION AFTER SEQUENTIAL MOVES =====");
        System.out.println("Initial total objective: " + initialObjectiveMinutes + " min");
        System.out.println("Final total objective:   " + totalLengthMinutes(shifts) + " min");
        System.out.println("Total improvement:      " + (initialObjectiveMinutes - totalLengthMinutes(shifts)) + " min");
        System.out.println("Total improvement:      " + ((initialObjectiveMinutes - totalLengthMinutes(shifts)) / 60.0) + " h");

        Utils.checkFeasibility(shifts, instance, TOTAL_SHIFT_LENGTH);
        Utils.printShiftStatistics(shifts, instance, TOTAL_SHIFT_LENGTH);

        Utils.resultsToCSV(shifts, instance, outputPath);
        System.out.println("\nSaved result to: " + outputPath);

        printMoveSummaryTable(summaries);
    }

    public static BestMoveResult findBestMove(
            List<Shift> shifts,
            List<Neighborhood> neighborhoods,
            RouteCompatibility compatibility,
            HTMInstance instance,
            double[][] travelTimesNight,
            double[][] travelTimesDay,
            double maxShiftDuration
    ) {
        BestMoveResult best = null;

        ObjectiveFunction objective = Objective.balancedObj(0.0, 0.0);

        for (Neighborhood neighborhood : neighborhoods) {
            List<Move> moves = neighborhood.generateMoves(shifts, compatibility);

            // System.out.println("\nChecking neighborhood: " + neighborhood.getClass().getSimpleName());
            // System.out.println("Number of candidate moves: " + moves.size());

            for (Move move : moves) {
                Evaluation eval = neighborhood.evaluateMoveDiffTimes(
                        move,
                        shifts,
                        instance,
                        travelTimesNight,
                        travelTimesDay,
                        maxShiftDuration,
                        objective
                );

                if (!eval.feasible) {
                    continue;
                }

                // Delete this if 15 minutes is allowed for more shifts (way faster that way)
                if (!isMoveAllowedUnder480Rule(
                        shifts,
                        neighborhood,
                        move,
                        instance,
                        travelTimesNight,
                        travelTimesDay)) {
                    continue;
                }

                if (best == null || eval.improvement > best.evaluation.improvement + EPS) {
                    best = new BestMoveResult(neighborhood, move, eval);
                }
            }
        }

        return best;
    }

    public static List<Neighborhood> buildNeighborhoods(List<String> names) {
        List<Neighborhood> neighborhoods = new ArrayList<>();

        for (String rawName : names) {
            String name = rawName.trim().toLowerCase();

            switch (name) {
                case "inter_shift":
                    neighborhoods.add(new InterShift());
                    break;
                case "inter_swap":
                    neighborhoods.add(new InterSwap());
                    break;
                case "intra_shift":
                    neighborhoods.add(new IntraShift());
                    break;
                case "intra_swap":
                    neighborhoods.add(new IntraSwap());
                    break;
                case "intra_2opt":
                    neighborhoods.add(new Intra2Opt());
                    break;
                case "inter_2opt_star":
                    neighborhoods.add(new Inter2OptStar());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown neighborhood name: " + rawName);
            }
        }

        return neighborhoods;
    }

    public static void printMoveDetails(BestMoveResult best, List<Shift> shifts) {
        Move m = best.move;

        System.out.println("Neighborhood: " + best.neighborhood.getClass().getSimpleName());
        // System.out.println("Estimated improvement: " + best.evaluation.improvement + " min");
        // System.out.println("Estimated improvement: " + (best.evaluation.improvement / 60.0) + " h");

        System.out.println("Route 1: " + m.route1);
        System.out.println("Route 2: " + m.route2);
        System.out.println("Index 1: " + m.index1);
        System.out.println("Index 2: " + m.index2);

        Shift s1 = shifts.get(m.route1);
        Shift s2 = shifts.get(m.route2);

        Integer stop1 = null;
        Integer stop2 = null;

        if (m.index1 >= 0 && m.index1 < s1.route.size()) {
            stop1 = s1.route.get(m.index1);
        }

        if (m.index2 >= 0 && m.index2 < s2.route.size()) {
            stop2 = s2.route.get(m.index2);
        }

        // if (m.index1 >= 0 && m.index1 < s1.route.size()) {
        //     System.out.println("Stop at route1/index1: " + s1.route.get(m.index1));
        // }
        // if (m.index2 >= 0 && m.index2 < s2.route.size()) {
        //     System.out.println("Stop at route2/index2: " + s2.route.get(m.index2));
        // }

        String nbh = best.neighborhood.getClass().getSimpleName();

        if (nbh.equals("InterShift")) {
            System.out.println("Move stop " + stop1 + " from shift " + m.route1 + " to shift " + m.route2 + " at position " + m.index2);
        }

        else if (nbh.equals("IntraShift")) {
            System.out.println("Move stop " + stop1 + " inside shift " + m.route1 + " from position " + m.index1 + " to position " + m.index2);
        }

        else if (nbh.equals("InterSwap")) {
            System.out.println("Swap stop " + stop1 + " (shift " + m.route1 + ")" + " with stop " + stop2 + " (shift " + m.route2 + ")");
        }

        else if (nbh.equals("IntraSwap")) {
            System.out.println("Swap stop " + stop1 + " with stop " + stop2 + " inside shift " + m.route1);
        }

        else if (nbh.equals("Intra2Opt")) {
            System.out.println("Reverse segment between positions " + m.index1 + " and " + m.index2 + " in shift " + m.route1);
        }

        else if (nbh.equals("Inter2OptStar")) {
            System.out.println("Exchange route tails of shift " +  m.route1 + " and shift " + m.route2);
        }
    }

    public static double totalLengthMinutes(List<Shift> shifts) {
        double total = 0.0;
        for (Shift s : shifts) {
            total += s.totalTime;
        }
        return total;
    }

    public static double totalLengthHours(List<Shift> shifts) {
        return totalLengthMinutes(shifts) / 60.0;
    }

    public static class BestMoveResult {
        Neighborhood neighborhood;
        Move move;
        Evaluation evaluation;

        public BestMoveResult(Neighborhood neighborhood, Move move, Evaluation evaluation) {
            this.neighborhood = neighborhood;
            this.move = move;
            this.evaluation = evaluation;
        }
    }
    public static boolean becomesOvertime(double oldLength, double newLength) {
    return oldLength <= REGULAR_SHIFT_LENGTH + EPS
            && newLength > REGULAR_SHIFT_LENGTH + EPS;
}

public static boolean isMoveAllowedUnder480Rule(
        List<Shift> shifts,
        Neighborhood neighborhood,
        Move move,
        HTMInstance instance,
        double[][] travelTimesNight,
        double[][] travelTimesDay
) {
    String nbh = neighborhood.getClass().getSimpleName();

    if (nbh.equals("IntraShift") || nbh.equals("IntraSwap") || nbh.equals("Intra2Opt")) {
        Shift s = shifts.get(move.route1);

        List<Shift> candidate = Utils.deepCopyShifts(shifts);
        candidate = neighborhood.applyMove(move, candidate, instance, travelTimesNight, travelTimesDay);
        Utils.recomputeAllShiftsDiffTimes(candidate, instance, travelTimesNight, travelTimesDay);

        double oldL = s.totalTime;
        double newL = candidate.get(move.route1).totalTime;

        return !becomesOvertime(oldL, newL);
    }

    if (nbh.equals("InterShift") || nbh.equals("InterSwap") || nbh.equals("Inter2OptStar")) {
        Shift s1 = shifts.get(move.route1);
        Shift s2 = shifts.get(move.route2);

        List<Shift> candidate = Utils.deepCopyShifts(shifts);
        candidate = neighborhood.applyMove(move, candidate, instance, travelTimesNight, travelTimesDay);
        Utils.recomputeAllShiftsDiffTimes(candidate, instance, travelTimesNight, travelTimesDay);

        double oldL1 = s1.totalTime;
        double oldL2 = s2.totalTime;
        double newL1 = candidate.get(move.route1).totalTime;
        double newL2 = candidate.get(move.route2).totalTime;

        return !becomesOvertime(oldL1, newL1) && !becomesOvertime(oldL2, newL2);
    }

    return true;
}

public static class MoveSummary {
    int iteration;
    String neighborhood;
    double improvementMinutes;
    double cumulativeImprovementMinutes;
    double objectiveMinutes;

    public MoveSummary(int iteration, String neighborhood,
                       double improvementMinutes,
                       double cumulativeImprovementMinutes,
                       double objectiveMinutes) {
        this.iteration = iteration;
        this.neighborhood = neighborhood;
        this.improvementMinutes = improvementMinutes;
        this.cumulativeImprovementMinutes = cumulativeImprovementMinutes;
        this.objectiveMinutes = objectiveMinutes;
    }
}

public static void printMoveSummaryTable(List<MoveSummary> summaries) {
    System.out.println("\n===== MOVE SUMMARY =====");
    System.out.printf("%-8s %-18s %-18s %-22s %-18s%n",
            "Move", "Neighborhood", "Improvement (min)",
            "Cumulative impr. (min)", "Objective (min)");

    for (MoveSummary s : summaries) {
        System.out.printf("%-8d %-18s %-18.3f %-22.3f %-18.3f%n",
                s.iteration,
                s.neighborhood,
                s.improvementMinutes,
                s.cumulativeImprovementMinutes,
                s.objectiveMinutes);
    }
}
}