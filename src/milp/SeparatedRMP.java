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
    private final boolean isNight;

    public SeparatedRMP(
        HTMInstance instance,
        List<Stop> stops, 
        double[][] distances,
        double maxDuration,
        double minDuration,
        int numberOfShifts,
        double bigM,
        boolean isNight) throws GRBException {
        
        super(instance, stops, distances, maxDuration, minDuration, numberOfShifts, bigM);
        this.isNight = isNight;
    }

    @Override
    public List<Stop> getAllStops() {
        if (isNight) {
            return nightStops;
        } else {
            return dayStops;
        }
    }

    @Override
    public double[][] getAllDistances() {
        if (isNight) {
            return nightDistances;
        } else {
            return dayDistances;
        }
    }

    @Override
    public double[] getDayDuals() throws GRBException {
        if (isNight) {
            return new double[0];
        } else {
             return getAllDuals();
        }
    }

    @Override
    public double[] getNightDuals() throws GRBException {
        if (isNight) {
            return getAllDuals();
        } else {
            return new double[0];
        }   
    }

    @Override
    public double[] getAllDuals() throws GRBException {
        double[] duals = new double[constraints.size()];
        for (int i = 0; i < constraints.size(); i++) {
            duals[i] = constraints.get(i).get(GRB.DoubleAttr.Pi);
        }
        return duals;
    }

    @Override
    public void addColumn(Shift newShift) throws GRBException {
        GRBColumn newColumn = new GRBColumn();

        for (Stop stop : getAllStops()) {
            if (newShift.route.contains(stop.objectId)) {
                newColumn.addTerm(1.0, constraints.get(stop.objectId));
            }
        }

        GRBVar shiftVar = model.addVar(0.0, 1.0, newShift.travelTime, 'C', newColumn, String.format("shift %d", shifts.size()));
        shiftVars.put(shifts.size(), shiftVar);
        shifts.add(newShift);
        
        model.update();
    }

    @Override
    public void setMaxDurationConstraint() throws GRBException {
        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).serviceTime >= maxDuration) {
                shiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
            }
        }
    }

    @Override
    public void setMinDurationConstraint() throws GRBException {
        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).serviceTime <= minDuration) {
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
                if (shifts.get(i).serviceTime > max) {
                    max = shifts.get(i).serviceTime;
                }
            }
        }
        return max;
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
        for (Stop stop : getAllStops()) {

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
