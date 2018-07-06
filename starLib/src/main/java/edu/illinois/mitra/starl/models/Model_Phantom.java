package edu.illinois.mitra.starl.models;

import edu.illinois.mitra.starl.exceptions.ItemFormattingException;
import edu.illinois.mitra.starl.motion.DjiController;
import edu.illinois.mitra.starl.motion.DroneBTI;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;
import edu.illinois.mitra.starl.objects.Point3i;
import edu.illinois.mitra.starl.objects.PositionList;

/**
 * This class represents a simple model of the quadcopter
 * @author Yixiao Lin
 * @version 1.0
 */

public class Model_Phantom extends Model_Drone {

	public Model_Phantom(String received) throws ItemFormattingException {
		super(received);
	}

	public Model_Phantom(String name, int x, int y) {
		super(name, x, y);
	}

	public Model_Phantom(String name, int x, int y, int z) {
		super(name, x, y, z);
	}

	public Model_Phantom(String name, int x, int y, int z, int yaw) {
		super(name, x, y, z, yaw);
	}

	public Model_Phantom(String name, int x, int y, int z, double yaw, double pitch, double roll) {
		super(name, x, y, z, yaw, pitch, roll);
	}

	public Model_Phantom(ItemPosition t_pos) {
		super(t_pos);
	}

	@Override
	public int radius() { return 340; }

	@Override
	public double height() { return 50; }

	@Override
	public double mass() { return 1.216; }	// Phantom 3 mass with battery + propellers is 1216g

	@Override
	public double max_gaz() { return 1000; }

	@Override
	public double max_pitch_roll(){ return 20; }

	@Override
	public double max_yaw_speed() { return 200; }

	@Override
	public Class<? extends DroneBTI> getBluetoothInterface() {
		return DjiController.class;
	}
}
