package milp;
import java.util.*;

import com.gurobi.gurobi.*;
import core.Stop;
import core.Shift;
import core.HTMInstance;


public class CombinedRMP extends RestrictedMasterProblem {

    private final Map<Integer, GRBVar> dayDummyVars = new HashMap<>();
    private final Map<Integer, GRBVar> nightDummyVars = new HashMap<>();
    private final Map<Integer, GRBVar> dayShiftVars = new HashMap<>();
    private final Map<Integer, GRBVar> nightShiftVars = new HashMap<>();
    private final List<Shift> shifts = new ArrayList<>();
    private final Map<Integer, GRBConstr> constraintsDay = new HashMap<>();
    private final Map<Integer, GRBConstr> constraintsNight = new HashMap<>();
    
    public CombinedRMP(
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
        double[] duals = new double[constraintsDay.size()];
        for (int i = 0; i < constraintsDay.size(); i++) {
            duals[i] = constraintsDay.get(i).get(GRB.DoubleAttr.Pi);
        }
        return duals;
    }

    @Override
    public double[] getNightDuals() throws GRBException {
        double[] duals = new double[constraintsNight.size()];
        for (int i = 0; i < constraintsNight.size(); i++) {
            duals[i] = constraintsNight.get(i).get(GRB.DoubleAttr.Pi);
        }
        return duals;
    }

    @Override
    public void addColumn(Shift newShift) throws GRBException {
        if (newShift.nightShift == 0) {
            addDayColumn(newShift);
            addNightColumn(newShift);
        } else {
            addNightColumn(newShift);
        }
    }

    @Override
    public double getLongestDrivingTime() throws GRBException {
        double max = 0.0;

        for (int i = 0; i < shifts.size(); i++) {
            Shift shift = shifts.get(i);

            if (shift.nightShift == 0) {
                if (nightShiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9 || dayShiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9) {
                    if (shift.travelTime > max) {
                        max = shifts.get(i).travelTime;
                    }
                }
            } else {
                if (nightShiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9) {
                    if (shift.travelTime > max) {
                        max = shifts.get(i).travelTime;
                    }
                }
            }
            
        }

        return max;
    }

    @Override
    public double getLongestShiftDuration() throws GRBException {
        double max = 0.0;

        for (int i = 0; i < shifts.size(); i++) {
            Shift shift = shifts.get(i);

            if (shift.nightShift == 0) {
                if (nightShiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9 || dayShiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9) {
                    if (shift.travelTime > max) {
                        max = shifts.get(i).totalTime;
                    }
                }
            } else {
                if (nightShiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9) {
                    if (shift.travelTime > max) {
                        max = shifts.get(i).totalTime;
                    }
                }
            }
            
        }
        
        return max;
    }

    @Override
    public void setMinDurationConstraint() throws GRBException {
        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).totalTime <= minDuration) {
                if (shifts.get(i).nightShift == 0) {
                    dayShiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
                    nightShiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
                } else {
                    nightShiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
                }
            }
        }
    }


    @Override
    public void setMaxDurationConstraint() throws GRBException {
        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).totalTime >= maxDuration) {
                if (shifts.get(i).nightShift == 0) {
                    dayShiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
                    nightShiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
                } else {
                    nightShiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
                }
            }
        }
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



    public void addNightColumn(Shift newShift) throws GRBException {
        GRBColumn newColumn = new GRBColumn();

        for (Integer stop : newShift.route.subList(0, newShift.route.size() - 1)) {
            newColumn.addTerm(1.0, constraintsNight.get(stop));
        }

        GRBVar shiftVar = model.addVar(0.0, 1.0, newShift.travelTime, 'C', String.format("shift nigth %d", shifts.size()));
        nightShiftVars.put(shifts.size(), shiftVar);
        shifts.add(newShift);
        
        model.update();

    }


    public void addDayColumn(Shift newShift) throws GRBException {
        GRBColumn newColumn = new GRBColumn();

        for (Integer stop : newShift.route.subList(0, newShift.route.size() - 1)) {
            newColumn.addTerm(1.0, constraintsDay.get(stop));
        }

        GRBVar shiftVar = model.addVar(0.0, 1.0, newShift.travelTime, 'C', String.format("shift day %d", shifts.size()));
        dayShiftVars.put(shifts.size(), shiftVar);
        
        model.update();

    }

    @Override
    public void addShiftDummyAndConstr() throws GRBException {

        GRBVar dummyDay = model.addVar(0.0, 1.0, 0.0, 'C', "dummy day");
        GRBVar dummyNight = model.addVar(0.0, 1.0, 0.0, 'C', "dummy night");
        dayDummyVars.put(0, dummyDay);
        nightDummyVars.put(0, dummyNight);

        GRBLinExpr constrExprDay = new GRBLinExpr();
        GRBLinExpr constrExprNight = new GRBLinExpr();
        constrExprDay.addTerm(1.0 * numberOfShifts, dummyDay);
        constrExprNight.addTerm(1.0 * numberOfShifts, dummyNight);

        GRBConstr numberOfShiftsDay = model.addConstr(constrExprDay, GRB.LESS_EQUAL, 1.0 * this.numberOfShifts, "number of day shifts");
        GRBConstr numberOfShiftsNight = model.addConstr(constrExprNight, GRB.LESS_EQUAL, 1.0 * this.numberOfShifts, "number of night shifts");
        constraintsDay.put(0, numberOfShiftsDay);
        constraintsNight.put(0, numberOfShiftsNight);

        model.update();
    }

    @Override
    public void addStopDummyAndConstr() throws GRBException {
        for (Stop stop : stops) {

            if (stop.idMaximo.equals("Depot")) {
                continue;
            }

            GRBVar dummy = model.addVar(0.0, 1.0, bigM, 'C', String.format("dummy %d", stop.objectId));
            GRBLinExpr constrExpr = new GRBLinExpr();
            constrExpr.addTerm(1.0, dummy);
            GRBConstr coverStop = model.addConstr(constrExpr, GRB.GREATER_EQUAL, 1.0, String.format("cover stop %d", stop.objectId));
            
            if (stop.nightShift == 0) {
                dayDummyVars.put(stop.objectId, dummy);
                constraintsDay.put(stop.objectId, coverStop);
                constraintsNight.put(stop.objectId, coverStop);
            } else {
                nightDummyVars.put(stop.objectId, dummy);
                constraintsNight.put(stop.objectId, coverStop);
            }

        }
    }
}
