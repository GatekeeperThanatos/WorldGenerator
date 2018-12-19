package worldgenerator.ui;

import javax.swing.*;

import worldgenerator.map.Map;

import java.awt.*;
import java.awt.image.BufferedImage;


public class MapPane extends JPanel {
	/** */
	private static final long serialVersionUID = 1L;
	/** */
	private static MapCanvas canvas;

	/** */
    public MapPane(boolean isDoubleBuffered, Map map, int type, int labelType) {
        super(isDoubleBuffered);
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());
        canvas = new MapCanvas(map, type, labelType);
        
        JScrollPane pane = new JScrollPane(canvas);
        pane.getViewport().setBackground(Color.DARK_GRAY);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        pane.setBackground(Color.BLACK);
        add(pane, BorderLayout.CENTER);
        
        Rectangle mapArea = canvas.getVisibleRect();
        mapArea.setBounds(0, 0, mapArea.width, mapArea.height);
        canvas.scrollRectToVisible(mapArea);
    }
}
