package worldgenerator.geometry;

import java.awt.Color;
import java.util.*;

public final class Site implements Coordinates {
	
    final private static double EPSILON = .005;

    public ArrayList<Edge> edges;
    private ArrayList<Orientation> edgeOrientations;
    private ArrayList<Point> points;
    private static Stack<Site> pool = new Stack<Site>();
    private Point coordinates;
    public Color color;
    public double weight;
    private int siteIndex;

    public static Site create(Point p, int index, double weight, Color color) {
        if (pool.size() > 0) {
            return pool.pop().init(p, index, weight, color);
        } else {
            return new Site(p, index, weight, color);
        }
    }

    public static void sortSites(ArrayList<Site> sites) {
        Collections.sort(sites, new Comparator<Site>() {
            @Override
            public int compare(Site o1, Site o2) {
                return (int) Site.compare(o1, o2);
            }
        });
    }

    private static double compare(Site s1, Site s2) {
        int returnValue = Voronoi.compareByYThenX(s1, s2);
        int tempIndex;
        if (returnValue == -1) {
            if (s1.siteIndex > s2.siteIndex) {
                tempIndex = s1.siteIndex;
                s1.siteIndex = s2.siteIndex;
                s2.siteIndex = tempIndex;
            }
        } else if (returnValue == 1) {
            if (s2.siteIndex > s1.siteIndex) {
                tempIndex = s2.siteIndex;
                s2.siteIndex = s1.siteIndex;
                s1.siteIndex = tempIndex;
            }

        }

        return returnValue;
    }

    private static boolean closeEnough(Point p0, Point p1) {
        return Point.distance(p0, p1) < EPSILON;
    }

    @Override
    public Point getCoordinates() {
        return coordinates;
    }
    

    public Site(Point p, int index, double weight, Color color) {
        init(p, index, weight, color);
    }

    private Site init(Point p, int index, double weight, Color color) {
        coordinates = p;
        siteIndex = index;
        this.weight = weight;
        this.color = color;
        edges = new ArrayList<Edge>();
        points = null;
        return this;
    }

    @Override
    public String toString() {
        return "Site (" + siteIndex + "): " + getCoordinates();
    }

    private void move(Point p) {
        clear();
        coordinates = p;
    }

    public void dispose() {
        coordinates = null;
        clear();
        pool.push(this);
    }

    private void clear() {
        if (edges != null) {
            edges.clear();
            edges = null;
        }
        if (edgeOrientations != null) {
            edgeOrientations.clear();
            edgeOrientations = null;
        }
        if (points != null) {
            points.clear();
            points = null;
        }
    }

    void addEdge(Edge edge) {
        edges.add(edge);
    }

    public Edge nearestEdge() {
        Collections.sort(edges, new Comparator<Edge>() {
            @Override
            public int compare(Edge o1, Edge o2) {
                return (int) Edge.compareSitesDistances(o1, o2);
            }
        });
        return edges.get(0);
    }

    ArrayList<Site> neighborSites() {
        if (edges == null || edges.isEmpty()) {
            return new ArrayList<Site>();
        }
        if (edgeOrientations == null) {
            reorderEdges();
        }
        ArrayList<Site> list = new ArrayList<Site>();
        for (Edge edge : edges) {
            list.add(neighborSite(edge));
        }
        return list;
    }

    private Site neighborSite(Edge edge) {
        if (this == edge.getLeftSite()) {
            return edge.getRightSite();
        }
        if (this == edge.getRightSite()) {
            return edge.getLeftSite();
        }
        return null;
    }

    ArrayList<Point> region(Rectangle clippingBounds) {
        if (edges == null || edges.isEmpty()) {
            return new ArrayList<Point>();
        }
        if (edgeOrientations == null) {
            reorderEdges();
            points = clipToBounds(clippingBounds);
            if ((new Polygon(points)).winding() == Rotation.CLOCKWISE) {
                Collections.reverse(points);
            }
        }
        return points;
    }

    private void reorderEdges() {
        EdgeSorter reorderer = new EdgeSorter(edges, Vertex.class);
        edges = reorderer.getEdges();
        edgeOrientations = reorderer.getEdgeOrientations();
        reorderer.dispose();
    }

    private ArrayList<Point> clipToBounds(Rectangle bounds) {
        ArrayList<Point> points = new ArrayList<Point>();
        int n = edges.size();
        int i = 0;
        Edge edge;
        while (i < n && (edges.get(i).isVisible() == false)) {
            ++i;
        }

        if (i == n) {
            return new ArrayList<Point>();
        }
        edge = edges.get(i);
        Orientation orientation = edgeOrientations.get(i);
        points.add(edge.getVisibleVertices().get(orientation));
        points.add(edge.getVisibleVertices().get((Orientation.other(orientation))));

        for (int j = i + 1; j < n; ++j) {
            edge = edges.get(j);
            if (edge.isVisible() == false) {
                continue;
            }
            connect(points, j, bounds, false);
        }
        connect(points, i, bounds, true);

        return points;
    }

    private void connect(ArrayList<Point> points, int j, Rectangle bounds, boolean done) {
        Point rightPoint = points.get(points.size() - 1);
        Edge newEdge = edges.get(j);
        Orientation newOrientation = edgeOrientations.get(j);
        Point newPoint = newEdge.getVisibleVertices().get(newOrientation);
        
        if (!closeEnough(rightPoint, newPoint)) {
            if (rightPoint.x != newPoint.x && rightPoint.y != newPoint.y) {
                int rightCheck = BoundsCheck.check(rightPoint, bounds);
                int newCheck = BoundsCheck.check(newPoint, bounds);
                double px, py;
                
                if ((rightCheck & BoundsCheck.RIGHT) != 0) {
                    px = bounds.right;
                    
                    if ((newCheck & BoundsCheck.BOTTOM) != 0) {
                        py = bounds.bottom;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.TOP) != 0) {
                        py = bounds.top;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.LEFT) != 0) {
                        if (rightPoint.y - bounds.y + newPoint.y - bounds.y < bounds.height) {
                            py = bounds.top;
                        } else {
                            py = bounds.bottom;
                        }
                        points.add(new Point(px, py));
                        points.add(new Point(bounds.left, py));
                    }
                } else if ((rightCheck & BoundsCheck.LEFT) != 0) {
                    px = bounds.left;
                    
                    if ((newCheck & BoundsCheck.BOTTOM) != 0) {
                        py = bounds.bottom;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.TOP) != 0) {
                        py = bounds.top;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.RIGHT) != 0) {
                        if (rightPoint.y - bounds.y + newPoint.y - bounds.y < bounds.height) {
                            py = bounds.top;
                        } else {
                            py = bounds.bottom;
                        }
                        points.add(new Point(px, py));
                        points.add(new Point(bounds.right, py));
                    }
                } else if ((rightCheck & BoundsCheck.TOP) != 0) {
                    py = bounds.top;
                    if ((newCheck & BoundsCheck.RIGHT) != 0) {
                        px = bounds.right;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.LEFT) != 0) {
                        px = bounds.left;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.BOTTOM) != 0) {
                        if (rightPoint.x - bounds.x + newPoint.x - bounds.x < bounds.width) {
                            px = bounds.left;
                        } else {
                            px = bounds.right;
                        }
                        points.add(new Point(px, py));
                        points.add(new Point(px, bounds.bottom));
                    }
                } else if ((rightCheck & BoundsCheck.BOTTOM) != 0) {
                    py = bounds.bottom;
                    
                    if ((newCheck & BoundsCheck.RIGHT) != 0) {
                        px = bounds.right;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.LEFT) != 0) {
                        px = bounds.left;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.TOP) != 0) {
                        if (rightPoint.x - bounds.x + newPoint.x - bounds.x < bounds.width) {
                            px = bounds.left;
                        } else {
                            px = bounds.right;
                        }
                        points.add(new Point(px, py));
                        points.add(new Point(px, bounds.top));
                    }
                }
            }
            if (done) {
                return;
            }
            points.add(newPoint);
        }
        Point newRightPoint = newEdge.getVisibleVertices().get(Orientation.other(newOrientation));
        if (!closeEnough(points.get(0), newRightPoint)) {
            points.add(newRightPoint);
        }
    }

    public double getX() {
        return coordinates.x;
    }

    public double getY() {
        return coordinates.y;
    }

    public double distanceTo(Coordinates p) {
        return Point.distance(p.getCoordinates(), this.coordinates);
    }
}

final class BoundsCheck {

    final public static int TOP = 1;
    final public static int BOTTOM = 2;
    final public static int LEFT = 4;
    final public static int RIGHT = 8;

    public static int check(Point point, Rectangle bounds) {
        int value = 0;
        if (point.x == bounds.left) {
            value |= LEFT;
        }
        if (point.x == bounds.right) {
            value |= RIGHT;
        }
        if (point.y == bounds.top) {
            value |= TOP;
        }
        if (point.y == bounds.bottom) {
            value |= BOTTOM;
        }
        return value;
    }

    public BoundsCheck() {
        throw new Error("BoundsCheck constructor unused");
    }
}
