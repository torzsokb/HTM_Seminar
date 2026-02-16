package search;
public class Compatibility {

    public static RouteCompatibility sameNightShift() {
        return (s1, s2) -> s1.nightShift == s2.nightShift;
    }
}
