package milp;
import java.util.*;

import com.gurobi.gurobi.*;
import core.Stop;
import core.Shift;
import core.HTMInstance;


public class SeparatedRMP extends RestrictedMasterProblem {

    private final Map<Integer, GRBVar> dummyVars = new HashMap<>();
    private final Map<Integer, GRBVar> shiftVars = new HashMap<>();
    private final List<Shift> shifts = new ArrayList<>();
    private final Map<Integer, GRBConstr> constraints = new HashMap<>();

    public SeparatedRMP(
        HTMInstance instance,
        List<Stop> stops, 
        double[][] distances,
        double maxDuration,
        double minDuration,
        int numberOfShifts,
        double bigM) throws GRBException {
        
        super(instance, stops, distances, maxDuration, minDuration, numberOfShifts, bigM);
    }

    @Override
    public double[] getDayDuals() throws GRBException {
        double[] duals = new double[constraints.size()];
        for (int i = 0; i < constraints.size(); i++) {
            duals[i] = constraints.get(i).get(GRB.DoubleAttr.Pi);
        }
        return duals;
    }

    @Override
    public double[] getNightDuals() throws GRBException {
        return new double[0];
    }

    @Override
    public void addColumn(Shift newShift) throws GRBException {
        GRBColumn newColumn = new GRBColumn();

        for (Integer stop : newShift.route.subList(0, newShift.route.size() - 1)) {
            newColumn.addTerm(1.0, constraints.get(stop));
        }

        GRBVar shiftVar = model.addVar(0.0, 1.0, newShift.travelTime, 'C', String.format("shift %d", shifts.size()));
        shiftVars.put(shifts.size(), shiftVar);
        shifts.add(newShift);
        
        model.update();
    }

    @Override
    public void setMaxDurationConstraint() throws GRBException {
        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).totalTime >= maxDuration) {
                shiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
            }
        }
    }

    @Override
    public void setMinDurationConstraint() throws GRBException {
        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).totalTime <= minDuration) {
                shiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
            }
        }
    }

    @Override
    public double getLongestDrivingTime() throws GRBException {
        double max = 0.0;
        for (int i = 0; i < shifts.size(); i++) {
            if (shiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9) {
                if (shifts.get(i).travelTime > max) {
                    max = shifts.get(i).travelTime;
                }
            }
        }
        return max;
    }

    @Override
    public double getLongestShiftDuration() throws GRBException {
        double max = 0.0;
        for (int i = 0; i < shifts.size(); i++) {
            if (shiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9) {
                if (shifts.get(i).totalTime > max) {
                    max = shifts.get(i).totalTime;
                }
            }
        }
        return max;
    }

    @Override
    public double[][] getDayDistances() {
        return this.distances;
    }

    @Override
    public double[][] getNightDistances() {
        return this.distances;
    }

    @Override
    public List<Stop> getDayStops() {
        return this.stops;
    }

    @Override
    public List<Stop> getNightStops() {
        return this.stops;
    }



    public void addShiftDummyAndConstr() throws GRBException {
        GRBVar dummy = model.addVar(0.0, 1.0, 0.0, 'C', "dummy 0");
        dummyVars.put(0, dummy);
        
        GRBLinExpr constrExpr = new GRBLinExpr();
        constrExpr.addTerm(1.0 * this.numberOfShifts, dummy);

        GRBConstr numberOfShiftsConstr = model.addConstr(constrExpr, GRB.LESS_EQUAL, 1.0 * this.numberOfShifts, "number of shifts");
        constraints.put(0, numberOfShiftsConstr);

        model.update();
    }

    public void addStopDummyAndConstr() throws GRBException {
        for (Stop stop : stops) {

            if (stop.idMaximo.equals("Depot")) {
                continue;
            }

            GRBVar dummy = model.addVar(0.0, 1.0, bigM, 'C', String.format("dummy %d", stop.objectId));
            dummyVars.put(stop.objectId, dummy);
            
            GRBLinExpr constrExpr = new GRBLinExpr();
            constrExpr.addTerm(1.0, dummy);

            GRBConstr coverStop = model.addConstr(constrExpr, GRB.GREATER_EQUAL, 1.0, String.format("cover stop %d", stop.objectId));
            constraints.put(stop.objectId, coverStop);

            model.update();
        }
    }
}
