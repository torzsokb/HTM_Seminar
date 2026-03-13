package neighborhoods;

import core.HTMInstance;
import core.Shift;
import core.Stop;
import core.Utils;
import milp.TSP;
import search.*;

import java.util.ArrayList;
import java.util.List;

public class InterShiftInfeas implements Neighborhood {
    private static final double EPS = 1e-6;
    private final int MAX_NIGHT_SHIFTS = 25;

    private double sumL = 0.0;
    private double sumL2 = 0.0;
    private double sumC = 0.0;
    private double sumC2 = 0.0;
    private final double maxDuration = 8 * 60;
    private final double maxOvertime = 0.0;
    private int m = 0;

    @Override
    public List<Move> generateMoves(List<Shift> shifts, RouteCompatibility compatibility, HTMInstance instance) {
        
        calculateGlobalSums(shifts);

        List<Move> moves = new ArrayList<>();

        int numNightShifts = Utils.countNightShifts(shifts);
        int numShifts = shifts.size();
        int numViolated = 0;

        boolean[] violated = new boolean[numShifts];

        if (numNightShifts > MAX_NIGHT_SHIFTS) {
            System.out.println("Too many night shifts: " + numNightShifts);
            return moves;
        }


        for (int i = 0; i < numShifts; i++) {
            if (shifts.get(i).totalTime > maxDuration + maxOvertime) {
                violated[i] = true;
                numViolated++;
            }
        }



        System.out.println("number of shifts: " + numShifts + " violated: " + numViolated);

        for (int violatedIdx = 0; violatedIdx < numShifts; violatedIdx++) {

            if (!(violated[violatedIdx])) {
                continue;
            }

            Shift violatedShift = shifts.get(violatedIdx);

            for (int feasibleIdx = 0; feasibleIdx < numShifts; feasibleIdx++) {

                if (violated[feasibleIdx]) {
                    continue;
                }

                Shift feasibleShift = shifts.get(feasibleIdx);
                
                // if (violatedShift.route.size() < 2) {
                //     continue;
                // }

                if (numNightShifts == MAX_NIGHT_SHIFTS && !compatibility.compatible(violatedShift, feasibleShift)) {
                    continue;
                }

                for (int i = 1; i < violatedShift.route.size() - 1; i++) {
                    for (int j = 1; j < feasibleShift.route.size() - 1; j++) {
                        moves.add(new Move(violatedIdx, feasibleIdx, i, j, Move.MoveType.INTER_SHIFT));
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

        int node = s1.route.get(move.index1);

        Stop stop = instance.getStops().get(node);

        double service = stop.serviceTime;

        int prev1 = (move.index1 == 0) ? 0 : s1.route.get(move.index1 - 1);
        int next1 = (move.index1 == s1.route.size() - 1) ? 0 : s1.route.get(move.index1 + 1);

        double deltaRemove =
                -travelTimes[prev1][node]
                        - travelTimes[node][next1]
                        + travelTimes[prev1][next1];

        int prev2 = (move.index2 == 0) ? 0 : s2.route.get(move.index2 - 1);
        int next2 = (move.index2 == s2.route.size()) ? 0 : s2.route.get(move.index2);

        double deltaInsert =
                -travelTimes[prev2][next2]
                        + travelTimes[prev2][node]
                        + travelTimes[node][next2];

        double newL1 = s1.totalTime - service + deltaRemove;
        double newL2 = s2.totalTime + service + deltaInsert;

        if (newL2 > maxShiftDuration) {
            return new Evaluation(0, false);
        }

        double oldViolationS1 = Math.max(0, (s1.totalTime - maxDuration - maxOvertime));
        double newViolationS1 = Math.max(0, (newL1 - maxShiftDuration - maxOvertime));

        double newC1 = s1.serviceTime - service;
        double newC2 = s2.serviceTime + service;

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

        double improvement = oldObj - newObj - (5 * (oldViolationS1 -  newViolationS1));
        if (Math.abs(improvement) < EPS) improvement = 0.0;

        return new Evaluation(improvement, true);
    }

    @Override
    public Evaluation evaluateMoveDiffTimes(
            Move move,
            List<Shift> shifts,
            HTMInstance instance,
            double[][] travelTimesNight,
            double[][] travelTimesDay,
            double maxShiftDuration,
            ObjectiveFunction objectiveFunction
    ) {
        Objective.BalancedObj obj = (Objective.BalancedObj) objectiveFunction;

        double lambdaL = obj.lambdaL;
        double lambdaC = obj.lambdaC;

        Shift s1 = shifts.get(move.route1);
        Shift s2 = shifts.get(move.route2);

        int node = s1.route.get(move.index1);

        Stop stop = instance.getStops().get(node);

        double service = stop.serviceTime;

        int prev1 = (move.index1 == 0) ? 0 : s1.route.get(move.index1 - 1);
        int next1 = (move.index1 == s1.route.size() - 1) ? 0 : s1.route.get(move.index1 + 1);

        double deltaRemove = 0.0;
        if (s1.nightShift == 1) {
            deltaRemove =
                -travelTimesNight[prev1][node]
                        - travelTimesNight[node][next1]
                        + travelTimesNight[prev1][next1];
        } else {
            deltaRemove =
                -travelTimesDay[prev1][node]
                        - travelTimesDay[node][next1]
                        + travelTimesDay[prev1][next1];
        }

        int prev2 = (move.index2 == 0) ? 0 : s2.route.get(move.index2 - 1);
        int next2 = (move.index2 == s2.route.size()) ? 0 : s2.route.get(move.index2);

        double deltaInsert = 0.0;

        // Use night or day travel times 
        if (s2.nightShift == 1 || instance.getStops().get(node).nightShift == 1) {
            deltaInsert =
                -travelTimesNight[prev2][next2]
                        + travelTimesNight[prev2][node]
                        + travelTimesNight[node][next2];
        } else {
            deltaInsert =
                -travelTimesDay[prev2][next2]
                        + travelTimesDay[prev2][node]
                        + travelTimesDay[node][next2];
        }

        double newL1 = s1.totalTime - service + deltaRemove;
        double newL2 = s2.totalTime + service + deltaInsert;

        if (newL1 > maxShiftDuration || newL2 > maxShiftDuration) {
            return new Evaluation(0, false);
        }

        double newC1 = s1.serviceTime - service;
        double newC2 = s2.serviceTime + service;

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
            double[][] travelTimesNight,
            double[][] travelTimesDay
    ) {
        List<Shift> newShifts = new ArrayList<>(shifts);

        Shift s1 = newShifts.get(move.route1);
        Shift s2 = newShifts.get(move.route2);

        int node = s1.route.remove(move.index1);
        s2.route.add(move.index2, node);
        s1.nightShift = Utils.containsNightStop(s1.route, instance) ? 1 : 0;
        s2.nightShift = Utils.containsNightStop(s2.route, instance) ? 1 : 0;

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
