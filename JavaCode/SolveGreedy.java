import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class SolveGreedy {
    static final double shiftLength = 7 * 60;


    public static void main(String[] args) {
        File data = new File("data_all.txt");
        File travelTimesFile = new File("travel_times_collapsedv2.txt");

        // Choose cleaning time: {20, code, abri}
        String cleaningIndicator = "abri";

        // Choose night stop indicator: {Night_shift, Type_halte}
        String nightIndicator = "Night_shift";

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

                // Do the greedy on night stops or day stops
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


                // Make csv file
                // resultsToCSV(shifts, allStops, "results_Greedy_abri.csv");

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


