package milp;
import java.util.*;
import core.Stop;
import core.Shift;

public interface RCESPP {
    public List<Shift> getNewShifts(double[][] distances, List<Stop> stops, double[] duals);
}
