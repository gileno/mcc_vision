package utils;

import java.io.IOException;

import main.CloudletTransmitter;

import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.ShutdownSignalException;

public class RecognitionSender implements Runnable {

	private String ipServer;
	private byte[] picture;
	private CloudletUtils tcpClientCloudlet;
	private String result;
	private CloudletTransmitter cloudletTransmitterAbstract;


	public RecognitionSender(String ipServer, CloudletUtils tcpClientCloudlet, CloudletTransmitter cloudletTransmitterAbstract) {
		super();
		this.ipServer = ipServer;
		this.tcpClientCloudlet = tcpClientCloudlet;
		this.cloudletTransmitterAbstract = cloudletTransmitterAbstract;
	}
	
	public RecognitionSender(String ipServer, byte[] picture, CloudletUtils tcpClientCloudlet, CloudletTransmitter cloudletTransmitterAbstract) {
		super();
		this.ipServer = ipServer;
		this.picture = picture;
		this.tcpClientCloudlet = tcpClientCloudlet;
		this.cloudletTransmitterAbstract = cloudletTransmitterAbstract;
	}

	public String sendPictureToServerRecognition() throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
		
		cloudletTransmitterAbstract.rankServers("RECOGNITION");
		cloudletTransmitterAbstract.bestIpServer = cloudletTransmitterAbstract.getReturnedPingResultList().get(0).getName();// TODO Refactor this method to be inside the RecognitionSender and run by thread (TAKE CARE)
		if (cloudletTransmitterAbstract.bestIpServer == null) {
			cloudletTransmitterAbstract.returnResultToClient("No recog. servers available");
		} else {
			System.out.println("CLOUDLET - Best server host for recognition: " + cloudletTransmitterAbstract.bestIpServer);

			System.out.println("CLOUDLET - Sending picture to server " + ipServer + " for RECOGNITION...");
			tcpClientCloudlet.connect(ipServer, CloudletTransmitter.PORT_SERVER_RECOG);
			return tcpClientCloudlet.sendBytesForRecognition(picture);
		}
		return null;
	}


	public String sendPictureToServerRecognition(byte[] picture) throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
		cloudletTransmitterAbstract.rankServers("RECOGNITION");
		cloudletTransmitterAbstract.bestIpServer = cloudletTransmitterAbstract.getReturnedPingResultList().get(0).getName();// TODO Refactor this method to be inside the RecognitionSender and run by thread (TAKE CARE)
		if (cloudletTransmitterAbstract.bestIpServer == null) {
			cloudletTransmitterAbstract.returnResultToClient("No recog. servers available");
		} else {
			System.out.println("CLOUDLET - Best server host for recognition: " + cloudletTransmitterAbstract.bestIpServer);

			System.out.println("CLOUDLET - Sending picture to server " + ipServer + " for RECOGNITION...");
			tcpClientCloudlet.connect(ipServer, CloudletTransmitter.PORT_SERVER_RECOG);
			return tcpClientCloudlet.sendBytesForRecognition(picture);
		}
		return null;
	}
	
	public String getResult() {
		return result;
	}

	@Override
	public void run() {
		try {
			result = sendPictureToServerRecognition();
			cloudletTransmitterAbstract.addResult(result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			cloudletTransmitterAbstract.addResult("error");
		} catch (ShutdownSignalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConsumerCancelledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
