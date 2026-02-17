package neighborhoods;

import core.HTMInstance;
import core.Shift;
import search.*;

import java.util.ArrayList;
import java.util.List;

public class IntraSwap implements Neighborhood {
    private static final double EPS = 1e-6;

    private double sumL = 0.0;
    private double sumL2 = 0.0;
    private double sumC = 0.0;
    private double sumC2 = 0.0;

    @Override
    public List<Move> generateMoves(List<Shift> shifts, RouteCompatibility compatibility) {
        calculateGlobalSums(shifts);

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

        double deltaTravel = oldCost - newCost;

        double oldL = s.totalTime;

        double newL = oldL - deltaTravel;
        if (newL > maxShiftDuration) {
            return new Evaluation(0, false);
        }

        double sumLNew  = sumL  - oldL + newL;
        double sumL2New = sumL2 - oldL * oldL + newL * newL;

        int m = 50;
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

        int nodeI = s.route.get(i);
        int nodeJ = s.route.get(j);

        // Swap
        s.route.set(i, nodeJ);
        s.route.set(j, nodeI);

        return newShifts;
    }

    private void calculateGlobalSums(List<Shift> shifts) {
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
