package edu.illinois.mitra.starl.comms;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.util.Log;
import edu.illinois.mitra.starl.exceptions.ItemFormattingException;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.GpsReceiver;
import edu.illinois.mitra.starl.objects.Common;
import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.PositionList;

public class UdpGpsReceiver extends Thread implements GpsReceiver {
	private static final String TAG = "GPSReceiver";
	private static final String ERR = "Critical Error";
	
	public PositionList robotPositions;
	public PositionList waypointPositions;

	private GlobalVarHolder gvh;

	private DatagramSocket mSocket;
	private InetAddress myLocalIP;
	private boolean running = true;
	private String name = null;
	private boolean received = false;

	public UdpGpsReceiver(GlobalVarHolder gvh,String hostname, int port, PositionList robotPositions, PositionList waypointPositions) {
		super();
		this.gvh = gvh;
		
		name = gvh.id.getName();
		this.robotPositions = robotPositions;
		this.waypointPositions = waypointPositions;

		try {
			myLocalIP = getLocalAddress();
			mSocket = new DatagramSocket(port);
		} catch (IOException e) {
			gvh.log.e(TAG, "Unable to create socket!");
			e.printStackTrace();
		}
		
		gvh.log.i(TAG, "Listening to GPS host on port " + port);
		gvh.trace.traceEvent(TAG, "Created");
	}

	@Override
	public synchronized void start() {
		gvh.log.i("GPSReceiver", "Starting GPS receiver");
		running = true;
		super.start();
	}

	@Override
	public void run() {
    		byte[] buf = new byte[2048]; 
    		
    		while(running) {
    	    	try {
		    	// Receive a message
    			DatagramPacket packet = new DatagramPacket(buf, buf.length); 
				mSocket.receive(packet);
    			InetAddress remoteIP = packet.getAddress();
    			if(remoteIP.equals(myLocalIP))
    				continue;

    			String line = new String(packet.getData(), 0, packet.getLength());
    		
    			// Parse the received string
    			String [] parts = line.split("\n");
    			if(received == false) {
    				gvh.log.i(TAG, "RECEIVED FIRST PACKET!");
    				gvh.plat.sendMainMsg(Common.MESSAGE_LOCATION, Common.GPS_RECEIVING);
    				received = true;
    			}    			
    			for(int i = 0; i < parts.length; i++) {
    				if(parts[i].length() >= 2) {
		    			switch(parts[i].charAt(0)) {
		    			case '@':
		    				try {
		    					ItemPosition newpos = new ItemPosition(parts[i]);
		    					waypointPositions.update(newpos);
		    					gvh.trace.traceEvent(TAG, "Received Waypoint", newpos);
		    				} catch(ItemFormattingException e){
		    					gvh.log.e(TAG, "Invalid item formatting: " + e.getError());
		    				}
		    				break;
		    			case '#':
		    				try {
		    					ItemPosition newpos = new ItemPosition(parts[i]);
		    					robotPositions.update(newpos);
		    					gvh.sendRobotEvent(Common.EVENT_GPS);
		    					if(newpos.getName().equals(name)) {
		    						gvh.trace.traceEvent(TAG, "Received Position", newpos);
		    						gvh.sendRobotEvent(Common.EVENT_GPS_SELF);
		    					}
		    				} catch(ItemFormattingException e){
		    					gvh.log.e(TAG, "Invalid item formatting: " + e.getError());
		    				}
		    				break;
		    			case 'G':
		    				gvh.trace.traceEvent(TAG, "Received launch command");
		    				int[] args = Common.partsToInts(parts[i].substring(3).split(" "));
		    				gvh.plat.sendMainMsg(Common.MESSAGE_LAUNCH, args[0], args[1]);
		    				break;
		    			case 'A':
		    				gvh.trace.traceEvent(TAG, "Received abort command");
		    				gvh.plat.sendMainMsg(Common.MESSAGE_ABORT, null);
		    				break;
		    			default:
		    				gvh.log.e(ERR, "Unknown GPS message received: " + line);
		    				break;
		    			}
    				}
    			}
			} catch (IOException e) {
				gvh.plat.sendMainMsg(Common.MESSAGE_LOCATION, Common.GPS_OFFLINE);
			}
    	}
	}
	
    private InetAddress getLocalAddress() throws IOException {
		try {
		    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
		        NetworkInterface intf = en.nextElement();
		        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
		            InetAddress inetAddress = enumIpAddr.nextElement();
		            if (!inetAddress.isLoopbackAddress()) {
		            	return inetAddress;
		            }
		        }
		    }
		} catch (SocketException ex) {
		    gvh.log.e(TAG, ex.toString());
		}
		return null;
    }
    
    /* (non-Javadoc)
	 * @see edu.illinois.mitra.starl.comms.GpsReceiver#cancel()
	 */
    @Override
    public void cancel() {
    	running = false;
    	gvh.plat.sendMainMsg(Common.MESSAGE_LOCATION, Common.GPS_OFFLINE);
        try {
        	mSocket.disconnect();
            mSocket.close();
        } catch (Exception e) {
            gvh.log.e(ERR, "close of connect socket failed" + e);
            e.printStackTrace();
        }
        gvh.log.i(TAG, "Closed UDP GPS socket");
		gvh.trace.traceEvent(TAG, "Cancelled");
    }

	@Override
	public PositionList getRobots() {
		return robotPositions;		
	}

	@Override
	public PositionList getWaypoints() {
		return waypointPositions;
	}
}