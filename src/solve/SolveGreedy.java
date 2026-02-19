package solve;
import core.*;

import java.util.ArrayList;
import java.util.List;

public class SolveGreedy {
    static final double shiftLength = 7 * 60;
    static final double totalShiftLength = 8 * 60;

    public static void main(String[] args) throws Exception {
        String instancePath = "src/core/data_all.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        List<Integer> nightIdx = Utils.getAllowedIndices(instance, 1);
        List<Integer> dayIdx   = Utils.getAllowedIndices(instance, 0);

        List<Shift> nightShifts = Utils.buildGreedyShifts(instance, travelTimes, nightIdx, 1, shiftLength);
        List<Shift> dayShifts   = Utils.buildGreedyShifts(instance, travelTimes, dayIdx, 0, shiftLength);

        List<Shift> greedy = new ArrayList<>();
        greedy.addAll(nightShifts);
        greedy.addAll(dayShifts);

        // Print and save results
        Utils.printShiftStatistics(greedy, instance, totalShiftLength);
        Utils.checkFeasibility(greedy, instance, totalShiftLength);

        double obj = Utils.totalObjective(greedy);
        System.out.println("Total objective value: " + obj);
    }
}


