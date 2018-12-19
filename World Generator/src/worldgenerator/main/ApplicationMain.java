package worldgenerator.main;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Random;

import javax.swing.JFrame;

import worldgenerator.geometry.Voronoi;
import worldgenerator.map.Map;
import worldgenerator.map.views.BiomeMap;
import worldgenerator.ui.MapPane;

public class ApplicationMain {
	
	public static void main(String[] args) {
		//Mercator aspect ratio is 1:1,65, equirectangular aspect ratio is 1:2
		final int GRAPH_WIDTH = 7480; //7480 works (mercator) 12000 works (equirectangular)
		final int GRAPH_HEIGHT = 4000; //4000 works (mercator) 6000 works (equirectangular)
		final int SITES = 100000; //50 to 190_000 works
		final int TYPE = 1; //1 for Elevation, 2 for Plates
		final double SEA_LEVEL = 1; //TODO: Higher for less land, lower for more
		final double OCEANIC_RATE = 0.65; //Affects the number of oceanic plates (TODO: WIP)
		final int PLATES = 36; //The number of tectonic plates on the map
		final double TEMPERATURE_MODIFIER = 1; //TODO: WIP
		final double MOISTURE_MODIFIER = 1; //TODO: WIP
		final boolean RANDOM_SEED = true; //True for random seeds, false for systemTime
		final long SEED = 127L; //Affects generation, only applicable if RANDOM_SEED is false
		final int LABELS = 0; //0 for none, 1 for elevations 
				
		MapPane p = new MapPane(true, 
				createVoronoiGraph(GRAPH_WIDTH, GRAPH_HEIGHT, SITES, 2, (RANDOM_SEED) ? System.nanoTime() : SEED, 
        		SEA_LEVEL, OCEANIC_RATE, PLATES, 
        		TEMPERATURE_MODIFIER, MOISTURE_MODIFIER), TYPE, LABELS);
		p.setBackground(Color.BLACK);
		JFrame f = new JFrame("WorldGenerator");
		f.add(p);
		f.setBackground(Color.BLACK);
		f.setVisible(true);
		f.pack();
		f.setSize(new Dimension(1600, 900));
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public static Map createVoronoiGraph(int width, int height, int numSites, int numLloydRelaxations, long seed, 
    		double seaLevel, double oceanicRate, int numberOfPlates, 
    		double temperatureMod, double moistureMod) {
        final Random r = new Random(seed);

        //make the intial underlying voronoi structure
        final Voronoi v = new Voronoi(numSites, width, height, r, null);

        //assemble the voronoi strucutre into a usable graph object representing a map
        final BiomeMap graph = new BiomeMap(v, numLloydRelaxations, r, seaLevel, oceanicRate, numberOfPlates, temperatureMod, moistureMod);

        return graph;
    }
}
