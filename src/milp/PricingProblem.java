package milp;

import column_generation.*;

import java.io.IOException;
import java.util.*;
import core.*;

public class PricingProblem implements RCESPP {

    private final PricingHeuristic heuristic;

    public PricingProblem(PricingHeuristic heuristic) {
        this.heuristic = heuristic;
    }

    @Override
    public List<Shift> getNewShifts(
        double[][] travelTimes, List<Stop> stops, double[] duals, double maxDuration,
        double minDuration) {
            List<Shift> newShifts = new ArrayList<>();
            try {
                newShifts = heuristic.generateShifts(
                    stops,
                    travelTimes,
                    duals
      );
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        return newShifts;
    }

    // @Override
    // public List<Shift> getNewShifts(double[][] distances, List<Stop> stops, double[] duals, double maxDuration,
    //         double minDuration) {
    //     // TODO Auto-generated method stub
    //     throw new UnsupportedOperationException("Unimplemented method 'getNewShifts'");
    // }
}
