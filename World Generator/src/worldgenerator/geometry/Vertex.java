package worldgenerator.geometry;

import java.util.Stack;

final public class Vertex extends Object implements Coordinates {

    final public static Vertex VERTEX_AT_INFINITY = new Vertex(Double.NaN, Double.NaN);
    final private static Stack<Vertex> pool = new Stack<Vertex>();

    private int vertexIndex;
    private static int numberOfVertices = 0;
    private Point coordinate;

    private static Vertex create(double x, double y) {
    	
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return VERTEX_AT_INFINITY;
        }
        
        if (pool.size() > 0) {

            return pool.pop().init(x, y);
        } else {
            return new Vertex(x, y);
        }
    }

    @Override
    public Point getCoordinates() {
        return coordinate;
    }

    public int getVertexIndex() {
        return vertexIndex;
    }

    public Vertex(double x, double y) {
        init(x, y);
    }

    private Vertex init(double x, double y) {
        coordinate = new Point(x, y);
        return this;
    }

    public void dispose() {
        coordinate = null;
        pool.push(this);
    }

    public void setIndex() {
        vertexIndex = numberOfVertices++;
    }

    @Override
    public String toString() {
        return "Vertex (" + vertexIndex + ")";
    }
    
    public static Vertex intersect(Halfedge halfedge1, Halfedge halfedge2) {
        Edge edge1, edge2, edge;
        Halfedge halfedge;
        double determinant, intersectionX, intersectionY;
        boolean rightOfSite;

        edge1 = halfedge1.edge;
        edge2 = halfedge2.edge;
        if (edge1 == null || edge2 == null) {
            return null;
        }
        if (edge1.getRightSite() == edge2.getRightSite()) {
            return null;
        }

        determinant = edge1.a * edge2.b - edge1.b * edge2.a;
        if (-1.0e-10 < determinant && determinant < 1.0e-10) {
            return null;
        }

        intersectionX = (edge1.c * edge2.b - edge2.c * edge1.b) / determinant;
        intersectionY = (edge2.c * edge1.a - edge1.c * edge2.a) / determinant;

        if (Voronoi.compareByYThenX(edge1.getRightSite(), edge2.getRightSite()) < 0) {
            halfedge = halfedge1;
            edge = edge1;
        } else {
            halfedge = halfedge2;
            edge = edge2;
        }
        rightOfSite = intersectionX >= edge.getRightSite().getX();
        if ((rightOfSite && halfedge.leftRight == Orientation.LEFT)
                || (!rightOfSite && halfedge.leftRight == Orientation.RIGHT)) {
            return null;
        }

        return Vertex.create(intersectionX, intersectionY);
    }

    public double getX() {
        return coordinate.x;
    }

    public double getY() {
        return coordinate.y;
    }
}
