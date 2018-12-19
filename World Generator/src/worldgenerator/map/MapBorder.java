package worldgenerator.map;

import worldgenerator.geometry.MathUtils;
import worldgenerator.geometry.Point;

public class MapBorder {
	public int index;
	public MapSite site1, site2;
	public MapVertex vertex1, vertex2;
	public Point midpoint;
	public int river;
	public boolean isPlateBoundry;
	
	public void setVornoi(MapVertex vertex1, MapVertex vertex2) {
	    this.vertex1 = vertex1;
	    this.vertex2 = vertex2;
	    midpoint = new Point((vertex1.location.x + vertex2.location.x) / 2, (vertex1.location.y + vertex2.location.y) / 2);
	}
	
	public MapVertex getVertexOppositeTo(MapVertex vertex) {
		if(vertex == vertex1) {
			return vertex2;
		}else if(vertex == vertex2) {
			return vertex1;
		}else {
			return null;
		}
	}
	
	public double getSiteSlope() {
		return Math.abs(site1.elevation - site2.elevation);
	}
	
	public double getVertexSlope() {
		return Math.abs(vertex1.elevation - vertex2.elevation);
	}

	public double length() {
		return MathUtils.distance(vertex1.location, vertex2.location);
	}
}
