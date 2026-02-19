package search;

import java.util.Random;

public class Acceptance {
    
    private static double temperature; 
    private static double coolingRate;  

    private static final Random rnd = new Random(10);

    public static AcceptanceFunction greedy() {
        return (improvement) -> improvement > 0;
    }
    
    public static AcceptanceFunction alwaysTrue() {
        return (improvement) -> true;
    }
    
    public static void initSimulatedAnnealing(double initialTemp, double rate) {
        temperature = initialTemp;
        coolingRate = rate;
    }

    public static AcceptanceFunction simulatedAnnealing() {
        return (improvement) -> {
            if (improvement > 0) return true;
            if (improvement == 0.0) return false;
            double prob = Math.exp(improvement / temperature);
            return rnd.nextDouble() < prob;
        };
    }

    public static void coolDown() {
        temperature *= coolingRate;
    }

    public static double getTemperature() {
        return temperature;
    }
}
