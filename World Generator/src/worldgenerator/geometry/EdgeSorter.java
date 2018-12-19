package worldgenerator.geometry;

import java.util.ArrayList;

public final class EdgeSorter {

    private ArrayList<Edge> edges;
    private ArrayList<Orientation> edgeOrientations;

    public ArrayList<Edge> getEdges() {
        return edges;
    }

    public ArrayList<Orientation> getEdgeOrientations() {
        return edgeOrientations;
    }

    public EdgeSorter(ArrayList<Edge> origEdges, Class<?> criterion) {
        if (criterion != Vertex.class && criterion != Site.class) {
            throw new Error("Edges: criterion must be Vertex or Site");
        }
        edges = new ArrayList<Edge>();
        edgeOrientations = new ArrayList<Orientation>();
        if (origEdges.size() > 0) {
            edges = reorderEdges(origEdges, criterion);
        }
    }

    public void dispose() {
        edges = null;
        edgeOrientations = null;
    }

    private ArrayList<Edge> reorderEdges(ArrayList<Edge> inputtedEdges, Class<?> criterion) {
        int i;
        int n = inputtedEdges.size();
        Edge edge;
        ArrayList<Boolean> done = new ArrayList<Boolean>(n);
        int numberDone = 0;
        for (int k = 0; k < n; k++) {
            done.add( false);
        }
        ArrayList<Edge> newEdges = new ArrayList<Edge>();

        i = 0;
        edge = inputtedEdges.get(i);
        newEdges.add(edge);
        edgeOrientations.add(Orientation.LEFT);
        Coordinates firstPoint = (criterion == Vertex.class) ? edge.getLeftVertex() : edge.getLeftSite();
        Coordinates lastPoint = (criterion == Vertex.class) ? edge.getRightVertex() : edge.getRightSite();

        if (firstPoint == Vertex.VERTEX_AT_INFINITY || lastPoint == Vertex.VERTEX_AT_INFINITY) {
            return new ArrayList<Edge>();
        }

        done.set(i, true);
        numberDone++;

        while (numberDone < n) {
            for (i = 1; i < n; ++i) {
                if (done.get(i)) {
                    continue;
                }
                edge = inputtedEdges.get(i);
                Coordinates leftPoint = (criterion == Vertex.class) ? edge.getLeftVertex() : edge.getLeftSite();
                Coordinates rightPoint = (criterion == Vertex.class) ? edge.getRightVertex() : edge.getRightSite();
                if (leftPoint == Vertex.VERTEX_AT_INFINITY || rightPoint == Vertex.VERTEX_AT_INFINITY) {
                    return new ArrayList<Edge>();
                }
                if (leftPoint == lastPoint) {
                    lastPoint = rightPoint;
                    edgeOrientations.add(Orientation.LEFT);
                    newEdges.add(edge);
                    done.set(i, true);
                } else if (rightPoint == firstPoint) {
                    firstPoint = leftPoint;
                    edgeOrientations.add(0, Orientation.LEFT);
                    newEdges.add(0, edge);
                    done.set(i, true);
                } else if (leftPoint == firstPoint) {
                    firstPoint = rightPoint;
                    edgeOrientations.add(0, Orientation.RIGHT);
                    newEdges.add(0, edge);

                    done.set(i, true);
                } else if (rightPoint == lastPoint) {
                    lastPoint = leftPoint;
                    edgeOrientations.add(Orientation.RIGHT);
                    newEdges.add(edge);
                    done.set(i, true);
                }
                if (done.get(i)) {
                    numberDone++;
                }
            }
        }

        return newEdges;
    }
}