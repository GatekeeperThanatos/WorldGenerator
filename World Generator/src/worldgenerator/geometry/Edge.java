package worldgenerator.geometry;

import java.util.HashMap;
import java.util.Stack;

public final class Edge {

    final private static Stack<Edge> pool = new Stack<Edge>();
    final public static Edge DELETED = new Edge();
    
    private HashMap<Orientation, Point> visibleVertices;
    private HashMap<Orientation, Site> sites;
    private Vertex leftVertex, rightVertex;
    private static int numberOfEdges = 0;
    private int edgeIndex;
    public double a, b, c;

    public static Edge createBisectingEdge(Site site0, Site site1) {
        double dx, dy, absdx, absdy;
        double a, b, c;

        dx = site1.getX() - site0.getX();
        dy = site1.getY() - site0.getY();
        absdx = dx > 0 ? dx : -dx;
        absdy = dy > 0 ? dy : -dy;
        c = site0.getX() * dx + site0.getY() * dy + (dx * dx + dy * dy) * 0.5;
        if (absdx > absdy) {
            a = 1.0;
            b = dy / dx;
            c /= dx;
        } else {
            b = 1.0;
            a = dx / dy;
            c /= dy;
        }

        Edge edge = Edge.create();

        edge.setLeftSite(site0);
        edge.setRightSite(site1);
        site0.addEdge(edge);
        site1.addEdge(edge);

        edge.leftVertex = null;
        edge.rightVertex = null;

        edge.a = a;
        edge.b = b;
        edge.c = c;
        //trace("createBisectingEdge: a ", edge.a, "b", edge.b, "c", edge.c);

        return edge;
    }

    private static Edge create() {
        Edge edge;
        if (pool.size() > 0) {
            edge = pool.pop();
            edge.init();
        } else {
            edge = new Edge();
        }
        return edge;
    }

    public LineSegment delaunayLine() {
        return new LineSegment(getLeftSite().getCoordinates(), getRightSite().getCoordinates());
    }

    public LineSegment voronoiEdge() {
        if (!isVisible()) {
            return new LineSegment(null, null);
        }
        return new LineSegment(visibleVertices.get(Orientation.LEFT),
                visibleVertices.get(Orientation.RIGHT));
    }
    
    public Vertex getLeftVertex() {
        return leftVertex;
    }

    public Vertex getRightVertex() {
        return rightVertex;
    }

    public Vertex vertex(Orientation o) {
        return (o == Orientation.LEFT) ? leftVertex : rightVertex;
    }

    public void setVertex(Orientation o, Vertex v) {
        if (o == Orientation.LEFT) {
            leftVertex = v;
        } else {
            rightVertex = v;
        }
    }

    public boolean isPartOfConvexHull() {
        return (leftVertex == null || rightVertex == null);
    }

    public double sitesDistance() {
        return Point.distance(getLeftSite().getCoordinates(), getRightSite().getCoordinates());
    }

    public static double compareSitesDistancesMAX(Edge edge1, Edge edge2) {
        double length1 = edge1.sitesDistance();
        double length2 = edge2.sitesDistance();
        if (length1 < length2) {
            return 1;
        }
        if (length1 > length2) {
            return -1;
        }
        return 0;
    }

    public static double compareSitesDistances(Edge edge0, Edge edge1) {
        return -compareSitesDistancesMAX(edge0, edge1);
    }

    public HashMap<Orientation, Point> getVisibleVertices() {
        return visibleVertices;
    }

    public boolean isVisible() {
        return visibleVertices != null;
    }

    public void setLeftSite(Site s) {
        sites.put(Orientation.LEFT, s);
    }

    public Site getLeftSite() {
        return sites.get(Orientation.LEFT);
    }

    public void setRightSite(Site s) {
        sites.put(Orientation.RIGHT, s);
    }

    public Site getRightSite() {
        return sites.get(Orientation.RIGHT);
    }

    public Site site(Orientation leftRight) {
        return sites.get(leftRight);
    }

    public void dispose() {
        leftVertex = null;
        rightVertex = null;
        if (visibleVertices != null) {
            visibleVertices.clear();
            visibleVertices = null;
        }
        sites.clear();
        sites = null;

        pool.push(this);
    }

    private Edge() {
        edgeIndex = numberOfEdges++;
        init();
    }

    private void init() {
        sites = new HashMap<Orientation, Site>();
    }

    public String toString() {
        return "Edge " + edgeIndex + "; sites " + sites.get(Orientation.LEFT) + ", " + sites.get(Orientation.RIGHT)
                + "; endVertices " + (leftVertex != null ? leftVertex.getVertexIndex() : "null") + ", "
                + (rightVertex != null ? rightVertex.getVertexIndex() : "null") + "::";
    }

    public void clipVertices(Rectangle bounds) {
        double xMIN = bounds.x;
        double yMIN = bounds.y;
        double xMAX = bounds.right;
        double yMAX = bounds.bottom;

        Vertex vertex1, vertex2;
        double x1, x2, y1, y2;

        if (a == 1.0 && b >= 0.0) {
            vertex1 = rightVertex;
            vertex2 = leftVertex;
        } else {
            vertex1 = leftVertex;
            vertex2 = rightVertex;
        }

        if (a == 1.0) {
            y1 = yMIN;
            if (vertex1 != null && vertex1.getY() > yMIN) {
                y1 = vertex1.getY();
            }
            if (y1 > yMAX) {
                return;
            }
            x1 = c - b * y1;

            y2 = yMAX;
            if (vertex2 != null && vertex2.getY() < yMAX) {
                y2 = vertex2.getY();
            }
            if (y2 < yMIN) {
                return;
            }
            x2 = c - b * y2;

            if ((x1 > xMAX && x2 > xMAX) || (x1 < xMIN && x2 < xMIN)) {
                return;
            }

            if (x1 > xMAX) {
                x1 = xMAX;
                y1 = (c - x1) / b;
            } else if (x1 < xMIN) {
                x1 = xMIN;
                y1 = (c - x1) / b;
            }

            if (x2 > xMAX) {
                x2 = xMAX;
                y2 = (c - x2) / b;
            } else if (x2 < xMIN) {
                x2 = xMIN;
                y2 = (c - x2) / b;
            }
        } else {
            x1 = xMIN;
            if (vertex1 != null && vertex1.getX() > xMIN) {
                x1 = vertex1.getX();
            }
            if (x1 > xMAX) {
                return;
            }
            y1 = c - a * x1;

            x2 = xMAX;
            if (vertex2 != null && vertex2.getX() < xMAX) {
                x2 = vertex2.getX();
            }
            if (x2 < xMIN) {
                return;
            }
            y2 = c - a * x2;

            if ((y1 > yMAX && y2 > yMAX) || (y1 < yMIN && y2 < yMIN)) {
                return;
            }

            if (y1 > yMAX) {
                y1 = yMAX;
                x1 = (c - y1) / a;
            } else if (y1 < yMIN) {
                y1 = yMIN;
                x1 = (c - y1) / a;
            }

            if (y2 > yMAX) {
                y2 = yMAX;
                x2 = (c - y2) / a;
            } else if (y2 < yMIN) {
                y2 = yMIN;
                x2 = (c - y2) / a;
            }
        }

        visibleVertices = new HashMap<Orientation, Point>();
        if (vertex1 == leftVertex) {
            visibleVertices.put(Orientation.LEFT, new Point(x1, y1));
            visibleVertices.put(Orientation.RIGHT, new Point(x2, y2));
        } else {
            visibleVertices.put(Orientation.RIGHT, new Point(x1, y1));
            visibleVertices.put(Orientation.LEFT, new Point(x2, y2));
        }
    }
}