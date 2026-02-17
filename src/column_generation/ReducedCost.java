package column_generation;

import java.util.List;
import core.*;

public class ReducedCost {

    /**
     * Compute reduced cost matrix from travel time matrix and duals.
     * @param travelTimes original travel times
     * @param duals 1D array of duals, duals[0] = number of shifts, duals[1..N] = node duals
     * @return reduced cost matrix
     */
    public static double[][] computeReducedCost(double[][] travelTimes, double[] duals) {
        int n = travelTimes.length;
        double[][] reducedCosts = new double[n][n];
    
        for (int i = 0; i < n; i++) {
            double nodeDual = duals[i + 1]; // skip first dual (shift dual)
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    reducedCosts[i][j] = Double.POSITIVE_INFINITY; // no self loops
                } else {
                    reducedCosts[i][j] = travelTimes[i][j] - nodeDual;
                }
            }
        }
        return reducedCosts;
    }

    public static double computeShiftReducedCost(
        Shift shift,
        double[][] reducedCosts,
        double shiftDual) {

    double cost = -shiftDual;   // subtract shift dual once

    List<Integer> r = shift.route;

    for (int i = 0; i < r.size() - 1; i++) {
        cost += reducedCosts[r.get(i)][r.get(i + 1)];
    }

    return cost;
}

}
