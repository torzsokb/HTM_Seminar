package search;

public class Move {
    public final int route1;
    public final int route2;
    public final int index1;
    public final int index2;
    public final MoveType type;

    public Move(int route1, int route2, int index1, int index2, MoveType type) {
        this.route1 = route1;
        this.route2 = route2;
        this.index1 = index1;
        this.index2 = index2;
        this.type = type;
    }

    public enum MoveType {
        INTER_SHIFT,
        INTER_SWAP,
        INTRA_SWAP,
        INTRA_SHIFT,
        INTRA_2OPT,
        INTER_2OPT_STAR
    }
}
