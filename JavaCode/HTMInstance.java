//Packages used
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HTMInstance {
    private final List<Stop> stops;

    // Constructor
    public HTMInstance(List<Stop> stops) {
        this.stops = stops;
    }

    // Get methods
    public List<Stop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public int getNStops() {
        return stops.size();
    }

    public Stop getDepot() {
        return stops.get(0);
    }

    // Construct a transport instance by reading from a file
    public static HTMInstance read(File instanceFileName, String cleaningIndicator, String nightStopIndicator) throws IOException {
        List<Stop> stops = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(instanceFileName))) {
            String line;

            // Read header (and ignore it)
            line = br.readLine();
            if (line == null) {
                return new HTMInstance(stops);
            }

            // Loop over all stops to obtain their information
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] c = line.split("\t");

                // Choose cleaning time: {20, code, abri}
                double cleaningTime = 0.0;
                switch (cleaningIndicator) {
                    case "20":
                        cleaningTime = Double.parseDouble(c[6]);
                        break;
                    case "code":
                        cleaningTime = Double.parseDouble(c[7]);
                        break;
                    case "abri":
                        cleaningTime = Double.parseDouble(c[8]);
                        break;
                    default:
                        System.out.println("Invalid cleaning indicator.");
                        break;
                }

                // Choose night stop indicator: {Night_shift, Type_halte}
                int nightStop = 0;
                if (nightStopIndicator.equals("Night_shift")) {
                    nightStop = Integer.parseInt(c[4]);
                } else if (nightStopIndicator.equals("Type_halte")) {
                    if (c[5].equals("Tramhalte")) {
                        nightStop = 1;
                    }
                } else {
                    System.out.println("Invalid night stop indicator.");
                }

                stops.add(new Stop(
                        Integer.parseInt(c[0]),
                        c[1],
                        Double.parseDouble(c[2]),
                        Double.parseDouble(c[3]),
                        nightStop,
                        cleaningTime
                ));
            }
            stops.sort(Comparator.comparingInt(s -> s.objectId));
            // Create instance with the obtained information
            return new HTMInstance(stops);
        }
    }

    // Code snippet to test if the file was read correctly

    /*
    public static void main(String[] args) {
        File fileToRead = new File("halteinfo_stops.txt");
        try {
            TransportInstance instance = TransportInstance.read(fileToRead);

            // Test if instance is read correctly
            System.out.println("Number of stops is " + instance.getNStops());
            for (Stop stop : instance.getStops()) {
                System.out.println(
                    "Stop " + stop.objectId +
                    " has ID_MAXIMO " + stop.idMaximo +
                    " at (" + stop.latitude + ", " + stop.longitude + ")"
                );
            }
        }
        catch (IOException ex) {
            System.out.println("There was an error reading file " + fileToRead);
            ex.printStackTrace();
        }
    }
     */


}


