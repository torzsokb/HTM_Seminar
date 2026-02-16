package solve; 
import core.*; 
import neighborhoods.*; 
import search.*;
import java.util.ArrayList; 
import java.util.Arrays; 
import java.util.List;

public class SolveLocalSearch {

    public static void main(String[] args) throws Exception {
        String instancePath = "src/core/data_all.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        List<Integer> nightIdx = Utils.getAllowedIndices(instance, 1);
        List<Integer> dayIdx   = Utils.getAllowedIndices(instance, 0);

        double shiftLength = 7*60;

        List<Shift> nightShifts = Utils.buildGreedyShifts(instance, travelTimes, nightIdx, 1, shiftLength);
        List<Shift> dayShifts   = Utils.buildGreedyShifts(instance, travelTimes, dayIdx, 0, shiftLength);

        List<Shift> initial = new ArrayList<>();
        initial.addAll(nightShifts);
        initial.addAll(dayShifts);

        double initial_obj_value = Utils.totalObjective((initial));

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
        
        // Acceptance.initSimulatedAnnealing(6000.0, 0.99);
        // AcceptanceFunction acceptSA = Acceptance.simulatedAnnealing();

        RouteCompatibility compatibility = Compatibility.sameNightShift();

        int new_shiftlength = 7*60;
        LocalSearch ls = new LocalSearch(
                neighborhoods,
                acceptGreedy,
                compatibility,
                ImprovementChoice.FIRST,   
                1000,       
                new_shiftlength
        );
        long startTime = System.currentTimeMillis();
        System.out.println("Running local search...");
        List<Shift> improved = ls.run(initial, instance, travelTimes);

        Utils.recomputeAllShifts(improved, instance, travelTimes);

        double new_obj_value = Utils.totalObjective(improved) + 50;

        System.out.println("\nLocal search complete.");

        System.out.println("New objective value: " + new_obj_value);

        double improvement = initial_obj_value - new_obj_value;

        System.out.println("Improvement: " + improvement);
        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime-startTime)/1000.0;
        System.out.println("Time taken: " + (timeTaken) + " s" );

        // Utils.printShiftStatistics(improved, instance, new_shiftlength);

        // Utils.checkFeasibility(improved, instance, new_shiftlength);


        // for (int r = 0; r < improved.size(); r++) {
        //     Shift s = improved.get(r);
        //     System.out.println("Route " + r + ": " + s.route + " | total=" + s.totalTime);
        // }
    }
}
