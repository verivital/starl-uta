package edu.illinois.mitra.starl.harness;

import java.util.ArrayList;

import edu.illinois.mitra.starl.comms.UDPMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.ComThread;
import edu.illinois.mitra.starl.interfaces.SimComChannel;

public class SimComThread implements ComThread {
	private ArrayList<UDPMessage> receivedList;
	private SimComChannel channel;
	private String name;
	private String IP;

	public SimComThread(GlobalVarHolder gvh, SimComChannel channel) {
		this.channel = channel;
		name = gvh.id.getName();
		IP = gvh.id.getParticipantsIPs().get(name);
		channel.registerMsgReceiver(this, IP);
	}
	
	@Override
	public void write(UDPMessage msg, String toIP) {
		//System.out.println("Sending " + msg.getContents().getContentsList().toString() + " to " + toIP);
		channel.sendMsg(IP, msg.toString(), toIP);
	}
	
	public void receive(String msg) {
		//System.out.println("Received " + msg);
		receivedList.add(new UDPMessage(msg, System.currentTimeMillis()));
	}

	
	@Override
	public void cancel() {
		// Doesn't do anything!
	}
	
	@Override
	public void start() {
		// Doesn't do anything because messages are received externally!
	}

	@Override
	public void setMsgList(ArrayList<UDPMessage> ReceivedMessageList) {
		receivedList = ReceivedMessageList;
	}
}
