package milp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import search.*;
import neighborhoods.*;
import core.*;

public class StartingSolution {
    static final double shiftLength = 7*60;
    static final double totalShiftLength = 8*60;

    public static List<Shift> startingSolution() throws Exception {
        String instancePath = "src/core/data_all.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        List<Integer> nightIdx = Utils.getAllowedIndices(instance, 1);
        List<Integer> dayIdx   = Utils.getAllowedIndices(instance, 0);

        List<Shift> nightShifts = Utils.buildGreedyShifts(instance, travelTimes, nightIdx, 1, shiftLength);
        List<Shift> dayShifts   = Utils.buildGreedyShifts(instance, travelTimes, dayIdx, 0, shiftLength);

        List<Shift> initial = new ArrayList<>();
        initial.addAll(nightShifts);
        initial.addAll(dayShifts);

        //ObjectiveFunction objectiveFunction = Objective.balancedObj(0.05, 0.05);
        ObjectiveFunction objectiveFunction = Objective.totalLength();

        double initial_obj_value = objectiveFunction.shifts(initial)/60.0;

        System.out.println("Initial solution built:");
        System.out.println("Night shifts: " + nightShifts.size());
        System.out.println("Day shifts:   " + dayShifts.size());
        System.out.println("Total shifts: " + initial.size());
        System.out.println("Total objective value: " + initial_obj_value);

        List<Neighborhood> neighborhoods = Arrays.asList(
            new IntraSwap(),
            new IntraShift(),
            new InterShift(),
            new InterSwap(),
            new Intra2Opt(),
            new Inter2OptStar()
        );

        AcceptanceFunction acceptGreedy = Acceptance.greedy();

        RouteCompatibility compatibility = Compatibility.sameNightShift();

        LocalSearch ls = new LocalSearch(
                neighborhoods,
                acceptGreedy,
                compatibility,
                ImprovementChoice.BEST,
                10000,       
                totalShiftLength,
                objectiveFunction
        );
        long startTime = System.currentTimeMillis();
        System.out.println("Running local search...");
        List<Shift> improved = ls.run(initial, instance, travelTimes);

        Utils.recomputeAllShifts(improved, instance, travelTimes);

        double new_obj_value = objectiveFunction.shifts(improved)/60.0;

        System.out.println("\nLocal search complete.");

        System.out.println("New objective value: " + new_obj_value);

        double improvement = initial_obj_value - new_obj_value;

        System.out.println("Improvement: " + improvement);
        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime-startTime)/1000.0;
        System.out.println("Time taken: " + (timeTaken) + " s" );

        Utils.printShiftStatistics(improved, instance, totalShiftLength);

        Acceptance.initSimulatedAnnealing(100.0, 0.98);
        AcceptanceFunction acceptSA = Acceptance.simulatedAnnealing();

        // LocalSearch ls_SA = new LocalSearch(
        //     neighborhoods,
        //     acceptSA,
        //     compatibility,
        //     ImprovementChoice.FIRST,
        //     1000,       
        //     totalShiftLength,
        //     objectiveFunction
        // );
        // List<Shift> improved_SA = ls_SA.run(improved, instance, travelTimes);
        
        return improved;
}

}
