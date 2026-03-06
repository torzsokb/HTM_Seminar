package milp;
import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBCallback;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import com.gurobi.gurobi.GRB.DoubleAttr;

import java.util.*;

import core.*; 
import neighborhoods.*; 
import search.*;
import java.util.*;

public class TSP extends GRBCallback{
    
    private GRBVar[][] x;

    public TSP(GRBVar[][] x) {
        this.x = x;
    }

    
    @Override
    protected void callback() {
        try {
            if (where == GRB.CB_MIPSOL) {
                
                int n = x.length;
                List<List<Integer>> cycles = findCycles(getSolution(x));

                for (List<Integer> cycle : cycles) {
                    
                    if (cycle.size() < 2) {
                        continue;
                    }

                    if (cycle.size() > n / 2) {
                        continue;
                    }

                    GRBLinExpr flowFromCycle = new GRBLinExpr();
                    GRBLinExpr flowToCycle = new GRBLinExpr();
                    boolean[] inCycle = new boolean[n];

                    for (Integer node : cycle) {
                        inCycle[node] = true;
                    }

                    for (int i = 0; i < n; i++) {
                        
                        if (!(inCycle[i])) {
                            continue;
                        }

                        for (int j = 0; j < n; j++) {

                            if (inCycle[j]) {
                                continue;
                            }
                            
                            flowFromCycle.addTerm(1.0, x[i][j]);
                            flowToCycle.addTerm(1.0, x[j][i]);
                            
                        }   
                    }

                    addLazy(flowToCycle, GRB.GREATER_EQUAL, 1.0);
                    addLazy(flowFromCycle, GRB.GREATER_EQUAL, 1.0);

                }
            }
        } 
        catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected static int[] getSuccessors(double[][] solution) {

        int n = solution.length;
        int[] successors = new int[n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (solution[i][j] >= 0.5) {
                    successors[i] = j;
                }
            }
        }

        return successors;

    }

    protected static List<List<Integer>> findCycles(double[][] solution) {
        
        int n = solution.length;
        int[] succesors = getSuccessors(solution);

        int numVisited = 0;
        boolean[] visited = new boolean[n];
        
        List<List<Integer>> cycles = new ArrayList<>();

        int current = 0;

        while (numVisited < n) {

            List<Integer> cycle = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                if (!(visited[i])) {
                    current = i;
                    break;
                }
            }


            while (!(visited[current])) {
                visited[current] = true;
                cycle.add(current);
                current = succesors[current];
                numVisited++;
            }

            cycles.add(cycle);
            
        }

        return cycles;

    }

    public static void optimizeShift(Shift shift, double[][] distances) {
        
        int flag = 0;
        int n = shift.route.size() - 1;

        try {

            GRBEnv env = new GRBEnv();
            GRBModel model = new GRBModel(env);

            env.set(GRB.IntParam.LogToConsole, flag);
            env.set(GRB.IntParam.OutputFlag, flag);
            model.set(GRB.IntParam.OutputFlag, flag);
            model.set(GRB.IntParam.LogToConsole, flag);
            model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
            model.set(GRB.IntParam.LazyConstraints, 1);
            

            GRBVar[][] vars = new GRBVar[n][n];

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {

                    if (i == j) {
                        vars[i][j] = model.addVar(0.0, 0.0, 0, GRB.BINARY, ("x[" + i + "," + j + "]"));
                        continue;
                    } 
                    
                    double obj = distances[shift.route.get(i)][shift.route.get(j)];
                    vars[i][j] = model.addVar(0.0, 1.0, obj, GRB.BINARY, ("x[" + i + "," + j + "]"));

                    if (j == i + 1) {
                        vars[i][j].set(DoubleAttr.Start, 1.0);
                    }
                }

                if (i + 1 == n) {
                    vars[i][0].set(DoubleAttr.Start, 1.0);
                }
            }

            for (int i = 0; i < n; i++) {

                GRBLinExpr inFlow = new GRBLinExpr();
                GRBLinExpr outFlow = new GRBLinExpr();

                for (int j = 0; j < n; j++) {
                    
                    if (i == j) {
                        continue;
                    }

                    inFlow.addTerm(1.0, vars[j][i]);
                    outFlow.addTerm(1.0, vars[i][j]);

                }

                model.addConstr(1.0, GRB.EQUAL, outFlow, ("outflow " + i));
                model.addConstr(1.0, GRB.EQUAL, inFlow, ("inflow " + i));

            }

            model.setCallback(new TSP(vars));
            model.optimize();

            if (flag == 0) {
                System.out.println("old travel time: " + shift.travelTime + " new travel time: " + model.get(GRB.DoubleAttr.ObjVal));
            }


            List<Integer> updatedRoute = new ArrayList<>();
            int[] successors = getSuccessors(model.get(GRB.DoubleAttr.X, vars));
            int current = 0;

            for (int i = 0; i < n; i++) {
                updatedRoute.add(shift.route.get(current));
                current = successors[current];
            }
            updatedRoute.add(0);

            shift.route = updatedRoute;
            shift.travelTime = model.get(GRB.DoubleAttr.ObjVal);
            shift.recomputeTotalTime();

            model.dispose();
            env.dispose();

            


        } 
        catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        double shiftLength = 7*60;
        double totalShiftLength = 8*60;
        String instancePath = "src/core/data_all_feas_typeHalte.txt";
        String travelPath   = "src/core/travel_times_collapsedv2.txt";

        String travelNightPath = "data/inputs/cleaned/travel_time_night_collapsedv2.txt";
        String travelDayPath = "data/inputs/cleaned/travel_time_day_collapsedv2.txt";

        HTMInstance instance = Utils.readInstance(instancePath, "feasible", "Night_shift");
        double[][] travelTimes = Utils.readTravelTimes(travelPath);

        double[][] travelTimesNight = Utils.readTravelTimes(travelNightPath);
        double[][] travelTimesDay = Utils.readTravelTimes(travelDayPath);

        //ObjectiveFunction objectiveBalanced = Objective.balancedObj(0.05, 0.05);
        ObjectiveFunction objectiveBasic = Objective.totalLength();

        // Choose initial shifts to use 
        List<Shift> initial = Utils.readShiftsFromCSVDiffTimes("src/results/HTM_data_initRes_typeHalte.csv", travelTimesNight, travelTimesDay);

        // Make sure they are feasible 
        Utils.makeFeasible(initial, instance, travelTimesNight, travelTimesDay);

        double initial_obj_value = objectiveBasic.shifts(initial)/60.0;

        System.out.println("Initial solution:");
        System.out.println("Total shifts: " + initial.size());
        System.out.println("Total objective value: " + initial_obj_value);

        Utils.checkFeasibility(initial, instance, totalShiftLength);

        
        
        // NORMAL LOCAL SEARCH 
        List<Neighborhood> neighborhoods = Arrays.asList(
            new Intra2Opt(),
            new InterSwap(),
            new IntraSwap(),
            new IntraShift(),
            new Inter2OptStar(),
            new InterShift()
        );

        AcceptanceFunction acceptGreedy = Acceptance.greedy();

        RouteCompatibility compatibility = Compatibility.sameNightShift();

        LocalSearch ls = new LocalSearch(
                neighborhoods,
                acceptGreedy,
                compatibility,
                ImprovementChoice.BEST,
                1000,       
                totalShiftLength,
                objectiveBasic,
                false
        );
        
        long startTime = System.currentTimeMillis();
        System.out.println("\nRunning local search...");
        List<Shift> improved = ls.runDiffTimes(initial, instance, travelTimesNight, travelTimesDay);

        Utils.recomputeAllShiftsDiffTimes(improved, instance, travelTimesNight, travelTimesDay);

        double new_obj_value = objectiveBasic.shifts(improved)/60.0;

        System.out.println("\nLocal search complete.");

        System.out.println("New objective value: " + new_obj_value);

        double improvement = initial_obj_value - new_obj_value;

        System.out.println("Improvement: " + improvement);
        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime-startTime)/1000.0;
        System.out.println("Time taken: " + (timeTaken) + " s" );

        int i = 0;
        double impr = 0;

        long TSPStartTime = System.currentTimeMillis();

        for (Shift shift : improved) {
            if (shift.nightShift == 1) {
                double old_time = shift.travelTime;
                optimizeShift(shift, travelTimesNight);
                Utils.recomputeShift(shift, instance, travelTimesNight);
                impr += old_time - shift.travelTime;
                System.out.println(shift.travelTime);
            } else {
                double old_time = shift.travelTime;
                optimizeShift(shift, travelTimesDay);
                Utils.recomputeShift(shift, instance, travelTimesDay);
                impr += old_time - shift.travelTime;
                System.out.println(shift.travelTime);
            }
            
            
            // i++;
            // if (i == 5) {
            //     break;
            // }

        }
        long TSPEndTime = System.currentTimeMillis();
        System.out.println("tsp running time: " + (TSPEndTime - TSPStartTime));

        System.out.println(impr);
    }
}
