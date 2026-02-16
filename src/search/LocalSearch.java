package search;

import core.HTMInstance;
import core.Shift;

import java.util.ArrayList;
import java.util.List;
import core.Utils;

public class LocalSearch {
    private final List<Neighborhood> neighborhoods;
    private final AcceptanceFunction acceptanceFunction;
    private final RouteCompatibility compatibility;
    private final ImprovementChoice improvementChoice;
    private final int maxIterations;
    private final double maxShiftDuration;

    public LocalSearch(
            List<Neighborhood> neighborhoods,
            AcceptanceFunction acceptanceFunction,
            RouteCompatibility compatibility,
            ImprovementChoice improvementChoice,
            int maxIterations,
            double maxShiftDuration
    ) {
        this.neighborhoods = neighborhoods;
        this.acceptanceFunction = acceptanceFunction;
        this.compatibility = compatibility;
        this.improvementChoice = improvementChoice;
        this.maxIterations = maxIterations;
        this.maxShiftDuration = maxShiftDuration;
    }

    public List<Shift> run(
            List<Shift> initialShifts,
            HTMInstance instance,
            double[][] travelTimes
    ) {
        List<Shift> shifts = new ArrayList<>(initialShifts);
        boolean improved = true;
        int iteration = 0;

        while (improved && iteration < maxIterations) {
            iteration++;
            improved = false;
        
            for (Neighborhood n : neighborhoods) {
        
                List<Move> moves = n.generateMoves(shifts, compatibility);
        
                Move bestMove = null;
                double bestImprovement = 0.0;
        
                for (Move m : moves) {
                    Evaluation eval = n.evaluateMove(
                            m,
                            shifts,
                            instance,
                            travelTimes,
                            maxShiftDuration
                    );
        
                    if (!eval.feasible) continue;
        
                    double improvement = eval.improvement;
        
                    if (!acceptanceFunction.accept(improvement)) continue;

                    // FIRST improvement
                    if (improvementChoice == ImprovementChoice.FIRST) {
        
                        // System.out.println("Neighborhood: " + n.getClass().getSimpleName());
                        // System.out.println("Improvement of iteration " + iteration + ": " + improvement);
        
                        shifts = n.applyMove(m, shifts, instance, travelTimes);
                        Utils.recomputeAllShifts(shifts, instance, travelTimes);
                        improved = true;
                        break;
                    }
        
                    if (improvementChoice == ImprovementChoice.BEST) {
                        if (improvement > bestImprovement) {
                            bestImprovement = improvement;
                            bestMove = m;
                        }
                    }
                }
        
                if (improvementChoice == ImprovementChoice.BEST && bestMove != null) {
                    // System.out.println("Neighborhood: " + n.getClass().getSimpleName());
                    // System.out.println("Improvement of iteration " + iteration + ": " + bestImprovement);
        
                    shifts = n.applyMove(bestMove, shifts, instance, travelTimes);
                    Utils.recomputeAllShifts(shifts, instance, travelTimes);
                    improved = true;
                }
        
                if (improved) break; // stop after first improving neighborhood
            }
            // ← Add the block here, after finishing this iteration of neighborhoods
            if (acceptanceFunction == Acceptance.simulatedAnnealing()) {
                Acceptance.coolDown();
                System.out.println("Temperature after iteration " + iteration + ": " + Acceptance.getTemperature());
            }
        }
        
        return shifts;
    }
}
