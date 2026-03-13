package search;

import core.HTMInstance;
import core.Shift;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import core.Utils;



public class LocalSearch {
    private final List<Neighborhood> neighborhoods;
    private final AcceptanceFunction acceptanceFunction;
    private final RouteCompatibility compatibility;
    private final ImprovementChoice improvementChoice;
    private final int maxIterations;
    private final double maxShiftDuration;
    private final ObjectiveFunction objectiveFunction;
    private boolean useSimulatedAnnealing;


    public LocalSearch(
            List<Neighborhood> neighborhoods,
            AcceptanceFunction acceptanceFunction,
            RouteCompatibility compatibility,
            ImprovementChoice improvementChoice,
            int maxIterations,
            double maxShiftDuration,
            ObjectiveFunction objectiveFunction,
            boolean useSimulatedAnnealing
    ) {
        this.neighborhoods = neighborhoods;
        this.acceptanceFunction = acceptanceFunction;
        this.compatibility = compatibility;
        this.improvementChoice = improvementChoice;
        this.maxIterations = maxIterations;
        this.maxShiftDuration = maxShiftDuration;
        this.objectiveFunction = objectiveFunction;
        this.useSimulatedAnnealing = useSimulatedAnnealing;
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
            
            if (useSimulatedAnnealing) {
                Acceptance.updateTemperature(iteration);
                //System.out.println("Temperature: " + Acceptance.getTemperature());
            }

            improved = false;

            if (useSimulatedAnnealing) {
                Random rnd = new Random(10);
                Collections.shuffle(neighborhoods, rnd);
            }
            for (Neighborhood n : neighborhoods) {

                List<Move> moves = n.generateMoves(shifts, compatibility, instance);
        
                if (useSimulatedAnnealing) {
                    Collections.shuffle(moves, new Random(iteration));
                }

                Move bestMove = null;
                double bestImprovement = Double.NEGATIVE_INFINITY;
        
                for (Move m : moves) {
                    Evaluation eval = n.evaluateMove(
                            m,
                            shifts,
                            instance,
                            travelTimes,
                            maxShiftDuration,
                            objectiveFunction
                    );
        
                    if (!eval.feasible) continue;
                    double improvement = eval.improvement;

                    if (!acceptanceFunction.accept(improvement)) continue;

                    // FIRST improvement
                    if (improvementChoice == ImprovementChoice.FIRST) {
                        // System.out.println("Neighborhood: " + n.getClass().getSimpleName());
                        // System.out.println("Improvement of iteration " + iteration + ": " + improvement);
                        
                        shifts = n.applyMove(m, shifts, instance, travelTimes, travelTimes);
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
        
                    shifts = n.applyMove(bestMove, shifts, instance, travelTimes, travelTimes);
                    Utils.recomputeAllShifts(shifts, instance, travelTimes);
                    improved = true;
                }

                if (iteration % 100 == 0) {
                    // System.out.println("Objective at iteration " + iteration + " is: " + Utils.totalObjective(shifts));
                }
        
                if (improved) break; 
            }
        }
        
        return shifts;
    }

    public List<Shift> runDiffTimes(
            List<Shift> initialShifts,
            HTMInstance instance,
            double[][] travelTimesNight,
            double[][] travelTimesDay
    ) {
        List<Shift> shifts = new ArrayList<>(initialShifts);
        boolean improved = true;
        int iteration = 0;
        List<Double> allTemperatures = new ArrayList<>();
        List<Double> allObjectives = new ArrayList<>();

        while (improved && iteration < maxIterations) {
            iteration++;
            if (useSimulatedAnnealing) {
                Acceptance.updateTemperature(iteration);
                // System.out.println("Temperature: " + Acceptance.getTemperature());
                allTemperatures.add(Acceptance.getTemperature());
            }

            improved = false;

            if (useSimulatedAnnealing) {
                Random rnd = new Random(1000*maxIterations+iteration);
                Collections.shuffle(neighborhoods, rnd);
            }
            for (Neighborhood n : neighborhoods) {

                List<Move> moves = n.generateMoves(shifts, compatibility, instance);

                
                if (useSimulatedAnnealing) {
                    Collections.shuffle(moves, new Random(iteration));
                }
        
                Move bestMove = null;
                double bestImprovement = Double.NEGATIVE_INFINITY;
        
                for (Move m : moves) {
                    Evaluation eval = n.evaluateMoveDiffTimes(
                            m,
                            shifts,
                            instance,
                            travelTimesNight,
                            travelTimesDay,
                            maxShiftDuration,
                            objectiveFunction
                    );
        
                    if (!eval.feasible) continue;
                    double improvement = eval.improvement;

                    if (!acceptanceFunction.accept(improvement)) continue;

                    // FIRST improvement
                    if (improvementChoice == ImprovementChoice.FIRST) {
                        // System.out.println("Neighborhood: " + n.getClass().getSimpleName());
                        // System.out.println("Improvement of iteration " + iteration + ": " + improvement);
                        
                        shifts = n.applyMove(m, shifts, instance, travelTimesNight, travelTimesDay);
                        Utils.recomputeAllShiftsDiffTimes(shifts, instance, travelTimesNight, travelTimesDay);
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
        
                    shifts = n.applyMove(bestMove, shifts, instance, travelTimesNight, travelTimesDay);
                    Utils.recomputeAllShiftsDiffTimes(shifts, instance, travelTimesNight, travelTimesDay);
                    improved = true;
                }

                if (iteration % 100 == 0) {
                    // System.out.println("Objective at iteration " + iteration + " is: " + Utils.totalObjective(shifts));
                }

                allObjectives.add(Utils.totalObjective(shifts));
        
                if (improved) break; 
            }
        
        }
        if (useSimulatedAnnealing) {
            try {
                temperaturesToFile(allTemperatures, "src/results/results_SA_feasible_alltemps.txt");

                temperaturesToFile(allObjectives, "src/results/results_SA_feasible_allobj.txt");
            } catch (IOException e) {
                throw new UncheckedIOException("\nFailed to temperatures to a file", e);
            }
        }
        return shifts;
    }


public static void temperaturesToFile(List<Double> values, String filename) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
        for (Double d : values) {
            writer.write(d.toString());
            writer.newLine();
        }
    }

    System.out.println("Wrote temperatures to file " + filename);
    }
}
