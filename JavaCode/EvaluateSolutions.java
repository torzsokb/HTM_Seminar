import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class EvaluateSolutions {
    static final double totalShiftLength = 8 * 60;

    public static void main(String[] args) {
        File travelTimesFile = new File("travel_times_collapsedv2.txt");

        File initResultsFile = new File("HTM_data_initRes.csv");

        File greedyResultsFile = new File("results_Greedy_abri.csv");

        try {
            System.out.println("Reading travel times ");
            double[][] travelTimes = readTravelTimes(travelTimesFile);

            try {
                System.out.println("Reading solutions");

                List<Shift> initShifts = readShifts(initResultsFile, travelTimes);

                evaluateShifts(initShifts);

                List<Shift> greedyShifts = readShifts(greedyResultsFile, travelTimes);

                evaluateShifts(greedyShifts);


            } catch (IOException ex) {
                System.out.println("There was an error reading file " + initResultsFile);
                ex.printStackTrace();
            }

        } catch (IOException ex) {
            System.out.println("There was an error reading file " + travelTimesFile);
            ex.printStackTrace();
        }
    }


    public static void evaluateShifts(List<Shift> shifts) {
        System.out.println("");

        double obj = totalObj(shifts);

        int violated = 0;
        int nightShifts = 0;
        double longestShiftLength = 0.0;
        double shortestShiftLength = 100000000.0;
        double longestCleaningTime = 0.0;
        double shortestCleaningTime = 100000000.0;

        for (Shift shift : shifts) {
            nightShifts += shift.nightShift;
            if (shift.totalTime > totalShiftLength) {
                violated++;
            }

            if (shift.totalTime < shortestShiftLength) {
                shortestShiftLength = shift.totalTime;
            } else if (shift.totalTime > longestShiftLength) {
                longestShiftLength = shift.totalTime;
            }
            if (shift.serviceTime < shortestCleaningTime) {
                shortestCleaningTime = shift.serviceTime;
            } else if (shift.serviceTime > longestCleaningTime) {
                longestCleaningTime = shift.serviceTime;
            }
        }

        if (violated == 0) {
            System.out.println("No violated shifts ! :D ");
        } else {
            System.out.println("Number of violated shifts: " + violated + ".");
        }

        System.out.println("Objective: " + obj / 60.0 + " hours.");

        System.out.println("Number of nighshifts: " + nightShifts);

        if (nightShifts > 25) {
            System.out.println("Too many night shifts!");
        }

        System.out.println("Average duration of shift: " + (obj / 50.0) / 60.0 + " hours.");

        System.out.println("Longest shift length: " + longestShiftLength / 60.0 + " hours.");

        System.out.println("Shortest shift length: " + shortestShiftLength / 60.0 + " hours.");

        System.out.println("Longest cleaning time: " + longestCleaningTime / 60.0 + " hours.");

        System.out.println("Shortest cleaning time: " + shortestCleaningTime / 60.0 + " hours.");
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

    public static List<Shift> readShifts(File csvFileName, double[][] travelTimes) throws IOException {
        List<Shift> shifts = new ArrayList<>();

        // Each row corresponds to a stop: map these stops to a route
        Map<String, List<double[]>> byRoute = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFileName))) {
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
                                    " in route=" + entry.getKey() + " file=" + csvFileName.getName()
                    );
                }
                travelSum += travelTimes[a][b];
            }

            // This Shift constructor will add prep+break itself via totalTime
            shifts.add(new Shift(route, travelSum, serviceSum, hasNightStop ? 1 : 0));
        }

        return shifts;
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
}
