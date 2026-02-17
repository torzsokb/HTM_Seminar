package column_generation;

import core.*;
import search.*;
import neighborhoods.*;
import java.util.*;

public class MainPricingTest {

    public static void main(String[] args) throws Exception {
        
        String instancePath = "src/core/data_all.txt";
        String travelPath = "src/core/travel_times_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        int numNodes = travelTimes.length;

        double[] duals = new double[numNodes + 1]; // duals[0] = shift dual, duals[1..n] = node duals
        duals[0] = 25.0; // shift dual
        for (int i = 1; i <= numNodes; i++) duals[i] = 0.0; // dummy node duals

        // For one route you only need the intra-neighbourhoods
        List<Neighborhood> neighborhoods = Arrays.asList(
                new IntraSwap(),
                new IntraShift(),
                new Intra2Opt()
        );

        AcceptanceFunction accept = Acceptance.greedy();
        RouteCompatibility compatibility = Compatibility.sameNightShift();

        double maxShiftDuration = 7 * 60;   // 7 hours
        double minShiftDuration = 4.5 * 60; // 4.5 hours
        int maxShifts = 1000;

        long startTime = System.currentTimeMillis();
        PricingHeuristic pricing = new PricingHeuristic(
                maxShiftDuration,
                minShiftDuration,
                maxShifts,
                neighborhoods,
                accept,
                compatibility
        );

        List<Shift> nightShifts = pricing.generateShifts(instance, travelTimes, duals, 1);
        System.out.println("Generated night shifts: " + nightShifts.size());
        // for (Shift s : nightShifts) {
        //     System.out.println("Shift route: " + s.route + " | totalTime: " + s.totalTime);
        // }

        List<Shift> dayShifts = pricing.generateShifts(instance, travelTimes, duals, 0);
        System.out.println("Generated day shifts: " + dayShifts.size());
        // for (Shift s : dayShifts) {
        //     System.out.println("Shift route: " + s.route + " | totalTime: " + s.totalTime);
        // }

        List<Shift> allShifts = new ArrayList<>();
        allShifts.addAll(nightShifts);
        allShifts.addAll(dayShifts);
        long endTime = System.currentTimeMillis();

        double number_neg_rc_shifts = allShifts.size();

        System.out.println("Total negative reduced cost shifts: " + number_neg_rc_shifts);
        System.out.println("Percentage of negative reduced cost shifts: " + (number_neg_rc_shifts/maxShifts*1.0)*100);
        System.out.println("Time taken: " + (endTime-startTime)/1000.0 + " s");
    }
}
