package search;
import java.util.List;

import core.HTMInstance;
import core.Shift;

public interface Neighborhood {
    List<Move> generateMoves(List<Shift> shifts, RouteCompatibility compatibility);
    Evaluation evaluateMove(Move move, List<Shift> shifts, HTMInstance instance, double[][] travelTimes, double maxShiftDuration);
    List<Shift> applyMove(Move move, List<Shift> shifts, HTMInstance instance, double[][] travelTimes);
}

