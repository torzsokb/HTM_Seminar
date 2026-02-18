package neighborhoods;

import core.HTMInstance;
import core.Shift;
import search.*;

import java.util.ArrayList;
import java.util.List;

public class IntraShift implements Neighborhood {
    private static final double EPS = 1e-6;



    @Override
    public List<Move> generateMoves(List<Shift> shifts, RouteCompatibility compatibility) {
        List<Move> moves = new ArrayList<>();

        for (int r = 0; r < shifts.size(); r++) {
            List<Integer> ids = shifts.get(r).route;
            int n = ids.size();

            if (n <= 2) continue;

            for (int i = 1; i < n - 1; i++) {          // exclude depot at 0 and n-1
                for (int j = 1; j < n - 1; j++) {      // exclude depot at 0 and n-1
                    if (i == j) continue;
                    moves.add(new Move(r, r, i, j, Move.MoveType.INTRA_SHIFT));
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
        Shift s = shifts.get(move.route1);
        List<Integer> ids = s.route;
        int n = ids.size();

        int i = move.index1;
        int j = move.index2;

        int node = ids.get(i);


        // Neighbors around removal position
        int prevI = (i == 0) ? 0 : ids.get(i - 1);
        int nextI = (i == n - 1) ? 0 : ids.get(i + 1);

        double deltaRemove =
                travelTimes[prevI][node] +
                travelTimes[node][nextI] -
                travelTimes[prevI][nextI];

        // Build list without the removed node
        List<Integer> idsRemoved = new ArrayList<>(ids);
        idsRemoved.remove(i);

        // Neighbors around insertion position
        int prevJ = (j == 0) ? 0 : idsRemoved.get(j - 1);
        int nextJ = (j == idsRemoved.size()) ? 0 : idsRemoved.get(j);

        double deltaInsert =
                travelTimes[prevJ][nextJ] -
                travelTimes[prevJ][node] -
                travelTimes[node][nextJ];

        double delta = deltaRemove + deltaInsert;

        double newDuration = s.totalTime - delta;
        if (newDuration > maxShiftDuration) {
            return new Evaluation(0, false);
        }
        
        if (Math.abs(delta) < EPS) delta = 0.0;
        return new Evaluation(delta, true);
    }

    @Override
    public List<Shift> applyMove(
            Move move,
            List<Shift> shifts,
            HTMInstance instance,
            double[][] travelTimes
    ) {
        List<Shift> newShifts = new ArrayList<>(shifts);

        Shift s = newShifts.get(move.route1);
        int i = move.index1;
        int j = move.index2;

        int node = s.route.remove(i);
        s.route.add(j, node);

        return newShifts;
    }
}
