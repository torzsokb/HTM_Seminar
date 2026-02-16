package search;
@FunctionalInterface
public interface AcceptanceFunction {
    boolean accept(double improvement);
}

