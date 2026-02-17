// package milp;

// import java.util.*;

// import com.gurobi.gurobi.GRBException;

// import core.Stop;
// import core.Shift;
// import core.HTMInstance;



// public class CGSolver {

//     private final ColumnGeneration CGDay;
//     private final ColumnGeneration CGNight;
//     private final double[][] distances;
//     private final double maxDuration;
//     private final double minDuration;
//     private final HTMInstance instance;
//     private final boolean separated;

//     public CGSolver(
//         HTMInstance instance,
//         double[][] distances,
//         double maxDuration,
//         double minDuration,
//         boolean separated,
//         int maxIter,
//         RCESPP pp
//     ) throws GRBException {

//         this.instance = instance;
//         this.maxDuration = maxDuration;
//         this.minDuration = minDuration;
//         if (separated) {
//             this.CGDay = new ColumnGeneration(new SeparatedRMP(instance, instance.getStops(), distances, maxDuration, minDuration, 25, maxDuration * 10), pp, maxIter, separated);
//         }


//     }
    

// }
