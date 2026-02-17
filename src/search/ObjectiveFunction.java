package search;

import core.Shift;
import java.util.List;

@FunctionalInterface
public interface ObjectiveFunction {
    double shifts(List<Shift> shifts);
}