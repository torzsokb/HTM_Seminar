package neighborhoods;

import core.HTMInstance;
import core.Shift;
import core.Stop;
import core.Utils;
import search.*;

import java.util.ArrayList;
import java.util.List;

public class InterSwap implements Neighborhood {
    private static final double EPS = 1e-6;
    private final int MAX_NIGHT_SHIFTS = 25;
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

                Shift s1 = shifts.get(r1);
                Shift s2 = shifts.get(r2);

                if (numNightShifts == MAX_NIGHT_SHIFTS && !compatibility.compatible(s1, s2)) {
                    continue;
                }

                List<Integer> ids1 = shifts.get(r1).route;
                List<Integer> ids2 = shifts.get(r2).route;

                for (int i = 0; i < ids1.size(); i++) {
                    for (int j = 0; j < ids2.size(); j++) {
                        moves.add(new Move(r1, r2, i, j, Move.MoveType.INTER_SWAP));
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

        int node1 = s1.route.get(move.index1);
        int node2 = s2.route.get(move.index2);

        Stop stop1 = instance.getStops().get(node1);
        Stop stop2 = instance.getStops().get(node2);

        double service1 = stop1.serviceTime;
        double service2 = stop2.serviceTime;

        // Neighbors in route 1
        int prev1 = (move.index1 == 0) ? 0 : s1.route.get(move.index1 - 1);
        int next1 = (move.index1 == s1.route.size() - 1) ? 0 : s1.route.get(move.index1 + 1);

        // Neighbors in route 2
        int prev2 = (move.index2 == 0) ? 0 : s2.route.get(move.index2 - 1);
        int next2 = (move.index2 == s2.route.size() - 1) ? 0 : s2.route.get(move.index2 + 1);

        // Old arcs
        double oldR1 = travelTimes[prev1][node1] + travelTimes[node1][next1];
        double oldR2 = travelTimes[prev2][node2] + travelTimes[node2][next2];

        // New arcs after swap
        double newR1 = travelTimes[prev1][node2] + travelTimes[node2][next1];
        double newR2 = travelTimes[prev2][node1] + travelTimes[node1][next2];

        double deltaR1 = oldR1 - newR1;
        double deltaR2 = oldR2 - newR2;

        double newDur1 = s1.totalTime - service1 + service2 - deltaR1;
        double newDur2 = s2.totalTime - service2 + service1 - deltaR2;

        if (newDur1 > maxShiftDuration || newDur2 > maxShiftDuration) {
            return new Evaluation(0, false);
        }

        double currentObj = s1.totalTime + s2.totalTime;
        double newObj = newDur1 + newDur2;
        double improvement = currentObj - newObj;
        // System.out.println("The improvement is " + improvement);
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

        int node1 = s1.route.get(move.index1);
        int node2 = s2.route.get(move.index2);

        // Swap the nodes
        s1.route.set(move.index1, node2);
        s2.route.set(move.index2, node1);
        s1.nightShift = Utils.containsNightStop(s1.route, instance) ? 1 : 0;
        s2.nightShift = Utils.containsNightStop(s2.route, instance) ? 1 : 0;

        return newShifts;
    }
}
