package core;
public class Stop {
    public final int objectId;
    public final String idMaximo;
    public final double latitude;
    public final double longitude;
    public final int nightShift;
    public double serviceTime;

    // Constructor
    public Stop(int objectId, String idMaximo, double latitude, double longitude, int nightShift, double serviceTime) {
        this.objectId = objectId;
        this.idMaximo = idMaximo;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nightShift = nightShift;
        this.serviceTime = serviceTime;
    }

    @Override
    public int hashCode() {
        return objectId;
    }

    @Override
    public boolean equals(Object other) {
        
        if (!(other instanceof Stop)) {
            return false;
        }

        return (other.hashCode() == hashCode());
    }
}

