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
    private final List<GRBConstr> constraints = new ArrayList<>();

    public SeparatedRMP(
        HTMInstance instance,
        List<Stop> stops, 
        double[][] distances,
        int maxDuration,
        int minDuration,
        int k,
        double bigM) throws GRBException {
        
        super(instance, stops, distances, maxDuration, minDuration, k, bigM);
    }

    @Override
    public double[] getDuals() throws GRBException {
        double[] duals = new double[constraints.size()];
        for (int i = 0; i < constraints.size(); i++) {
            duals[i] = constraints.get(i).get(GRB.DoubleAttr.Pi);
        }
        return duals;
    }

    @Override
    public void addColumn(Shift shift) throws GRBException {
        GRBColumn newColumn = new GRBColumn();
        
        for (int i = 0; i < stops.size(); i++) {
            if (shift.route.contains(i)) {
                newColumn.addTerm(1.0, constraints.get(i));
            }
        }

        GRBVar shiftVar = model.addVar(0.0, 1.0, shift.travelTime, 'C', String.format("shift %d", shifts.size()));
        shiftVars.put(shifts.size(), shiftVar);
        shifts.add(shift);
        
        model.update();
    }

    @Override
    public void setup() throws GRBException {
        addShiftDummyAndConstr();
        addStopDummyAndConstr();
    }

    public void addShiftDummyAndConstr() throws GRBException {
        GRBVar dummy = model.addVar(0.0, 1.0, 0.0, 'C', "dummy_0");
        dummyVars.put(0, dummy);
        
        GRBLinExpr constrExpr = new GRBLinExpr();
        constrExpr.addTerm(1.0 * this.k, dummy);

        GRBConstr numberOfShifts = model.addConstr(constrExpr, GRB.LESS_EQUAL, 1.0 * this.k, "number of shifts");
        constraints.add(numberOfShifts);

        model.update();
    }

    public void addStopDummyAndConstr() throws GRBException {
        for (int i = 1; i < stops.size(); i++) {
            
            GRBVar dummy = model.addVar(0.0, 1.0, bigM, 'C', String.format("dummy_%d", i));
            dummyVars.put(i, dummy);

            GRBLinExpr constrExpr = new GRBLinExpr();
            constrExpr.addTerm(1.0, dummy);

            GRBConstr coverStop = model.addConstr(constrExpr, GRB.GREATER_EQUAL, 1.0, String.format("cover stop %d", i));
            constraints.add(coverStop);

            model.update();
        }
    }
}
