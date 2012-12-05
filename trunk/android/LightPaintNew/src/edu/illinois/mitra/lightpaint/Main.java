package edu.illinois.mitra.lightpaint;

import java.io.IOException;
import java.util.Set;

import edu.illinois.mitra.lightpaint.activity.LightPaintActivity;
import edu.illinois.mitra.lightpaint.geometry.ImageEdge;
import edu.illinois.mitra.lightpaint.utility.SvgParser;
import edu.illinois.mitra.lightpaint.utility.WptWriter;
import edu.illinois.mitra.starlSim.data.CsvWriter;
import edu.illinois.mitra.starlSim.main.SimSettings;
import edu.illinois.mitra.starlSim.main.Simulation;

public class Main {
	private static final int WORLDSIZE = 2100;
	private static final int WORLDCENTER = 1500;//WORLDSIZE / 2;

	// TODO: Sometimes a segment is painted over twice, some line segments are absent. See box.svg for example. This *appears* to be a drawing glitch
	// TODO: Line segments are sometimes drawn from the wrong direction?

	private static final String WPT_PATH = "waypoints/";
	private static final String inputFilename = "knot";

	public static void main(String[] args) {
		SvgParser parser = new SvgParser(WORLDSIZE, WORLDSIZE, WORLDCENTER, WORLDCENTER);

		Set<ImageEdge> image = parser.parseImage("input_images/" + inputFilename + ".svg");
		System.out.println(image.size() + " lines in image");
		WptWriter.writeWpt(WPT_PATH + inputFilename + ".wpt", image);

		CsvWriter writer = null;
		try {
			writer = new CsvWriter("test3.csv", "N Robots", "Execution duration", "Requests made", "Assignments made", "Unpainted lines", "Total lines");
		} catch(IOException e) {
			e.printStackTrace();
		}

		for(int nbots : new int[] { 4,5,6,8,10 }) {
			SimSettings.Builder builder = new SimSettings.Builder().DRAWER(new LightPaintDrawer(true)).WAYPOINT_FILE(WPT_PATH + inputFilename + ".wpt");
			builder.N_BOTS(nbots);
			builder.TIC_TIME_RATE(5);
			builder.MAX_FPS(5);
			builder.INITIAL_POSITIONS_FILE(WPT_PATH + "startpoints_benchmark2.wpt");
			builder.DRAW_WAYPOINT_NAMES(false).DRAW_WAYPOINTS(false);
			builder.MSG_LOSSES_PER_HUNDRED(0).GRID_XSIZE(WORLDSIZE).GRID_YSIZE(WORLDSIZE).TRACE_OUT_DIR(null);

			SimSettings settings = builder.build();
			for(int i = 1; i <= 5; i++) {
				System.out.println("RUN " + i);
				System.out.println("Starting with " + settings.N_BOTS + " robots.");
				Simulation sim = new Simulation(LightPaintActivity.class, settings);
				try {
					sim.start();
				} catch(Exception e) {
					continue;
				}
				Object[] res = sim.getResults().get(0).toArray();
				System.out.println("Elapsed simulation time: " + sim.getSimulationDuration() / 1000.0 + " sec");

				writer.commit(nbots, sim.getSimulationDuration() / 1000.0, res[1], res[2], res[3], image.size());
				sim.closeWindow();
			}
		}

		writer.close();
	}
}