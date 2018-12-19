package worldgenerator.map.views;


import java.awt.Color;
import java.util.Random;

import worldgenerator.geometry.Voronoi;
import worldgenerator.map.Map;
import worldgenerator.map.MapSite;

/**
 * TestGraphImpl.java
 *
 * Supplies information for Voronoi graphing logic:
 *
 * 1) biome mapping information based on a Site's elevation and moisture
 *
 * 2) color mapping information based on biome, and for bodies of water
 *
 * @author Connor
 */
public class BiomeMap extends Map {
	
	public static enum ErrorColour {
		
	}
	
    public static enum ColorData {
    	//TODO: MORE BIOMES!
    
        OCEAN_TRENCH(0x08088A), DEEP_OCEAN(0x0404B4), MEDIUM_OCEAN(0x0040FF), LOW_OCEAN(0x013ADF), SHALLOW_OCEAN(0x0040FF), COASTAL_OCEAN(0x0080FF), 
        LAKE(0x336699), BEACH(0xa09077), SNOW(0xffffff),
        TUNDRA(0xbbbbaa), BARE(0x888888), SCORCHED(0x555555), TAIGA(0x99aa77),
        SHURBLAND(0x889977), TEMPERATE_DESERT(0xc9d29b),
        TEMPERATE_RAIN_FOREST(0x448855), TEMPERATE_DECIDUOUS_FOREST(0x679459),
        GRASSLAND(0x88aa55), SUBTROPICAL_DESERT(0xd2b98b), SHRUBLAND(0x889977),
        ICE(0x99ffff), MARSH(0x2f6666), TROPICAL_RAIN_FOREST(0x337755),
        TROPICAL_SEASONAL_FOREST(0x559944), COAST(0x33335a),
        LAKESHORE(0x225588), RIVER(0x225588);
        public Color color;

        ColorData(int color) {
            this.color = new Color(color);
        }
    }
    
    public static enum ElevationColours {
    	Color16(51, 51, 51), Color15(61, 61, 61), Color14(71, 71, 71), Color13(81, 81, 81), Color12(91, 91, 91), 
    	Color11(102, 102, 102), Color10(122, 122, 122), Color9(142, 142, 142), Color8(162, 162, 162), Color7(182, 182, 182), 
    	Color6(204, 204, 204), Color5(224, 224, 224), Color4(225, 225, 204), Color3(51, 153, 255), Color2(0, 0, 204), Color1(0, 0, 153),
    	ERROR_COLOUR(200, 200, 0);
        public Color color;

        ElevationColours(int r, int g, int b) {
            this.color = new Color(r, g, b);
        }
    }

    public BiomeMap(Voronoi v, int numLloydRelaxations, Random r, 
    		double seaLevel, double oceanicRate, int numberOfPlates, 
    		double temperatureMod, double moistureMod) {
        super(v, numLloydRelaxations, r, seaLevel, oceanicRate, numberOfPlates, temperatureMod, moistureMod);
        RIVER = ColorData.RIVER.color;
    }
    
    public static Color getElevationColour(MapSite p) {
    	if (p.elevation > 1) {
            return ElevationColours.Color16.color;
        } else if (p.elevation == 1) {
        	return ElevationColours.Color15.color;
        } else if (p.elevation > 0.9) {
        	 return ElevationColours.Color14.color;
        } else if(p.elevation > 0.8){
        	 return ElevationColours.Color13.color;
        } else if (p.moisture > 0.7) {
        	 return ElevationColours.Color12.color;
        } else if (p.moisture > 0.6) {
        	 return ElevationColours.Color11.color;
        } else if (p.elevation > 0.5){
        	 return ElevationColours.Color10.color;
        } else if (p.elevation > 0.4) {
        	 return ElevationColours.Color9.color;
        } else if (p.elevation > 0.3) {
        	 return ElevationColours.Color8.color;
        } else if (p.elevation > 0.2) {
        	 return ElevationColours.Color7.color;
        } else if (p.elevation > 0.1) {
        	 return ElevationColours.Color6.color;
        } else if(p.elevation > 0) {
        	 return ElevationColours.Color5.color;
        }else if(p.elevation == 0) {
        	 return ElevationColours.Color4.color;
        } else if (p.elevation >= -0.5){
        	 return ElevationColours.Color3.color;
        }else if (p.elevation > -1){
       	 	 return ElevationColours.Color2.color;
        } else if (p.elevation <= -1){
        	 return ElevationColours.Color1.color;
        } else {
        	 return ElevationColours.ERROR_COLOUR.color;
        }
    }

    @Override
	public Color getColor(Enum<?> biome) {
        return ((ColorData) biome).color;
    }

    @Override
	public Enum getBiome(MapSite p) {
    	if (p.elevation > 0.9) {
            if (p.moisture > 0.50) {
                return ColorData.SNOW;
            } else if (p.moisture > 0.33) {
                return ColorData.TUNDRA;
            } else if (p.moisture > 0.08) {
                return ColorData.BARE;
            } else {
                return ColorData.SCORCHED;
            }
        } else if (p.elevation > 0.6) {
            if (p.moisture > 0.66) {
                return ColorData.TAIGA;
            } else if (p.moisture > 0.33) {
                return ColorData.SHRUBLAND;
            } else {
                return ColorData.TEMPERATE_DESERT;
            }
        } else if (p.elevation > 0.3) {
            if (p.moisture > 0.83) {
                return ColorData.TEMPERATE_RAIN_FOREST;
            } else if (p.moisture > 0.50) {
                return ColorData.TEMPERATE_DECIDUOUS_FOREST;
            } else if (p.moisture > 0.25) {
                return ColorData.GRASSLAND;
            } else {
                return ColorData.TEMPERATE_DESERT;
            }
        } else if (p.elevation > 0) {
            if (p.moisture > 0.66) {
                return ColorData.TROPICAL_RAIN_FOREST;
            } else if (p.moisture > 0.33) {
                return ColorData.TROPICAL_SEASONAL_FOREST;
            } else if (p.moisture > 0.20) {
                return ColorData.GRASSLAND;
            } else {
                return ColorData.SUBTROPICAL_DESERT;
            }
        } else if (p.elevation > -0.025){
        	return ColorData.COASTAL_OCEAN;
        } else if (p.elevation > -0.06){
        	return ColorData.SHALLOW_OCEAN;
        } else if (p.elevation > -0.15){
        	return ColorData.LOW_OCEAN;
        } else if (p.elevation > -0.2){
        	return ColorData.MEDIUM_OCEAN;
        } else if (p.elevation > -0.4){
        	return ColorData.DEEP_OCEAN;
        } else {
            return ColorData.OCEAN_TRENCH;
        }
    }
}
