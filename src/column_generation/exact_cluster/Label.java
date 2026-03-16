package column_generation.exact_cluster;

import java.util.*;

class Label {

    final int lastNode;        // index in cluster (0..n-1)
    final double time;         // accumulated time (travel + service)
    final double cost;         // accumulated reduced cost
    final BitSet visited;      // visited customers (cluster indices)
    final List<Integer> path;  // sequence of node indices (cluster indices)

    Label(int lastNode,
          double time,
          double cost,
          BitSet visited,
          List<Integer> path) {

        this.lastNode = lastNode;
        this.time = time;
        this.cost = cost;
        this.visited = visited;
        this.path = path;
    }

    static Label initial() {
        BitSet v = new BitSet();
        List<Integer> p = new ArrayList<>();
        p.add(0); // depot index
        return new Label(0, 0.0, 0.0, v, p);
    }
}
