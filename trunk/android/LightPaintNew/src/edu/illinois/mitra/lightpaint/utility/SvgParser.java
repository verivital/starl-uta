package edu.illinois.mitra.lightpaint.utility;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.geometry.ImagePoint;

public class SvgParser extends XmlReader {

	public SvgParser(int maxSizeX, int maxSizeY, int centerX, int centerY) {
		super(false);
		scale = new ImagePoint(maxSizeX, maxSizeY);
		center = new ImagePoint(centerX, centerY);
	}

	private ImagePoint center;
	private ImagePoint scale;

	public Set<ImageEdge> parseImage(String filename) {
		try {
			super.buildDocument(new FileInputStream(filename));
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		Set<ImageEdge> retval = new HashSet<ImageEdge>();

		// Isolate all line nodes
		NodeList lines = super.getExpression("//line");
		for(int i = 0; i < lines.getLength(); i++) {
			Node line = lines.item(i);
			NamedNodeMap attrib = line.getAttributes();
			int x1 = (int) Double.parseDouble(attrib.getNamedItem("x1").getNodeValue());
			int x2 = (int) Double.parseDouble(attrib.getNamedItem("x2").getNodeValue());
			int y1 = (int) Double.parseDouble(attrib.getNamedItem("y1").getNodeValue());
			int y2 = (int) Double.parseDouble(attrib.getNamedItem("y2").getNodeValue());

			retval.add(new ImageEdge(new ImagePoint(x1, y1), new ImagePoint(x2, y2)));
		}

		NodeList paths = super.getExpression("//path");
		for(int i = 0; i < paths.getLength(); i++) {
			Node path = paths.item(i);
			String pathString = path.getAttributes().getNamedItem("d").getNodeValue();
			retval.addAll(pathToLines(pathString));
		}

		return scaleAndCenterEdges(retval);
	}

	private static final String[] INVALID_PATH_COMMANDS = { "c", "C", "s", "S", "q", "Q", "t", "T", "a", "A" };
	private static final Pattern PATH_PATTERN = Pattern.compile("[a-zA-Z]([\\s,]?[-]?[0-9]+[\\s,]?[-]?[0-9]+[\\s,]?){1,}");
	private static final Set<ImageEdge> EMPTY_EDGE_SET = new HashSet<ImageEdge>();

	private Set<ImageEdge> pathToLines(String pathString) {
		// Break the string into commands first
		// Must split along any character, preserving which command the
		// character is placed in
		// For example "m50,100L60,200" -> "m50,100" and "L60,200"

		for(String invalid : INVALID_PATH_COMMANDS) {
			if(pathString.contains(invalid)) {
				System.err.println("Image contains a path with curves! Skipping.");
				return EMPTY_EDGE_SET;
			}
		}

		List<String> commands = new ArrayList<String>(pathString.length() / 4);

		Matcher matcher = PATH_PATTERN.matcher(pathString);
		int idx = 0;
		while(matcher.find(idx)) {
			String cmd = pathString.substring(matcher.start(), matcher.end());
			commands.add(cmd);
			idx = matcher.end();
		}

		Set<ImageEdge> retval = new HashSet<ImageEdge>();

		// Step through the commands
		ImagePoint current = new ImagePoint(0, 0);
		for(String commandString : commands) {
			char cmdType = commandString.charAt(0);
			ImagePoint cmdCoordinates = ImagePoint.fromString(commandString.substring(1));
			switch(cmdType) {
			case 'm':
				current = current.add(cmdCoordinates);
				break;
			case 'M':
				current = cmdCoordinates;
				break;
			case 'l':
				retval.add(new ImageEdge(current, current = current.add(cmdCoordinates)));
				break;
			case 'L':
				retval.add(new ImageEdge(current, current = cmdCoordinates));
				break;
			}
		}

		return retval;
	}

	private Set<ImageEdge> scaleAndCenterEdges(Collection<ImageEdge> edges) {
		// Find X range and Y range
		double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

		for(ImageEdge edge : edges) {
			for(ImagePoint point : edge.getEnds()) {
				minX = Math.min(minX, point.getX());
				maxX = Math.max(maxX, point.getX());
				minY = Math.min(minY, point.getY());
				maxY = Math.max(maxY, point.getY());
			}
		}

		double width = maxX - minX;
		double height = maxY - minY;
		
		List<ImageEdge> retval = new ArrayList<ImageEdge>();

		// Subtract minX and minY from all points to base the image at 0,0
		ImagePoint offset = new ImagePoint(-minX, -minY);
		for(ImageEdge edge : edges)
			retval.add(edge.translate(offset));
		
		// Scaling
		double scalefactor = Math.min(scale.getX() / width, scale.getY() / height);
		System.out.println("Scaling by " + scalefactor);
		for(int i = 0; i < retval.size(); i++)
			retval.set(i, retval.get(i).scale(scalefactor));
		
		// Centering

		double centerX = (scalefactor*width) / 2.0;
		double centerY = (scalefactor*height) / 2.0;
		offset = center.subtract(new ImagePoint(centerX, centerY));
		System.out.println("Translating by " + offset);

		for(int i = 0; i < retval.size(); i++)
			retval.set(i, retval.get(i).translate(offset));

		return new HashSet<ImageEdge>(retval);
	}
}