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
            if (!CGIter()) {
                break;
            } else {
                solved = true;
            }
        }

        return solved;
    }

    private boolean CGIter() throws GRBException {
        rmp.solve();

        if (!rmp.isFeasible()) {
            return false;
        }

        if (separated) {
            List<Shift> newShifts = pp.getNewShifts(rmp.getDayDistances(), rmp.getDayStops(), rmp.getDayDuals(), rmp.getMaxDuration(), rmp.getMinDuration());
            if (newShifts.size() != 0) {
                rmp.addColumns(newShifts);
                return true;
            } else {
                return false;
            }
        } else {
            boolean improvement = false;

            List<Shift> newDayShifts = pp.getNewShifts(
                rmp.getDayDistances(), 
                rmp.getDayStops(), 
                rmp.getDayDuals(), 
                rmp.getMaxDuration(), 
                rmp.getMinDuration()
            );
            if (newDayShifts.size() != 0) {
                rmp.addColumns(newDayShifts);
                improvement = true;
            }

            List<Shift> newNightShifts = pp.getNewShifts(
                rmp.getNightDistances(), 
                rmp.getNightStops(), 
                rmp.getNightDuals(), 
                rmp.getMaxDuration(), 
                rmp.getMinDuration()
            );
            if (newNightShifts.size() != 0) {
                rmp.addColumns(newNightShifts);
                improvement = true;
            }
            
            return improvement;
        }
    }
}
