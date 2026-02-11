//Packages used
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransportInstance {
    private final List<Stop> stops;

    // Constructor
    public TransportInstance(List<Stop> stops) {
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

    public List<Stop> getNightShifts() {
        List<Stop> nightShifts = new ArrayList<>();
        for (Stop stop : stops) {
            if (stop.nightShift == 1) {
                nightShifts.add(stop);
            }
        }
        return nightShifts;
    }

    public List<Stop> getDayShifts() {
        List<Stop> dayShifts = new ArrayList<>();
        for (Stop stop : stops) {
            if (stop.nightShift == 0) {
                dayShifts.add(stop);
            }
        }
        return dayShifts;
    }

    // Construct a transport instance by reading from a file
    public static TransportInstance read(File instanceFileName) throws IOException {
        List<Stop> stops = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(instanceFileName))) {
            String line;

            // Read header (and ignore it)
            line = br.readLine();
            if (line == null) {
                return new TransportInstance(stops);
            }

            // Loop over all stops to obtain their information
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] c = line.split("\t");

                stops.add(new Stop(
                        Integer.parseInt(c[0]),
                        c[1],
                        Double.parseDouble(c[2]),
                        Double.parseDouble(c[3]),
                        Integer.parseInt(c[4]),
                        Double.parseDouble(c[5])
                ));
            }

            // Create instance with the obtained information
            return new TransportInstance(stops);
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
