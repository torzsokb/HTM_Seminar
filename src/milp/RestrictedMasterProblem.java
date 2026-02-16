package milp;

import java.util.*;

import com.gurobi.gurobi.*;
import core.Stop;
import core.Shift;
import core.HTMInstance;

public abstract class RestrictedMasterProblem implements AutoCloseable{
    
    protected final HTMInstance instance;
    protected final List<Stop> stops;
    protected final double[][] distances;
    protected final int k;
    protected final double bigM;

    protected final int maxDuration;
    protected final int minDuration;

    protected final GRBEnv env;
    protected final GRBModel model;

    public RestrictedMasterProblem(
        HTMInstance instance,
        List<Stop> stops, 
        double[][] distances,
        int maxDuration,
        int minDuration,
        int k,
        double bigM) throws GRBException {
        
        this.instance = instance;
        this.stops = stops;
        this.distances = distances;
        this.maxDuration = maxDuration;
        this.minDuration = minDuration;
        this.k = k;
        this.bigM = bigM;
        
        this.env = new GRBEnv(true);
        this.env.set("OutputFlag", "1");
        this.env.start();
        
        this.model = new GRBModel(env);
        this.model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
    }

    public abstract void setup() throws GRBException;

    public abstract double[] getDuals() throws GRBException;

    public abstract void addColumn(Shift newShift) throws GRBException;

    public void addColumns(List<Shift> newShifts) throws GRBException {
        for (Shift shift : newShifts) {
            addColumn(shift);
        }
    }

    public void solve() throws GRBException {
        model.optimize();
    }

    @Override
    public void close() {
        try { model.dispose(); } catch (Exception ignored) {}
        try { env.dispose(); } catch (Exception ignored) {}
    }

    
    
}
