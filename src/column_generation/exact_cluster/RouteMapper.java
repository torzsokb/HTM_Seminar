package column_generation.exact_cluster;

import java.util.*;

public class RouteMapper {

    public static List<Integer> mapBackRoute(List<Integer> localRoute,
                                             int[] localToGlobal) {

        List<Integer> globalRoute = new ArrayList<>();
        for (int idx : localRoute) {
            globalRoute.add(localToGlobal[idx]);
        }
        return globalRoute;
    }
}
