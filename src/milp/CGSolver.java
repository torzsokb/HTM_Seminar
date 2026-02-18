package milp;

import java.util.*;

import core.Utils;
import core.HTMInstance;
import core.Shift;
import core.Stop;


public class CGSolver {

    static final String instancePath = "src/core/data_all.txt";
    static final String travelPath   = "src/core/travel_times_collapsedv2.txt";
    static final boolean separated = false;
    static final double maxDuration = 7 * 60;
    static final double minDuration = 4.5 * 60;
    static final int maxIter = 50;
    public static void main(String[] args) throws Exception {


        HTMInstance instance = Utils.readInstance(instancePath, "abri", "Night_shift");
        List<Stop> stops = instance.getStops();
        double[][] travelTimes = Utils.readTravelTimes(travelPath);


        if (separated) {
            SeparatedRMP dayRMP = new SeparatedRMP(instance, stops, travelTimes, maxDuration, minDuration, 25, maxDuration * 50, false);
            SeparatedRMP nightRMP = new SeparatedRMP(instance, stops, travelTimes, maxDuration, minDuration, 25, maxDuration * 50, true);
        } else {
            CombinedRMP RMP = new CombinedRMP(instance, stops, travelTimes, maxDuration, minDuration, 0, maxDuration * 50);
        }


    }

    
    
}
