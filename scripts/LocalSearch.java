import java.util.*;

/**
 * Local search for a set of routes/shifts.
 * - Input: current routes (each route is a list of stop IDs)
 * - Objective: sum of per-route TSP costs
 * - Neighborhoods:
 *    (1) relocate one stop from route a -> route b
 *    (2) swap one stop between route a and route b
 *
 * Assumes you already have a TSP solver. Plug it into TSPSolver below.
 */
public class LocalSearch {

    // !!!! insert TSP (Code Barny)
    public interface TSPSolver {
        // Return the best tour cost for these stops
        double solveCost(List<Integer> stops);
        }

    // Store results
    public static class RoutesSolution {
        public List<List<Integer>> routes;      // stop allocation per shift
        public double totalCost;                // total costs of all routes
        public double[] routeCost;              // cost of one route

        public RoutesSolution(List<List<Integer>> routes, double totalCost, double[] routeCost) {
            this.routes = routes;
            this.totalCost = totalCost;
            this.routeCost = routeCost;
        }
    }

    // Doing local search
    public static RoutesSolution localSearch(
            boolean[] stopNightOnly,
            boolean[] routeIsNight,
            List<List<Integer>> routes,
            TSPSolver tsp
    ) {

        // compute initial costs
        double[] routeCost = new double[routes.size()];
        double total = 0.0;
        for (int r = 0; r < routes.size(); r++) {
            routeCost[r] = tsp.solveCost(routes.get(r));
            total += routeCost[r];
        }

        // Go to neighbourhood 2 when no improvements found
        while (true) {
            boolean improved = false;

            // Neighbourhood 1: Move 1 job
            // go over the routes
            for (int ra = 0; ra < routes.size() && !improved; ra++) {
                List<Integer> A = routes.get(ra);
                if (A.isEmpty()) continue;

                // go over the jobs (indexes) in the current route
                for (int ia = 0; ia < A.size() && !improved; ia++) {
                    // safe stop id
                    int stop = A.get(ia);

                    // go over the other routes
                    for (int rb = 0; rb < routes.size() && !improved; rb++) {
                        if (rb == ra) continue;

                        // Build candidate routes by removing from A and adding to B
                        List<Integer> candA = new ArrayList<>(A);
                        candA.remove(ia);

                        List<Integer> candB = new ArrayList<>(routes.get(rb));
                        candB.add(stop);

                        // Check if night stops in night shift
                        if (stopNightOnly[stop] && !routeIsNight[rb]) continue;

                        // Recompute only changed route costs
                        double newCostA = tsp.solveCost(candA);
                        double newCostB = tsp.solveCost(candB);

                        double newTotal = total - routeCost[ra] - routeCost[rb] + newCostA + newCostB;

                        // accept if new total smaller than what we had before
                        if (newTotal < total - 1e-9) {
                            routes.set(ra, candA);
                            routes.set(rb, candB);
                            total = newTotal;
                            routeCost[ra] = newCostA;
                            routeCost[rb] = newCostB;
                            improved = true;
                        }
                    }
                }
            }
            if (improved) continue;

            // Neighbourhood 2: swap 2 jobs
            boolean swapped = false;

            // go over the routes
            for (int ra = 0; ra < routes.size() && !swapped; ra++) {
                List<Integer> A = routes.get(ra);
                if (A.isEmpty()) continue;

                // go over the other routes
                for (int rb = ra + 1; rb < routes.size() && !swapped; rb++) {
                    List<Integer> B = routes.get(rb);
                    if (B.isEmpty()) continue;

                    // go over the jobs (indexes) of the routes
                    for (int ia = 0; ia < A.size() && !swapped; ia++) {
                        for (int ib = 0; ib < B.size() && !swapped; ib++) {

                            int aStop = A.get(ia);
                            int bStop = B.get(ib);

                            // Build candidate routes by switching the jobs
                            List<Integer> candA = new ArrayList<>(A);
                            List<Integer> candB = new ArrayList<>(B);
                            candA.set(ia, bStop);
                            candB.set(ib, aStop);

                            // Check if night stops in night shift
                            if (stopNightOnly[bStop] && !routeIsNight[ra]) continue;
                            if (stopNightOnly[aStop] && !routeIsNight[rb]) continue;


                            // Recompute only changed route costs
                            double newCostA = tsp.solveCost(candA);
                            double newCostB = tsp.solveCost(candB);

                            double newTotal = total - routeCost[ra] - routeCost[rb] + newCostA + newCostB;

                            // accept if new total smaller than what we had before
                            if (newTotal < total - 1e-9) {
                                routes.set(ra, candA);
                                routes.set(rb, candB);
                                total = newTotal;
                                routeCost[ra] = newCostA;
                                routeCost[rb] = newCostB;
                                swapped = true;
                            }
                        }
                    }
                }
            }

            if (swapped) continue;
            // Stop when no improvements
            break;
        }

        return new RoutesSolution(routes, total, routeCost);
    }
}
