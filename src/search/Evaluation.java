package search;

public class Evaluation {
    public final double improvement;
    public final boolean feasible;

    public Evaluation(double improvement, boolean feasible) {
        this.improvement = improvement;
        this.feasible = feasible;
    }
}
