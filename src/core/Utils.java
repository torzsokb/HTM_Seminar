package core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class Utils {
    public static HTMInstance readInstance(
            String path,
            String cleaningIndicator,
            String nightIndicator
    ) throws IOException {
        File f = new File(path);
        return HTMInstance.read(f, cleaningIndicator, nightIndicator);
    }

    public static double[][] readTravelTimes(String path) throws IOException {
        File file = new File(path);
        ArrayList<double[]> rows = new ArrayList<>();
        int cols = -1;

        try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
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
        for (int i = 0; i < rows.size(); i++) {
            matrix[i] = rows.get(i);
        }
        return matrix;
    }


    public static List<Integer> getAllowedIndices(HTMInstance instance, int nightFlag) {
        List<Integer> id = new ArrayList<>();
        for (int i = 0; i < instance.getNStops(); i++) {
            if (i == 0) continue;
            if (instance.getStops().get(i).nightShift == nightFlag) {
                id.add(i);
            }
        }
        return id;
    }

    public static double totalObjective(List<Shift> shifts) {
        if (shifts == null || shifts.isEmpty()) {
            return 0.0;
        }
    
        double total = 0.0;
        for (Shift s : shifts) {
            if (s != null) {
                total += s.totalTime;
            }
        }
        return total/60.0;
    }

    public static void printShiftStatistics(List<Shift> shifts, HTMInstance instance, double shiftLimitMinutes) {

        List<Shift> day = new ArrayList<>();
        List<Shift> night = new ArrayList<>();
    
        for (Shift s : shifts) {
            if (s.nightShift == 1) night.add(s);
            else day.add(s);
        }
    
        System.out.println();
        System.out.println(" Day routes:");
        printGroupStats(day, shiftLimitMinutes);
    
        System.out.println();
        System.out.println(" Night routes:");
        printGroupStats(night, shiftLimitMinutes);
    
        System.out.println();
        System.out.println(" Overall:");
        printGroupStats(shifts, shiftLimitMinutes);
    }
    
    private static void printGroupStats(List<Shift> group, double shiftLimitMinutes) {
    
        int n = group.size();
        double totalTravel = 0.0;
        double totalService = 0.0;
        double totalLength = 0.0;
    
        double shortest = Double.POSITIVE_INFINITY;
        double longest = 0.0;

        double shortestClean = Double.POSITIVE_INFINITY;
        double longestClean = 0.0;
    
        int violated = 0;
    
        for (Shift s : group) {
            double len = s.totalTime;
            double clean = s.serviceTime;
    
            totalTravel += s.travelTime;
            totalService += s.serviceTime;
            totalLength += len;
    
            if (len < shortest) shortest = len;
            if (len > longest) longest = len;

            if (clean < shortestClean) shortestClean = clean;
            if (clean > longestClean) longestClean = clean;
    
            if (len > shiftLimitMinutes) violated++;
        }
    
        double avg = (n == 0) ? 0.0 : totalLength / n;

        System.out.printf("Number of routes:     %d%n", n);
        System.out.printf("Avg shift length:     %.5f h%n", avg / 60.0);
        System.out.printf("Shortest shift:       %.5f h%n", shortest / 60.0);
        System.out.printf("Longest shift:        %.5f h%n", longest / 60.0);
        System.out.printf("Shortest cleaning:    %.5f h%n", shortestClean / 60.0);
        System.out.printf("Longest cleaning:     %.5f h%n", longestClean / 60.0);
        System.out.printf("Total travel time:    %.5f h%n", totalTravel / 60.0);
        System.out.printf("Total service time:   %.5f h%n", totalService / 60.0);
        System.out.printf("Violated routes:      %d%n", violated);
    }
    

    public static void recomputeShift(Shift s, HTMInstance instance, double[][] travelTimes) {
        double travel = 0.0;
        double service = 0.0;
    
        if (!s.route.isEmpty()) {
    
            travel += travelTimes[0][s.route.get(0)];

            for (int k = 0; k < s.route.size() - 1; k++) {
                int a = s.route.get(k);
                int b = s.route.get(k + 1);
                travel += travelTimes[a][b];
            }
    
            travel += travelTimes[s.route.get(s.route.size() - 1)][0];
        }
    
        for (int id : s.route) {
            service += instance.getStops().get(id).serviceTime;
        }
    
        s.travelTime = travel;
        s.serviceTime = service;
        s.recomputeTotalTime();
    }
    
    
    public static void recomputeAllShifts(List<Shift> shifts, HTMInstance instance, double[][] travelTimes) {
        for (Shift s : shifts) {
            recomputeShift(s, instance, travelTimes);
        }
    }
    
    public static List<Shift> buildGreedyShifts(
            HTMInstance instance,
            double[][] travelTimes,
            List<Integer> allowed,
            int nightFlag,
            double shiftLength
    ) {
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

                if (next == -1) {
                    double back = travelTimes[current][depot];
                    travelTime += back;
                    route.add(depot);

                    shifts.add(new Shift(route, travelTime, serviceTime, nightFlag));
                    break;
                }

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

    public static void checkFeasibility(
        List<Shift> shifts,
        HTMInstance instance,
        double maxShiftDurationMinutes
) {
    System.out.println("\n=== FEASIBILITY CHECK ===");

    boolean feasible = true;

    int n = instance.getNStops();
    boolean[] seen = new boolean[n];

    // --------------------------------------------------
    // 1) Depot start/end validation
    // --------------------------------------------------
    for (int idx = 0; idx < shifts.size(); idx++) {
        Shift s = shifts.get(idx);

        if (s.route.isEmpty()) {
            System.out.println("Shift " + idx + " has empty route.");
            feasible = false;
            continue;
        }

        if (s.route.get(0) != 0) {
            System.out.println("Shift " + idx + " does NOT start at depot.");
            feasible = false;
        }

        if (s.route.get(s.route.size() - 1) != 0) {
            System.out.println("Shift " + idx + " does NOT end at depot.");
            feasible = false;
        }
    }

    // --------------------------------------------------
    // 2) Stop coverage (excluding depot)
    // --------------------------------------------------
    for (Shift s : shifts) {
        for (int id : s.route) {
            if (id == 0) continue;

            if (seen[id]) {
                System.out.println("Duplicate stop found: stop " + id);
                feasible = false;
            }
            seen[id] = true;
        }
    }

    for (int i = 1; i < n; i++) {
        if (!seen[i]) {
            System.out.println("Missing stop: stop " + i);
            feasible = false;
        }
    }

    // --------------------------------------------------
    // 3) Duration constraint
    // --------------------------------------------------
    int violatedDuration = 0;
    for (int idx = 0; idx < shifts.size(); idx++) {
        Shift s = shifts.get(idx);
        if (s.totalTime > maxShiftDurationMinutes) {
            System.out.printf("Shift %d violates duration: %.2f min%n",
                    idx, s.totalTime);
            violatedDuration++;
            feasible = false;
        }
    }

    // --------------------------------------------------
    // 4) Night shift constraint
    // --------------------------------------------------
    int nightShifts = 0;
    for (Shift s : shifts) {
        boolean isNight = false;
        for (int id : s.route) {
            if (id == 0) continue;

            if (instance.getStops().get(id).nightShift == 1) {
                isNight = true;
                break;
            }
        }
        if (isNight) nightShifts++;
    }

    if (nightShifts > 25) {
        System.out.println("Too many night shifts: " + nightShifts);
        feasible = false;
    }

    // --------------------------------------------------
    // Summary
    // --------------------------------------------------
    System.out.println("\nSummary:");
    System.out.println("Night shifts: " + nightShifts);
    System.out.println("Duration violations: " + violatedDuration);
    System.out.println("All constraints satisfied: " + (feasible ? "YES" : "NO"));

    if (feasible) {
        System.out.println("Solution is FEASIBLE.");
    } else {
        System.out.println("Solution is NOT FEASIBLE.");
    }
}
    public static int countNightShifts(List<Shift> shifts) {
        int count = 0;
        for (Shift s : shifts) {
            if (s.nightShift == 1) count++;
        }
        return count;
    }

    public static boolean containsNightStop(List<Integer> route, HTMInstance instance) {
        for (int id : route) {
            if (instance.getStops().get(id).nightShift == 1) {
                return true;
            }
        }
        return false;
    }
    public static List<Shift> deepCopyShifts(List<Shift> shifts) {
        List<Shift> copy = new ArrayList<>();
        for (Shift s : shifts) {
            List<Integer> routeCopy = new ArrayList<>(s.route);
            copy.add(new Shift(routeCopy, s.travelTime, s.serviceTime, s.nightShift));
        }
        return copy;
    }

    /**
     * Method that formats the route; nice for viewing the results in Java
     *
     * @param instance the HTM instance that contains all the stops
     * @param shiftId the IDs of the stops in the shift
     * @return string of the route that can be printed
     */
    public static String formatRoute(HTMInstance instance, List<Integer> shiftId) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < shiftId.size(); k++) {
            Stop s = instance.getStops().get(shiftId.get(k));
            sb.append(s.idMaximo);
            if (k < shiftId.size() - 1) sb.append(" -> ");
        }
        return sb.toString();
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
            System.out.println("Finished writing CSV to " + fileName);
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

    public static List<Shift> readShiftsFromCSV(String pathToCsv, double[][] travelTimes) throws IOException {
        List<Shift> shifts = new ArrayList<>();

        File csvFile = new File(pathToCsv);
        // Each row corresponds to a stop: map these stops to a route
        Map<String, List<double[]>> byRoute = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;

            // Read header (ignore)
            line = br.readLine();
            if (line == null) {
                return shifts;
            }

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] c = line.split(",", -1);
                if (c.length != 8) {
                    throw new IllegalArgumentException("Expected 8 columns, got " + c.length + " in line: " + line);
                }

                // Fixed indices (your spec)
                String routeName = c[1].trim(); // Route
                String orderStr  = c[2].trim(); // Order
                String nightStr  = c[3].trim(); // Night_shift
                String idStr     = c[6].trim(); // ID
                String servStr   = c[7].trim(); // Service/Cleaning time

                int stopId = Integer.parseInt(idStr);

                // Skip depot row (ID=0)
                if (stopId == 0) continue;

                if (routeName.isEmpty() || routeName.equalsIgnoreCase("NA")) {
                    throw new IllegalArgumentException("Missing Route for non-depot row: " + line);
                }

                if (orderStr.isEmpty() || orderStr.equalsIgnoreCase("NA")) {
                    throw new IllegalArgumentException("Order is NA/empty for non-depot row: " + line);
                }
                int order = Integer.parseInt(orderStr);

                int night = 0;
                if (!nightStr.isEmpty() && !nightStr.equalsIgnoreCase("NA")) {
                    int raw = Integer.parseInt(nightStr);
                    night = (raw == 1) ? 1 : 0;
                }

                double serviceTime = Double.parseDouble(servStr);

                byRoute.computeIfAbsent(routeName, k -> new ArrayList<>())
                        .add(new double[]{stopId, order, night, serviceTime});
            }
        }

        // Build Shift objects
        for (Map.Entry<String, List<double[]>> entry : byRoute.entrySet()) {
            List<double[]> rows = entry.getValue();
            rows.sort(Comparator.comparingDouble(r -> r[1])); // sort by Order

            // ---- route includes depot at start and end ----
            List<Integer> route = new ArrayList<>(rows.size() + 2);
            route.add(0); // start at depot

            double serviceSum = 0.0;
            boolean hasNightStop = false;

            for (double[] r : rows) {
                int stopId = (int) r[0];
                int night  = (int) r[2];
                double serv = r[3];

                route.add(stopId);
                serviceSum += serv;
                if (night == 1) hasNightStop = true;
            }

            route.add(0); // end at depot

            // ---- travel time includes depot legs (0->first and last->0) ----
            double travelSum = 0.0;
            for (int i = 0; i < route.size() - 1; i++) {
                int a = route.get(i);
                int b = route.get(i + 1);

                if (a < 0 || a >= travelTimes.length || b < 0 || b >= travelTimes[a].length) {
                    throw new IllegalArgumentException(
                            "travelTimes index out of bounds for edge " + a + "->" + b +
                                    " in route=" + entry.getKey() + " file=" + csvFile.getName()
                    );
                }
                travelSum += travelTimes[a][b];
            }

            // This Shift constructor will add prep+break itself via totalTime
            shifts.add(new Shift(route, travelSum, serviceSum, hasNightStop ? 1 : 0));
        }

        return shifts;
    }
}
