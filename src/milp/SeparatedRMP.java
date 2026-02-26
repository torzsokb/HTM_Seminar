package milp;
import java.util.*;

import com.gurobi.gurobi.*;
import core.Stop;
import core.Shift;
import core.HTMInstance;


public class SeparatedRMP extends RestrictedMasterProblem {

    
    public final boolean isNight;

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

    private List<Stop> getStops() {
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
        } 
        else {

            double[] duals = new double[nDayStops];
            int i = 0;

            for (Stop stop : dayStops) {
                duals[i] = constraintsDay.get(stop.objectId).get(GRB.DoubleAttr.Pi);
                
                i++;
                
            }
            // System.out.print("day duals 0: " + duals[0]);

            return duals;
        }
    }

    @Override
    public double[] getNightDuals() throws GRBException {
        if (isNight) {
            double[] duals = new double[nNightStops];

            int i = 0;

            for (Stop stop : nightStops) {
                duals[i] = constraintsNight.get(stop.objectId).get(GRB.DoubleAttr.Pi);
                // System.out.println("dual_" + i + ": " + duals[i]);
                i++;
            }
            // System.out.println("Number of high duals " + countHighDuals);
            // System.out.print("night duals 0: " + duals[0]);
            return duals;

        } 
        else {
            return new double[0];
        }   
    }

    @Override
    public double[] getAllDuals() throws GRBException {
        if (isNight) {
            return getNightDuals();
        }
        else {
            return getDayDuals();
        }
    }

    public Map<Integer, Shift> getShifts() {
        if (isNight) {
            return nightShifts;
        } else {
            return dayShifts;
        }
    }

    public Map<Integer, GRBConstr> getConstraints() throws GRBException {
        if (isNight) {
            return constraintsNight;
        } else {
            return constraintsDay;
        }
    }

    public Map<Integer, GRBVar> getShiftVars() throws GRBException {
        if (isNight) {
            return nightShiftVars;
        } else {
            return dayShiftVars;
        }
    }

    public Map<Integer, GRBVar> getDummyVars() throws GRBException {
        if (isNight) {
            return nightDummyVars;
        } else {
            return dayDummyVars;
        }
    }
    

    @Override
    public void addColumn(Shift newShift) throws GRBException {
        if (isNight) {
            addNightColumn(newShift);
        } else {
            addDayColumn(newShift);
        }
    }


    @Override
    public void setMaxDurationConstraint() throws GRBException {
        // for (int i = 0; i < getShifts().size(); i++) {
        //     if (getShifts().get(i).serviceTime >= maxDuration) {
        //         shiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
        //     }
        // }
    }

    @Override
    public void setMinDurationConstraint() throws GRBException {
        // for (int i = 0; i < shifts.size(); i++) {
        //     if (shifts.get(i).serviceTime <= minDuration) {
        //         shiftVars.get(i).set(GRB.DoubleAttr.UB, 0.0);
        //     }
        // }
    }


    @Override
    public double getLongestDrivingTime() throws GRBException {
        double max = 0.0;
        // for (int i = 0; i < shifts.size(); i++) {
        //     if (shiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9) {
        //         if (shifts.get(i).travelTime > max) {
        //             max = shifts.get(i).travelTime;
        //         }
        //     }
        // }
        return max;
    }



    @Override
    public double getLongestShiftDuration() throws GRBException {
        double max = 0.0;
        // for (int i = 0; i < shifts.size(); i++) {
        //     if (shiftVars.get(i).get(GRB.DoubleAttr.X) >= 0.9) {
        //         if (shifts.get(i).serviceTime > max) {
        //             max = shifts.get(i).serviceTime;
        //         }
        //     }
        // }
        return max;
    }


    public void addShiftDummyAndConstr() throws GRBException {
        GRBVar dummy = model.addVar(0.0, 1.0 * numberOfShifts, 0.0, 'C', "dummy 0");
        getDummyVars().put(0, dummy);
        
        GRBLinExpr constrExpr = new GRBLinExpr();
        constrExpr.addTerm(1.0, dummy);

        GRBConstr numberOfShiftsConstr = model.addConstr(constrExpr, GRB.LESS_EQUAL, 1.0 * this.numberOfShifts, "number of shifts");
        getConstraints().put(0, numberOfShiftsConstr);

        model.update();
    }

    public void addStopDummyAndConstr() throws GRBException {
        for (Stop stop : getAllStops()) {

            if (stop.idMaximo.equals("Depot")) {
                continue;
            }

            GRBVar dummy = model.addVar(0.0, 1.0, bigM, 'C', String.format("dummy %d", stop.objectId));
            getDummyVars().put(stop.objectId, dummy);
            
            GRBLinExpr constrExpr = new GRBLinExpr();
            constrExpr.addTerm(1.0, dummy);

            GRBConstr coverStop = model.addConstr(constrExpr, GRB.GREATER_EQUAL, 1.0, String.format("cover stop %d", stop.objectId));
            getConstraints().put(stop.objectId, coverStop);

            model.update();
        }
    }

    @Override
    public void setMinDurationConstraint(double minDuration) throws GRBException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setMinDurationConstraint'");
    }

    @Override
    public void setMaxDurationConstraint(double maxDuration) throws GRBException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setMaxDurationConstraint'");
    }

    @Override
    public List<Shift> getSolution() throws GRBException {
        List<Shift> solution = new ArrayList<>();

        for (Map.Entry<Integer, GRBVar> shiftVar : getShiftVars().entrySet()) {
            if (shiftVar.getValue().get(GRB.DoubleAttr.X) >= 0.5) {
                solution.add(dayShifts.get(shiftVar.getKey()));
            }
        }

        return solution;
    }

    @Override
    public void printSolution() throws GRBException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'printSolution'");
    }

    @Override
    public void printDummyState() throws GRBException {
        boolean dummiesActive = false;

        for (Stop stop  : getAllStops()) {

            GRBVar dummyVar = getDummyVars().get(stop.objectId);
            double value = dummyVar.get(GRB.DoubleAttr.X);
            String name = dummyVar.get(GRB.StringAttr.VarName);

            if (value >= 0.5) {
                dummiesActive = true;
                System.out.print("variable: " + name + ", value: " + value);
            }
            
        }

        if (!dummiesActive) {
            System.out.print("no day dummies active");
        }

        
    }

    @Override
    protected void printStopCoverageMetrics() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'printStopCoverageMetrics'");
    }
}
