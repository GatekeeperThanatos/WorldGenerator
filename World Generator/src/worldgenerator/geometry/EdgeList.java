package worldgenerator.geometry;

import java.util.ArrayList;

public final class EdgeList implements Disposable {

    private double deltax;
    private double _xmin;
    private int hashSize;
    private ArrayList<Halfedge> hash;
    public Halfedge leftEnd;
    public Halfedge rightEnd;

    @Override
    public void dispose() {
        Halfedge halfEdge = leftEnd;
        Halfedge prevHe;
        while (halfEdge != rightEnd) {
            prevHe = halfEdge;
            halfEdge = halfEdge.edgeListRightNeighbor;
            prevHe.dispose();
        }
        leftEnd = null;
        rightEnd.dispose();
        rightEnd = null;

        hash.clear();
        hash = null;
    }

    public EdgeList(double xmin, double deltax, int numberOfSites) {
        _xmin = xmin;
        this.deltax = deltax;
        hashSize = 2 * numberOfSites;

        hash = new ArrayList<Halfedge>(hashSize);

        // two dummy Halfedges:
        leftEnd = Halfedge.createDummy();
        rightEnd = Halfedge.createDummy();
        leftEnd.edgeListLeftNeighbor = null;
        leftEnd.edgeListRightNeighbor = rightEnd;
        rightEnd.edgeListLeftNeighbor = leftEnd;
        rightEnd.edgeListRightNeighbor = null;
        
        for(int i = 0; i < hashSize; i++){
            hash.add(null);
        }
        
        hash.set(0, leftEnd);
        hash.set(hashSize - 1, rightEnd);
    }

    /**
     * Insert newHalfedge to the right of lb
     *
     * @param lb
     * @param newHalfedge
     *
     */
    public void insert(Halfedge lb, Halfedge newHalfedge) {
        newHalfedge.edgeListLeftNeighbor = lb;
        newHalfedge.edgeListRightNeighbor = lb.edgeListRightNeighbor;
        lb.edgeListRightNeighbor.edgeListLeftNeighbor = newHalfedge;
        lb.edgeListRightNeighbor = newHalfedge;
    }

    /**
     * This function only removes the Halfedge from the left-right list. We
     * cannot dispose it yet because we are still using it.
     *
     * @param halfEdge
     *
     */
    public void remove(Halfedge halfEdge) {
        halfEdge.edgeListLeftNeighbor.edgeListRightNeighbor = halfEdge.edgeListRightNeighbor;
        halfEdge.edgeListRightNeighbor.edgeListLeftNeighbor = halfEdge.edgeListLeftNeighbor;
        halfEdge.edge = Edge.DELETED;
        halfEdge.edgeListLeftNeighbor = halfEdge.edgeListRightNeighbor = null;
    }

    /**
     * Find the rightmost Halfedge that is still left of p
     *
     * @param p
     * @return
     *
     */
    public Halfedge edgeListLeftNeighbor(Point p) {
        int i, bucket;
        Halfedge halfEdge;

        /* Use hash table to get close to desired halfedge */
        bucket = (int) ((p.x - _xmin) / deltax * hashSize);
        if (bucket < 0) {
            bucket = 0;
        }
        if (bucket >= hashSize) {
            bucket = hashSize - 1;
        }
        halfEdge = getHash(bucket);
        if (halfEdge == null) {
            for (i = 1; true; ++i) {
                if ((halfEdge = getHash(bucket - i)) != null) {
                    break;
                }
                if ((halfEdge = getHash(bucket + i)) != null) {
                    break;
                }
            }
        }
        /* Now search linear list of halfedges for the correct one */
        if (halfEdge == leftEnd || (halfEdge != rightEnd && halfEdge.isLeftOf(p))) {
            do {
                halfEdge = halfEdge.edgeListRightNeighbor;
            } while (halfEdge != rightEnd && halfEdge.isLeftOf(p));
            halfEdge = halfEdge.edgeListLeftNeighbor;
        } else {
            do {
                halfEdge = halfEdge.edgeListLeftNeighbor;
            } while (halfEdge != leftEnd && !halfEdge.isLeftOf(p));
        }

        /* Update hash table and reference counts */
        if (bucket > 0 && bucket < hashSize - 1) {
            hash.set(bucket, halfEdge);
        }
        return halfEdge;
    }

    /* Get entry from hash table, pruning any deleted nodes */
    private Halfedge getHash(int b) {
        Halfedge halfEdge;

        if (b < 0 || b >= hashSize) {
            return null;
        }
        halfEdge = hash.get(b);
        if (halfEdge != null && halfEdge.edge == Edge.DELETED) {
            /* Hash table points to deleted halfedge.  Patch as necessary. */
            hash.set(b, null);
            // still can't dispose halfEdge yet!
            return null;
        } else {
            return halfEdge;
        }
    }
}