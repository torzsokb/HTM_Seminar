import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class SolveSA {
    static final double shiftLength = 7 * 60;

    static final double totalShiftLength = 8 * 60;

    // Choose cleaning time: {20, code, abri}
    static final String cleaningIndicator = "abri";

    // Choose night stop indicator: {Night_shift, Type_halte}
    static final String nightIndicator = "Night_shift";

    public static void main(String[] args) {
        File data = new File("data_all.txt");
        File travelTimesFile = new File("travel_times_collapsedv2.txt");

        try {
            HTMInstance allStops = HTMInstance.read(data, cleaningIndicator, nightIndicator);
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

                double shortestShiftLength = 1000.0;
                double longestShiftLength = 0.0;

                double shortestCleaningTime = 1000.0;
                double longestCleaningTime = 0.0;

                System.out.println("Nightshifts:");
                for (int r = 0; r < nightShifts.size(); r++) {
                    Shift shift = nightShifts.get(r);
                    System.out.println("Shift " + (r + 1) + ": " + formatRoute(allStops, shift.route));
                    System.out.println("Takes " + (shift.totalTime / 60.0) + " hours.");
                    if (shift.totalTime > longestShiftLength) {
                        longestShiftLength = (shift.totalTime) ;
                    } else if (shift.totalTime < shortestShiftLength) {
                        shortestShiftLength = (shift.totalTime) ;
                    }

                    if (shift.serviceTime > longestCleaningTime) {
                        longestCleaningTime = (shift.serviceTime) ;
                    } else if (shift.serviceTime < shortestCleaningTime) {
                        shortestCleaningTime = (shift.serviceTime) ;
                    }

                }

                System.out.println("Dayshifts:");
                for (int r = 0; r < dayShifts.size(); r++) {
                    Shift shift = dayShifts.get(r);
                    System.out.println("Shift " + (r + 1) + ": " + formatRoute(allStops, shift.route));
                    System.out.println("Takes " + (shift.totalTime / 60.0) + " hours.");
                    if (shift.totalTime > longestShiftLength) {
                        longestShiftLength = (shift.totalTime) ;
                    } else if (shift.totalTime < shortestShiftLength) {
                        shortestShiftLength = (shift.totalTime) ;
                    }

                    if (shift.serviceTime > longestCleaningTime) {
                        longestCleaningTime = (shift.serviceTime) ;
                    } else if (shift.serviceTime < shortestCleaningTime) {
                        shortestCleaningTime = (shift.serviceTime) ;
                    }
                }

                System.out.println("\nObjective (total time): " + (obj / 60.0) + " hours.");
                System.out.println("Number of shifts: " + shifts.size());
                System.out.println("Average duration of shift: " + ((obj / 60.0) /shifts.size()) + " hours.");
                System.out.println("Shortest shift length: " + shortestShiftLength / 60.0 + " hours.");
                System.out.println("Longest shift length: " + longestShiftLength / 60.0 + " hours.");
                System.out.println("Shortest cleaning time: " + shortestCleaningTime / 60.0 + " hours.");
                System.out.println("Longest cleaning time: " + longestCleaningTime / 60.0 + " hours.");
                System.out.println("Total runtime: " + runTime);


                // Do the SA
                System.out.println("\nSolving SA:");
                long saStartRunTime = System.currentTimeMillis();
                List<Shift> saShifts = solveSA(shifts, allStops, travelTimes);
                long saEndRunTime = System.currentTimeMillis();
                long saRunTime = saEndRunTime - saStartRunTime;

                shortestShiftLength = 1000.0;
                longestShiftLength = 0.0;

                shortestCleaningTime = 1000.0;
                longestCleaningTime = 0.0;

                int nNight = 0;
                for (Shift shift : saShifts) {
                    if (isNightShift(shift, allStops)){
                        shift.nightShift = 1;
                        nNight++;
                    } else {
                        shift.nightShift = 0;
                    }

                    if (shift.totalTime > longestShiftLength) {
                        longestShiftLength = (shift.totalTime) ;
                    } else if (shift.totalTime < shortestShiftLength) {
                        shortestShiftLength = (shift.totalTime) ;
                    }

                    if (shift.serviceTime > longestCleaningTime) {
                        longestCleaningTime = (shift.serviceTime) ;
                    } else if (shift.serviceTime < shortestCleaningTime) {
                        shortestCleaningTime = (shift.serviceTime) ;
                    }
                }
                double newObj = totalObj(saShifts);
                for (int r = 0; r < saShifts.size(); r++) {
                    Shift shift = saShifts.get(r);
                    System.out.println("Shift " + (r + 1) + ": " + formatRoute(allStops, shift.route));
                    System.out.println("Takes " + (shift.totalTime / 60.0) + " hours.");
                }

                System.out.println("\nObjective (total time): " + (newObj / 60.0) + " hours.");
                System.out.println("Improvement: " + ((obj - newObj) / 60.0) + " hours.");
                System.out.println("Number of nightshifts: " + nNight);
                System.out.println("Average duration of shift: " + ((newObj / 60.0) / saShifts.size()) + " hours.");
                System.out.println("Shortest shift length: " + shortestShiftLength / 60.0 + " hours.");
                System.out.println("Longest shift length: " + longestShiftLength / 60.0 + " hours.");
                System.out.println("Shortest cleaning time: " + shortestCleaningTime / 60.0 + " hours.");
                System.out.println("Longest cleaning time: " + longestCleaningTime / 60.0 + " hours.");
                System.out.println("Total runtime for SA: " + saRunTime);

                // Make csv file
                //resultsToCSV(saShifts, allStops, "results_SA_1102.csv");

            } catch (IOException ex) {
                System.out.println("There was an error reading file " + travelTimesFile);
                ex.printStackTrace();
            }

        } catch (IOException ex) {
            System.out.println("There was an error reading file " + data);
            ex.printStackTrace();
        }
    }

    public static List<Shift> solveGreedy(HTMInstance instance, double[][] travelTimes, List<Integer> allowed, int nightFlag) {
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

    static String formatRoute(HTMInstance instance, List<Integer> routeIdx) {
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

    private static List<Integer> getAllowedIndices(HTMInstance instance, int nightFlag) {
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

    public static List<Shift> solveSA(List<Shift> initialShifts, HTMInstance instance, double[][] travelTimes) {
        // SA parameters
        double T = 6000.0;
        double Tstep = 0.95;
        int MaxN = 2000;
        int MaxNe = 400;
        int MaxIt = 30000000;
        int timeLimit = 180000;

        Random rng = new Random(420);

        double bestObj = totalObj(initialShifts);

        List<Shift> currentBest = deepCopy(initialShifts, instance, travelTimes);

        List<Shift> improvedShifts = deepCopy(currentBest, instance, travelTimes);

        double currentObj = totalObj(currentBest);

        int rejectedInfeasible = 0;
        int accepted = 0;
        int newBestFound = 0;
        int unchanged = 0;

        // Initialise counters
        int it = 0;
        int it_p = 0;
        int nb_ne = 0;

        // Either use time limit or max iterations
        long startSA = System.currentTimeMillis();

        // Do the SA loop

        while (System.currentTimeMillis() - startSA < timeLimit) {
            //while(it < MaxIt) {
            it++;
            it_p++;

            // Make neighbour
            List<Shift> neighbour = new ArrayList<>(currentBest);

            // Choose two random stops from random shifts
            int stopId1 =  pickRandomStop(neighbour, rng);
            int stopId2 =  pickRandomStop(neighbour, rng);

            // Avoid exact same stop
            for (int tries = 0; tries < 10 && stopId1 == stopId2; tries++) {
                stopId2 = pickRandomStop(neighbour, rng);
            }
            if (stopId1 == stopId2) continue;

            double[] deltaOut = new double[1];
            int[] idxOut = new int[2];       // for 1 or 2 changed shifts
            idxOut[0] = idxOut[1] = -1;

            // Choose neighbourhood
            boolean moved = false;

            double rMove = rng.nextDouble();

            if (nightIndicator.equals("Night_shift")) {
                if (rMove < 0.50) {
                    moved = intraSwap(neighbour, instance, travelTimes, rng, deltaOut, idxOut);
                } else if (rMove < 0.85) {
                    moved = insertBest(neighbour, instance, travelTimes, rng, deltaOut, idxOut);
                } else {
                    moved = swapAndInsert(neighbour, instance, travelTimes, rng, deltaOut, idxOut);
                }
            } else if (nightIndicator.equals("Type_halte")) {
                if (rMove < 0.40) {
                    moved = intraSwap(neighbour, instance, travelTimes, rng, deltaOut, idxOut);
                } else if (rMove < 0.80) {
                    moved = insertBest(neighbour, instance, travelTimes, rng, deltaOut, idxOut);
                } else {
                    moved = swapAndInsert(neighbour, instance, travelTimes, rng, deltaOut, idxOut);
                }
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
            double newObj = currentObj + deltaOut[0];
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

    private static boolean intraSwap(List<Shift> neighbour, HTMInstance instance, double[][] travelTimes, Random rng, double[] deltaOut, int[] idxOut) {
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
        List<Integer> newRoute = new ArrayList<>(s.route);
        int tmp = newRoute.get(i);
        newRoute.set(i, newRoute.get(j));
        newRoute.set(j, tmp);

        Shift rebuilt = buildShiftFromRoute(newRoute, instance, travelTimes);
        if (rebuilt.totalTime > totalShiftLength) return false;

        neighbour.set(sIdx, rebuilt);

        deltaOut[0] = rebuilt.totalTime - s.totalTime;
        idxOut[0] = sIdx;
        idxOut[1] = -1;
        return true;
    }


    private static boolean insertBest(List<Shift> neighbour, HTMInstance instance, double[][] travelTimes, Random rng, double[] deltaOut, int[] idxOut) {
        int fromIdx, toIdx;
        Shift from, to;

        int tries = 0;
        do {
            if (++tries > 50) return false;
            fromIdx = rng.nextInt(neighbour.size());
            from = neighbour.get(fromIdx);
        } while (from.route.size() < 3);

        // choose stop to move (exclude depots)
        int removePos = 1 + rng.nextInt(from.route.size() - 2);
        int stopId = from.route.get(removePos);

        // choose destination shift
        toIdx = rng.nextInt(neighbour.size());
        if (toIdx == fromIdx) return false;
        to = neighbour.get(toIdx);

        // Stop type
        boolean movingStopIsNight = (instance.getStops().get(stopId).nightShift == 1);

        boolean fromWasNight = isNightShift(from, instance);
        boolean toWasNight   = isNightShift(to, instance);

        // Mutate routes
        List<Integer> fromRoute = new ArrayList<>(from.route);
        List<Integer> toRoute   = new ArrayList<>(to.route);

        fromRoute.remove(removePos);

        // avoid empty-from shift
        if (fromRoute.size() < 3) return false;

        // best insertion position into toRoute
        int bestPos = -1;
        double bestDelta = Double.POSITIVE_INFINITY;
        for (int pos = 1; pos < toRoute.size(); pos++) {
            double delta = insertionDelta(toRoute, pos, stopId, travelTimes);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestPos = pos;
            }
        }
        if (bestPos == -1) return false;

        toRoute.add(bestPos, stopId);

        // Rebuild shifts
        Shift rebuiltFrom = buildShiftFromRoute(fromRoute, instance, travelTimes);
        Shift rebuiltTo   = buildShiftFromRoute(toRoute,   instance, travelTimes);

        // Time feasibility
        if (rebuiltFrom.totalTime > totalShiftLength || rebuiltTo.totalTime > totalShiftLength) return false;

        // Night limit feasibility
        boolean fromIsNight = isNightShift(rebuiltFrom, instance);
        boolean toIsNight   = isNightShift(rebuiltTo,   instance);

        // Check night limit if needed
        if (movingStopIsNight && !toWasNight && toIsNight) {
            int currentNightCount = countNightShiftsDerived(neighbour, instance);

            int nextNightCount = currentNightCount
                    - (fromWasNight ? 1 : 0) - (toWasNight ? 1 : 0)
                    + (fromIsNight  ? 1 : 0) + (toIsNight  ? 1 : 0);

            if (nextNightCount > 25) return false;
        }

        // insert
        neighbour.set(fromIdx, rebuiltFrom);
        neighbour.set(toIdx, rebuiltTo);

        deltaOut[0] = (rebuiltFrom.totalTime + rebuiltTo.totalTime) - (from.totalTime + to.totalTime);
        idxOut[0] = fromIdx;
        idxOut[1] = toIdx;
        return true;
    }

    private static boolean swapAndInsert(List<Shift> neighbour, HTMInstance instance, double[][] travelTimes, Random rng, double[] deltaOut, int[] idxOut) {
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

        if (a.route.size() < 3 || b.route.size() < 3) return false;

        // BEFORE night statuses
        boolean aWasNight = isNightShift(a, instance);
        boolean bWasNight = isNightShift(b, instance);

        // pick customer positions
        int aPos = 1 + rng.nextInt(a.route.size() - 2);
        int bPos = 1 + rng.nextInt(b.route.size() - 2);

        int aStop = a.route.get(aPos);
        int bStop = b.route.get(bPos);

        boolean aStopIsNight = (instance.getStops().get(aStop).nightShift == 1);
        boolean bStopIsNight = (instance.getStops().get(bStop).nightShift == 1);

        // Work on copies
        List<Integer> aRoute = new ArrayList<>(a.route);
        List<Integer> bRoute = new ArrayList<>(b.route);

        aRoute.remove(aPos);
        bRoute.remove(bPos);

        // insert aStop into bRoute best place
        int bestPosInB = -1;
        double bestDeltaB = Double.POSITIVE_INFINITY;
        for (int pos = 1; pos < bRoute.size(); pos++) {
            double delta = insertionDelta(bRoute, pos, aStop, travelTimes);
            if (delta < bestDeltaB) {
                bestDeltaB = delta;
                bestPosInB = pos;
            }
        }
        if (bestPosInB == -1) return false;
        bRoute.add(bestPosInB, aStop);

        // insert bStop into aRoute best place
        int bestPosInA = -1;
        double bestDeltaA = Double.POSITIVE_INFINITY;
        for (int pos = 1; pos < aRoute.size(); pos++) {
            double delta = insertionDelta(aRoute, pos, bStop, travelTimes);
            if (delta < bestDeltaA) {
                bestDeltaA = delta;
                bestPosInA = pos;
            }
        }
        if (bestPosInA == -1) return false;
        aRoute.add(bestPosInA, bStop);

        // Rebuild
        Shift rebuiltA = buildShiftFromRoute(aRoute, instance, travelTimes);
        Shift rebuiltB = buildShiftFromRoute(bRoute, instance, travelTimes);

        if (rebuiltA.totalTime > totalShiftLength || rebuiltB.totalTime > totalShiftLength) return false;

        // Night limit check
        boolean aIsNight = isNightShift(rebuiltA, instance);
        boolean bIsNight = isNightShift(rebuiltB, instance);

        // Check night limit if needed
        boolean createsNewNightShift =
                (!aWasNight && aIsNight && bStopIsNight) || (!bWasNight && bIsNight && aStopIsNight);

        if (createsNewNightShift) {
            int currentNightCount = countNightShiftsDerived(neighbour, instance);

            int nextNightCount = currentNightCount
                    - (aWasNight ? 1 : 0) - (bWasNight ? 1 : 0)
                    + (aIsNight  ? 1 : 0) + (bIsNight  ? 1 : 0);

            if (nextNightCount > 25) return false;
        }

        // Commit
        neighbour.set(aIdx, rebuiltA);
        neighbour.set(bIdx, rebuiltB);

        deltaOut[0] = (rebuiltA.totalTime + rebuiltB.totalTime) - (a.totalTime + b.totalTime);
        idxOut[0] = aIdx;
        idxOut[1] = bIdx;
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
    private static List<Shift> deepCopy(List<Shift> shifts, HTMInstance instance, double[][] travelTimes) {
        List<Shift> copy = new ArrayList<>(shifts.size());
        for (Shift s : shifts) {
            List<Integer> newRoute = new ArrayList<>(s.route);
            Shift rebuilt = buildShiftFromRoute(newRoute, instance, travelTimes);
            copy.add(rebuilt);
        }
        return copy;
    }

    private static Shift buildShiftFromRoute(List<Integer> route, HTMInstance instance, double[][] travelTimes) {
        double travel = 0.0;
        double cleaning = 0.0;

        for (int i = 0; i < route.size() - 1; i++) {
            int a = route.get(i);
            int b = route.get(i + 1);
            travel += travelTimes[a][b];
        }

        boolean night = false;
        for (int id : route) {
            if (id != 0) cleaning += instance.getStops().get(id).serviceTime;
            if (id != 0 && instance.getStops().get(id).nightShift == 1) night = true;
        }

        return new Shift(route, travel, cleaning, night ? 1 : 0);
    }

    private static int countNightShiftsDerived(List<Shift> shifts, HTMInstance instance) {
        int c = 0;
        for (Shift s : shifts) if (isNightShift(s, instance)) c++;
        return c;
    }

    public static boolean isNightShift(Shift shift, HTMInstance instance) {
        for (int stopId : shift.route) {
            if (stopId != 0 && instance.getStops().get(stopId).nightShift == 1) {
                return true;
            }
        }
        return false;
    }

    public static void resultsToCSV(List<Shift> allShifts, HTMInstance instance, String fileName) {
        // Build lookup: objectId -> Stop
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


