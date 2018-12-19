package worldgenerator.map.formations;

import java.util.ArrayList;

import worldgenerator.map.*;

public class River{
	private ArrayList<MapVertex> river;
	
	public River(ArrayList<MapVertex> river) {
		this.river = river;
		//TODO: Might need to sort by elevation, forking rivers may present issues
	}
	
	public River() {
		this(new ArrayList<MapVertex>());
	}
	
	public int length() {
		return river.size();
	}
	
	public void addVertex(MapVertex v) {
		river.add(v);
		v.isPartOfRiver = true;
	}
	
	public ArrayList<MapVertex> getRiver() {
		return river;
	}
	
	public static class RiverList{
		private ArrayList<River> rivers = new ArrayList<River>();
		
		public void addRiver(River r) {
			rivers.add(r);
		}
		
		public void removeRiver(River r) {
			rivers.remove(r);
		}
		
		public boolean contains(River r) {
			return rivers.contains(r);
		}
		
		public ArrayList<River> getRivers(){
			return rivers;
		}
	}
}

