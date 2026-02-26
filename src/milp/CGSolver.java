package milp;

import java.util.*;
import column_generation.*;
import neighborhoods.*;
import search.Acceptance;
import search.AcceptanceFunction;
import search.Compatibility;
import search.Neighborhood;
import search.RouteCompatibility;
import core.Utils;
import core.HTMInstance;
import core.Shift;
import core.Stop;


public class CGSolver {

    static final String instancePath = "src/core/data_all.txt";
    static final String travelPath   = "src/core/travel_times_collapsedv2.txt";
    static final boolean separated = false;
    static final double maxDuration = 7 * 60;
    static final double minDuration = 4.5* 60;
    static final int maxIter = 200;


    public static void main(String[] args) throws Exception {


        HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");
        List<Stop> stops = instance.getStops();
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        List<Neighborhood> neighborhoods = Arrays.asList(
            new IntraSwap(),
            new IntraShift(),
            new Intra2Opt()
        );

        List<Shift> initialSol = StartingSolution.startingSolution();

        AcceptanceFunction acceptanceFunction = Acceptance.greedy();
        RouteCompatibility compatibility = Compatibility.sameNightShift();
        PricingHeuristic pricingHeuristic = new PricingHeuristic(maxDuration, minDuration, 100, neighborhoods, acceptanceFunction, compatibility, instance);
        PricingProblem pp = new PricingProblem(pricingHeuristic);
        RolloutHeur rh = new RolloutHeur(20, 10, 10, 10);
        // SuperPricingHeuristic sph = new SuperPricingHeuristic(maxDuration, minDuration, 50, neighborhoods, acceptanceFunction, compatibility, instance);
        RollingSpaceRCESPP rs = new RollingSpaceRCESPP(18,10, 60);
        HeuristicNWRCESPP heur = new HeuristicNWRCESPP(10,10, 10);

        if (separated) {
            SeparatedRMP dayRMP = new SeparatedRMP(instance, stops, travelTimes, maxDuration, minDuration, 25, maxDuration * 50, false);
            SeparatedRMP nightRMP = new SeparatedRMP(instance, stops, travelTimes, maxDuration,  minDuration, 25, maxDuration * 50, true);

        } else {
            List<Stop> nightStops = RestrictedMasterProblem.createNightStops(stops);
            double[][] nightDist = RestrictedMasterProblem.getSubDistanceMatrix(travelTimes, stops, nightStops);
            CombinedRMP RMP = new CombinedRMP(instance, nightStops, nightDist, maxDuration, minDuration, 25, maxDuration *61);
            ColumnGeneration CG = new ColumnGeneration(RMP, pp, maxIter, separated);
            // boolean a = CG.CGIter();
            // CG.addStartingSol(initialSol); 
            CG.solveSingleObj();
        }   
    }    
}
