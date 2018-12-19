package worldgenerator.map.formations;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import worldgenerator.geometry.*;
import worldgenerator.geometry.MathUtils.*;
import worldgenerator.map.*;
import worldgenerator.map.formations.River.RiverList;

public class Terrain {
	public int fails;
	final public RiverList rivers = new RiverList();
	final public ArrayList<MapBorder> edges = new ArrayList<MapBorder>();
    final public ArrayList<MapVertex> vertices = new ArrayList<MapVertex>();
    final public ArrayList<MapSite> sites = new ArrayList<MapSite>();
    final public ArrayList<TectonicPlate> plates = new ArrayList<TectonicPlate>();
    final public Rectangle bounds;
    final private Random r;
    protected Color RIVER;

    public Terrain(Voronoi v, int numLloydRelaxations, Random r, double seaLevel, 
    		double oceanicRate, int numberOfPlates, double temperatureMod, double moistureMod) {
    	fails = 0;
        this.r = r;
        bounds = v.getPlotBounds();
        generateTerrain(r, v, seaLevel, oceanicRate, numberOfPlates, temperatureMod, moistureMod);
    }
    
    private void generateTerrain(Random rand, Voronoi v, double seaLevel, double oceanicRate, int numberOfPlates, double temperatureMod, double moistureMod) {
    	buildGraph(v);
    	improveCorners();
        generateTerrain(oceanicRate, numberOfPlates, temperatureMod, moistureMod, seaLevel, rand); //Testing, need minor plates
        System.out.println("System had "+fails+" fails");
    }
    
    //-------------------
    //TERRAIN & ELEVATION
    //-------------------
    
    private void generateTerrain(double oceanicRate, int plateCount, double temperatureMod, double moistureMod, 
    		double seaLevel, Random rand) {
    	generateElevation(oceanicRate, plateCount, 9, 0.6, rand); //TODO: Externalise
    	//assignOceanCoastAndLand(.3, seaLevel); //default.3
    	//Volcanism
        //Mineral Wealth
        //Wind
        //Assign moisture by noise
        calculateDownslopes();
    	//generateWeather(6, 0.3, true); //TODO: Externalise
        assignPolygonMoisture();
        assignCornerMoisture();
        //Biomes
        //Natural Wealth
    	generateBiomes(); //?
    }
    
    private void generateElevation(double oceanicRate, int plateCount, int octaves, double weighting, Random rand) {
    	//TODO: Add random mountain ranges based on ancient tectonic plates, based on world age
    	
    	generateTectonicPlates(oceanicRate, plateCount, rand);
    	generatePlateBoundries();
    	ArrayList<MapVertex> borderVertices = plateBorderVertices();
    	int[] innerBorderIndices = calculatePlateStress(borderVertices);
    	blurPlateBoundryStress(borderVertices, 3, 0.4);
    	ArrayList<ElevationContainer> borderElevations = generateBorderElevationQueue(borderVertices, innerBorderIndices);
    	processBorderElevationQueue(borderElevations);
    	//calculateNonBorderVertexElevations();
        if(weighting != 0) assignVertexNoiseElevation(octaves, weighting, rand);
    	calculateSiteElevationAverage();
        //reduceElevationAtEdge();
    }
    
    private ArrayList<TectonicPlate> generateTectonicPlates(double oceanicRate, int plateCount, Random rand) {
    	//TODO: Make map wrap E-W
    	Random plateRandom = new Random(rand.nextLong());
    	ArrayList <MapSite> unplatedSites = new ArrayList<MapSite>();
    	ArrayList <TectonicPlate> unplatedSitePlates = new ArrayList<TectonicPlate>();
    	
    	int failedCount = 0, totalFails = 0;
    	
    	while(plates.size() < plateCount && failedCount < 10000) {
    		MapVertex v = vertices.get(rand.nextInt(vertices.size()));
    		boolean adjacentToPlate = false;
    		
    		for(int i = 0; i < v.adjacentSites.size(); i++) {
    			if(v.adjacentSites.get(i).isAssignedToPlate()) {
    				failedCount++;
    				adjacentToPlate = true;
    				break;
    			}
    		}
    		if(adjacentToPlate) continue;
    		failedCount = 0;
        	boolean isOceanicPlate = plateRandom.nextInt(9)+1 <= oceanicRate*10;
        	TectonicPlate plate = new TectonicPlate(
        			new Color(r.nextInt(0xFFFFFF)), 
        			MathUtils.randomUnitVector(r), 
        			plateRandom.nextBoolean() ?  Math.random()*(Math.PI/30) : Math.random()*(-Math.PI/30), 
    				plateRandom.nextBoolean() ?  Math.random()*(Math.PI/30) : Math.random()*(-Math.PI/30), 
        			isOceanicPlate ? -(rand.nextInt(7)+1)/10 : (rand.nextInt(7)+1)/10, 
        			isOceanicPlate, 
        			v);
        	plates.add(plate);
        	
        	for(int i = 0; i < v.adjacentSites.size(); i++) {
        		if(!v.adjacentSites.get(i).isAssignedToPlate()) {
        			v.adjacentSites.get(i).plate = plate;
        			plate.sites.add(v.adjacentSites.get(i));
        		}
        	}
        	
        	for(int i = 0; i < v.adjacentSites.size(); i++) {
        		MapSite site = v.adjacentSites.get(i);
        		for(int j = 0; j < site.adjacentSites.size(); j++) {
        			MapSite adjacentSite = site.adjacentSites.get(j);
        			if(!adjacentSite.isAssignedToPlate()) {
        				unplatedSites.add(adjacentSite);
        				unplatedSitePlates.add(plate);
        			}
        		}
        	}
    	}
    	while(unplatedSites.size() > 0) {
    		int siteIndex = unplatedSites.size()+1;
    		while(siteIndex >= unplatedSites.size()) {
    			siteIndex *= rand.nextDouble();
    		}
    		MapSite site = unplatedSites.get(siteIndex);
    		TectonicPlate plate = unplatedSitePlates.get(siteIndex);
    		unplatedSites.remove(siteIndex);
    		unplatedSitePlates.remove(siteIndex);
    		
    		if(!site.isAssignedToPlate() && (!plate.onEdge || (rand.nextBoolean()))) {
    			site.plate = plate;
    			plate.sites.add(site);
    			for (int i = 0; i < site.adjacentVertices.size(); i++) {
    				if(site.adjacentVertices.get(i).isOnMapEdge) {
        				plate.onEdge = true;
        			}
				}
    			
    			for(int i = 0; i < site.adjacentSites.size(); i++) {
    				if(!site.adjacentSites.get(i).isAssignedToPlate()) {
    					unplatedSites.add(site.adjacentSites.get(i));
    					unplatedSitePlates.add(plate);
    				}
    			}
    		}
    	}
    	
    	int oceanicPlates = 0, landPlates = 0;
    	for (TectonicPlate tectonicPlate : plates) {
			if(tectonicPlate.oceanic) {
				oceanicPlates++;
			} else {
				landPlates++;
			}
		}
    	System.out.println("Land Plates: "+landPlates+"\nOceanic Plates: "+oceanicPlates);
    	
    	boolean allPlated = false;
    	int platingFails = 0;
    	while(platingFails < sites.size() && !allPlated) {
    		allPlated = true;
    		for (MapSite site : sites) {
				for (MapSite neighbor : site.adjacentSites) {
					if(neighbor.plate != null && site.plate == null) {
						site.plate = neighbor.plate;
					} if(neighbor.plate == null && site.plate != null) {
						neighbor.plate = site.plate;
					} if(neighbor.plate == null || site.plate == null) {
						allPlated = false;
					}
				}
    		}
    		if(!allPlated) platingFails++;
    	}
    	fails += platingFails + totalFails;
    	calculateSitePlateOriginDistance(plates);
    	return plates;
    }
    
    private void calculateSitePlateOriginDistance(ArrayList<TectonicPlate> plates) {
    	ArrayList<VDOContainer> distanceQueue = new ArrayList<VDOContainer>();
    	for(int i = 0; i < plates.size(); i++) {
    		MapVertex vertex = plates.get(i).plateOrigin;
    		vertex.distanceToPlateOrigin = 0;
    		for (int j = 0; j < vertex.adjacentBorders.size(); j++) {
				distanceQueue.add(new VDOContainer(
					vertex.adjacentBorders.get(j).length(), 
					vertex.adjacentBorders.get(j).getVertexOppositeTo(vertex)
				));
			}
    	}
    	if(distanceQueue.size() == 0) return;
    	
    	double lastIndex = distanceQueue.size();
    	for (int i = 0; i < lastIndex; i++) {
    		double distanceToPlateOrigin = distanceQueue.get(i).distanceToPlateOrigin;
			MapVertex vertex = distanceQueue.get(i).vertex;
			if(vertex.distanceToPlateOrigin == 0 || vertex.distanceToPlateOrigin > distanceToPlateOrigin) {
				vertex.distanceToPlateOrigin = distanceToPlateOrigin;
				for (int j = 0; j < vertex.adjacentBorders.size(); j++) {
					distanceQueue.add(new VDOContainer(
						vertex.adjacentBorders.get(j).length(),
						vertex.adjacentBorders.get(j).getVertexOppositeTo(vertex)
					));
				}
			}
		}
    	for (int i = (int) lastIndex; i > 0; i--) {
    		distanceQueue.remove(i);
		}
    	distanceQueue.sort(new Comparator<VDOContainer>() {
			@Override
			public int compare(VDOContainer o1, VDOContainer o2) {
				if(o1.distanceToPlateOrigin > o2.distanceToPlateOrigin) {
					return 1;
				} else if (o1.distanceToPlateOrigin < o2.distanceToPlateOrigin) {
					return -1;
				} else {
					return 0;
				}
			}
		});
    }
    
    private void generatePlateBoundries() {
		for (int i = 0; i <	edges.size(); i++) {
			MapBorder border = edges.get(i);
			if(border.site1.plate != border.site2.plate) {
				border.isPlateBoundry = true;
				
				if(border.vertex1 != null) border.vertex1.isOnPlateBorder = true;
				if(border.vertex2 != null) border.vertex2.isOnPlateBorder = true;
				
				if(border.site1 != null&& border.site1.plate != null) border.site1.plate.plateBorders.add(border);
				if(border.site2 != null && border.site2.plate != null) border.site2.plate.plateBorders.add(border);
			}
		}
	}
    
    private ArrayList<MapVertex> plateBorderVertices() {
		ArrayList<MapVertex> borderVertices = new ArrayList<MapVertex>();for (int i = 0; i < vertices.size(); i++) {
			MapVertex vertex = vertices.get(i);
			if(vertex.isOnPlateBorder) {
				borderVertices.add(vertex);
				if(vertex.adjacentSites.get(0).plate != null) {
					vertex.adjacentSites.get(0).plate.plateCorners.add(vertex);
				}
				TectonicPlate[] platesAroundVertex = new TectonicPlate[vertex.adjacentSites.size()];
				platesAroundVertex[0] = vertex.adjacentSites.get(0).plate;
				
				for (int j = 0; j < platesAroundVertex.length; j++) {
					for (int k = 0; k < platesAroundVertex.length; k++) {
						if(vertex.adjacentSites.get(j).plate != vertex.adjacentSites.get(k).plate) {
							if(vertex.adjacentSites.get(j).plate != null) {
								vertex.adjacentSites.get(j).plate.plateCorners.add(vertex);
							}
						}
					}
				}
			}
		}
		return borderVertices;
    }
	
	private int[] calculatePlateStress(ArrayList<MapVertex> borderVertices) {
		int[] innerBorderIndices = new int[borderVertices.size()];
		for (int i = 0; i < borderVertices.size(); i++) {
			int innerBorderIndex = 0;
			MapBorder innerBorder = null;
			MapVertex vertex = borderVertices.get(i);
			vertex.distanceToPlateBorder = 0;
			
			for (int j = 0; j < vertex.adjacentBorders.size(); j++) {
				MapBorder border = vertex.adjacentBorders.get(j);
				if(!border.isPlateBoundry) {
					innerBorder = border;
					innerBorderIndex = j;
					break;
				}
			}
			
			if(innerBorder != null) {
				innerBorderIndices[i] = innerBorderIndex;
				
				MapBorder 
					plateBorder1 = vertex.adjacentBorders.get((innerBorderIndex+1) % vertex.adjacentBorders.size()),
					plateBorder2 = vertex.adjacentBorders.get((innerBorderIndex+2) % vertex.adjacentBorders.size());
				MapVertex
					oppVertex1 = plateBorder1.getVertexOppositeTo(vertex),
					oppVertex2 = plateBorder2.getVertexOppositeTo(vertex);
				TectonicPlate
					plate1 = innerBorder.site1.plate,
					plate2 = plateBorder1.site1.plate != plate1 ? plateBorder1.site1.plate : plateBorder1.site2.plate;
				Vector2D
					borderVector = oppVertex1.vectorTo(oppVertex2),
					borderNormal = borderVector.crossProduct();
				if(plate1 != null && plate2 != null) {
					vertex.tectonicPressure = calculateStress(0, plate1.calculateMovement(vertex.location), plate2.calculateMovement(vertex.location), borderVector, borderNormal);
					vertex.tectonicShear = calculateStress(1, plate1.calculateMovement(vertex.location), plate2.calculateMovement(vertex.location), borderVector, borderNormal);
				}
				
			} else {
				innerBorderIndices[i] = -1; //Might be an issue
				double pressureSum = 0, shearSum = 0;
				for (int j = 0; j < vertex.adjacentSites.size()-1; j++) {
					TectonicPlate plate = vertex.adjacentSites.get(j).plate;
					Vector2D 
						borderVector = vertex.adjacentVertices.get(j).vectorTo(vertex),
						borderNormal = borderVector.crossProduct();
					if(plate != null) {
						pressureSum += calculateStress(0, plate.calculateMovement(vertex.location), plate.calculateMovement(vertex.location), borderVector, borderNormal);
						shearSum += calculateStress(1, plate.calculateMovement(vertex.location), plate.calculateMovement(vertex.location), borderVector, borderNormal);
					}
				}
				vertex.tectonicPressure = pressureSum;
				vertex.tectonicShear = shearSum;
			}
		}
		return innerBorderIndices;
	}
	
	private double calculateStress(int i, Vector2D plateMovement1, Vector2D plateMovement2, Vector2D borderVector, Vector2D borderNormal) {
		double stress = 0;
		Vector2D netMovement = plateMovement1.subFrom(plateMovement2);
		if(i == 0) {
			//PRESSURE
			Vector2D pressureVector = Vector2D.projectVectors(netMovement, borderNormal);
			stress = pressureVector.dot(borderNormal) > 0 ? -(pressureVector.length()) : pressureVector.length();
		} else if(i == 1) {
			//SHEAR
			stress = netMovement.projectOnVector(borderVector).length();
		}
		
		return 2 / (1 + Math.exp(-stress / 30)) - 1;
	}
	
	private void blurPlateBoundryStress(ArrayList<MapVertex> borderVertices, int iterations, double weighting) {
		double[] newVertexPressure = new double[borderVertices.size()];
		double[] newVertexShear = new double[borderVertices.size()];
		
		for (int i = 0; i < iterations; i++) {
			for (int j = 0; j < borderVertices.size(); j++) {
				MapVertex vertex = borderVertices.get(j);
				double 
					avgPressure = 0, 
					avgShear = 0;
				int numNeighbors = 0;
				for (int k = 0; k < vertex.adjacentVertices.size(); k++) {
					MapVertex neighbor = vertex.adjacentVertices.get(k);
					if(neighbor.isOnPlateBorder) {
						avgPressure += neighbor.tectonicPressure;
						avgShear += neighbor.tectonicShear;
						numNeighbors++;
					}
				}
				newVertexPressure[j] = vertex.tectonicPressure * weighting + (avgPressure/numNeighbors) * (1 - weighting);
				newVertexShear[j] = vertex.tectonicShear * weighting + (avgShear/numNeighbors) * (1 - weighting);
			}
			for (int j = 0; j < borderVertices.size(); j++) {
				MapVertex vertex = borderVertices.get(j);
				if(vertex.isOnPlateBorder) {
					vertex.tectonicPressure = newVertexPressure[j];
					vertex.tectonicShear = newVertexShear[j];
				}
			}
		}
	}
	
	private ArrayList<ElevationContainer> generateBorderElevationQueue(ArrayList<MapVertex> borderVertices, int[] innerBorderIndices) {
		ArrayList<ElevationContainer> borderElevations = new ArrayList<ElevationContainer>();
		
		for (int i = 0; i < borderVertices.size(); i++) {
			MapVertex vertex = borderVertices.get(i);
			int innerBorderIndex = innerBorderIndices[i];
			
			if(innerBorderIndex != -1) {
				MapBorder 
					innerBorder = vertex.adjacentBorders.get(innerBorderIndex),
					plateBorder = vertex.adjacentBorders.get((innerBorderIndex + 1) % vertex.adjacentVertices.size());
				TectonicPlate
					plate1 = innerBorder.site1.plate,
					plate2 = plateBorder.site1.plate != plate1 ? plateBorder.site1.plate : plateBorder.site2.plate;
				int calculationType = 0;
				double
					plate1Elevation = plate1 != null ? plate1.baseElevation : -0.01,
					plate2Elevation = plate2 != null ? plate2.baseElevation : -0.01;
				vertex.elevation = plate1.baseElevation;
				
				if(vertex.tectonicPressure > 0.3) { //These were just =
					if(plate1 != null && plate2 != null && plate1.oceanic == plate2.oceanic) {
						calculationType = 1; //Colliding 1
						vertex.elevation += (plate1.oceanic ? -1 : 1) * (Math.max(plate1Elevation, plate2Elevation) + vertex.tectonicPressure);
					}else if(plate1 != null && plate2 != null && plate1.oceanic && !plate2.oceanic) {
						calculationType = 3; //Superducting 2
						vertex.elevation += Math.abs(Math.max(plate1Elevation, plate2Elevation) + vertex.tectonicPressure);
					}else if(plate1 != null && plate2 != null && !plate1.oceanic && plate2.oceanic){
						calculationType = 2; //Subducting 3
						vertex.elevation += -Math.abs(Math.max(plate1Elevation, plate2Elevation) + vertex.tectonicPressure);
					}
				} else if(vertex.tectonicPressure < -0.3) {
					calculationType = 4; // Shearing 3
					vertex.elevation = (plate1Elevation + plate2Elevation) / 2;
				} else if(vertex.tectonicShear > 0.3 && plate1 != null && plate2 != null) {
					calculationType = 5; // Diverging 5 
					vertex.elevation += Math.min(
							-Math.abs((Math.max(plate1Elevation, plate2Elevation) + vertex.tectonicShear) / 8), 
							plate1.oceanic ? -Math.abs((Math.max(plate1Elevation, plate2Elevation) + vertex.tectonicShear) / 5) : plate1Elevation-Math.abs(plate2Elevation));
				} else {
					calculationType = 0; //Dormant 0
					vertex.elevation = (plate1Elevation + plate2Elevation) / 2;
				}
				MapVertex nextVertex = innerBorder.getVertexOppositeTo(vertex);
				vertex.calculationType = nextVertex.calculationType = calculationType;
				vertex.origin = nextVertex.origin = vertex;
				
				if(!nextVertex.isOnPlateBorder) {
					ElevationContainer container = new ElevationContainer();
					container.origin.originVertex = vertex;
					container.origin.plate = plate1;
					container.origin.calculationType = calculationType;
					container.origin.pressure = vertex.tectonicPressure;
					container.origin.shear = vertex.tectonicShear;
					container.vertex = vertex;
					container.nextVertex = nextVertex;
					container.border = innerBorder;
					container.distanceToPlateBorder = innerBorder.length();
					borderElevations.add(container);
				}
			} else {
				double maxElevation = Double.NEGATIVE_INFINITY;
				for(MapSite v : vertex.adjacentSites) {
					if(v.plate != null && v.plate.baseElevation > maxElevation) {
						maxElevation = v.plate.baseElevation;
					}
				}
				if(maxElevation == Double.NEGATIVE_INFINITY) maxElevation = 0;
				
				if (vertex.tectonicPressure > 0.3) { //These were just =
					vertex.elevation += maxElevation + vertex.tectonicPressure;
				} else if (vertex.tectonicPressure < -0.3) {
					vertex.elevation += maxElevation + vertex.tectonicPressure / 4;
				} else if (vertex.tectonicShear > 0.3) {
					vertex.elevation += maxElevation + vertex.tectonicShear / 8;
				} else {
					vertex.elevation += maxElevation / 3;
				}
			}
		}
		boolean done = false;
		while(!done) {
			done = true;
			for (MapVertex vertex : vertices) {
				if(vertex.calculationType == -1) {
					done = false;
					for (MapVertex v : vertex.adjacentVertices) {
						if(v.calculationType != -1 && vertex.calculationType == -1) {
							vertex.calculationType = v.calculationType;
						}
					}
				}
			}
		}
		
		for (MapVertex vertex : vertices) {
			fails += vertex.elevationFailCount;
			vertex.elevationFailCount = 0;
		}
		return borderElevations;
	}

	private void processBorderElevationQueue(ArrayList<ElevationContainer> borderElevations) {
		if(borderElevations.size() == 0) return;
		int lastIndex = borderElevations.size();
		for (int i = 0; i < lastIndex; i++) {
			ElevationContainer ec = borderElevations.get(i);
			MapVertex vertex = ec.nextVertex;
			
			if(!vertex.elevationCalculated) {
				double 
					elevation = ec.origin.plate.baseElevation,
					volcanism = 0,
					tectonic_activity = 0;
				vertex.distanceToPlateBorder = ec.distanceToPlateBorder;
				int type = ec.origin.calculationType; 
				if(type == 1) {
					elevation = calculateCollidingElevation(
							vertex.distanceToPlateBorder, 
							vertex.distanceToPlateOrigin, 
							ec.origin.originVertex.elevation, 
							ec.origin.plate.baseElevation, 
							ec.origin.pressure, 
							ec.origin.shear
						);
					tectonic_activity = ((vertex.tectonicShear*0.45)+(vertex.tectonicPressure*0.05))/vertex.distanceToPlateBorder;
					volcanism = ec.origin.plate.baseElevation > 0 ? 0 : (elevation/ec.origin.originVertex.elevation)*(vertex.distanceToPlateBorder/vertex.distanceToPlateOrigin);
				} else if(type == 2) {
					elevation = calculateSubductingElevation(
							vertex.distanceToPlateBorder, 
							vertex.distanceToPlateOrigin, 
							ec.origin.originVertex.elevation, 
							ec.origin.plate.baseElevation, 
							ec.origin.pressure, 
							ec.origin.shear
						);
					tectonic_activity = ((vertex.tectonicShear*0.45)+(vertex.tectonicPressure*0.05))/vertex.distanceToPlateBorder;
				} else if(type == 3) {
					elevation = calculateSuperductingElevation(
							vertex.distanceToPlateBorder, 
							vertex.distanceToPlateOrigin, 
							ec.origin.originVertex.elevation, 
							ec.origin.plate.baseElevation, 
							ec.origin.pressure, 
							ec.origin.shear
						);
					volcanism += elevation/ec.origin.originVertex.elevation;
					tectonic_activity = ((vertex.tectonicShear*0.45)+(vertex.tectonicPressure*0.05))/vertex.distanceToPlateBorder;
				} else if(type == 4) {
					elevation += calculateDormantElevation(
							vertex.distanceToPlateBorder, 
							vertex.distanceToPlateOrigin, 
							ec.origin.originVertex.elevation, 
							ec.origin.plate.baseElevation, 
							ec.origin.pressure, 
							ec.origin.shear
						) * ec.origin.plate.baseElevation > 0 ? 1 : -1;
					tectonic_activity = calculateShearingEarthquakes( //Elevation was this
							vertex.distanceToPlateBorder, 
							vertex.distanceToPlateOrigin, 
							ec.origin.originVertex.elevation, 
							ec.origin.plate.baseElevation, 
							ec.origin.pressure, 
							ec.origin.shear
						);
				} else if(type == 5) {
					elevation += -Math.abs(calculateDivergingElevation(
							vertex.distanceToPlateBorder, 
							vertex.distanceToPlateOrigin, 
							ec.origin.originVertex.elevation, 
							ec.origin.plate.baseElevation, 
							ec.origin.pressure, 
							ec.origin.shear
						));
					volcanism = ec.origin.originVertex.elevation/ec.origin.plate.baseElevation;
				} else {
					elevation += calculateDormantElevation(
							vertex.distanceToPlateBorder, 
							vertex.distanceToPlateOrigin, 
							ec.origin.originVertex.elevation, 
							ec.origin.plate.baseElevation, 
							ec.origin.pressure, 
							ec.origin.shear
						) * ec.origin.plate.baseElevation > 0 ? 1 : -1;
				}
				
				vertex.elevation += elevation;
				if(vertex.elevation != 0 || vertex.elevationFailCount >= sites.size()*2) {
					vertex.elevationCalculated = true;
				}else {
					vertex.elevationFailCount++;
					fails++;
				}
			}
			
			for (int j = 0; j < vertex.adjacentBorders.size(); j++) {
				MapBorder border = vertex.adjacentBorders.get(j);
				if(!border.isPlateBoundry) {
					MapVertex nextVertex = null;
					for (MapVertex v : vertex.adjacentVertices) {
						if(border.getVertexOppositeTo(vertex) == v) {
							nextVertex = v;
						}
					}
					
					double distanceToPlateBorder = vertex.distanceToPlateBorder + border.length();
					if(nextVertex != null && nextVertex.distanceToPlateBorder > distanceToPlateBorder) {
						ElevationContainer nextEC = new ElevationContainer();
						nextEC.origin = ec.origin;
						nextEC.vertex = vertex;
						nextEC.nextVertex = nextVertex;
						nextEC.border = border;
						nextEC.distanceToPlateBorder = distanceToPlateBorder;
						borderElevations.add(nextEC);
						lastIndex++;
					}
				}
			}
		}
		for (int i = lastIndex-1; i > 0; i--) {
			borderElevations.remove(i);
		}
		borderElevations.sort(new Comparator<Terrain.ElevationContainer>() {
			@Override
			public int compare(ElevationContainer o1, ElevationContainer o2) {
				if(o1.vertex.distanceToPlateBorder > o2.vertex.distanceToPlateBorder) {
					return 1;
				}else if(o1.vertex.distanceToPlateBorder < o2.vertex.distanceToPlateBorder) {
					return -1;
				}else {
					return 0;
				}
			}
		});
	}
	
	private double calculateCollidingElevation(double distanceToPlateBorder, double distanceToPlateOrigin, double borderElevation, double plateElevation, double pressure, double shear) {
		double t = distanceToPlateBorder / (distanceToPlateBorder + distanceToPlateOrigin);
		if (t < 0.5) {
			t *= 2;
			return (plateElevation + Math.pow(t - 1, 2) * (borderElevation - plateElevation)) / (borderElevation > 0 ? 1 : 4);
		} else {
			return plateElevation;
		}
	}
	
	private double calculateSuperductingElevation(double distanceToPlateBorder, double distanceToPlateOrigin, double borderElevation, double plateElevation, double pressure, double shear) {
		double t = distanceToPlateBorder / (distanceToPlateBorder + distanceToPlateOrigin);
		if (t < 0.2) {
			t = t / 0.2;
			return (borderElevation + t * (plateElevation - borderElevation) + pressure) / 2;
		} else if (t < 0.5) {
			t = (t - 0.2) / 0.3;
			return (plateElevation + Math.pow(t - 1, 2) * pressure) / 2;
		} else {
			return plateElevation;
		}
	}
	
	private double calculateSubductingElevation(double distanceToPlateBorder, double distanceToPlateOrigin, double borderElevation, double plateElevation, double pressure, double shear) {
		double t = distanceToPlateBorder / (distanceToPlateBorder + distanceToPlateOrigin);
		return plateElevation + Math.pow(t - 1, 2) * (borderElevation - plateElevation);
	}
	
	private double calculateDivergingElevation(double distanceToPlateBorder, double distanceToPlateOrigin, double borderElevation, double plateElevation, double pressure, double shear) {
		double t = distanceToPlateBorder / (distanceToPlateBorder + distanceToPlateOrigin);
		if (t < 0.3) {
			t /= 0.3;
			return plateElevation + Math.pow(t - 1, 2) * (borderElevation - plateElevation);
		} else {
			return plateElevation;
		}
	}
	
	private double calculateShearingEarthquakes(double distanceToPlateBorder, double distanceToPlateOrigin, double borderElevation, double plateElevation, double pressure, double shear) {
		double t = distanceToPlateBorder / (distanceToPlateBorder + distanceToPlateOrigin);
		if (t < 0.2) {
			t /= 0.2;
			return plateElevation + (Math.pow(t - 1, 2) * (borderElevation - plateElevation));
		}
		else {
			return plateElevation;
		}
	}
	
	private double calculateDormantElevation(double distanceToPlateBorder, double distanceToPlateOrigin, double borderElevation, double plateElevation, double pressure, double shear) {
		double t = distanceToPlateBorder / (distanceToPlateBorder + distanceToPlateOrigin);
		double 
			elevationDifference = borderElevation - plateElevation,
			elevationFactor = borderElevation/plateElevation;
		return (borderElevation+plateElevation)/2;
	}
	
	private void calculateSiteElevationAverage() {
		
		for (MapSite site : sites) {
			double elevation = 0;
			for (int j = 0; j < site.adjacentVertices.size(); ++j) {
				elevation += site.adjacentVertices.get(j).elevation;
			}
			site.elevation = site.plate.baseElevation + (elevation / site.adjacentVertices.size());
		}
	}
    
    private void assignVertexNoiseElevation(int octaves, double weighting, Random r) { //See light and darkness russian world gen to see noise to voronoi
    	NoiseGenerator noisegen = new NoiseGenerator(r, octaves, (int)(bounds.width*0.61), (int)(bounds.height*0.61));
    	double[][] noise = noisegen.getPerlinNoise();
    	boolean done = false;
    	for (MapVertex vertex : vertices) {
    		vertex.elevationCalculated = false;
    	}
    	while(!done) {
    		done = true;
    		for (MapVertex vertex : vertices) {
    			if(!vertex.elevationCalculated) {
        			double elevation = vertex.elevation;
    				
    				if(elevation == 0.0) {
        				done = false;
        				for(MapVertex v: vertex.adjacentVertices) {
            				elevation += v.elevation;
            			}
        				elevation /= vertex.adjacentVertices.size();
        				if(elevation == 0.0) {
        					fails++;
        				}
        			}
        				
    				boolean terrestrial = false;
    				for (MapSite site : vertex.adjacentSites) {
    					if (!site.plate.oceanic) {
    						terrestrial = true;
    						break;
    					}
					}
    	    		vertex.elevation += terrestrial ? 
    	    				(noise
    	    					[(int)(vertex.location.x*0.60)]
    							[(int)(vertex.location.y*0.60)]-0.2) * weighting:
    						(noise
            					[(int)(vertex.location.x*0.60)]
                    			[(int)(vertex.location.y*0.60)]-0.8) * weighting;
    	    		vertex.elevationCalculated = true;
        			vertex.elevationFailCount++;
    			}
    			if(vertex.elevationFailCount >= sites.size()*5) {
    				vertex.elevationCalculated = true;
    				fails++;
    			}
    		}
    	}//TODO: When making noise optional, externalize this
    }

    private void reduceElevationAtEdge() { 
    	//TODO: Turn this method into a way to make the edges of the map water, as not to cut landmasses
        LinkedList<MapVertex> queue = new LinkedList<MapVertex>();
        for (MapVertex c : vertices) {
            if (c.isOnMapEdge) {
                c.elevation = 0;
                queue.add(c);
            }
        }

        while (!queue.isEmpty()) {
            MapVertex c = queue.pop();
            for (MapVertex a : c.adjacentVertices) {
            	a.elevation = a.elevation * (r.nextInt(10)/100);
                queue.add(a);
            }
        }
    }
	
	private void generateWeather(int riverFactor, double moistureRequirement, boolean moistureLocked, 
			double temperatureMod, double moistureMod, double seaLevel, double waterThreshold) {
		//createRivers(riverFactor, moistureRequirement, moistureLocked);
		assignTemperature(bounds, temperatureMod);
		//assignOceanCoastAndLand(seaLevel, waterThreshold);
	}
	
	private void assignTemperature(Rectangle bounds, double temperatureMod) {
		double equatorLocation = bounds.height/2; //c
		for (MapSite site : sites) {
			site.temperature = 40 - (40*(MathUtils.distance(site.location, new Point(site.location.x, equatorLocation))/equatorLocation));
		}
	}
	
	private void generateBiomes() {
		
	}
	
	private void assignOceanCoastAndLand(double seaLevel, double waterThreshold) {
		for (final MapSite center : sites) {
            int numWater = 0;
            for (final MapVertex c : center.adjacentVertices) {
            	c.ocean = c.elevation <= seaLevel;
                if (c.isOnMapEdge) {
                    center.border = center.water = center.ocean = true;
                } if (c.water) {
                    numWater++;
                }
            }
            center.water = center.ocean || ((double) numWater / center.adjacentVertices.size() >= waterThreshold);
        }
		for (MapSite center : sites) {
            boolean oceanNeighbor = false;
            boolean landNeighbor = false;
            for (MapSite n : center.adjacentSites) {
                oceanNeighbor |= n.ocean;
                landNeighbor |= !n.water;
            }
            center.coast = oceanNeighbor && landNeighbor;
        }
	}

    public ArrayList<MapVertex> landCorners() {
        final ArrayList<MapVertex> list = new ArrayList<MapVertex>();
        for (MapVertex c : vertices) {
            if (!c.ocean && !c.coast) {
                list.add(c);
            }
        }
        return list;
    }

    private void calculateDownslopes() {
        for (MapVertex c : vertices) {
            MapVertex down = c;
            //System.out.println("ME: " + c.elevation);
            for (MapVertex a : c.adjacentVertices) {
                //System.out.println(a.elevation);
                if (a.elevation <= down.elevation) {
                    down = a;
                }
            }
            c.downslope = down;
        }
    }

    private void createRivers(int riverFactor, double moistureRequirement, boolean moistureLocked) {
        for (int i = 0; i < (bounds.width)/(riverFactor); i++) { // was for (int i = 0; i < bounds.width / 2; i++)
            MapVertex c = vertices.get(r.nextInt(vertices.size()));
            if(moistureLocked) {
            	int attempts = 0;
            	while((c.moisture >= moistureRequirement || c == null)&&attempts<1000) {
            		c = vertices.get(r.nextInt(vertices.size()));
            		attempts++;
            	}
            }
            if (c.ocean || c.elevation <= 0 || c.elevation > 0.6) {
                continue;
            }
            River river = new River();
            boolean flatland = false;
            
            while (!c.coast && !flatland) {
                if (c == c.downslope) {
                    break;
                }
                if(c.elevation <= c.downslope.elevation + (c.elevation*0.1)) {
                	flatland = true; //TODO: Wetland
                }
                MapBorder edge = lookupEdgeFromCorner(c, c.downslope);
                if (edge != null && (!edge.vertex1.water || !edge.vertex2.water)) {
                    edge.river++;
                    c.river++;
                    c.downslope.river++;  // TODO: fix double count
                    if(!c.isPartOfRiver)river.addVertex(c);
                }
                c = c.downslope;
            } if(!rivers.contains(river)) {
            	rivers.addRiver(river);
            }
        }
    }

    private MapBorder lookupEdgeFromCorner(MapVertex c, MapVertex downslope) {
        for (MapBorder e : c.adjacentBorders) {
            if (e.vertex1 == downslope || e.vertex2 == downslope) {
                return e;
            }
        }
        return null;
    }

    private void assignCornerMoisture() {
       
        for (MapVertex c : vertices) {
            double total = 0;
            for (MapSite s : c.adjacentSites) {
				total += s.moisture;
			}
            c.moisture = total/c.adjacentSites.size();
        }
    }

    private void assignPolygonMoisture() {
    	NoiseGenerator generator = new NoiseGenerator(new Random(r.nextLong()), 7, (int)(bounds.width*0.666), (int)(bounds.height*0.666));
    	double[][] noise  = generator.getPerlinNoise();
        for (MapSite center : sites) {
            center.moisture = noise[(int) (center.location.x*0.66)][(int) (center.location.y*0.66)];
        }
    }

    private void improveCorners() {
        Point[] newP = new Point[vertices.size()];
        for (MapVertex c : vertices) {
            if (c.isOnMapEdge) {
                newP[c.index] = c.location;
            } else {
                double x = 0;
                double y = 0;
                for (MapSite center : c.adjacentSites) {
                    x += center.location.x;
                    y += center.location.y;
                }
                newP[c.index] = new Point(x / c.adjacentSites.size(), y / c.adjacentSites.size());
            }
        }
        vertices.stream().forEach((c) -> {
            c.location = newP[c.index];
        });
        edges.stream().filter((e) -> (e.vertex1 != null && e.vertex2 != null)).forEach((e) -> {
            e.setVornoi(e.vertex1, e.vertex2);
        });
    }
    
    private void buildGraph(Voronoi v) {
        final HashMap<Point, MapSite> pointCenterMap = new HashMap<Point, MapSite>();
        final ArrayList<Point> points = v.siteCoords();
        points.stream().forEach((p) -> {
            MapSite c = new MapSite();
            c.location = p;
            c.index = sites.size();
            sites.add(c);
            pointCenterMap.put(p, c);
        });

        //bug fix
        sites.stream().forEach((c) -> {
            v.region(c.location);
        });

        final ArrayList<Edge> libedges = v.edges();
        final HashMap<Integer, MapVertex> pointCornerMap = new HashMap<Integer, MapVertex>();

        for (Edge libedge : libedges) {
            final LineSegment vEdge = libedge.voronoiEdge();
            final LineSegment dEdge = libedge.delaunayLine();

            final MapBorder edge = new MapBorder();
            edge.index = edges.size();
            edges.add(edge);

            edge.vertex1 = makeCorner(pointCornerMap, vEdge.p0);
            edge.vertex2 = makeCorner(pointCornerMap, vEdge.p1);
            edge.site1 = pointCenterMap.get(dEdge.p0);
            edge.site2 = pointCenterMap.get(dEdge.p1);

            // Centers point to edges. Corners point to edges.
            if (edge.site1 != null) {
                edge.site1.adjacentBorders.add(edge);
            }
            if (edge.site2 != null) {
                edge.site2.adjacentBorders.add(edge);
            }
            if (edge.vertex1 != null) {
                edge.vertex1.adjacentBorders.add(edge);
            }
            if (edge.vertex2 != null) {
                edge.vertex2.adjacentBorders.add(edge);
            }

            // Centers point to centers.
            if (edge.site1 != null && edge.site2 != null) {
                addToCenterList(edge.site1.adjacentSites, edge.site2);
                addToCenterList(edge.site2.adjacentSites, edge.site1);
            }

            // Corners point to corners
            if (edge.vertex1 != null && edge.vertex2 != null) {
                addToCornerList(edge.vertex1.adjacentVertices, edge.vertex2);
                addToCornerList(edge.vertex2.adjacentVertices, edge.vertex1);
            }

            // Centers point to corners
            if (edge.site1 != null) {
                addToCornerList(edge.site1.adjacentVertices, edge.vertex1);
                addToCornerList(edge.site1.adjacentVertices, edge.vertex2);
            }
            if (edge.site2 != null) {
                addToCornerList(edge.site2.adjacentVertices, edge.vertex1);
                addToCornerList(edge.site2.adjacentVertices, edge.vertex2);
            }

            // Corners point to centers
            if (edge.vertex1 != null) {
                addToCenterList(edge.vertex1.adjacentSites, edge.site1);
                addToCenterList(edge.vertex1.adjacentSites, edge.site2);
            }
            if (edge.vertex2 != null) {
                addToCenterList(edge.vertex2.adjacentSites, edge.site1);
                addToCenterList(edge.vertex2.adjacentSites, edge.site2);
            }
        }
    }

    // Helper functions for the following for loop; ideally these
    // would be inlined
    private void addToCornerList(ArrayList<MapVertex> list, MapVertex c) {
        if (c != null && !list.contains(c)) {
            list.add(c);
        }
    }

    private void addToCenterList(ArrayList<MapSite> list, MapSite c) {
        if (c != null && !list.contains(c)) {
            list.add(c);
        }
    }

    //ensures that each corner is represented by only one corner object
    private MapVertex makeCorner(HashMap<Integer, MapVertex> pointCornerMap, Point p) {
        if (p == null) {
            return null;
        }
        int index = (int) ((int) p.x + (int) (p.y) * bounds.width * 2);
        MapVertex c = pointCornerMap.get(index);
        if (c == null) {
            c = new MapVertex();
            c.location = p;
            c.isOnMapEdge = bounds.liesOnAxes(p);
            c.index = vertices.size();
            vertices.add(c);
            pointCornerMap.put(index, c);
        }
        return c;
    }
    
    /* TODO: Biome getBiome(MapSite site) {
    	if (site.ocean) {
            return Biome.OCEAN;
        } else if (site.water) {
            if (site.elevation < 0.1) {
                return Biome.MARSH;
            }
            if (site.elevation > 0.8) {
                return Biome.ICE;
            }
            return Biome.LAKE;
        //} else if (p.coast) { Removed for now, TODO: make edges beaches instead
            //return ColorData.BEACH;
        } else if (site.elevation > 0.8) {
            if (site.moisture > 0.50) {
                return Biome.SNOW;
            } else if (site.moisture > 0.33) {
                return Biome.TUNDRA;
            } else if (site.moisture > 0.16) {
                return Biome.BARE;
            } else {
                return Biome.SCORCHED;
            }
        } else if (site.elevation > 0.6) {
            if (site.moisture > 0.66) {
                return Biome.TAIGA;
            } else if (site.moisture > 0.33) {
                return Biome.SHRUBLAND;
            } else {
                return Biome.TEMPERATE_DESERT;
            }
        } else if (site.elevation > 0.3) {
            if (site.moisture > 0.83) {
                return Biome.TEMPERATE_RAIN_FOREST;
            } else if (site.moisture > 0.50) {
                return Biome.TEMPERATE_DECIDUOUS_FOREST;
            } else if (site.moisture > 0.16) {
                return Biome.GRASSLAND;
            } else {
                return Biome.TEMPERATE_DESERT;
            }
        } else {
            if (site.moisture > 0.66) {
                return Biome.TROPICAL_RAIN_FOREST;
            } else if (site.moisture > 0.33) {
                return Biome.TROPICAL_SEASONAL_FOREST;
            } else if (site.moisture > 0.16) {
                return Biome.GRASSLAND;
            } else {
                return Biome.SUBTROPICAL_DESERT;
            }
        }

    }*/
    
    private class VDOContainer{
    	public VDOContainer(double distanceToPlateOrigin, MapVertex vertex) {
			this.vertex = vertex;
			this.distanceToPlateOrigin = distanceToPlateOrigin;
		}
		public MapVertex vertex;
    	public double distanceToPlateOrigin;
    }
    
    private class ElevationContainer {
    	public Origin origin = new Origin();
    	public MapVertex 
    		vertex, 
    		nextVertex;
    	public MapBorder border;
    	public double distanceToPlateBorder;
    	
    	protected class Origin{
    		public MapVertex originVertex;
    		public TectonicPlate plate;
    		public int calculationType;
    		public double pressure, shear;
    	}
    }
}
