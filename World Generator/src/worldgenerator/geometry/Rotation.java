package worldgenerator.geometry;

public final class Rotation {

    final public static Rotation CLOCKWISE = new Rotation("clockwise");
    final public static Rotation COUNTERCLOCKWISE = new Rotation("counterclockwise");
    final public static Rotation NONE = new Rotation("none");
    private String rotation;

    private Rotation(String name) {
        super();
        rotation = name;
    }

    @Override
    public String toString() {
        return rotation;
    }
}