package worldgenerator.geometry;

/**
 * Point.java
 *
 * @author Connor
 */
public class Point {

    
    public double x, y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public static double distance(Point p1, Point p2) {
        return MathUtils.distance(p1, p2);
    }
    
    public double distanceTo(Point p) {
        return MathUtils.distance(this, p);
    }
    
    @Override
    public String toString() {
        return x + ", " + y;
    }

    public double l2() {
        return x * x + y * y;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }
}
