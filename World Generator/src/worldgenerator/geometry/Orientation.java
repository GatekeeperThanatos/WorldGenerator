package worldgenerator.geometry;

public final class Orientation {

    final public static Orientation LEFT = new Orientation( "left");
    final public static Orientation RIGHT = new Orientation( "right");
    private String orientation;

    public Orientation(String orientationName) {
        orientation = orientationName;
    }

    public static Orientation other(Orientation leftRight) {
        return leftRight == LEFT ? RIGHT : LEFT;
    }

    @Override
    public String toString() {
        return orientation;
    }
}
