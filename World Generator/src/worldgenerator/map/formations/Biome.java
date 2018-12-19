package worldgenerator.map.formations;

import java.awt.Color;

public enum Biome {
	//TODO: MORE BIOMES! Elevation based biomes (Cloud forest), Salinity based biomes (Saltplanes), Wetland differentiation, Shrubland, Monsoon Rainforest, Mangroves etc
    GLACIER(179, 255, 255), CORAL_REEF(191, 128, 255),
    POLAR_OCEAN(49, 151, 253), TEMPERATE_OCEAN(50, 152, 254), TROPICAL_OCEAN(51, 153, 255), //Add depths
    COLD_DESERT(255, 166, 77), TEMPERATE_DESERT(255, 204, 153), DESERT(255, 230, 204),
    TUNDRA(230, 255, 230), GRASSLAND(102, 255, 102), SAVANNAH(204, 255, 102),
    TAIGA(204, 255, 204), TEMPERATE_FOREST(41, 163, 41), TROPICAL_FOREST(71, 209, 71),
    BOREAL_FOREST(0, 51, 0), TEMPERATE_RAINFOREST(77, 153, 0), RAINFOREST(51, 102, 0),
    POLAR_WETLAND(153, 153, 102), TEMPERATE_WETLAND(153, 102, 51), TROPICAL_WETLAND(102, 102, 51); //Add different types, these aren't realistic
	
    public Color color;

    Biome(int r, int g, int b) {
        this.color = new Color(r, g, b);
    }
}
