package neighborhoods;

import core.HTMInstance;
import core.Shift;
import core.Utils;
import search.*;

import java.util.ArrayList;
import java.util.List;

public class Inter2OptStar implements Neighborhood {

    private static final double EPS = 1e-6;
    private final int MAX_NIGHT_SHIFTS = 25;

    public final double breakTime = 30.0;
    public final double prepTime = 30.0;

    @Override
    public List<Move> generateMoves(List<Shift> shifts, RouteCompatibility compatibility) {
        List<Move> moves = new ArrayList<>();
        int numNightShifts = Utils.countNightShifts(shifts);

        if (numNightShifts > MAX_NIGHT_SHIFTS) {
            System.out.println("Too many night shifts: " + numNightShifts);
            return moves;
        }
        
        for (int r1 = 0; r1 < shifts.size(); r1++) {
            for (int r2 = r1 + 1; r2 < shifts.size(); r2++) {
                if (r1 == r2) continue;
                Shift s1 = shifts.get(r1);
                Shift s2 = shifts.get(r2);

                if (numNightShifts == MAX_NIGHT_SHIFTS && !compatibility.compatible(s1, s2)) {
                    continue;
                }

                List<Integer> ids1 = shifts.get(r1).route;
                List<Integer> ids2 = shifts.get(r2).route;

                if (ids1.size() < 2 || ids2.size() < 2) continue;

                for (int i = 0; i < ids1.size() - 2; i++) {
                    for (int j = 0; j < ids2.size() - 2; j++) {
                        moves.add(new Move(r1, r2, i, j, Move.MoveType.INTER_2OPT_STAR));
                    }
                }
                
            }
        }
        return moves;
    }

    @Override
    
    public Evaluation evaluateMove(
            Move move,
            List<Shift> shifts,
            HTMInstance instance,
            double[][] travelTimes,
            double maxShiftDuration,
            ObjectiveFunction objectiveFunction
    ) {
        Shift s1 = shifts.get(move.route1);
        Shift s2 = shifts.get(move.route2);

        List<Integer> r1 = s1.route;
        List<Integer> r2 = s2.route;

        int i = move.index1;
        int j = move.index2;

        // Build hypothetical new routes (same logic as applyMove)
        List<Integer> newR1 = new ArrayList<>();
        newR1.addAll(r1.subList(0, i + 1));
        newR1.addAll(r2.subList(j + 1, r2.size()));

        List<Integer> newR2 = new ArrayList<>();
        newR2.addAll(r2.subList(0, j + 1));
        newR2.addAll(r1.subList(i + 1, r1.size()));

        // The tail logic was not working and giving me too many bugs so I naively recompute the whole shift duration
        double newDur1 = computeRouteDuration(newR1, instance, travelTimes);
        double newDur2 = computeRouteDuration(newR2, instance, travelTimes);

        if (newDur1 > maxShiftDuration || newDur2 > maxShiftDuration)
            return new Evaluation(0, false);

        // Improvement
        double oldTotal = s1.totalTime + s2.totalTime;
        double newTotal = newDur1 + newDur2;
        double improvement = oldTotal - newTotal;

        if (Math.abs(improvement) < EPS) improvement = 0.0;

        return new Evaluation(improvement, true);
    }

    @Override
    public List<Shift> applyMove(
            Move move,
            List<Shift> shifts,
            HTMInstance instance,
            double[][] travelTimes
    ) {
        List<Shift> newShifts = new ArrayList<>(shifts);

        Shift s1 = newShifts.get(move.route1);
        Shift s2 = newShifts.get(move.route2);

        List<Integer> r1 = s1.route;
        List<Integer> r2 = s2.route;

        int i = move.index1;
        int j = move.index2;

        // Build new routes
        List<Integer> newR1 = new ArrayList<>();
        newR1.addAll(r1.subList(0, i + 1));
        newR1.addAll(r2.subList(j + 1, r2.size()));

        List<Integer> newR2 = new ArrayList<>();
        newR2.addAll(r2.subList(0, j + 1));
        newR2.addAll(r1.subList(i + 1, r1.size()));

        s1.route = newR1;
        s2.route = newR2;

        s1.totalTime = computeRouteDuration(newR1, instance, travelTimes);
        s2.totalTime = computeRouteDuration(newR2, instance, travelTimes);

        s1.nightShift = Utils.containsNightStop(newR1, instance) ? 1 : 0;
        s2.nightShift = Utils.containsNightStop(newR2, instance) ? 1 : 0;

        return newShifts;
    }

    
    private double computeRouteDuration(
            List<Integer> route,
            HTMInstance instance,
            double[][] travelTimes
    ) {
        double t = 0;
    
        // Depot → first
        t += travelTimes[0][route.get(0)];
    
        // Internal arcs
        for (int i = 0; i < route.size() - 1; i++)
            t += travelTimes[route.get(i)][route.get(i + 1)];
    
        // Last → depot
        t += travelTimes[route.get(route.size() - 1)][0];
    
        // Service times
        for (int id : route)
            t += instance.getStops().get(id).serviceTime;

        // Add break and prep time
        t = t + breakTime + prepTime;
    
        return t;
    }
}
