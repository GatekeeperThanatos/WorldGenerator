package worldgenerator.ui;

import javax.swing.*;
import javax.swing.event.MouseInputListener;

import worldgenerator.map.Map;
import worldgenerator.map.MapBorder;
import worldgenerator.map.MapSite;
import worldgenerator.map.MapVertex;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class MapCanvas extends JComponent implements MouseWheelListener, MouseMotionListener, MouseListener, MouseInputListener{
	/** */
	private static final long serialVersionUID = 1L;
	/** */
	public static final double SCALE_STEP = 0.06d;
	/** */
	private Dimension initialSize;
	/** */
	private Rectangle2D mapBack;
	/** */
	private Point origin;
	/** */
	private BufferedImage mapImage;
	/** */
	private double zoom;
	/** */
	private double previousZoom = zoom;
	/** */
	private double scrollX = 0d;
	/** */
	private double scrollY = 0d;
	private MapSite selected;
	private Map map;
	private int type;

	/** */
	public MapCanvas(Map map, int type, int labelType) {
		this(1.0, map.createMap(type, labelType));
		this.map = map;
		this.type = type;
	}

	/** */
	private MapCanvas(double zoom, BufferedImage mapImage) {
		this.zoom = zoom;
		this.mapImage = mapImage;
		this.mapBack = new Rectangle2D.Double(0, 0, mapImage.getWidth(), mapImage.getHeight());
		addMouseWheelListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);
		setAutoscrolls(false);
		Rectangle mapRect = new Rectangle((int) (mapImage.getWidth()), (int) (mapImage.getHeight()));
		setMinimumSize(new Dimension(10, 10));
		setPreferredSize(new Dimension((int) (mapImage.getWidth()), (int) (mapImage.getHeight())));
		setMaximumSize(new Dimension(mapImage.getWidth()+50, mapImage.getHeight()+50));
		//TODO: If there is an issue, I had set it to new Dimension(map.getWidth()*10, map.getHeight()*10) for some reason
		
		Action toOrigin = new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
	            scrollRectToVisible(mapRect);
		    }
		};
		getInputMap().put(KeyStroke.getKeyStroke("F"), "toOrigin");
		getActionMap().put("toOrigin", toOrigin);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D g2d = (Graphics2D) g.create();
		
		g2d.clearRect(0, 0, getWidth(), getHeight());
		g2d.scale(zoom, zoom);
		
		Rectangle size = getBounds();
		
		double tx = ((size.getWidth() - mapBack.getWidth() * zoom) / 2) / zoom;
		double ty = ((size.getHeight() - mapBack.getHeight() * zoom) / 2) / zoom;
		
		g2d.translate(tx, ty);
		g2d.setColor(Color.LIGHT_GRAY);
		g2d.fill(mapBack);
		g2d.drawImage(mapImage, getInsets().left, getInsets().top, (int) (mapImage.getWidth()), (int) (mapImage.getHeight()), null);
		g2d.setColor(Color.DARK_GRAY);
		g2d.setStroke(new BasicStroke(5.0f));
		g2d.draw(mapBack);
		
		g2d.dispose();
	}

	@Override
	public void setSize(Dimension size) {
		super.setSize(size);
		if (initialSize == null) {
			this.initialSize = size;
		}
	}

	@Override
	public void setPreferredSize(Dimension preferredSize) {
		super.setPreferredSize(preferredSize);
		if (initialSize == null) {
			this.initialSize = preferredSize;
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		double zoomFactor = -SCALE_STEP * e.getPreciseWheelRotation() * zoom;
		zoom = Math.abs(zoom + zoomFactor);
		Dimension d = new Dimension((int) (initialSize.width * zoom), (int) (initialSize.height * zoom));
		setPreferredSize(d);
		setSize(d);
		validate();
		setZoomTarget(e.getPoint());
		previousZoom = zoom;
		
	}

	public void setZoomTarget(Point2D point) {
		Rectangle size = getBounds();
		Rectangle visibleRect = getVisibleRect();
		scrollX = size.getCenterX();
		scrollY = size.getCenterY();
		if (point != null) {
			scrollX = point.getX() / previousZoom * zoom - (point.getX() - visibleRect.getX());
			scrollY = point.getY() / previousZoom * zoom - (point.getY() - visibleRect.getY());
		}

		visibleRect.setRect(scrollX, scrollY, visibleRect.getWidth(), visibleRect.getHeight());
		scrollRectToVisible(visibleRect);
	}

	public void mouseDragged(MouseEvent e) {
		if (origin != null) {
			int deltaX = origin.x - e.getX();
			int deltaY = origin.y - e.getY();
			Rectangle view = getVisibleRect();
			view.x += deltaX;
			view.y += deltaY;
			scrollRectToVisible(view);
		}
	}
	
	public void mousePressed(MouseEvent e) {
		origin = new Point(e.getX(), e.getY());
	}
	
	public void mouseMoved(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
		if(map.bounds.inBounds(e.getPoint().getX(), e.getPoint().getY())) {
			selected = map.isInSite(
				new worldgenerator.geometry.Point(
					e.getPoint().getX(), 
					e.getPoint().getY()
				)
			); //TODO: Make graphic for selected tile and info
			
			if(type == 1) {
				System.out.println(
						"Selected MapSite \n"
						+ "\tElevation: " + selected.elevation +"\n"
						+ "\tMoisture: " + selected.moisture +"\n"
						+ "\tTemperature: " + selected.temperature +"\n"
						+ "\tSalinity: " + selected.salinity +"\n"
						+ "\tLocation: " + selected.location
					);
			} else if (type == 2) {
				System.out.println(
				"Plate: \n"
						+ "\tBase Elevation: " + selected.plate.baseElevation +"\n"
						+ "\tOceanic Plate: " +selected.plate.oceanic
				);
			}
			
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
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
        if (edgeCorner2 != null) {

            if (closeEnough(edgeCorner1.location.x, edgeCorner2.location.x, 1)) {
                drawTriangle(g, edgeCorner1, edgeCorner2, c);
            } else {
                int[] x = new int[4];
                int[] y = new int[4];
                x[0] = (int) c.location.x;
                y[0] = (int) c.location.y;
                x[1] = (int) edgeCorner1.location.x;
                y[1] = (int) edgeCorner1.location.y;
                x[2] = (int) ((closeEnough(edgeCorner1.location.x, map.bounds.x, 1) || 
                		closeEnough(edgeCorner2.location.x, map.bounds.x, .5)) ? map.bounds.x : map.bounds.right);
                y[2] = (int) ((closeEnough(edgeCorner1.location.y, map.bounds.y, 1) || 
                		closeEnough(edgeCorner2.location.y, map.bounds.y, .5)) ? map.bounds.y : map.bounds.bottom);

                x[3] = (int) edgeCorner2.location.x;
                y[3] = (int) edgeCorner2.location.y;

                g.fillPolygon(x, y, 4);
                c.area += 0; //TODO: area of polygon given vertices
            }
        }
    }
	
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

    //also records the area of each voronoi cell
    public void paint(Graphics2D g, boolean drawBiomes, boolean drawRivers, 
    		boolean drawSites, boolean drawCorners, boolean drawDelaunay, 
    		boolean drawVoronoi, boolean drawPlates, boolean drawPlateBorders,
    		int labelType) {
        
    	g.setColor(Color.BLACK);
        g.drawRect((int) map.bounds.x, (int) map.bounds.y, (int) map.bounds.width, (int) map.bounds.height);
        Graphics2D pixelCenterGraphics = map.pixelCenterMap.createGraphics();

        //draw via triangles
        for (MapSite c : map.sites) {
            if(drawPlates) {
            	drawPolygon(g, c, c.plate.colour);
            }
            if(drawBiomes) {
            	drawPolygon(g, c, map.getColor(map.getBiome(c))); //BiomeMap.getElevationColour(c)
            }
            drawPolygon(pixelCenterGraphics, c, map.getColor(map.getBiome(c))); //TODO: Biome Painting redo new Color(c.index)
            if(labelType == 1) {
            	g.setColor(Color.GREEN);
                String elevationString = (c.elevation>0?"+":"")+c.elevation; 
                g.drawString(elevationString.substring(0, 4), (int)c.location.x-11, (int)c.location.y+5);
            }
        }

        for (MapBorder e : map.edges) {
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
                g.setColor(map.RIVER);
                g.drawLine((int) e.vertex1.location.x, (int) e.vertex1.location.y, (int) e.vertex2.location.x, (int) e.vertex2.location.y);
            }
        }

        if (drawSites) {
            g.setColor(Color.BLACK);
            map.sites.stream().forEach((s) -> {
                g.fillOval((int) (s.location.x - 2), (int) (s.location.y - 2), 4, 4);
            });
        }

        if (drawCorners) {
            g.setColor(Color.WHITE);
            map.vertices.stream().forEach((c) -> {
                g.fillOval((int) (c.location.x - 2), (int) (c.location.y - 2), 4, 4);
            });
        }
         //TODO: POSSIBLE ISSUE USING HEIGHT and width
    }
}
