package search;

import java.util.Random;

public class Acceptance {

    private static double startTemp;
    private static double endTemp;
    private static int maxIterations;
    private static int nPeriods;

    private static double temperature;

    private static final Random rnd = new Random(10);

    public static void initSimulatedAnnealing(
            double start,
            double end,
            int maxIters,
            int periods

    ) {
        startTemp = start;
        endTemp = end;
        maxIterations = maxIters;
        nPeriods = periods;
        temperature = startTemp;
    }

    public static AcceptanceFunction simulatedAnnealing() {
        return (improvement) -> {
            if (improvement > 0) return true;
            if (improvement == 0.0) return false;

            double prob = Math.exp(improvement / temperature);
            return rnd.nextDouble() < prob;
        };
    }

    public static void updateTemperature(int iteration) {

        double v = iteration;
        double vLNS = maxIterations;
    
        // Linear component
        double T_linear = startTemp +
                (endTemp - startTemp) * (v / vLNS);
    
        // Oscillating component
        double T_osc = startTemp +
                (endTemp - startTemp) *
                Math.pow(
                    Math.sin(Math.PI * nPeriods * v / vLNS),
                    2
                );
    
        // Combined temperature
        temperature = Math.sqrt(T_linear * T_osc);
    }

    public static double getTemperature() {
        return temperature;
    }

    public static AcceptanceFunction greedy() {
        return (improvement) -> improvement > 0;
    }
    
    public static AcceptanceFunction alwaysTrue() {
        return (improvement) -> true;
    }
}
