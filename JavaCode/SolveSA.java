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

                // Count number of night shifts and max & min shift length & cleaning time
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
                // resultsToCSV(saShifts, allStops, "results_SA_3min.csv");

            } catch (IOException ex) {
                System.out.println("There was an error reading file " + travelTimesFile);
                ex.printStackTrace();
            }

        } catch (IOException ex) {
            System.out.println("There was an error reading file " + data);
            ex.printStackTrace();
        }
    }

    /**
     * Solves the greedy algorithm
     *
     * @param instance the HTM instance with all stops
     * @param travelTimes the travel times
     * @param allowed the ids of the stops currently allowed (either night or day stops)
     * @param nightFlag 1 if night shifts are made, 0 if day shifts are made
     * @return a list of shifts
     */
    public static List<Shift> solveGreedy(HTMInstance instance, double[][] travelTimes, List<Integer> allowed, int nightFlag) {
        int n = instance.getNStops();
        int depot = 0;

        boolean[] isAllowed = new boolean[n];
        for (int idx : allowed) {
            isAllowed[idx] = true;
        }

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

                    // Must be able to return to the depot in the time allowed
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

    /**
     * Method that formats the route; nice for viewing the results in Java
     *
     * @param instance the HTM instance that contains all the stops
     * @param shiftId the IDs of the stops in the shift
     * @return string of the route that can be printed
     */
    static String formatRoute(HTMInstance instance, List<Integer> shiftId) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < shiftId.size(); k++) {
            Stop s = instance.getStops().get(shiftId.get(k));
            sb.append(s.idMaximo);
            if (k < shiftId.size() - 1) sb.append(" -> ");
        }
        return sb.toString();
    }

    /**
     * Calculates the total objective (= total time of all shifts)
     * @param shifts the shifts
     * @return the objective
     */
    public static double totalObj(List<Shift> shifts) {
        double sum = 0.0;
        for (Shift shift : shifts) {
            sum += shift.totalTime;
        }
        return sum;
    }

    /**
     * Returns the ids of the stops that are allowed in the greedy instance (day stops vs night stops)
     *
     * @param instance the HTMinstance containing all the stops
     * @param nightFlag == 1 if night shift, == 0 if day shift
     * @return list of allowed indices
     */
    private static List<Integer> getAllowedIndices(HTMInstance instance, int nightFlag) {
        List<Integer> id = new ArrayList<>();
        for (int i = 0; i < instance.getNStops(); i++) {
            // skip depot as a "to visit" stop
            if (i == 0) continue;

            // Get only shifts with the correct nightFlag
            if (instance.getStops().get(i).nightShift == nightFlag) {
                id.add(i);
            }
        }
        return id;
    }

    /**
     * Reads the text file with the travel times
     *
     * @param file the file to be read
     * @return an array containing all travel times
     * @throws IOException if the file is incorrectly read
     */
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

    /**
     * Method that does the actual simulated annealing algorithm
     *
     * @param initialShifts initial shifst (e.g. obtained by the greedy)
     * @param instance the HTMInstance containing information on all the stops
     * @param travelTimes the travel times
     * @return a list of optimal shifts found by the SA algorithm
     */
    public static List<Shift> solveSA(List<Shift> initialShifts, HTMInstance instance, double[][] travelTimes) {
        // SA parameters, in order: starting temperature and step factor
        double T = 6000.0;
        double Tstep = 0.95;

        // Maximum number of iterations with the same probability
        int MaxN = 2000;

        // Maximum number of worse moves that are accepted
        int MaxNe = 400;

        // Choose either maximum number of iterations or a time limit
        int MaxIt = 30000000;
        int timeLimit = 180000;

        Random rng = new Random(420);

        // Calculate initial (=best) objective and shifts
        double bestObj = totalObj(initialShifts);
        List<Shift> improvedShifts = deepCopy(initialShifts, instance, travelTimes);

        List<Shift> currentBest = deepCopy(initialShifts, instance, travelTimes);
        double currentObj = totalObj(currentBest);

        // For inspecting the SA , check how many solutions are one of the following
        int rejectedInfeasible = 0;
        int accepted = 0;
        int newBestFound = 0;
        int unchanged = 0;

        // Initialise counters: number of iterations, number of iterations without cooling, number of worse neighbours
        // accepted
        int it = 0;
        int it_p = 0;
        int nb_ne = 0;

        // Either use time limit or max iterations
        long startSA = System.currentTimeMillis();

        // Do the SA loop: choose either time limit or max number of iterations

        while (System.currentTimeMillis() - startSA < timeLimit) {
            //while(it < MaxIt) {

            // Update number of iterations (without cooling) have passed
            it++;
            it_p++;

            // Make neighbour
            List<Shift> neighbour = new ArrayList<>(currentBest);

            // Stores the change in objective
            double[] deltaOut = new double[1];

            // Which shifts are changed by the move
            int[] shiftIdOut = new int[2];
            shiftIdOut[0] = shiftIdOut[1] = -1;

            // Choose neighbourhood: which one depends on probability and the night indicator
            boolean moved = false;

            double rMove = rng.nextDouble();

            if (nightIndicator.equals("Night_shift")) {
                if (rMove < 0.50) {
                    moved = intraSwap(neighbour, instance, travelTimes, rng, deltaOut, shiftIdOut);
                } else if (rMove < 0.85) {
                    moved = insertBest(neighbour, instance, travelTimes, rng, deltaOut, shiftIdOut);
                } else {
                    moved = swapAndInsert(neighbour, instance, travelTimes, rng, deltaOut, shiftIdOut);
                }
            }
            // If you consider all tram stops as night stops, we have a lot more opportunities to cross day stops with
            // night stops -> so more inter moves increase the performance
            else if (nightIndicator.equals("Type_halte")) {
                if (rMove < 0.40) {
                    moved = intraSwap(neighbour, instance, travelTimes, rng, deltaOut, shiftIdOut);
                } else if (rMove < 0.80) {
                    moved = insertBest(neighbour, instance, travelTimes, rng, deltaOut, shiftIdOut);
                } else {
                    moved = swapAndInsert(neighbour, instance, travelTimes, rng, deltaOut, shiftIdOut);
                }
            }

            // If no move was made because of infeasibility, skip the next steps
            if (!moved) continue;

            // Accept / reject decision
            double newObj = currentObj + deltaOut[0];

            // Check if objective changed with this move
            if (Math.abs(newObj - currentObj) < 1e-9) {
                unchanged++;
            }

            // Accept immediately if objective is improved
            if (newObj < currentObj) {
                currentBest = neighbour;
                currentObj = newObj;
                accepted++;
            } else {
                // Else only accept due to cooling
                double r = rng.nextDouble();
                double P = Math.exp((currentObj - newObj) / T);
                if (r < P) {
                    currentBest = neighbour;
                    currentObj = newObj;

                    // Worse neighbour accepted: update counter
                    nb_ne++;
                    accepted++;
                }
            }

            // Track overall best solution
            if (currentObj < bestObj) {
                improvedShifts = deepCopy(currentBest, instance, travelTimes);
                bestObj = currentObj;
                newBestFound++;
            }

            // Cooling: triggered when too many worse neighbours are accepted or too many iterations with the same
            // probability have passed
            if (nb_ne > MaxNe || it_p > MaxN) {
                T = T * Tstep;
                if (T < 1e-6) {
                    T = 1e-6;
                }

                // Reset counters
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


    /**
     * Performs the intra swap neighbourhood: swaps two stops of a shift
     *
     * @param neighbour the current neighbour
     * @param instance HTMInstance
     * @param travelTimes traveltimes
     * @param rng random element
     * @param deltaOut change in objective
     * @param idxOut idxOut[0] is the id of the shift that is changed
     * @return true if the move is made (feasible) and false if not
     */
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

        // Pick two random different stops (excluding the depot)
        int n = s.route.size();
        int i = 1 + rng.nextInt(n - 2);
        int j = 1 + rng.nextInt(n - 2);

        // Same stop, so move is not done
        if (i == j) return false;

        // Perform the swap
        List<Integer> newShift = new ArrayList<>(s.route);
        int temp = newShift.get(i);
        newShift.set(i, newShift.get(j));
        newShift.set(j, temp);

        // Rebuild the shift with the swap
        Shift rebuilt = buildShiftFromRoute(newShift, instance, travelTimes);

        // Check if the new shift does not exceed total shift length
        if (rebuilt.totalTime > totalShiftLength) {
            return false;
        }

        // Change the neighbour
        neighbour.set(sIdx, rebuilt);

        // Save the change in objective and the change in shift
        deltaOut[0] = rebuilt.totalTime - s.totalTime;
        idxOut[0] = sIdx;
        idxOut[1] = -1;
        return true;
    }

    /**
     * Performs the inter insert neighbourhood: inserts a random stop into the best place in a different shift
     *
     * @param neighbour neighbour
     * @param instance HTMInstance
     * @param travelTimes traveltimes
     * @param rng random element
     * @param deltaOut change in objective
     * @param idxOut IdxOut[0] is the first shift, IdxOut[1] is the second shift affected
     * @return true if the move was feasible and thus happened, false if not
     */
    private static boolean insertBest(List<Shift> neighbour, HTMInstance instance, double[][] travelTimes, Random rng, double[] deltaOut, int[] idxOut) {
        int fromIdx;
        int toIdx;
        Shift from;
        Shift to;

        // Pick a shift with at least one non-depot stop that will be moved
        int tries = 0;
        do {
            if (++tries > 50) return false;
            fromIdx = rng.nextInt(neighbour.size());
            from = neighbour.get(fromIdx);
        } while (from.route.size() < 3);

        // Choose a random stop in the chosen shift
        int removePos = 1 + rng.nextInt(from.route.size() - 2);
        int stopId = from.route.get(removePos);

        // Choose a different shift that the stop will be moved ot
        toIdx = rng.nextInt(neighbour.size());
        if (toIdx == fromIdx) {
            return false;
        }
        to = neighbour.get(toIdx);

        // Store the stop type for night shift limit
        boolean movingStopIsNight = (instance.getStops().get(stopId).nightShift == 1);

        // Store if the involved shifts were night or day shifts
        boolean fromWasNight = isNightShift(from, instance);
        boolean toWasNight   = isNightShift(to, instance);

        // Create route copies to avoid changing existing Shifts
        List<Integer> fromRoute = new ArrayList<>(from.route);
        List<Integer> toRoute   = new ArrayList<>(to.route);

        fromRoute.remove(removePos);

        // Avoid that the first shift is now empty
        if (fromRoute.size() < 3) {
            return false;
        }

        // Find the best position to insert the stop in the toShift: stop will be inserted at position "pos"
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

        // Insert the stop into the chosen best position
        toRoute.add(bestPos, stopId);

        // Rebuild shifts
        Shift rebuiltFrom = buildShiftFromRoute(fromRoute, instance, travelTimes);
        Shift rebuiltTo   = buildShiftFromRoute(toRoute,   instance, travelTimes);

        // Time feasibility
        if (rebuiltFrom.totalTime > totalShiftLength || rebuiltTo.totalTime > totalShiftLength) {
            return false;
        }

        // Night limit feasibility: check only if necessary
        boolean fromIsNight = isNightShift(rebuiltFrom, instance);
        boolean toIsNight   = isNightShift(rebuiltTo,   instance);

        // If the moved stop was a nightStop and it was moved to a day shift
        if (movingStopIsNight && !toWasNight && toIsNight) {
            // Count number of night shifts after this move is performed
            int currentNightCount = countNightShiftsDerived(neighbour, instance);

            int nextNightCount = currentNightCount
                    - (fromWasNight ? 1 : 0) - (toWasNight ? 1 : 0)
                    + (fromIsNight  ? 1 : 0) + (toIsNight  ? 1 : 0);

            // If the night shift limit is reached, cannot make the move
            if (nextNightCount > 25) {
                return false;
            }
        }

        // Do the insertion into the actual routes
        neighbour.set(fromIdx, rebuiltFrom);
        neighbour.set(toIdx, rebuiltTo);

        // Get the change in objective and the ids of the changed shifts
        deltaOut[0] = (rebuiltFrom.totalTime + rebuiltTo.totalTime) - (from.totalTime + to.totalTime);
        idxOut[0] = fromIdx;
        idxOut[1] = toIdx;
        return true;
    }

    /**
     * Does the inter swap and insert neighbourhood: swaps two stops and inserts them into the best position in the
     * different shift
     *
     * @param neighbour neighbour
     * @param instance HTMInstance
     * @param travelTimes travel times
     * @param rng random element
     * @param deltaOut change in objective
     * @param idxOut IdxOut[0] and IdxOut[1] are the Ids of the changed shifts
     * @return true if the move was feasible and thus made, false if not
     */
    private static boolean swapAndInsert(List<Shift> neighbour, HTMInstance instance, double[][] travelTimes, Random rng, double[] deltaOut, int[] idxOut) {
        // pick two different shifts with at least one stop each
        int tries = 0;
        int shift1Id;
        int shift2Id;
        Shift shift1;
        Shift shift2;

        do {
            if (++tries > 50) return false;
            shift1Id = rng.nextInt(neighbour.size());
            shift2Id = rng.nextInt(neighbour.size());
        } while (shift1Id == shift2Id);

        shift1 = neighbour.get(shift1Id);
        shift2 = neighbour.get(shift2Id);

        // Check if both shifts have at least one stop
        if (shift1.route.size() < 3 || shift2.route.size() < 3) {
            return false;
        }

        // Check if the shifts were night or day shifts before the swap
        boolean shift1WasNight = isNightShift(shift1, instance);
        boolean shift2WasNight = isNightShift(shift2, instance);

        // Pick random stops from the shifts
        int shift1Pos = 1 + rng.nextInt(shift1.route.size() - 2);
        int shift2Pos = 1 + rng.nextInt(shift2.route.size() - 2);

        int shift1Stop = shift1.route.get(shift1Pos);
        int shift2PStop = shift2.route.get(shift2Pos);

        // Check if the random stops are night or day stops
        boolean shift1StopIsNight = (instance.getStops().get(shift1Stop).nightShift == 1);
        boolean shift2StopIsNight = (instance.getStops().get(shift2PStop).nightShift == 1);

        // Make copies of the routes
        List<Integer> shift1Route = new ArrayList<>(shift1.route);
        List<Integer> shift2PRoute = new ArrayList<>(shift2.route);

        shift1Route.remove(shift1Pos);
        shift2PRoute.remove(shift2Pos);

        // Insert the stop from shift1 into the best position in shift2
        int bestPosInShift2 = -1;
        double bestDeltaShift2 = Double.POSITIVE_INFINITY;
        for (int pos = 1; pos < shift2PRoute.size(); pos++) {
            double delta = insertionDelta(shift2PRoute, pos, shift1Stop, travelTimes);
            if (delta < bestDeltaShift2) {
                bestDeltaShift2 = delta;
                bestPosInShift2 = pos;
            }
        }
        if (bestPosInShift2 == -1) return false;

        // Add the stop from shift1 into the best position in shift2
        shift2PRoute.add(bestPosInShift2, shift1Stop);

        // Now do the same vice versa: insert the stop from shift2 into the best position in shift1
        int bestPosInShift1 = -1;
        double bestDeltaShift1 = Double.POSITIVE_INFINITY;
        for (int pos = 1; pos < shift1Route.size(); pos++) {
            double delta = insertionDelta(shift1Route, pos, shift2PStop, travelTimes);
            if (delta < bestDeltaShift1) {
                bestDeltaShift1 = delta;
                bestPosInShift1= pos;
            }
        }
        if (bestPosInShift1 == -1) return false;

        // Add the stop from shift2 into the best position in shift1
        shift1Route.add(bestPosInShift1, shift2PStop);

        // Rebuild the shifts
        Shift rebuiltShift1 = buildShiftFromRoute(shift1Route, instance, travelTimes);
        Shift rebuiltShift2 = buildShiftFromRoute(shift2PRoute, instance, travelTimes);

        // Check time limit feasibility
        if (rebuiltShift1.totalTime > totalShiftLength || rebuiltShift2.totalTime > totalShiftLength) {
            return false;
        }

        // Check night shift limit if necessary
        boolean shift1IsNight = isNightShift(rebuiltShift1, instance);
        boolean shift2IsNight = isNightShift(rebuiltShift2, instance);

        // Only check night shift limit if a new night shift was created due to this move
        boolean createsNewNightShift =
                (!shift1WasNight && shift1IsNight && shift2StopIsNight) || (!shift2WasNight && shift2IsNight && shift1StopIsNight);

        // If indeed a new night shift was created due to this swap, check limit
        if (createsNewNightShift) {
            int currentNightCount = countNightShiftsDerived(neighbour, instance);

            int nextNightCount = currentNightCount
                    - (shift1WasNight ? 1 : 0) - (shift2WasNight ? 1 : 0)
                    + (shift1IsNight  ? 1 : 0) + (shift2IsNight  ? 1 : 0);

            // Move is not possible if night shift limit is reached
            if (nextNightCount > 25){
                return false;
            }
        }

        // Do the actual move
        neighbour.set(shift1Id, rebuiltShift1);
        neighbour.set(shift2Id, rebuiltShift2);

        // Return the change in objective and the shifts that were affected
        deltaOut[0] = (rebuiltShift1.totalTime + rebuiltShift2.totalTime) - (shift1.totalTime + shift2.totalTime);
        idxOut[0] = shift1Id;
        idxOut[1] = shift2Id;
        return true;
    }


    /**
     * Returns change in objective by inserting a stop in a specific position in a route
     * @param route route that is changed
     * @param pos position
     * @param stopId ID of the stop that is inserted
     * @param travelTimes travel times
     * @return the change in objective
     */
    private static double insertionDelta(List<Integer> route, int pos, int stopId, double[][] travelTimes) {
        int prev = route.get(pos - 1);
        int next = route.get(pos);
        return travelTimes[prev][stopId] + travelTimes[stopId][next] - travelTimes[prev][next];
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

    /**
     * Builds a Shift object from a route (= list of stop IDs that follow one another in a shift)
     * @param route the route
     * @param instance HTMInstance
     * @param travelTimes travel times
     * @return the Shift object made from the routes
     */
    private static Shift buildShiftFromRoute(List<Integer> route, HTMInstance instance, double[][] travelTimes) {
        double travel = 0.0;
        double cleaning = 0.0;

        // Counts travel times
        for (int i = 0; i < route.size() - 1; i++) {
            int a = route.get(i);
            int b = route.get(i + 1);
            travel += travelTimes[a][b];
        }

        boolean night = false;
        for (int id : route) {
            if (id != 0) {
                // Adds cleaning time of the stop
                cleaning += instance.getStops().get(id).serviceTime;
            }
            // Checks if the Shift is a night or day shift
            if (id != 0 && instance.getStops().get(id).nightShift == 1) {
                night = true;
            }
        }

        return new Shift(route, travel, cleaning, night ? 1 : 0);
    }

    /**
     * Counts the number of night shifts in a list of shifts
     * @param shifts list of shifts
     * @param instance HTMInstance
     * @return the number of night shifts
     */
    private static int countNightShiftsDerived(List<Shift> shifts, HTMInstance instance) {
        int nNightShifts = 0;
        for (Shift s : shifts) {
            if (isNightShift(s, instance)) {
                nNightShifts++;
            }
        }
        return nNightShifts;
    }

    /**
     * Relays if a given shift is a night shift or not, where a night shift is a shift that contains at least one night
     * stop
     * @param shift shift
     * @param instance HTMInstance
     * @return true if it is a night shift, false is not
     */
    public static boolean isNightShift(Shift shift, HTMInstance instance) {
        for (int stopId : shift.route) {
            if (stopId != 0 && instance.getStops().get(stopId).nightShift == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Exports the results to a CSV file in the desired format
     *
     * @param shifts list of shifts
     * @param instance HTMinstance containing information on the stops
     * @param fileName the name of the file (change in main)
     */
    public static void resultsToCSV(List<Shift> shifts, HTMInstance instance, String fileName) {
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

            // Each shift
            for (int shiftId = 0; shiftId < shifts.size(); shiftId++) {
                Shift shift = shifts.get(shiftId);
                int shiftName = shiftId + 1;

                // Each stop in the shift, in order
                for (int order = 0; order < shift.route.size(); order++) {
                    int stopId = shift.route.get(order);

                    Stop stop = byId.get(stopId);
                    if (stop == null) {
                        throw new IllegalArgumentException("No Stop found for objectId=" + stopId
                                + " (shift " + shiftName + ", order " + (order + 1) + ")");
                    }

                    // Save in the desired format
                    if (stop.objectId != 0) {
                        out.printf("%s,%s,%s,%d,%s,%s,%d,%s%n",
                                escapeCsv(stop.idMaximo),
                                shiftName,
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

    /**
     * Necessary for writing ID_MAXIMO so it is correctly exported to the csv file
     * @param s string containing the ID_MAXIMO
     * @return the correct output for the csv file
     */
    private static String escapeCsv(String s) {
        if (s == null) return "";
        boolean mustQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (!mustQuote) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

}


