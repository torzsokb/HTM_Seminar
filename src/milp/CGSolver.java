package milp;

import java.util.*;

import com.gurobi.gurobi.GRBException;

import core.Stop;
import core.Shift;
import core.HTMInstance;



public class CGSolver {

    private final HTMInstance instance;
    private final List<Stop> stops;
    private final int k;
    private final double[][] distances;
    private final int maxDuration;
    private final int minDuration;
    private final boolean separateDayNight;
    private final int maxIter;
    private RestrictedMasterProblem RMP;
    private PricingProblem PP;

    public CGSolver(
        HTMInstance instance,
        List<Stop> stops,
        int k,
        double[][] distances,
        int maxDuration,
        int minDuration,
        boolean separateDayNight,
        int maxIter) throws GRBException {
        
        this.instance = instance;
        this.stops = stops;
        this.k = k;
        this.distances = distances;
        this.maxDuration = maxDuration;
        this.minDuration = minDuration;
        this.separateDayNight = separateDayNight;
        this.maxIter = maxIter;

        double bigM = 999999.9;

        this.RMP = new SeparatedRMP(instance, stops, distances, maxDuration, minDuration, k, bigM);
        // this.PP = new PricingProblem(distances, stops, maxDuration, minDuration);
        
        }

    public void solve() throws GRBException{
        RMP.setup();

        for (int i = 0; i < maxIter; i++) {
            RMP.solve();
            double[] duals = RMP.getDuals();
            PP.updateDuals(duals);
            List<Shift> newShifts = PP.getNewShifts();
            if (newShifts.size() == 0) {
                break;
            } else {
                RMP.addColumns(newShifts);
            }
           
        }
    }

    
}
