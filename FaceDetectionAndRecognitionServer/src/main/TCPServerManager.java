package main;

import java.io.FileNotFoundException;
import java.io.IOException;

import servers.TCPServerDetection;
import servers.TCPServerForPing;
import servers.TCPServerRecognition;
import utils.Configuration;

/**
 * This class starts the three types of servers at the same time using a thread for each of them.
 * @author airton
 */
public class TCPServerManager implements Runnable {

	private TCPServerRecognition tcpServerForImageRecognition;
	private TCPServerDetection tcpServerForImageDetection;
	private TCPServerForPing tcpServerForPing;
	
	static int PORT_SERVER_RECOG = 6791;
	static int PORT_SERVER_DETECT = 6792;
	static int PING_PORT_SERVER = 6793;
	
	static String DEFAULT_SERVER_NAME = "server";
	static double DEFAULT_LOAD = 1;
	
	public TCPServerManager(double serverCapacity) {
		this(serverCapacity, DEFAULT_SERVER_NAME, DEFAULT_LOAD);
	}
	
	public TCPServerManager(double serverCapacity, String name, double load) {
		tcpServerForImageRecognition = new TCPServerRecognition(name);
		tcpServerForImageDetection = new TCPServerDetection(name);
		tcpServerForPing = new TCPServerForPing(name, serverCapacity, load);
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Configuration config = Configuration.getConfiguration();
		new Thread(new TCPServerManager(config.getServerCapacity(), config.getServerName(), config.getServerLoad())).start();
	}

	@Override
	public void run() {
		new Thread(tcpServerForImageRecognition).start();
		new Thread(tcpServerForImageDetection).start();
		new Thread(tcpServerForPing).start();
	}
}
