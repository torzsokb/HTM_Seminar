package neighborhoods;

import core.HTMInstance;
import core.Shift;
import search.*;

import java.util.ArrayList;
import java.util.List;

public class IntraShift implements Neighborhood {
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

            if (ids.size() <= 2) continue;

            for (int i = 1; i < ids.size() - 1; i++) {          // exclude depot at 0 and n-1
                for (int j = 1; j < ids.size() - 1; j++) {      // exclude depot at 0 and n-1
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
        Objective.BalancedObj obj = (Objective.BalancedObj) objectiveFunction;
        double lambdaL = obj.lambdaL;
        double lambdaC = obj.lambdaC;

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

        double deltaTravel  = deltaRemove + deltaInsert;

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

        int node = s.route.remove(i);
        s.route.add(j, node);

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
