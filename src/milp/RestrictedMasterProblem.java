package milp;

import java.util.*;

import com.gurobi.gurobi.*;
import core.Stop;
import core.Shift;
import core.HTMInstance;

public abstract class RestrictedMasterProblem implements AutoCloseable {
    
    protected final HTMInstance instance;
    protected final List<Stop> allStops;
    protected final List<Stop> dayStops;
    protected final List<Stop> nightStops;
    protected final double[][] allDistances;
    protected final double[][] dayDistances;
    protected final double[][] nightDistances;
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

        this.allStops = stops;
        this.dayStops = createDayStops(stops);
        this.nightStops = createNightStops(stops);

        this.allDistances = distances;
        this.nightDistances = getSubDistanceMatrix(distances, stops, nightStops);
        this.dayDistances = getSubDistanceMatrix(distances, stops, dayStops);

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

    public abstract double[] getAllDuals() throws GRBException;

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

    public double[][] getDayDistances() {
        return dayDistances;
    }

    public double[][] getNightDistances() {
        return nightDistances;
    }

    public double[][] getAllDistances() {
        return allDistances;
    }

    public List<Stop> getDayStops() {
        return dayStops;
    }

    public List<Stop> getNightStops() {
        return nightStops;
    }

    public List<Stop> getAllStops() {
        return allStops;
    }

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

    public boolean isInfeasible() throws GRBException {
        return (model.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE);
    }

    @Override
    public void close() {
        try { model.dispose(); } catch (Exception ignored) {}
        try { env.dispose(); } catch (Exception ignored) {}
    }


    public static List<Stop> createNightStops(List<Stop> allStops) {
        List<Stop> nightStops = new ArrayList<>();
        for (Stop stop : allStops) {
            if (stop.nightShift != 0) {
                nightStops.add(stop);
            }
        }
        return nightStops;
    }

    public static List<Stop> createDayStops(List<Stop> allStops) {
        List<Stop> dayStops = new ArrayList<>();
        for (Stop stop : allStops) {
            if (stop.nightShift != 1) {
                dayStops.add(stop);
            }
        }
        return dayStops;
    }

    public static double[][] getSubDistanceMatrix(double[][] travelTimes, List<Stop> allStops, List<Stop> selectedStops) {
        double[][] subMatrix = new double[selectedStops.size()][selectedStops.size()];
        for (int i = 0; i < selectedStops.size(); i++) {
            for (int j = 0; j < selectedStops.size(); j++) {
                subMatrix[i][j] = travelTimes[selectedStops.get(i).objectId][selectedStops.get(j).objectId];
            }
        }
        return subMatrix;

    }

    // public abstract void printDummyState() throws GRBException;
    
    
}
