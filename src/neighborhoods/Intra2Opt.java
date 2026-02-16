package neighborhoods;

import core.HTMInstance;
import core.Shift;
import search.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Intra2Opt implements Neighborhood {
    private static final double EPS = 1e-6;

    @Override
    public List<Move> generateMoves(List<Shift> shifts, RouteCompatibility compatibility) {
        List<Move> moves = new ArrayList<>();

        for (int r = 0; r < shifts.size(); r++) {
            List<Integer> ids = shifts.get(r).route;
            int n = ids.size();

            if (n < 3) continue;

            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    moves.add(new Move(r, r, i, j, Move.MoveType.INTRA_2OPT));
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

        if (n < 3) {
            return new Evaluation(0, false);
        }

        int a = (i == 0) ? 0 : ids.get(i - 1);
        int b = ids.get(i);
        int c = ids.get(j);
        int d = (j == n - 1) ? 0 : ids.get(j + 1);

        // External edges
        double deltaExt =
                travelTimes[a][b] +
                travelTimes[c][d] -
                travelTimes[a][c] -
                travelTimes[b][d];

        // Internal edges (forward vs reversed)
        double origInternal = 0.0;
        double revInternal = 0.0;

        for (int k = i; k < j; k++) {
            int u = ids.get(k);
            int v = ids.get(k + 1);
            origInternal += travelTimes[u][v];
            revInternal += travelTimes[v][u];
        }

        double deltaInternal = origInternal - revInternal;
        double delta = deltaExt + deltaInternal;

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

        // Reverse the segment [i, j]
        Collections.reverse(s.route.subList(i, j + 1));

        return newShifts;
    }
}
