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
    public double[] getAllDuals() throws GRBException {
        double[] duals = new double[constraintsNight.size()];
        for (int i = 0; i < constraintsNight.size(); i++) {
            duals[i] = constraintsNight.get(i).get(GRB.DoubleAttr.Pi);
        }
        return duals;
    }


    @Override
    public double[] getNightDuals() throws GRBException {
        double[] duals = new double[nightStops.size()];
        for (int i = 0; i < nightStops.size(); i++) {
            duals[i] = constraintsNight.get(nightStops.get(i).objectId).get(GRB.DoubleAttr.Pi);
        }
        return duals;
    }

    @Override
    public void addColumn(Shift newShift) throws GRBException {
        if (isNightShift(newShift)) {
            addNightColumn(newShift);
        } else {
            addDayColumn(newShift);
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
                        max = shifts.get(i).serviceTime;
                    }
                }
            } else {
                if (nightShiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9) {
                    if (shift.travelTime > max) {
                        max = shifts.get(i).serviceTime;
                    }
                }
            }
            
        }
        
        return max;
    }

    @Override
    public void setMinDurationConstraint() throws GRBException {
        for (int i = 0; i < shifts.size(); i++) {
            if (shifts.get(i).serviceTime <= minDuration) {
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
            if (shifts.get(i).serviceTime >= maxDuration) {
                if (shifts.get(i).nightShift == 0) {
                    dayShiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
                    nightShiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
                } else {
                    nightShiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
                }
            }
        }
    }


    public void addNightColumn(Shift newShift) throws GRBException {
        GRBColumn newColumn = new GRBColumn();

        for (Stop stop : allStops) {
            if (newShift.route.contains(stop.objectId)) {
                newColumn.addTerm(1.0, constraintsNight.get(stop.objectId));
            }
        }

        GRBVar shiftVar = model.addVar(0.0, 1.0, newShift.travelTime, 'C', newColumn, String.format("shift nigth %d", shifts.size()));
        nightShiftVars.put(shifts.size(), shiftVar);
        shifts.add(newShift);
        
        model.update();

    }


    public void addDayColumn(Shift newShift) throws GRBException {
        GRBColumn newColumn = new GRBColumn();

        for (Stop stop : dayStops) {
            if (newShift.route.contains(stop.objectId)) {
                newColumn.addTerm(1.0, constraintsDay.get(stop.objectId));
            }
        }

        GRBVar shiftVar = model.addVar(0.0, 1.0, newShift.travelTime, 'C', newColumn, String.format("shift day %d", shifts.size()));
        dayShiftVars.put(shifts.size(), shiftVar);
        
        model.update();

    }


    @Override
    public void addShiftDummyAndConstr() throws GRBException {

        GRBVar dummyDay = model.addVar(0.0, 1.0 * numberOfShifts, 0.0, 'C', "dummy day");
        GRBVar dummyNight = model.addVar(0.0, 1.0 * numberOfShifts, 0.0, 'C', "dummy night");
        dayDummyVars.put(0, dummyDay);
        nightDummyVars.put(0, dummyNight);

        GRBLinExpr constrExprDay = new GRBLinExpr();
        GRBLinExpr constrExprNight = new GRBLinExpr();
        constrExprDay.addTerm(1.0, dummyDay);
        constrExprNight.addTerm(1.0, dummyNight);

        GRBConstr numberOfShiftsDay = model.addConstr(constrExprDay, GRB.LESS_EQUAL, 1.0 * this.numberOfShifts, "number of day shifts");
        GRBConstr numberOfShiftsNight = model.addConstr(constrExprNight, GRB.LESS_EQUAL, 1.0 * this.numberOfShifts, "number of night shifts");
        constraintsDay.put(0, numberOfShiftsDay);
        constraintsNight.put(0, numberOfShiftsNight);

        model.update();
    }


    @Override
    public void addStopDummyAndConstr() throws GRBException {
        for (Stop stop : allStops) {

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

            model.update();

        }
    }

    @Override
    public List<Shift> getSolution() throws GRBException {
        List<Shift> solution = new ArrayList<>();

        for (int i = 0; i < shifts.size(); i++) {
            Shift shift = shifts.get(i);
            if (shift.nightShift == 0) {
                if (nightShiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.5 || dayShiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.5) {
                    solution.add(shift);
                }
            } else {
                if (nightShiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.5) {
                    solution.add(shift);
                }
            }
        }

        return solution;
    }
    

    @Override
    public void printDummyState() throws GRBException {
        boolean dayDummiesActive = false;
        boolean nightDummiesActive = false;

        for (Stop stop  : dayStops) {

            GRBVar dayDummy = dayDummyVars.get(stop.objectId);
            double value = dayDummy.get(GRB.DoubleAttr.X);
            String name = dayDummy.get(GRB.StringAttr.VarName);

            if (value >= 0.5) {
                dayDummiesActive = true;
                System.out.print("variable: " + name + ", value: " + value);
            }
            
        }

        if (!dayDummiesActive) {
            System.out.print("no day dummies active");
        }

        for (Stop stop  : nightStops) {

            GRBVar nightDummy = nightDummyVars.get(stop.objectId);
            double value = nightDummy.get(GRB.DoubleAttr.X);
            String name = nightDummy.get(GRB.StringAttr.VarName);

            if (value >= 0.5) {
                nightDummiesActive = true;
                System.out.print("variable: " + name + ", value: " + value);
            }
            
        }

        if (!nightDummiesActive) {
            System.out.print("no night dummies active");
        }

    }
    
    
    @Override
    public void printSolution() throws GRBException {
        printDummyState();
        for (Shift shift : getSolution()) {
            System.out.print(shift);
        }
    }
}
