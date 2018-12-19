package worldgenerator.map;

import java.util.ArrayList;

import worldgenerator.geometry.Point;
import worldgenerator.map.formations.TectonicPlate;

public class MapSite {
	public int index, elevationFailCount = 0;
    public Point location;
    
    public ArrayList<MapVertex> adjacentVertices = new ArrayList<MapVertex>();
    public ArrayList<MapSite> adjacentSites = new ArrayList<MapSite>();
    public ArrayList<MapBorder> adjacentBorders = new ArrayList<MapBorder>();
    
    public boolean border, ocean, water, coast, elevationCalculated;
    public double area;
    public TectonicPlate plate;
    
    //Factors
    public double elevation;
    public double moisture;
    public double salinity;
    public double temperature;
    
    public Enum biome;
	//public boolean isOnPlateBorder;
    
    public MapSite() {
    }

    public MapSite(Point location) {
        this.location = location;
    }
    
    public boolean isAssignedToPlate() {
    	return plate != null;
    }
}
