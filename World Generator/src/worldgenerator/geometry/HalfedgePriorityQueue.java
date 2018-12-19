package worldgenerator.geometry;

import java.util.ArrayList;

public final class HalfedgePriorityQueue // also known as heap
{

    private ArrayList<Halfedge> hash;
    private int count;
    private int minBucket;
    private int hashSize;
    private double yMIN;
    private double deltaY;

    public HalfedgePriorityQueue(double ymin, double deltay, int sqrt_nsites) {
        yMIN = ymin;
        deltaY = deltay;
        hashSize = 4 * sqrt_nsites;
        initialize();
    }

    public void dispose() {
        // get rid of dummies
        for (int i = 0; i < hashSize; ++i) {
            hash.get(i).dispose();
        }
        hash.clear();
        hash = null;
    }

    private void initialize() {
        int i;

        count = 0;
        minBucket = 0;
        hash = new ArrayList<Halfedge>(hashSize);
        // dummy Halfedge at the top of each hash
        for (i = 0; i < hashSize; ++i) {
            hash.add(Halfedge.createDummy());
            hash.get(i).nextInPriorityQueue = null;
        }
    }

    public void insert(Halfedge halfEdge) {
        Halfedge previous, next;
        int insertionBucket = bucket(halfEdge);
        if (insertionBucket < minBucket) {
            minBucket = insertionBucket;
        }
        previous = hash.get(insertionBucket);
        while ((next = previous.nextInPriorityQueue) != null
                && (halfEdge.ystar > next.ystar || (halfEdge.ystar == next.ystar && halfEdge.vertex.getX() > next.vertex.getX()))) {
            previous = next;
        }
        halfEdge.nextInPriorityQueue = previous.nextInPriorityQueue;
        previous.nextInPriorityQueue = halfEdge;
        ++count;
    }

    public void remove(Halfedge halfEdge) {
        Halfedge previous;
        int removalBucket = bucket(halfEdge);

        if (halfEdge.vertex != null) {
            previous = hash.get(removalBucket);
            while (previous.nextInPriorityQueue != halfEdge) {
                previous = previous.nextInPriorityQueue;
            }
            previous.nextInPriorityQueue = halfEdge.nextInPriorityQueue;
            count--;
            halfEdge.vertex = null;
            halfEdge.nextInPriorityQueue = null;
            halfEdge.dispose();
        }
    }

    private int bucket(Halfedge halfEdge) {
        int theBucket = (int) ((halfEdge.ystar - yMIN) / deltaY * hashSize);
        if (theBucket < 0) {
            theBucket = 0;
        }
        if (theBucket >= hashSize) {
            theBucket = hashSize - 1;
        }
        return theBucket;
    }

    private boolean isEmpty(int bucket) {
        return (hash.get(bucket).nextInPriorityQueue == null);
    }

    /**
     * move _minBucket until it contains an actual Halfedge (not just the dummy
     * at the top);
     *
     */
    private void adjustMinBucket() {
        while (minBucket < hashSize - 1 && isEmpty(minBucket)) {
            ++minBucket;
        }
    }

    public boolean empty() {
        return count == 0;
    }

    /**
     * @return coordinates of the Halfedge's vertex in V*, the transformed
     * Voronoi diagram
     *
     */
    public Point min() {
        adjustMinBucket();
        Halfedge answer = hash.get(minBucket).nextInPriorityQueue;
        return new Point(answer.vertex.getX(), answer.ystar);
    }

    /**
     * remove and return the min Halfedge
     *
     * @return
     *
     */
    public Halfedge extractMin() {
        Halfedge answer;

        // get the first real Halfedge in _minBucket
        answer = hash.get(minBucket).nextInPriorityQueue;

        hash.get(minBucket).nextInPriorityQueue = answer.nextInPriorityQueue;
        count--;
        answer.nextInPriorityQueue = null;

        return answer;
    }
}