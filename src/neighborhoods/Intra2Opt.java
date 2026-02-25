package neighborhoods;

import core.HTMInstance;
import core.Shift;
import search.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Intra2Opt implements Neighborhood {
    private static final double EPS = 1e-6;

    private double sumL = 0.0;
    private double sumL2 = 0.0;
    private double sumC = 0.0;
    private double sumC2 = 0.0;
    private int m = 0;

    @Override
    public List<Move> generateMoves(List<Shift> shifts, RouteCompatibility compatibility) {
        calculateGlobalSums(shifts);
        List<Move> moves = new ArrayList<>();

        for (int r = 0; r < shifts.size(); r++) {
            List<Integer> ids = shifts.get(r).route;

            if (ids.size()< 3) continue;

            for (int i = 1; i < ids.size() - 2; i++) {
                for (int j = i + 1; j < ids.size() -1; j++) {
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
            double maxShiftDuration,
            ObjectiveFunction objectiveFunction
    ) {
        Objective.BalancedObj obj = (Objective.BalancedObj) objectiveFunction;
        double lambdaL = obj.lambdaL;
        double lambdaC = obj.lambdaC;

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

        double deltaTravel = deltaExt + deltaInternal;

        double oldL = s.totalTime;

        double newL = oldL - deltaTravel;

        if (newL > maxShiftDuration) {
            return new Evaluation(0, false);
        }

        double sumLNew  = sumL  - oldL + newL;
        double sumL2New = sumL2 - oldL * oldL + newL * newL;

        double sseLOld = sumL2 - (sumL * sumL) / m;
        double sseCOld = sumC2 - (sumC * sumC) / m;

        double sseLNew = sumL2New - (sumLNew * sumLNew) / m;

        double oldObj = sumL + lambdaL * sseLOld + lambdaC * sseCOld;
        double newObj = sumLNew + lambdaL * sseLNew + lambdaC * sseCOld;

        double improvement = oldObj - newObj;

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

        Shift s = newShifts.get(move.route1);
        int i = move.index1;
        int j = move.index2;

        // Reverse the segment [i, j]
        Collections.reverse(s.route.subList(i, j + 1));

        return newShifts;
    }

    private void calculateGlobalSums(List<Shift> shifts) {
        m = (shifts == null) ? 0 : shifts.size();

        sumL = sumL2 = 0.0;
        sumC = sumC2 = 0.0;

        for (Shift s : shifts) {
            if (s == null) continue;

            double L = s.totalTime;
            double C = s.serviceTime;

            sumL  += L;
            sumL2 += L * L;

            sumC  += C;
            sumC2 += C * C;
        }
    }
}
