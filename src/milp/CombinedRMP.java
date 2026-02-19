package milp;
import java.util.*;

import com.gurobi.gurobi.*;
import core.Stop;
import core.Shift;
import core.HTMInstance;


public class CombinedRMP extends RestrictedMasterProblem {    
    
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
        
        double[] duals = new double[nDayStops];
        int i = 0;

        for (Stop stop : dayStops) {
            duals[i] = constraintsDay.get(stop.objectId).get(GRB.DoubleAttr.Pi);
            i++;
            
        }

        return duals;
    }

    @Override
    public double[] getAllDuals() throws GRBException {

        double[] duals = new double[nStops];
        int i = 0;

        for (Stop stop : allStops) {
            duals[i] = constraintsNight.get(stop.objectId).get(GRB.DoubleAttr.Pi);
            i++;
            
        }

        return duals;
    }

    @Override
    public double[] getNightDuals() throws GRBException {
        
        double[] duals = new double[nNightStops];
        int i = 0;

        for (Stop stop : nightStops) {
            duals[i] = constraintsNight.get(stop.objectId).get(GRB.DoubleAttr.Pi);
            i++;
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

    public void addNightColumn(Shift newShift) throws GRBException {
        
        int shiftSignature = newShift.getSignature();
        Shift duplicateShift = nightShifts.get(shiftSignature);
        
        if (duplicateShift == null) {

            GRBColumn newColumn = new GRBColumn();
            double obj = newShift.travelTime;
            String name = String.format("shift nigth %d", shiftSignature);

            for (Integer stopId : newShift.getUniqueStops()) {
                newColumn.addTerm(1.0, constraintsNight.get(stopId));
            }

            GRBVar shiftVar = model.addVar(0.0, 1.0, obj, 'C', newColumn, name);
            nightShiftVars.put(shiftSignature, shiftVar);
            nightShifts.put(shiftSignature, newShift);
            model.update();
            return;

        } 

        if (duplicateShift.travelTime > newShift.travelTime) {
            nightShifts.put(shiftSignature, newShift);
            nightShiftVars.get(shiftSignature).set(GRB.DoubleAttr.Obj, newShift.travelTime);
            model.update();
        }
    }

    public void addDayColumn(Shift newShift) throws GRBException {

        int shiftSignature = newShift.getSignature();
        Shift duplicateShift = dayShifts.get(shiftSignature);
        
        if (duplicateShift == null) {

            GRBColumn newColumn = new GRBColumn();
            double obj = newShift.travelTime;
            String name = String.format("shift day %d", shiftSignature);

            for (Integer stopId : newShift.getUniqueStops()) {
                newColumn.addTerm(1.0, constraintsDay.get(stopId));
            }

            GRBVar shiftVar = model.addVar(0.0, 1.0, obj, 'C', newColumn, name);
            dayShiftVars.put(shiftSignature, shiftVar);
            dayShifts.put(shiftSignature, newShift);
            model.update();
            return;

        } 

        if (duplicateShift.travelTime > newShift.travelTime) {
            dayShifts.put(shiftSignature, newShift);
            dayShiftVars.get(shiftSignature).set(GRB.DoubleAttr.Obj, newShift.travelTime);
            model.update();
        }
    }

    @Override
    public double getLongestDrivingTime() throws GRBException {

        double max = 0.0;

        for (Integer shiftKey : nightShifts.keySet()) {

            GRBVar nightShiftVar = nightShiftVars.get(shiftKey);
            GRBVar dayShiftVar = dayShiftVars.get(shiftKey);

            if (nightShiftVar.get(GRB.DoubleAttr.X) >= 0.5) {
                double time = nightShifts.get(shiftKey).travelTime;
                if (time > max) {
                    max = time;
                }
            }

            if (dayShiftVar == null) {
                continue;
            }

            if (dayShiftVar.get(GRB.DoubleAttr.X) >= 0.5) {
                double time = dayShifts.get(shiftKey).travelTime;
                if (time > max) {
                    max = time;
                }
            }
            
        }

        return max;
    }

    @Override
    public double getLongestShiftDuration() throws GRBException {

        double max = 0.0;

        for (Integer shiftKey : nightShifts.keySet()) {

            GRBVar nightShiftVar = nightShiftVars.get(shiftKey);
            GRBVar dayShiftVar = dayShiftVars.get(shiftKey);

            if (nightShiftVar.get(GRB.DoubleAttr.X) >= 0.5) {
                double time = nightShifts.get(shiftKey).totalTimeNoBreak;
                if (time > max) {
                    max = time;
                }
            }

            if (dayShiftVar == null) {
                continue;
            }

            if (dayShiftVar.get(GRB.DoubleAttr.X) >= 0.5) {
                double time = dayShifts.get(shiftKey).totalTimeNoBreak;
                if (time > max) {
                    max = time;
                }
            }
            
        }
        
        return max;
    }

    @Override
    public void setMinDurationConstraint(double minDuration) throws GRBException {

        for (Integer shiftKey : nightShifts.keySet()) {

            double time = nightShifts.get(shiftKey).totalTimeNoBreak;
            
            if (time >= minDuration) {
                continue;
            }

            GRBVar nightShiftVar = nightShiftVars.get(shiftKey);
            GRBVar dayShiftVar = dayShiftVars.get(shiftKey);

            nightShiftVar.set(GRB.DoubleAttr.UB, 0.0);

            if (dayShiftVar == null) {
                continue;
            }

            dayShiftVar.set(GRB.DoubleAttr.UB, 0.0);

        }
    }

    @Override
    public void setMaxDurationConstraint(double maxDuration) throws GRBException {
        for (Integer shiftKey : nightShifts.keySet()) {

            double time = nightShifts.get(shiftKey).totalTimeNoBreak;
            
            if (time < minDuration) {
                continue;
            }

            GRBVar nightShiftVar = nightShiftVars.get(shiftKey);
            GRBVar dayShiftVar = dayShiftVars.get(shiftKey);

            nightShiftVar.set(GRB.DoubleAttr.UB, 0.0);

            if (dayShiftVar == null) {
                continue;
            }

            dayShiftVar.set(GRB.DoubleAttr.UB, 0.0);

        }
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

        for (Map.Entry<Integer, GRBVar> shiftVar : dayShiftVars.entrySet()) {
            if (shiftVar.getValue().get(GRB.DoubleAttr.X) >= 0.5) {
                solution.add(dayShifts.get(shiftVar.getKey()));
            }
        }

        for (Map.Entry<Integer, GRBVar> shiftVar : nightShiftVars.entrySet()) {
            if (shiftVar.getValue().get(GRB.DoubleAttr.X) >= 0.5) {
                solution.add(nightShifts.get(shiftVar.getKey()));
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
