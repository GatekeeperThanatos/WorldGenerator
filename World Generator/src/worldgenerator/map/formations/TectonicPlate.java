package worldgenerator.map.formations;

import java.awt.Color;
import java.util.ArrayList;

import worldgenerator.geometry.MathUtils.Vector2D;
import worldgenerator.geometry.Point;
import worldgenerator.map.*;

public class TectonicPlate {

	public ArrayList<MapSite> sites = new ArrayList<MapSite>();
	public ArrayList <MapBorder> plateBorders = new ArrayList<MapBorder>();
	public ArrayList<MapVertex> plateCorners = new ArrayList<MapVertex>();
	public Color colour;
	public Vector2D driftVector;
	public double driftForce, spinForce, baseElevation;
	public boolean oceanic, onEdge = false;
	public MapVertex plateOrigin;
	
	public TectonicPlate(Color colour, Vector2D driftVector, double driftForce, double spinForce, double baseElevation, boolean oceanic, MapVertex vertex) {
		this.colour = colour;
		this.driftVector = driftVector;
		this.driftForce = driftForce;
		this.spinForce = spinForce;
		this.baseElevation = baseElevation; 
		this.oceanic = oceanic;
		this.plateOrigin = vertex;
	}

	public Vector2D calculateMovement(Point p) {
		Vector2D 
			movement = driftVector.crossProduct().setLength(driftForce * Vector2D.projectVectors(new Vector2D(p), driftVector).distanceTo(p)),
			originVector = new Vector2D(plateOrigin.location.x, plateOrigin.location.y);
		movement.add(originVector.crossProduct().setLength(spinForce * Vector2D.projectVectors(new Vector2D(p), new Vector2D(plateOrigin.location)).distanceTo(p)));
		return movement;
	}
}
