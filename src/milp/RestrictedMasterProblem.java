package milp;

import java.util.*;

import com.gurobi.gurobi.*;
import core.Stop;
import core.Shift;
import core.HTMInstance;

public abstract class RestrictedMasterProblem implements AutoCloseable {
    
    protected final HTMInstance instance;
    protected final List<Stop> stops;
    protected final double[][] distances;
    protected final int numberOfShifts;
    protected final double bigM;

    protected double maxDuration;
    protected double minDuration;

    protected final GRBEnv env;
    protected final GRBModel model;

    public RestrictedMasterProblem(
        HTMInstance instance,
        List<Stop> stops, 
        double[][] distances,
        double maxDuration,
        double minDuration,
        int numberOfShifts,
        double bigM
    ) throws GRBException {
        
        this.instance = instance;
        this.stops = stops;
        this.distances = distances;
        this.maxDuration = maxDuration;
        this.minDuration = minDuration;
        this.numberOfShifts = numberOfShifts;
        this.bigM = bigM;
        
        this.env = new GRBEnv(true);
        this.env.set("OutputFlag", "1");
        this.env.start();
        
        this.model = new GRBModel(env);
        this.model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
    }

    

    public abstract double[] getDayDuals() throws GRBException;

    public abstract double[] getNightDuals() throws GRBException;

    public abstract void addShiftDummyAndConstr() throws GRBException;

    public abstract void addStopDummyAndConstr() throws GRBException;

    public abstract void addColumn(Shift newShift) throws GRBException;

    public abstract double getLongestDrivingTime() throws GRBException;

    public abstract double getLongestShiftDuration() throws GRBException;

    public void setMaxDuration(double maxDuration) {
        this.maxDuration = maxDuration;
    }

    public void setMinDuration(double minDuration) {
        this.minDuration = minDuration;
    }

    public abstract double[][] getDayDistances();

    public abstract double[][] getNightDistances();

    public abstract List<Stop> getDayStops();

    public abstract List<Stop> getNightStops();

    public double getMinDuration() {
        return this.minDuration;
    }

    public double getMaxDuration() {
        return this.maxDuration;
    }

    public abstract void setMinDurationConstraint() throws GRBException;

    public abstract void setMaxDurationConstraint() throws GRBException;

    public void setup() throws GRBException {
        addShiftDummyAndConstr();
        addStopDummyAndConstr();
    }


    public void addColumns(List<Shift> newShifts) throws GRBException {
        for (Shift shift : newShifts) {
            addColumn(shift);
        }
    }

    public void solve() throws GRBException {
        model.optimize();
    }

    public boolean isFeasible() throws GRBException {
        return (model.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE);
    }

    @Override
    public void close() {
        try { model.dispose(); } catch (Exception ignored) {}
        try { env.dispose(); } catch (Exception ignored) {}
    }

    
    
}
