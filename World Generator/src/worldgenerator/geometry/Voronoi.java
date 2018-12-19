package worldgenerator.geometry;

import java.awt.Color;
import java.util.*;

public final class Voronoi {

    private SiteList sites;
    private HashMap<Point, Site> sitesByLocation;
    private ArrayList<Triangle> triangles;
    private ArrayList<Edge> edges;
    private Rectangle bounds;

    public Rectangle getPlotBounds() {
        return bounds;
    }

    public void dispose() {
        int i, n;
        if (sites != null) {
            sites.dispose();
            sites = null;
        }
        if (triangles != null) {
            n = triangles.size();
            for (i = 0; i < n; ++i) {
                triangles.get(i).dispose();
            }
            triangles.clear();
            triangles = null;
        }
        if (edges != null) {
            n = edges.size();
            for (i = 0; i < n; ++i) {
                edges.get(i).dispose();
            }
            edges.clear();
            edges = null;
        }
        bounds = null;
        sitesByLocation = null;
    }

    public Voronoi(ArrayList<Point> points, ArrayList<Color> colors, Rectangle plotBounds) {
        init(points, colors, plotBounds);
        fortunesAlgorithm();
    }

    public Voronoi(ArrayList<Point> points, ArrayList<Color> colors) {
        double maxWidth = 0, maxHeight = 0;
        for (Point p : points) {
            maxWidth = Math.max(maxWidth, p.x);
            maxHeight = Math.max(maxHeight, p.y);
        }
        System.out.println(maxWidth + "," + maxHeight);
        init(points, colors, new Rectangle(0, 0, maxWidth, maxHeight));
        fortunesAlgorithm();
    }

    public Voronoi(int numSites, double maxWidth, double maxHeight, Random r, ArrayList<Color> colors) {
        ArrayList<Point> points = new ArrayList<Point>();
        for (int i = 0; i < numSites; i++) {
            points.add(new Point(r.nextDouble() * maxWidth, r.nextDouble() * maxHeight));
        }
        init(points, colors, new Rectangle(0, 0, maxWidth, maxHeight));
        fortunesAlgorithm();
    }

    private void init(ArrayList<Point> points, ArrayList<Color> colors, Rectangle plotBounds) {
        sites = new SiteList();
        sitesByLocation = new HashMap<Point, Site>();
        addSites(points, colors);
        bounds = plotBounds;
        triangles = new ArrayList<Triangle>();
        edges = new ArrayList<Edge>();
    }

    private void addSites(ArrayList<Point> points, ArrayList<Color> colors) {
        int length = points.size();
        for (int i = 0; i < length; ++i) {
            addSite(points.get(i), colors != null ? colors.get(i) : null, i);
        }
    }

    private void addSite(Point p, Color color, int index) {
        double weight = Math.random() * 100;
        Site site = Site.create(p, index, weight, color);
        sites.push(site);
        sitesByLocation.put(p, site);
    }

    public ArrayList<Edge> edges() {
        return edges;
    }

    public ArrayList<Point> region(Point p) {
        Site site = sitesByLocation.get(p);
        if (site == null) {
            return new ArrayList<Point>();
        }
        return site.region(bounds);
    }

    public ArrayList<Point> neighborSitesForSite(Point coord) {
        ArrayList<Point> points = new ArrayList<Point>();
        Site site = sitesByLocation.get(coord);
        if (site == null) {
            return points;
        }
        ArrayList<Site> sites = site.neighborSites();
        for (Site neighbor : sites) {
            points.add(neighbor.getCoordinates());
        }
        return points;
    }

    public ArrayList<Circle> circles() {
        return sites.circles();
    }

    private ArrayList<Edge> selectEdgesForSitePoint(Point coord, ArrayList<Edge> edgesToTest) {
        ArrayList<Edge> filtered = new ArrayList<Edge>();

        for (Edge e : edgesToTest) {
            if (((e.getLeftSite() != null && e.getLeftSite().getCoordinates() == coord)
                    || (e.getRightSite() != null && e.getRightSite().getCoordinates() == coord))) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    private ArrayList<LineSegment> visibleLineSegments(ArrayList<Edge> edges) {
        ArrayList<LineSegment> segments = new ArrayList<LineSegment>();

        for (Edge edge : edges) {
            if (edge.isVisible()) {
                Point p1 = edge.getVisibleVertices().get(Orientation.LEFT);
                Point p2 = edge.getVisibleVertices().get(Orientation.RIGHT);
                segments.add(new LineSegment(p1, p2));
            }
        }

        return segments;
    }

    private ArrayList<LineSegment> delaunayLinesForEdges(ArrayList<Edge> edges) {
        ArrayList<LineSegment> segments = new ArrayList<LineSegment>();

        for (Edge edge : edges) {
            segments.add(edge.delaunayLine());
        }

        return segments;
    }

    public ArrayList<LineSegment> voronoiBoundaryForSite(Point coord) {
        return visibleLineSegments(selectEdgesForSitePoint(coord, edges));
    }

    public ArrayList<LineSegment> delaunayLinesForSite(Point coord) {
        return delaunayLinesForEdges(selectEdgesForSitePoint(coord, edges));
    }

    public ArrayList<LineSegment> voronoiDiagram() {
        return visibleLineSegments(edges);
    }

    public ArrayList<LineSegment> hull() {
        return delaunayLinesForEdges(hullEdges());
    }

    private ArrayList<Edge> hullEdges() {
        ArrayList<Edge> filtered = new ArrayList<Edge>();

        for (Edge e : edges) {
            if (e.isPartOfConvexHull()) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    public ArrayList<Point> hullPointsInOrder() {
        ArrayList<Edge> hullEdges = hullEdges();

        ArrayList<Point> points = new ArrayList<Point>();
        if (hullEdges.isEmpty()) {
            return points;
        }

        EdgeSorter reorderer = new EdgeSorter(hullEdges, Site.class);
        hullEdges = reorderer.getEdges();
        ArrayList<Orientation> orientations = reorderer.getEdgeOrientations();
        reorderer.dispose();

        Orientation orientation;

        int n = hullEdges.size();
        for (int i = 0; i < n; ++i) {
            Edge edge = hullEdges.get(i);
            orientation = orientations.get(i);
            points.add(edge.site(orientation).getCoordinates());
        }
        return points;
    }

    public ArrayList<ArrayList<Point>> regions() {
        return sites.regions(bounds);
    }

    public ArrayList<Point> siteCoords() {
        return sites.siteCoords();
    }

    private void fortunesAlgorithm() {
        Site newSite, bottomSite, topSite, tempSite;
        Vertex v, vertex;
        Point newintstar = null;
        Orientation leftRight;
        Halfedge lbnd, rbnd, llbnd, rrbnd, bisector;
        Edge edge;

        Rectangle dataBounds = sites.getSitesBounds();

        int sqrt_nsites = (int) Math.sqrt(sites.getLength() + 4);
        HalfedgePriorityQueue heap = new HalfedgePriorityQueue(dataBounds.y, dataBounds.height, sqrt_nsites);
        EdgeList edgeList = new EdgeList(dataBounds.x, dataBounds.width, sqrt_nsites);
        ArrayList<Halfedge> halfEdges = new ArrayList<Halfedge>();
        ArrayList<Vertex> vertices = new ArrayList<Vertex>();

        Site bottomMostSite = sites.next();
        newSite = sites.next();

        for (;;) {
            if (heap.empty() == false) {
                newintstar = heap.min();
            }

            if (newSite != null
                    && (heap.empty() || compareByYThenX(newSite, newintstar) < 0)) {
                lbnd = edgeList.edgeListLeftNeighbor(newSite.getCoordinates());
                rbnd = lbnd.edgeListRightNeighbor;
                bottomSite = rightRegion(lbnd, bottomMostSite);
                
                edge = Edge.createBisectingEdge(bottomSite, newSite);
               
                edges.add(edge);

                bisector = Halfedge.create(edge, Orientation.LEFT);
                halfEdges.add(bisector);
                edgeList.insert(lbnd, bisector);
                
                if ((vertex = Vertex.intersect(lbnd, bisector)) != null) {
                    vertices.add(vertex);
                    heap.remove(lbnd);
                    lbnd.vertex = vertex;
                    lbnd.ystar = vertex.getY() + newSite.distanceTo(vertex);
                    heap.insert(lbnd);
                }

                lbnd = bisector;
                bisector = Halfedge.create(edge, Orientation.RIGHT);
                halfEdges.add(bisector);
                edgeList.insert(lbnd, bisector);
                
                if ((vertex = Vertex.intersect(bisector, rbnd)) != null) {
                    vertices.add(vertex);
                    bisector.vertex = vertex;
                    bisector.ystar = vertex.getY() + newSite.distanceTo(vertex);
                    heap.insert(bisector);
                }

                newSite = sites.next();
            } else if (heap.empty() == false) {
                lbnd = heap.extractMin();
                llbnd = lbnd.edgeListLeftNeighbor;
                rbnd = lbnd.edgeListRightNeighbor;
                rrbnd = rbnd.edgeListRightNeighbor;
                bottomSite = leftRegion(lbnd, bottomMostSite);
                topSite = rightRegion(rbnd, bottomMostSite);

                v = lbnd.vertex;
                v.setIndex();
                lbnd.edge.setVertex(lbnd.leftRight, v);
                rbnd.edge.setVertex(rbnd.leftRight, v);
                edgeList.remove(lbnd);
                heap.remove(rbnd);
                edgeList.remove(rbnd);
                leftRight = Orientation.LEFT;
                if (bottomSite.getY() > topSite.getY()) {
                    tempSite = bottomSite;
                    bottomSite = topSite;
                    topSite = tempSite;
                    leftRight = Orientation.RIGHT;
                }
                edge = Edge.createBisectingEdge(bottomSite, topSite);
                edges.add(edge);
                bisector = Halfedge.create(edge, leftRight);
                halfEdges.add(bisector);
                edgeList.insert(llbnd, bisector);
                edge.setVertex(Orientation.other(leftRight), v);
                if ((vertex = Vertex.intersect(llbnd, bisector)) != null) {
                    vertices.add(vertex);
                    heap.remove(llbnd);
                    llbnd.vertex = vertex;
                    llbnd.ystar = vertex.getY() + bottomSite.distanceTo(vertex);
                    heap.insert(llbnd);
                }
                if ((vertex = Vertex.intersect(bisector, rrbnd)) != null) {
                    vertices.add(vertex);
                    bisector.vertex = vertex;
                    bisector.ystar = vertex.getY() + bottomSite.distanceTo(vertex);
                    heap.insert(bisector);
                }
            } else {
                break;
            }
        }
        heap.dispose();
        edgeList.dispose();

        for (Halfedge halfEdge : halfEdges) {
            halfEdge.reallyDispose();
        }
        halfEdges.clear();
        
        for (Edge e : edges) {
            e.clipVertices(bounds);
        }
        
        for (Vertex v0 : vertices) {
            v0.dispose();
        }
        vertices.clear();


    }

    Site leftRegion(Halfedge he, Site bottomMostSite) {
        Edge edge = he.edge;
        if (edge == null) {
            return bottomMostSite;
        }
        return edge.site(he.leftRight);
    }

    Site rightRegion(Halfedge he, Site bottomMostSite) {
        Edge edge = he.edge;
        if (edge == null) {
            return bottomMostSite;
        }
        return edge.site(Orientation.other(he.leftRight));
    }

    public static int compareByYThenX(Site s1, Site s2) {
        if (s1.getY() < s2.getY()) {
            return -1;
        }
        if (s1.getY() > s2.getY()) {
            return 1;
        }
        if (s1.getX() < s2.getX()) {
            return -1;
        }
        if (s1.getX() > s2.getX()) {
            return 1;
        }
        return 0;
    }

    public static int compareByYThenX(Site s1, Point s2) {
        if (s1.getY() < s2.y) {
            return -1;
        }
        if (s1.getY() > s2.y) {
            return 1;
        }
        if (s1.getX() < s2.x) {
            return -1;
        }
        if (s1.getX() > s2.x) {
            return 1;
        }
        return 0;
    }
}
