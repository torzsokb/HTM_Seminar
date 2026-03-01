package milp;

import java.util.*;

import com.gurobi.gurobi.GRBException;

import core.Stop;
import core.Shift;

public class ColumnGeneration {

    private final RestrictedMasterProblem rmp;
    private final RCESPP pp;
    private final int maxIter;
    private final boolean separated;

    public ColumnGeneration(RestrictedMasterProblem rmp, RCESPP pp, int maxIter, boolean separated) throws GRBException {
        this.rmp = rmp;
        this.rmp.setup();
        this.pp = pp;
        this.maxIter = maxIter;
        this.separated = separated;
    }

    public void addStartingSol(List<Shift> shifts) throws GRBException {
        double obj = 0.0;
        for (Shift shift : shifts) {
            obj += shift.travelTime;
        }
        System.out.print("starting solution objective: " + obj);
        rmp.addColumns(shifts);
    }

    public void solveMinMax() throws GRBException {
        while (true) {
            if (solveSingleObj()) {
                double newMaxDuration = rmp.getLongestShiftDuration();
                rmp.setMaxDuration(newMaxDuration);
                rmp.setMaxDurationConstraint();
            } else {
                break;
            }
        }
    }

    public boolean solveSingleObj() throws GRBException {

        boolean solved = false;

        for (int i = 0; i < maxIter; i++) {
            System.out.print("iteration " + i + "\n");
            if (!CGIter()) {
                break;
            } else {
                solved = true;
            }
        }
        rmp.solveBinary();
        rmp.printSolution();
        

        return solved;
    }

    public boolean CGIter() throws GRBException {
        rmp.solve();
        // rmp.printSolution();

        if (rmp.isInfeasible()) {
            System.out.println("INFEASIBLE");
            return false;
        }

        System.out.print("rmp objective: " + rmp.getObj() + "\n");

        if (separated) {

            List<Shift> newShifts = pp.getNewShifts(
                rmp.getAllDistances(), 
                rmp.getAllStops(), 
                rmp.getAllDuals(), 
                rmp.getMaxDuration(), 
                rmp.getMinDuration()
            );

            if (newShifts.size() != 0) {
                rmp.addColumns(newShifts);
                return true;
            } else {
                return false;
            }

        } else {
            boolean improvement = false;

            // List<Shift> newDayShifts = pp.getNewShifts(
            //     rmp.getDayDistances(), 
            //     rmp.getDayStops(), 
            //     rmp.getDayDuals(), 
            //     rmp.getMaxDuration(), 
            //     rmp.getMinDuration()
            // );

            // List<Shift> newNightShifts = pp.getNewShifts(
            //     rmp.getNightDistances(), 
            //     rmp.getNightStops(), 
            //     rmp.getNightDuals(), 
            //     rmp.getMaxDuration(), 
            //     rmp.getMinDuration()
            // );

            List<Shift> newCombinedShifts = pp.getNewShifts(
                rmp.getAllDistances(), 
                rmp.getAllStops(), 
                rmp.getAllDuals(), 
                rmp.getMaxDuration(), 
                rmp.getMinDuration()
            );

            // if (newDayShifts.size() != 0) {
            //     rmp.addColumns(newDayShifts);
            //     improvement = true;
            // }

            // if (newNightShifts.size() != 0) {
            //     rmp.addColumns(newNightShifts);
            //     improvement = true;
            // }

            if (newCombinedShifts.size() != 0) {
                rmp.addColumns(newCombinedShifts);
                improvement = true;
            }
            rmp.printStopCoverageMetrics();
            return improvement;
        }
    }
}
