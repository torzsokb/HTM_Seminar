package neighborhoods;

import core.HTMInstance;
import core.Shift;
import search.*;

import java.util.ArrayList;
import java.util.List;

public class IntraSwap implements Neighborhood {
    private static final double EPS = 1e-6;

    @Override
    public List<Move> generateMoves(List<Shift> shifts, RouteCompatibility compatibility) {
        List<Move> moves = new ArrayList<>();

        for (int r = 0; r < shifts.size(); r++) {
            List<Integer> ids = shifts.get(r).route;
            int n = ids.size();

            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    moves.add(new Move(r, r, i, j, Move.MoveType.INTRA_SWAP));
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
            double maxShiftDuration
    ) {
        Shift s = shifts.get(move.route1);
        List<Integer> ids = s.route;
        int n = ids.size();

        int i = move.index1;
        int j = move.index2;

        int nodeI = ids.get(i);
        int nodeJ = ids.get(j);

        // Neighbors around i
        int prevI = (i == 0) ? 0 : ids.get(i - 1);
        int nextI = (i == n - 1) ? 0 : ids.get(i + 1);

        // Neighbors around j
        int prevJ = (j == 0) ? 0 : ids.get(j - 1);
        int nextJ = (j == n - 1) ? 0 : ids.get(j + 1);

        double oldCost, newCost;

        if (j == i + 1) {
            // Adjacent swap case
            oldCost =
                    travelTimes[prevI][nodeI] +
                    travelTimes[nodeI][nodeJ] +
                    travelTimes[nodeJ][nextJ];

            newCost =
                    travelTimes[prevI][nodeJ] +
                    travelTimes[nodeJ][nodeI] +
                    travelTimes[nodeI][nextJ];
        } else {
            // Non-adjacent swap
            oldCost =
                    travelTimes[prevI][nodeI] +
                    travelTimes[nodeI][nextI] +
                    travelTimes[prevJ][nodeJ] +
                    travelTimes[nodeJ][nextJ];

            newCost =
                    travelTimes[prevI][nodeJ] +
                    travelTimes[nodeJ][nextI] +
                    travelTimes[prevJ][nodeI] +
                    travelTimes[nodeI][nextJ];
        }

        double delta = oldCost - newCost;

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

        int nodeI = s.route.get(i);
        int nodeJ = s.route.get(j);

        // Swap
        s.route.set(i, nodeJ);
        s.route.set(j, nodeI);

        return newShifts;
    }
}
