package worldgenerator.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

import worldgenerator.geometry.*;
import worldgenerator.map.formations.River.RiverList;
import worldgenerator.map.views.BiomeMap;
import worldgenerator.map.formations.*;


public abstract class Map {
	public RiverList rivers;
	public ArrayList<MapBorder> edges;
    public ArrayList<MapVertex> vertices;
    public ArrayList<MapSite> sites;
    public ArrayList<TectonicPlate> plates;
    public Rectangle bounds;
    public Color RIVER;
    public BufferedImage pixelCenterMap;

    public Map(Voronoi v, int numLloydRelaxations, Random r, 
    		double seaLevel, double oceanicRate, int numberOfPlates, 
    		double temperatureMod, double moistureMod) {
        bounds = v.getPlotBounds();
        for (int i = 0; i < numLloydRelaxations; i++) {
            ArrayList<Point> points = v.siteCoords();
            for (Point p : points) {
                ArrayList<Point> region = v.region(p);
                double x = 0;
                double y = 0;
                for (Point c : region) {
                    x += c.x;
                    y += c.y;
                }
                x /= region.size();
                y /= region.size();
                p.x = x;
                p.y = y;
            }
            v = new Voronoi(points, null, v.getPlotBounds());
        }
        Terrain terrain = new Terrain(v, numLloydRelaxations, r, seaLevel, oceanicRate, numberOfPlates, temperatureMod, moistureMod);
        importTerrain(terrain);
        pixelCenterMap = new BufferedImage((int) bounds.width, (int) bounds.height, BufferedImage.TYPE_4BYTE_ABGR); //POSSIBLE ISSUE WITH WIDTH =/= HEIGHT? So far, no.
    }
    
    public MapSite isInSite(Point p) {
    	if(!bounds.inBounds(p)) return null;
    	double distance = Double.MAX_VALUE;
    	MapSite selected = null;
    	for (MapSite site : sites) {
    		double newDistance = MathUtils.distance(site.location, p);
    		if(newDistance < distance) {
    			selected = site;
    			distance = newDistance;
    		}
		}
    	return selected;
    }
    
    //-------------------
    //TERRAIN & ELEVATION
    //-------------------
    
    private void importTerrain(Terrain terrain) {
    	this.rivers = terrain.rivers;
    	this.edges = terrain.edges;
    	this.vertices = terrain.vertices;
    	this.sites = terrain.sites;
    	this.plates = terrain.plates;
    }

    public abstract Enum<?> getBiome(MapSite p);

    public abstract Color getColor(Enum<?> biome);

    private MapBorder edgeWithCenters(MapSite c1, MapSite c2) {
        for (MapBorder e : c1.adjacentBorders) {
            if (e.site1 == c2 || e.site2 == c2) {
                return e;
            }
        }
        return null;
    }

    private void drawTriangle(Graphics2D g, MapVertex c1, MapVertex c2, MapSite center) {
        int[] x = new int[3];
        int[] y = new int[3];
        x[0] = (int) center.location.x;
        y[0] = (int) center.location.y;
        x[1] = (int) c1.location.x;
        y[1] = (int) c1.location.y;
        x[2] = (int) c2.location.x;
        y[2] = (int) c2.location.y;
        g.fillPolygon(x, y, 3);
    }

    private boolean closeEnough(double site2, double d2, double diff) {
        return Math.abs(site2 - d2) <= diff;
    }

    public BufferedImage createMap(int type, int labelType) {
        int width = (int) bounds.width;
        int height = (int) bounds.height;

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = img.createGraphics();

        switch (type) {
		case 1:
			paintBiomes(g, labelType);
			break;
		case 2:
			paintPlates(g, labelType);
			break;
		case 3:
			//TODO
			break;
		default:
			break;
		}

        return img;
    }

    private void drawPolygon(Graphics2D g, MapSite c, Color color) {
        g.setColor(color);

        //only used if MapSite c is on the edge of the graph. allows for completely filling in the outer polygons
        MapVertex edgeCorner1 = null;
        MapVertex edgeCorner2 = null;
        c.area = 0;
        for (MapSite n : c.adjacentSites) {
            MapBorder e = edgeWithCenters(c, n);

            if (e.vertex1 == null) {
                continue;
            }

            //find a corner on the exterior of the graph
            //if this MapBorder e has one, then it must have two,
            //finding these two corners will give us the missing
            //triangle to render. this special triangle is handled
            //outside this for loop
            MapVertex cornerWithOneAdjacent = e.vertex1.isOnMapEdge ? e.vertex1 : e.vertex2;
            if (cornerWithOneAdjacent.isOnMapEdge) {
                if (edgeCorner1 == null) {
                    edgeCorner1 = cornerWithOneAdjacent;
                } else {
                    edgeCorner2 = cornerWithOneAdjacent;
                }
            }

            drawTriangle(g, e.vertex1, e.vertex2, c);
            c.area += Math.abs(c.location.x * (e.vertex1.location.y - e.vertex2.location.y)
                    + e.vertex1.location.x * (e.vertex2.location.y - c.location.y)
                    + e.vertex2.location.x * (c.location.y - e.vertex1.location.y)) / 2;
        }

        //handle the missing triangle
        if (edgeCorner2 != null) {
            //if these two outer corners are NOT on the same exterior edge of the graph,
            //then we actually must render a polygon (w/ 4 points) and take into consideration
            //one of the four corners (either 0,0 or 0,height or width,0 or width,height)
            //note: the 'missing polygon' may have more than just 4 points. this
            //is common when the number of sites are quite low (less than 5), but not a problem
            //with a more useful number of sites. 
            //TODO: find a way to fix this

            if (closeEnough(edgeCorner1.location.x, edgeCorner2.location.x, 1)) {
                drawTriangle(g, edgeCorner1, edgeCorner2, c);
            } else {
                int[] x = new int[4];
                int[] y = new int[4];
                x[0] = (int) c.location.x;
                y[0] = (int) c.location.y;
                x[1] = (int) edgeCorner1.location.x;
                y[1] = (int) edgeCorner1.location.y;

                //determine which corner this is
                x[2] = (int) ((closeEnough(edgeCorner1.location.x, bounds.x, 1) || closeEnough(edgeCorner2.location.x, bounds.x, .5)) ? bounds.x : bounds.right);
                y[2] = (int) ((closeEnough(edgeCorner1.location.y, bounds.y, 1) || closeEnough(edgeCorner2.location.y, bounds.y, .5)) ? bounds.y : bounds.bottom);

                x[3] = (int) edgeCorner2.location.x;
                y[3] = (int) edgeCorner2.location.y;

                g.fillPolygon(x, y, 4);
                c.area += 0; //TODO: area of polygon given vertices
            }
        }
    }

    public void paintBiomes(Graphics2D g, int labelType) {
        paint(g, true, true, false, false, false, false, false, true, labelType);
    }
    
    public void paintPlates(Graphics2D g, int labelType) {
        paint(g, false, false, false, false, false, false, true, false, labelType);
    }

    //also records the area of each voronoi cell
    public void paint(Graphics2D g, boolean drawBiomes, boolean drawRivers, 
    		boolean drawSites, boolean drawCorners, boolean drawDelaunay, 
    		boolean drawVoronoi, boolean drawPlates, boolean drawPlateBorders,
    		int labelType) {
        
    	g.setColor(Color.BLACK);
        g.drawRect((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height);
        Graphics2D pixelCenterGraphics = pixelCenterMap.createGraphics();

        //draw via triangles
        for (MapSite c : sites) {
            if(drawPlates) {
            	drawPolygon(g, c, c.plate.colour);
            }
            if(drawBiomes) {
            	drawPolygon(g, c, getColor(getBiome(c))); //BiomeMap.getElevationColour(c)
            }
            drawPolygon(pixelCenterGraphics, c, getColor(getBiome(c))); //TODO: Biome Painting redo new Color(c.index)
            if (labelType == 0) {
            } else if(labelType == 1) {
            	g.setColor(Color.BLACK);
                String elevationString = (c.elevation>0?"+":"")+c.elevation+"                                                    "; 
                g.drawString(elevationString.substring(0, 4), (int)c.location.x-11, (int)c.location.y+5);
            } else if (labelType == 2) {
            	g.setColor(Color.BLACK);
                String moistureString = (c.moisture>0?"+":"")+c.moisture+"                                                    ";
                g.drawString(moistureString.substring(0, 4), (int)c.location.x-11, (int)c.location.y+5);
            } else if (labelType == 3) {
            	g.setColor(Color.BLACK);
                String temperatureString = (c.temperature>0?"+":"")+c.temperature+"                                                    ";
                g.drawString(temperatureString.substring(0, 4), (int)c.location.x-11, (int)c.location.y+5);
            }
        }

        for (MapBorder e : edges) {
            if (drawDelaunay) {
                g.setStroke(new BasicStroke(1));
                g.setColor(Color.YELLOW);
                g.drawLine((int) e.site1.location.x, (int) e.site1.location.y, (int) e.site2.location.x, (int) e.site2.location.y);
            }
            if (drawVoronoi && e.vertex1 != null && e.vertex2 != null) {
                g.setStroke(new BasicStroke(1));
                g.setColor(Color.CYAN);
                g.drawLine((int) e.vertex1.location.x, (int) e.vertex1.location.y, (int) e.vertex2.location.x, (int) e.vertex2.location.y);
            }if(drawPlateBorders && e.isPlateBoundry && e.vertex1 != null && e.vertex2 != null) {
            	g.setStroke(new BasicStroke(1));
            	g.setColor(Color.RED);
            	g.drawLine((int) e.vertex1.location.x, (int) e.vertex1.location.y, (int) e.vertex2.location.x, (int) e.vertex2.location.y);
            }
            if (drawRivers && e.river > 0) {
                g.setStroke(new BasicStroke(1 + (int) Math.sqrt(e.river * 2)));
                g.setColor(RIVER);
                g.drawLine((int) e.vertex1.location.x, (int) e.vertex1.location.y, (int) e.vertex2.location.x, (int) e.vertex2.location.y);
            }
        }

        if (drawSites) {
            g.setColor(Color.BLACK);
            sites.stream().forEach((s) -> {
                g.fillOval((int) (s.location.x - 2), (int) (s.location.y - 2), 4, 4);
            });
        }

        if (drawCorners) {
            g.setColor(Color.WHITE);
            vertices.stream().forEach((c) -> {
                g.fillOval((int) (c.location.x - 2), (int) (c.location.y - 2), 4, 4);
            });
        }
         //TODO: POSSIBLE ISSUE USING HEIGHT and width
    }
}
