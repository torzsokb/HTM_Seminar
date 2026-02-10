import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class SolveSA {
    static final double shiftLength = 7 * 60;

    static final double totalShiftLength = 8 * 60;


    public static void main(String[] args) {
        File data = new File("data_collapsed_vabri.txt");
        File travelTimesFile = new File("travel_times_collapsedv2.txt");
        try {
            TransportInstance allStops = TransportInstance.read(data);

            /*
            // Change service times
            List<Stop> stops = allStops.getStops();

            // Change service times if we want to consider less than 20
            stops.get(0).serviceTime = 0.0;
            int singleStops = 0;
            int doubleStops = 0;
            int tripleStops = 0;
            int quadrupleStops = 0;
            for (int i = 1; i < stops.size(); i++) {
                if (stops.get(i).serviceTime == 20.0) {
                    stops.get(i).serviceTime = 17.5;
                    singleStops++;
                } else if (stops.get(i).serviceTime == 30.0) {
                    stops.get(i).serviceTime = 25.0;
                    doubleStops++;
                } else if (stops.get(i).serviceTime == 40.0) {
                    stops.get(i).serviceTime = 32.5;
                    tripleStops++;
                } else if (stops.get(i).serviceTime == 50.0) {
                    stops.get(i).serviceTime = 40.0;
                    quadrupleStops++;
                }
            }
            System.out.println(singleStops + ", " + doubleStops + ", " + tripleStops + ", "+ quadrupleStops);
             */

            long startTime = System.currentTimeMillis();

            try {
                System.out.println("Reading travel times ");
                double[][] travelTimes = readTravelTimes(travelTimesFile);

                System.out.println("Solving Greedy Algorithm ");
                List<Integer> nightIdx = getAllowedIndices(allStops, 1);
                List<Integer> dayIdx   = getAllowedIndices(allStops, 0);

                List<Shift> shifts = new ArrayList<>();

                System.out.println("Solving night shifts");
                List<Shift> nightShifts = solveGreedy(allStops, travelTimes, nightIdx, 1);

                System.out.println("Solving day shifts");
                List<Shift> dayShifts = solveGreedy(allStops, travelTimes, dayIdx, 0);

                shifts.addAll(nightShifts);
                shifts.addAll(dayShifts);

                double obj = totalObj(shifts);
                long endTime = System.currentTimeMillis();
                long runTime = endTime - startTime;

                System.out.println("Nightshifts:");
                for (int r = 0; r < nightShifts.size(); r++) {
                    Shift shift = nightShifts.get(r);
                    System.out.println("Shift " + (r + 1) + ": " + formatRoute(allStops, shift.route));
                    System.out.println("Takes " + (shift.totalTime / 60.0) + " hours.");
                }

                System.out.println("Dayshifts:");
                for (int r = 0; r < dayShifts.size(); r++) {
                    Shift shift = dayShifts.get(r);
                    System.out.println("Shift " + (r + 1) + ": " + formatRoute(allStops, shift.route));
                    System.out.println("Takes " + (shift.totalTime / 60.0) + " hours.");
                }

                System.out.println("\nObjective (total time): " + (obj / 60.0) + " hours.");
                System.out.println("Number of shifts: " + shifts.size());
                System.out.println("Average duration of shift: " + ((obj / 60.0) /shifts.size()) + " hours.");
                System.out.println("Average time spent cleaning per shift: " + ((totalCleaningTime(shifts) / 60.0) / shifts.size()));
                System.out.println("Total runtime: " + runTime);


                // Do the SA
                long saStartRunTime = System.currentTimeMillis();
                List<Shift> saShifts = solveSA(shifts, allStops, travelTimes);
                long saEndRunTime = System.currentTimeMillis();
                long saRunTime = saEndRunTime - saStartRunTime;

                int nNight = 0;
                for (Shift shift : saShifts) {
                    if (shift.nightShift == 1) {
                        nNight++;
                    } else if (isNightShift(shift, allStops)){
                        shift.nightShift = 1;
                        nNight++;
                    }
                }
                double newObj = totalObj(saShifts);
                System.out.println("\nObjective (total time): " + (newObj / 60.0) + " hours.");
                System.out.println("Improvement: " + ((obj - newObj) / 60.0) + " hours.");
                System.out.println("Number of nightshifts: " + nNight);
                System.out.println("Average duration of shift: " + ((newObj / 60.0) / saShifts.size()) + " hours.");
                System.out.println("Average time spent cleaning per shift: " + ((totalCleaningTime(saShifts) / 60.0) / saShifts.size()));
                System.out.println("Total runtime for SA: " + saRunTime);
                for (int r = 0; r < saShifts.size(); r++) {
                    Shift shift = saShifts.get(r);
                    System.out.println("Shift " + (r + 1) + ": " + formatRoute(allStops, shift.route));
                    System.out.println("Takes " + (shift.totalTime / 60.0) + " hours.");
                }

                // Make csv file
                resultsToCSV(saShifts, allStops, "results_SA_1002.csv");

            } catch (IOException ex) {
                System.out.println("There was an error reading file " + travelTimesFile);
                ex.printStackTrace();
            }

        } catch (IOException ex) {
            System.out.println("There was an error reading file " + data);
            ex.printStackTrace();
        }
    }

    public static List<Shift> solveGreedy(TransportInstance instance, double[][] travelTimes, List<Integer> allowed, int nightFlag) {
        int n = instance.getNStops();
        int depot = 0;

        boolean[] isAllowed = new boolean[n];
        for (int idx : allowed) isAllowed[idx] = true;

        boolean[] visited = new boolean[n];
        visited[depot] = true;

        int remaining = allowed.size();
        List<Shift> shifts = new ArrayList<>();

        while (remaining > 0) {
            List<Integer> route = new ArrayList<>();
            route.add(depot);

            int current = depot;

            double travelTime = 0.0;
            double serviceTime = 0.0;
            double elapsed = 0.0;

            while (true) {
                int next = -1;
                double best = Double.POSITIVE_INFINITY;

                for (int j = 1; j < n; j++) {
                    if (!isAllowed[j] || visited[j]) continue;

                    double toJ = travelTimes[current][j];
                    double back = travelTimes[j][depot];

                    double elapsedIfGoAndClean = elapsed + toJ + instance.getStops().get(j).serviceTime;
                    double totalIfReturn = elapsedIfGoAndClean + back;

                    if (totalIfReturn <= shiftLength && toJ < best) {
                        best = toJ;
                        next = j;
                    }
                }

                // No feasible next stop
                if (next == -1) {
                    // close route
                    double back = travelTimes[current][depot];
                    travelTime += back;
                    route.add(depot);

                    shifts.add(new Shift(route, travelTime, serviceTime, nightFlag));
                    break;
                }

                // go to next
                double toNext = travelTimes[current][next];
                double cleanNext = instance.getStops().get(next).serviceTime;

                travelTime += toNext;
                serviceTime += cleanNext;
                elapsed += toNext + cleanNext;

                current = next;
                route.add(current);
                visited[current] = true;
                remaining--;
            }
        }

        return shifts;
    }

    static String formatRoute(TransportInstance instance, List<Integer> routeIdx) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < routeIdx.size(); k++) {
            Stop s = instance.getStops().get(routeIdx.get(k));
            sb.append(s.idMaximo);
            if (k < routeIdx.size() - 1) sb.append(" -> ");
        }
        return sb.toString();
    }

    public static double totalObj(List<Shift> shifts) {
        double sum = 0.0;
        for (Shift shift : shifts) {
            sum += shift.totalTime;
        }
        return sum;
    }

    public static double totalCleaningTime(List<Shift> shifts) {
        double sum = 0.0;
        for (Shift shift : shifts) {
            sum += shift.serviceTime;
        }
        return sum;
    }

    private static List<Integer> getAllowedIndices(TransportInstance instance, int nightFlag) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < instance.getNStops(); i++) {
            if (i == 0) continue; // skip depot as a "to visit" stop
            if (instance.getStops().get(i).nightShift == nightFlag) {
                idx.add(i);
            }
        }
        return idx;
    }

    public static double[][] readTravelTimes(File file) throws IOException {
        ArrayList<double[]> rows = new ArrayList<>();
        int cols = -1;

        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("[,\\s]+");
                if (cols == -1) cols = parts.length;
                if (parts.length != cols) {
                    throw new IllegalArgumentException("Ragged row: expected " + cols + " values, got " + parts.length);
                }

                double[] row = new double[cols];
                for (int j = 0; j < cols; j++) {
                    row[j] = Double.parseDouble(parts[j]) / 60.0;
                }
                rows.add(row);
            }
        }

        double[][] matrix = new double[rows.size()][cols];
        for (int i = 0; i < rows.size(); i++) matrix[i] = rows.get(i);
        return matrix;
    }

    public static List<Shift> solveSA(List<Shift> initialShifts, TransportInstance instance, double[][] travelTimes) {
        // SA parameters
        double T = 6000.0;
        double Tstep = 0.95;
        int MaxN = 2000;
        int MaxNe = 400;
        int MaxIt = 20000000;

        Random rng = new Random(420);

        double bestObj = totalObj(initialShifts);

        List<Shift> currentBest = deepCopy(initialShifts, instance, travelTimes);

        List<Shift> improvedShifts = deepCopy(currentBest, instance, travelTimes);

        int rejectedInfeasible = 0;
        int accepted = 0;
        int newBestFound = 0;
        int unchanged = 0;

        // Initialise counters
        int it = 0;
        int it_p = 0;
        int nb_ne = 0;

        // Do the SA loop
        while (it < MaxIt) {
            it++;
            it_p++;

            // Make neighbour
            List<Shift> neighbour = deepCopy(currentBest, instance, travelTimes);

            double currentObj = totalObj(neighbour);

            // Choose two random stops from random shifts
            int stopId1 =  pickRandomStop(neighbour, rng);
            int stopId2 =  pickRandomStop(neighbour, rng);

            // Avoid exact same stop
            for (int tries = 0; tries < 10 && stopId1 == stopId2; tries++) {
                stopId2 = pickRandomStop(neighbour, rng);
            }
            if (stopId1 == stopId2) continue;

            // Choose neighbourhood
            boolean moved = false;

            double rMove = rng.nextDouble();
            if (rMove < 0.50) {
                moved = intraSwap(neighbour, instance, travelTimes, rng);
            } else if (rMove < 0.85) {
                moved = insertBest(neighbour, instance, travelTimes, rng);
            } else {
                moved = swapAndInsert(neighbour, instance, travelTimes, rng);
            }

            if (!moved) continue;

            // Evaluate neighbour
            int[] loc1 = findStop(neighbour, stopId1);
            int[] loc2 = findStop(neighbour, stopId2);
            if (loc1[0] < 0 || loc2[0] < 0) {
                continue;
            }

            // First check feasibility
            if (neighbour.get(loc1[0]).totalTime > totalShiftLength ||
                    (loc2[0] != loc1[0] && neighbour.get(loc2[0]).totalTime > totalShiftLength)) {
                rejectedInfeasible++;
                continue;
            }

            // Accept / reject decision
            double newObj = totalObj(neighbour);
            if (Math.abs(newObj - currentObj) < 1e-9) unchanged++;

            if (newObj < currentObj) {
                currentBest = neighbour;
                currentObj = newObj;
                accepted++;
            } else {
                double r = rng.nextDouble();
                double P = Math.exp((currentObj - newObj) / T);
                if (r < P) {
                    currentBest = neighbour;
                    currentObj = newObj;
                    nb_ne++;
                    accepted++;
                }
            }

            // Track best solution
            if (currentObj < bestObj) {
                improvedShifts = deepCopy(currentBest, instance, travelTimes);
                bestObj = currentObj;
                newBestFound++;
            }

            // Cooling
            if (nb_ne > MaxNe || it_p > MaxN) {
                T = T * Tstep;
                if (T < 1e-6) T = 1e-6;
                nb_ne = 0;
                it_p = 0;
            }
        }

        System.out.println("\nInfeas: " + rejectedInfeasible);
        System.out.println("Accepted: " + accepted);
        System.out.println("New best found: " + newBestFound);
        System.out.println("Unchanged: " + unchanged);
        return improvedShifts;
    }

    private static int  pickRandomStop(List<Shift> shifts, Random rng) {
        while (true) {
            // Pick random shift
            int shiftID = rng.nextInt(shifts.size());
            Shift shift = shifts.get(shiftID);

            int pos = rng.nextInt(shift.route.size());
            int stopId = shift.route.get(pos);

            // skip depot
            if (stopId == 0) continue;

            return stopId;
        }
    }

    private static int[] findStop(List<Shift> shifts, int stopID) {
        for (int s = 0; s < shifts.size(); s++) {
            List<Integer> r = shifts.get(s).route;
            for (int i = 0; i < r.size(); i++) {
                if (r.get(i) == stopID) return new int[]{s, i};
            }
        }
        return new int[]{-1, -1};
    }

    private static boolean intraSwap(List<Shift> neighbour, TransportInstance instance, double[][] travelTimes, Random rng) {
        // pick a shift with at least 2 non-depot stops
        int tries = 0;
        int sIdx;
        Shift s;
        do {
            if (++tries > 50) return false;
            sIdx = rng.nextInt(neighbour.size());
            s = neighbour.get(sIdx);
        } while (s.route.size() < 4);

        int n = s.route.size();
        int i = 1 + rng.nextInt(n - 2);
        int j = 1 + rng.nextInt(n - 2);
        if (i == j) return false;

        // swap
        int tmp = s.route.get(i);
        s.route.set(i, s.route.get(j));
        s.route.set(j, tmp);

        Shift rebuilt = buildShiftFromRoute(new ArrayList<>(s.route), s.nightShift, instance, travelTimes);
        if (rebuilt.totalTime > totalShiftLength) return false;

        neighbour.set(sIdx, rebuilt);
        return true;
    }

    private static boolean insertBest(List<Shift> neighbour, TransportInstance instance, double[][] travelTimes, Random rng) {
        // pick from shift with at least one customer
        int fromIdx, toIdx;
        Shift from, to;

        int tries = 0;
        do {
            if (++tries > 50) return false;
            fromIdx = rng.nextInt(neighbour.size());
            from = neighbour.get(fromIdx);
        } while (from.route.size() < 3);

        // pick a stop position in from (exclude depots)
        int removePos = 1 + rng.nextInt(from.route.size() - 2);
        int stopId = from.route.get(removePos);

        // remove it
        from.route.remove(removePos);

        // if from becomes [0,0] that’s okay (empty route), but you might want to avoid it:
        if (from.route.size() < 2) return false;

        // pick to shift (random)
        toIdx = rng.nextInt(neighbour.size());
        to = neighbour.get(toIdx);

        // Check day and night route feasibility
        int currentNightShifts = 0;
        for (Shift shift : neighbour) {
            if (shift.nightShift == 1) {
                currentNightShifts++;
            } else if (isNightShift(shift, instance)) {
                currentNightShifts++;
            }
        }
        if (from.nightShift != to.nightShift || currentNightShifts > 25) {
            return false;
        }

        // find best insertion position in to (between 1..size-1)
        int bestPos = -1;
        double bestDelta = Double.POSITIVE_INFINITY;

        for (int pos = 1; pos < to.route.size(); pos++) {
            double delta = insertionDelta(to.route, pos, stopId, travelTimes);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestPos = pos;
            }
        }
        if (bestPos == -1) return false;

        to.route.add(bestPos, stopId);

        // rebuild affected shifts & check feasibility
        Shift rebuiltFrom = buildShiftFromRoute(new ArrayList<>(from.route), from.nightShift, instance, travelTimes);
        Shift rebuiltTo   = buildShiftFromRoute(new ArrayList<>(to.route),   to.nightShift,   instance, travelTimes);

        if (rebuiltFrom.totalTime > totalShiftLength || rebuiltTo.totalTime > totalShiftLength) {
            return false;
        }

        neighbour.set(fromIdx, rebuiltFrom);
        neighbour.set(toIdx, rebuiltTo);
        return true;
    }

    private static boolean swapAndInsert(List<Shift> neighbour, TransportInstance instance, double[][] travelTimes, Random rng) {
        // pick two different shifts with at least one customer each
        int tries = 0;
        int aIdx, bIdx;
        Shift a, b;

        do {
            if (++tries > 50) return false;
            aIdx = rng.nextInt(neighbour.size());
            bIdx = rng.nextInt(neighbour.size());
        } while (aIdx == bIdx);

        a = neighbour.get(aIdx);
        b = neighbour.get(bIdx);

        // Check day and night route feasibility
        int currentNightShifts = 0;
        for (Shift shift : neighbour) {
            if (shift.nightShift == 1) {
                currentNightShifts++;
            } else if (isNightShift(shift, instance)) {
                currentNightShifts++;
            }
        }
        if (a.nightShift != b.nightShift || currentNightShifts > 25) {
            return false;
        }

        if (a.route.size() < 3 || b.route.size() < 3) return false;

        // pick random customer positions
        int aPos = 1 + rng.nextInt(a.route.size() - 2);
        int bPos = 1 + rng.nextInt(b.route.size() - 2);

        int aStop = a.route.get(aPos);
        int bStop = b.route.get(bPos);

        // remove them (remove larger index first if same list, but here different lists)
        a.route.remove(aPos);
        b.route.remove(bPos);

        // best insertion of aStop into b
        int bestPosInB = -1;
        double bestDeltaB = Double.POSITIVE_INFINITY;
        for (int pos = 1; pos < b.route.size(); pos++) {
            double delta = insertionDelta(b.route, pos, aStop, travelTimes);
            if (delta < bestDeltaB) {
                bestDeltaB = delta;
                bestPosInB = pos;
            }
        }
        if (bestPosInB == -1) return false;
        b.route.add(bestPosInB, aStop);

        // best insertion of bStop into a
        int bestPosInA = -1;
        double bestDeltaA = Double.POSITIVE_INFINITY;
        for (int pos = 1; pos < a.route.size(); pos++) {
            double delta = insertionDelta(a.route, pos, bStop, travelTimes);
            if (delta < bestDeltaA) {
                bestDeltaA = delta;
                bestPosInA = pos;
            }
        }
        if (bestPosInA == -1) return false;
        a.route.add(bestPosInA, bStop);

        Shift rebuiltA = buildShiftFromRoute(new ArrayList<>(a.route), a.nightShift, instance, travelTimes);
        Shift rebuiltB = buildShiftFromRoute(new ArrayList<>(b.route), b.nightShift, instance, travelTimes);

        if (rebuiltA.totalTime > totalShiftLength || rebuiltB.totalTime > totalShiftLength) {
            return false;
        }

        neighbour.set(aIdx, rebuiltA);
        neighbour.set(bIdx, rebuiltB);
        return true;
    }


    private static boolean swap(List<Shift> neighbour, TransportInstance instance, double[][] travelTimes, int stopId1, int stopId2) {
        // Don't swap if depot or same stop
        if (stopId1 == 0 || stopId2 == 0) return false;
        if (stopId1 == stopId2) return false;

        int[] loc1 = findStop(neighbour, stopId1);
        int[] loc2 = findStop(neighbour, stopId2);
        if (loc1[0] < 0 || loc2[0] < 0) return false;

        Shift s1 = neighbour.get(loc1[0]);
        Shift s2 = neighbour.get(loc2[0]);

        int i1 = loc1[1];
        int i2 = loc2[1];

        if (s1.route.get(i1) == 0 || s2.route.get(i2) == 0) return false;

        s1.route.set(i1, stopId2);
        s2.route.set(i2, stopId1);

        neighbour.set(loc1[0], buildShiftFromRoute(new ArrayList<>(s1.route), s1.nightShift, instance, travelTimes));
        if (loc2[0] != loc1[0]) {
            neighbour.set(loc2[0], buildShiftFromRoute(new ArrayList<>(s2.route), s2.nightShift, instance, travelTimes));
        }
        return true;
    }

    private static boolean insert(List<Shift> neighbour,
                               TransportInstance instance, double[][] travelTimes,
                               int stopId1, int stopId2) {

        // Don't swap if depot or same stop
        if (stopId1 == 0 || stopId2 == 0) return false;
        if (stopId1 == stopId2) return false;

        int[] loc1 = findStop(neighbour, stopId1);
        int[] loc2 = findStop(neighbour, stopId2);
        if (loc1[0] < 0 || loc2[0] < 0) return false;

        Shift from = neighbour.get(loc1[0]);
        Shift to   = neighbour.get(loc2[0]);

        int idxA = loc1[1];
        int idxB = loc2[1];

        // safety: don't remove depot, don't insert after depot at end incorrectly
        if (from.route.get(idxA) == 0 || to.route.get(idxB) == 0) return false;

        // Remove stop1
        int removed = from.route.remove(idxA);

        // If same shift and we removed before stop2, stop2 index shifts left
        if (loc1[0] == loc2[0] && idxA < idxB) idxB--;

        // Insert immediately after stop2
        int insertPos = idxB + 1;

        // keep last element as depot (0). If insertPos would go past end, clamp.
        if (insertPos > to.route.size() - 1) insertPos = to.route.size() - 1;

        to.route.add(insertPos, removed);

        // Rebuild both affected shifts
        neighbour.set(loc1[0], buildShiftFromRoute(new ArrayList<>(from.route), from.nightShift, instance, travelTimes));
        if (loc2[0] != loc1[0]) {
            neighbour.set(loc2[0], buildShiftFromRoute(new ArrayList<>(to.route), to.nightShift, instance, travelTimes));
        }
        return true;
    }

    private static double insertionDelta(List<Integer> route, int pos, int stopId, double[][] tt) {
        int prev = route.get(pos - 1);
        int next = route.get(pos);
        return tt[prev][stopId] + tt[stopId][next] - tt[prev][next];
    }

    /**
     * Makes a copy of a given list of shifts
     * @param shifts the shifts to be copied
     * @return a copy of the schedule
     */
    private static List<Shift> deepCopy(List<Shift> shifts, TransportInstance instance, double[][] travelTimes) {
        List<Shift> copy = new ArrayList<>(shifts.size());
        for (Shift s : shifts) {
            List<Integer> newRoute = new ArrayList<>(s.route);
            Shift rebuilt = buildShiftFromRoute(newRoute, s.nightShift, instance, travelTimes);
            copy.add(rebuilt);
        }
        return copy;
    }

    private static Shift buildShiftFromRoute(List<Integer> route, int nightFlag,TransportInstance instance, double[][] travelTimes) {
        double travel = 0.0;
        double service = 0.0;

        for (int i = 0; i < route.size() - 1; i++) {
            int a = route.get(i);
            int b = route.get(i + 1);
            travel += travelTimes[a][b];
        }

        // count service for non-depot stops
        for (int id : route) {
            if (id != 0) service += instance.getStops().get(id).serviceTime;
        }

        return new Shift(route, travel, service, nightFlag);
    }

    public static boolean isNightShift(Shift shift, TransportInstance instance) {
        for (int i = 0; i < shift.route.size(); i++) {
            if (shift.nightShift == 1) {
                return true;
            } else {
                int stopID = shift.route.get(i);
                if (instance.getStops().get(stopID).nightShift == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void resultsToCSV(List<Shift> allShifts, TransportInstance instance, String fileName) {
        // Build lookup: objectId -> Stop (fast)
        Map<Integer, Stop> byId = new HashMap<>();
        for (Stop s : instance.getStops()) {
            byId.put(s.objectId, s);
        }

        Stop depot = instance.getDepot();

        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {
            // Header
            out.println("ID_MAXIMO,Route,Order,Night_shift,longitude,latitude,ID,Service_time");

            // Depot row once
            out.printf("%s,%s,%s,%d,%s,%s,%d,%s%n",
                    escapeCsv(depot.idMaximo),
                    "NA",
                    "NA",
                    100,
                    depot.longitude,
                    depot.latitude,
                    depot.objectId,
                    depot.serviceTime
            );

            // Each shift (name shift by index: 1..k)
            for (int shiftIdx = 0; shiftIdx < allShifts.size(); shiftIdx++) {
                Shift shift = allShifts.get(shiftIdx);
                int routeName = shiftIdx + 1;

                // Each stop in the route, in order
                for (int order = 0; order < shift.route.size(); order++) {
                    int stopId = shift.route.get(order);

                    Stop stop = byId.get(stopId);
                    if (stop == null) {
                        throw new IllegalArgumentException("No Stop found for objectId=" + stopId
                                + " (shift " + routeName + ", order " + (order + 1) + ")");
                    }

                    if (stop.objectId != 0) {
                        out.printf("%s,%s,%s,%d,%s,%s,%d,%s%n",
                                escapeCsv(stop.idMaximo),
                                routeName,
                                order,
                                shift.nightShift,
                                stop.longitude,
                                stop.latitude,
                                stop.objectId,
                                stop.serviceTime
                        );
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write CSV to " + fileName, e);
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        boolean mustQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (!mustQuote) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

}

