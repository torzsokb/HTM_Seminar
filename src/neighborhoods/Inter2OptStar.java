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

    private double sumL = 0.0;
    private double sumL2 = 0.0;
    private double sumC = 0.0;
    private double sumC2 = 0.0;
    private int m = 0;

    @Override
    public List<Move> generateMoves(List<Shift> shifts, RouteCompatibility compatibility) {
        calculateGlobalSums(shifts);
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
        Objective.BalancedObj obj = (Objective.BalancedObj) objectiveFunction;
        double lambdaL = obj.lambdaL;
        double lambdaC = obj.lambdaC;

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

        double[] new1 = computeRouteDuration(newR1, instance, travelTimes);
        double[] new2 = computeRouteDuration(newR2, instance, travelTimes);

        double newL1 = new1[0];
        double newL2 = new2[0];

        if (newL1 > maxShiftDuration || newL2 > maxShiftDuration) {
            return new Evaluation(0, false);
        }

        double newC1 = new1[1];
        double newC2 = new2[1];

        double L1 = s1.totalTime, L2 = s2.totalTime;
        double C1 = s1.serviceTime, C2 = s2.serviceTime;

        double sumLNew  = sumL  - L1 - L2 + newL1 + newL2;
        double sumL2New = sumL2 - L1*L1 - L2*L2 + newL1*newL1 + newL2*newL2;

        double sumCNew  = sumC  - C1 - C2 + newC1 + newC2;
        double sumC2New = sumC2 - C1*C1 - C2*C2 + newC1*newC1 + newC2*newC2;

        double sseLOld = sumL2 - (sumL * sumL) / m;
        double sseCOld = sumC2 - (sumC * sumC) / m;

        double sseLNew = sumL2New - (sumLNew * sumLNew) / m;
        double sseCNew = sumC2New - (sumCNew * sumCNew) / m;

        double oldObj = sumL + lambdaL * sseLOld + lambdaC * sseCOld;
        double newObj = sumLNew + lambdaL * sseLNew + lambdaC * sseCNew;

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

        double[] d1 = computeRouteDuration(newR1, instance, travelTimes);
        double[] d2 = computeRouteDuration(newR2, instance, travelTimes);

        s1.totalTime = d1[0];
        s1.serviceTime = d1[1];

        s2.totalTime = d2[0];
        s2.serviceTime = d2[1];

        s1.nightShift = Utils.containsNightStop(newR1, instance) ? 1 : 0;
        s2.nightShift = Utils.containsNightStop(newR2, instance) ? 1 : 0;

        return newShifts;
    }


    private double[] computeRouteDuration(
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
        double s = 0;
        for (int id : route) {
            t += instance.getStops().get(id).serviceTime;
            s += instance.getStops().get(id).serviceTime;
        }


        // Add break and prep time
        t = t + breakTime + prepTime;

        return new double[]{t, s};
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
