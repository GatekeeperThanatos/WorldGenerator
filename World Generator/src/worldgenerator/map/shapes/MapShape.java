package worldgenerator.map.shapes;



import java.util.Random;
import worldgenerator.geometry.*;

public interface MapShape {

    /**
     * Uses specific algorithm to check point.
     *
     * @param p Corner location.
     * @param bounds Graph bounds.
     * @param random Voronoi's randomizer to keep identical results for user's seed.
     * @return
     */
    boolean isWater(Point p, Rectangle bounds, Random random);

}