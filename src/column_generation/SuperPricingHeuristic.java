package column_generation;

import milp.*;
import core.*;
import search.*;

import java.io.IOException;
import java.util.*;

public class SuperPricingHeuristic implements RCESPP {

    private PricingHeuristic localSearchHeur;
    private RolloutHeur rolloutHeur;
    private double maxShiftDuration;
    private double minShiftDuration;
    private int maxShifts;
    private List<Neighborhood> neighborhoods;
    private AcceptanceFunction acceptFunction;
    private RouteCompatibility compatibility;
    private HTMInstance instance;

    private static final double HIGH_DUAL_THRESHOLD = 40.0;

    public SuperPricingHeuristic(double maxShiftDuration, double minShiftDuration, int maxShifts,
                                 List<Neighborhood> neighborhoods,
                                 AcceptanceFunction acceptFunction,
                                 RouteCompatibility compatibility,
                                 HTMInstance instance) {

        this.maxShiftDuration = maxShiftDuration;
        this.minShiftDuration = minShiftDuration;
        this.maxShifts = maxShifts;
        this.neighborhoods = neighborhoods;
        this.acceptFunction = acceptFunction;
        this.compatibility = compatibility;
        this.instance = instance;

        this.localSearchHeur = new PricingHeuristic(maxShiftDuration, minShiftDuration, maxShifts,
                neighborhoods, acceptFunction, compatibility, instance);

        this.rolloutHeur = new RolloutHeur(100, 10, 10, 10);
    }

    @Override
    public List<Shift> getNewShifts(double[][] distances, List<Stop> stops, double[] duals,
                                    double maxDuration, double minDuration) {

        // Count high duals (excluding depot at duals[0])
        int countHighDuals = 0;
        double highestDual = 0;
        for (int i = 1; i < duals.length; i++) {
            if (duals[i] > HIGH_DUAL_THRESHOLD) countHighDuals++;
            if (duals[i] > highestDual) highestDual = duals[i];
        }
        System.out.println("Number of high duals: " + countHighDuals);
        System.out.println("Highest dual: " + highestDual);

        try {
            if (countHighDuals > 0) {
                System.out.println("Using Rollout Heuristic");
                return rolloutHeur.getNewShifts(distances, stops, duals, maxDuration, minDuration);

            } else {
                System.out.println("Using spamming Heuristic");
                return localSearchHeur.generateShifts(stops, distances, duals);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error generating shifts", e);
        }
    }
}
