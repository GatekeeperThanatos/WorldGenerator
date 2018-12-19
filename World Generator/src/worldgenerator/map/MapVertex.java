package worldgenerator.map;

import java.util.ArrayList;

import worldgenerator.geometry.*;
import worldgenerator.geometry.MathUtils.Vector2D;


public class MapVertex{
	
	public ArrayList<MapSite> adjacentSites = new ArrayList<MapSite>();
    public ArrayList<MapVertex> adjacentVertices = new ArrayList<MapVertex>();
    public ArrayList<MapBorder> adjacentBorders = new ArrayList<MapBorder>();
    public Point location;
    public MapVertex downslope, origin;
    public int index, calculationType = -1;
    public boolean isOnMapEdge, isOnPlateBorder;
    public boolean water, ocean, coast, isPartOfRiver = false, elevationCalculated = false;
    public int river;
    public double 
    	distanceToPlateOrigin, 
    	distanceToPlateBorder;
    
    //Factors
    public double 
    	moisture = 0,
    	salinity = 0,
    	temperature = 0,
    	elevation = 0, //May cause issues initializing
    	tectonicShear = 0,
    	tectonicPressure = 0,
    	volcanism;
	public int elevationFailCount = 0;
	
	public double distanceTo(Point p) {
		return MathUtils.distance(location, p);
	}

	public Vector2D vectorTo(MapVertex vertex) {
		Vector2D thisVertex = new Vector2D(location);
		
		return thisVertex.subFrom(new Vector2D(vertex.location));
	}
}
