package milp;

import column_generation.*;

import java.io.IOException;
import java.util.*;
import core.*;

public class PricingProblem {

    private PricingHeuristic heuristic;

    public PricingProblem(PricingHeuristic heuristic) {
        this.heuristic = heuristic;
    }

    public List<Shift> solve(
            HTMInstance instance,
            double[][] travelTimes,
            double[] duals,
            int nightShift) throws IOException {

        // Call heuristic pricing
        List<Shift> newColumns = heuristic.generateShifts(
                instance,
                travelTimes,
                duals,
                nightShift
        );

        return newColumns;
    }
}
