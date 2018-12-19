package worldgenerator.io;

import java.awt.Desktop;
import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;
import org.w3c.dom.Element;

import worldgenerator.map.Map;

/**
 * A class that deals with XML files containing Map objects
 * @author Calum Bennie
 *
 */
public class MapFileIO {
	//TODO: FIX THIS CLASS
	/** */
	private MapFileIO(){throw new Error("MapFileIO cannot be instanciated");}
	/** The root directory of the application */
	private static File rootDirectory;
	
	/** The save directory of the application */
	private static File saveDirectory;
	public static File saveDir() {return saveDirectory;}
	
	/** The asset directory of the application */
	private static File assetDirectory;
	
	/** The application manual */
	private static File manualFile;
	
	/** The application error logs */
	private static File errorLog;
	
	/** Sets up the file system for the application */
	public static void createSystem(){
		rootDirectory = new File("terranova");
		saveDirectory = new File(rootDirectory, "saves");
		assetDirectory = new File(rootDirectory, "assets");
		manualFile = new File(assetDirectory, "manual.pdf");
		errorLog = new File(assetDirectory, "errorlog.txt");
		saveDirectory.mkdirs();
		assetDirectory.mkdirs();
		try {
			errorLog.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Saves the image to the saves directory */
	public static void saveImage(Map map) {
		/*
		try {
			ImageIO.write(map.biomeImage(), "png", new File(MapFileIO.saveDir(), map.fileName()+"_"+"Image.png"));
		} catch (IOException e) {
			MapFileIO.writeError("Could not save image");
			e.printStackTrace();
		}
		*/
	}
	
	/** Opens the manual.pdf file, {@link manualFile}, found in {@link assetDirectory}*/
	public static void openManual(){
		if (Desktop.isDesktopSupported()) {
		    try {
		        Desktop.getDesktop().open(manualFile);
		    } catch (IOException ex) {
		        writeError("Could not open manual", "Please check that the file is present and that you have assigned a default program to open .pdf files.");
		    }
		}
	}
	
	/**
	 * Reads an XML file containing a map object
	 * @param file The file to read
	 * @return The map object saved in the file
	 */
	public static Map read(File file) {
		Map map = null;
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			doc.getDocumentElement().normalize();

			String name = doc.getElementsByTagName("Name").item(0).getTextContent();
			long seed = Long.parseLong(doc.getElementsByTagName("Seed").item(0).getTextContent());
			int width = Integer.parseInt(doc.getElementsByTagName("Width").item(0).getTextContent());
			int height = Integer.parseInt(doc.getElementsByTagName("Height").item(0).getTextContent());
			int numberOfSites = Integer.parseInt(doc.getElementsByTagName("Sites").item(0).getTextContent());
			int shape = Integer.parseInt(doc.getElementsByTagName("Shape").item(0).getTextContent());
			int elev = Integer.parseInt(doc.getElementsByTagName("Elevation").item(0).getTextContent());
			int moist = Integer.parseInt(doc.getElementsByTagName("Moisture").item(0).getTextContent());
			
			//map = new Map(name, seed, width, height, numberOfSites, 3, shape, elev, moist);
		} catch (Exception e) {
			writeError("The map file could not be read");
		}
		return map;
	}
	public static void writeError(String text) {
		writeError(text,"");
	}
	public static void writeError(String text, String info) {
		JOptionPane.showMessageDialog(null, "Error: "+text+"\n \n"+info+"\n"
				+"Error logged to: " +errorLog.getAbsolutePath());
		try {
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
			FileWriter errWriter = new FileWriter(errorLog);
			Timestamp time = new Timestamp(System.currentTimeMillis());
			errWriter.write("("+sdf.format(time)+") "+text+"\n");
			errWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes a map to a file, if the file does not exist, create it
	 * @param map The map to save
	 */
	/*
	public static void write(Map map) {
		
		try {
			File saveFile = new File(saveDirectory, map.fileName()+".xml"); //TerraNovamapMarkupLanguage - Prevents user loading other xml files
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			builder = builderFactory.newDocumentBuilder();
			Document xmlFile = builder.newDocument();
			
			Element root = xmlFile.createElement("Map");
			xmlFile.appendChild(root);
			
			Element name = xmlFile.createElement("Name");
			root.appendChild(name);
			name.appendChild(xmlFile.createTextNode(map.mapName()));
			
			Element seed = xmlFile.createElement("Seed");
			root.appendChild(seed);
			seed.appendChild(xmlFile.createTextNode(String.valueOf(map.mapSeed())));
			
			Element width = xmlFile.createElement("Width");
			root.appendChild(width);
			width.appendChild(xmlFile.createTextNode(String.valueOf(map.mapWidth())));
			
			Element height = xmlFile.createElement("Height");
			root.appendChild(height);
			height.appendChild(xmlFile.createTextNode(String.valueOf(map.mapHeight())));
			
			Element sites = xmlFile.createElement("Sites");
			root.appendChild(sites);
			sites.appendChild(xmlFile.createTextNode(String.valueOf(map.getNumberOfSites())));
			
			Element shape = xmlFile.createElement("Shape");
			root.appendChild(shape);
			shape.appendChild(xmlFile.createTextNode(String.valueOf(map.getShapeType())));
			
			Element elevation = xmlFile.createElement("Elevation");
			root.appendChild(elevation);
			elevation.appendChild(xmlFile.createTextNode(String.valueOf(map.getElevationType())));
			
			Element moisture = xmlFile.createElement("Moisture");
			root.appendChild(moisture);
			moisture.appendChild(xmlFile.createTextNode(String.valueOf(map.getMoistureType())));
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			StreamResult result = new StreamResult(saveFile);
			DOMSource source = new DOMSource(xmlFile);
			transformer.transform(source, result);
			saveFile.createNewFile();
			
			
		} catch (ParserConfigurationException e) {
			writeError("File could not be created", "The system encountered a parsing error");
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			writeError("File could not be created", "A TransformerConfigurationException occured");
			e.printStackTrace();
		} catch (TransformerException e) {
			writeError("File could not be created", "A TransformerException occured");
			e.printStackTrace();
		} catch (IOException e) {
			writeError("File could not be created", "An IOException occured");
			e.printStackTrace();
		}
		
		
	}*/
}