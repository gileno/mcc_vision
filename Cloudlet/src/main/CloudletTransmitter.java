package main;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import recognition.RecognitionClient;
import utils.CloudletUtils;
import utils.Configuration;
import utils.FaceDetectionUtils;
import utils.PingResult;
import utils.RabbitMQUtils;

import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Main class of the cloudlet, here the cloudlet server is started. 
 * The cloudlet works as a load balancer forwarding requests 
 * to servers capable to perform detection/recognition.
 * @author airton
 *
 */
public class CloudletTransmitter {
	private Socket connectionSocket;
	private ServerSocket welcomeSocket;
	protected static String ROOT_PATH = "";
	public static String PATTERN_DATA_PATH = ROOT_PATH+"pattern_data/";
	protected static String DETECTED_IMAGES_DIR = ROOT_PATH+"detected_images/";
	protected static String RECEIVED_IMAGES_DIR = ROOT_PATH+"received_images/";
	protected static String TRAINING_DATABASE_IMAGE_LIST = PATTERN_DATA_PATH + "facedata.xml";
	protected  List<String> detectedImageNames = new ArrayList<String>();
	protected  List<byte[]> detectedImagesBytes = new ArrayList<byte[]>();
	protected static String ipServer = "127.0.0.1"; // Properties?
	protected List<PingResult> returnedPingResultList  = new ArrayList<PingResult>();
	SortedMap<Long, String> ipServersAux;
	String resultFromServerRecog;
	public String bestIpServer = null;
	private RabbitMQUtils rabbitMQUtils = new RabbitMQUtils();
	
	/*Servers ports*/
	public static int PORT_CLOUDLET_SERVER = 6790;
	public static int PORT_SERVER_RECOG = 6791;
	public static int PORT_SERVER_DETECT = 6792;
	public static int PING_PORT_SERVER = 6793;
	
	public static int IMG_WIDTH = 200;
	public static int IMG_HEIGHT = 200;
	
	CloudletUtils tcpClientCloudlet;
	private  int counter; //represent how many faces where already returned over the recognition phase
	
	protected String originalImageName;
	private static final int SCALE = 2;
	public List<CvRect> cvRectList;
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		CloudletTransmitter cloudetTransmitter = new CloudletTransmitter(
				Configuration.getConfiguration().getServers());
		try {
			cloudetTransmitter.startCloudlet();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	

	public CloudletTransmitter(){
		
	}
			
	public CloudletTransmitter(List<String> nameServers) {
		System.out.println("Starting Cloudlet...");
		try {
			welcomeSocket = new ServerSocket(PORT_CLOUDLET_SERVER);
			this.ipServersAux = new TreeMap<Long, String>();
			tcpClientCloudlet = new CloudletUtils();
			for (String name : nameServers) {
				returnedPingResultList.add(new PingResult(name));
			}
			// startCloudlet();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	
	public void startCloudlet() throws IOException, FileNotFoundException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
		byte[] bytesFromClient;
		long startTime,endTime;
		
		clearFile();
		
		for (int i = 0; i < 35; i++) {
			Thread.sleep(5000);
			startTime = System.currentTimeMillis();
			// RECEIVE FROM CLIENT
//			connectionSocket = welcomeSocket.accept();
//			System.out.println("CLOUDLET - Connection accepted... CLIENT-CLOUDLET");
//			bytesFromClient = readBytesFromClient();
//			System.out.println("CLOUDLET - Bytes received: " + bytesFromClient);

			// TRANSMIT TO SERVER AND RECEIVE FROM SERVER

			String rootPath = new File("").getAbsolutePath().replace('\\', '/');
//			bytesFromClient = CloudletUtils.imageToBytes(rootPath+"/group17.jpg");
			
			// DETECTION
			/* This method fill the list detectedImageBytes with the detected faces */
//			sendPictureToServerDetection(bytesFromClient);
			
			List<String> imagesNamesList = FaceDetectionUtils.dedectFacesCropAndSave(rootPath+"/picture15.png");
			for (String imageFile : imagesNamesList) {
				detectedImagesBytes.add(CloudletUtils.imageToBytes(DETECTED_IMAGES_DIR + imageFile));
			}
			
			//RECOGNITION
			/*To each face it calculates the best server and sends it for recognition*/
			distributeRecognizeAndReturnResult();
			
			endTime = System.currentTimeMillis();
			
			writeFile(""+ (endTime - startTime));
    		CloudletUtils.cleanDirContent(DETECTED_IMAGES_DIR);
			System.out.println("************************");
			System.out.println(i+1);
			System.out.println("************************");
		}
		
		System.out.println();
		System.out.println("FIM !!!!!!!!!!!!!!!!!!");
	}
	
	
	
	
	public boolean writeFile(String text){
        try {
            File file = new File("Log.txt");
            FileOutputStream out = new FileOutputStream(file, true);
            out.write("\n".getBytes());
            out.write(text.getBytes());
            out.flush();
            out.close();    
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean clearFile(){
    	File file = new File("Log.txt");
    	try {
			RandomAccessFile r = new RandomAccessFile(file, "rws");
			r.setLength(0);
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return false;
    }
	
	public void distributeRecognizeAndReturnResult() throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException{
		String result = "";
		
		CountDownLatch startSignal = new CountDownLatch(1);
	    CountDownLatch doneSignal = new CountDownLatch(this.returnedPingResultList.size());
		
	    this.rankServers("RECOGNITION");
	    Map<String, Integer> servers = this.getPicturesNumberByTotalCost(detectedImagesBytes.size());
	
	    //AQUI VAI SEM O RANQUEAMENTO
	    //Map<String, Integer> servers = this.getPicturesNumberEqualy();
	    
	    
		List<RecognitionClient> recognitionList = new ArrayList<RecognitionClient>();
		int index = 0;
		int threadIndex = 0;
		List<byte[]> pictures;
		for (Entry<String, Integer> entry : servers.entrySet()) {
			pictures = new ArrayList<byte[]>();
			for (int i = index; i < (index + entry.getValue()); i++) {
				pictures.add(detectedImagesBytes.get(i));
			}
			index = index + entry.getValue();
			recognitionList.add(new RecognitionClient(pictures, entry.getKey(), this, startSignal, doneSignal));
			new Thread(recognitionList.get(threadIndex)).start();
			System.out.println("o servidor "+ entry.getKey() + " vai receber " + entry.getValue() + "faces"); 
			threadIndex++;
		}
		
		startSignal.countDown();
		System.out.println("waiting for threads to finish");
		try {
			doneSignal.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		for(RecognitionClient client: recognitionList){
			result += client.getResult(); 
		}

		// RETURN TO CLIENT
		System.out.println("CLOUDLET - Returning result to mobile device...");
//		result = observer.getResult();
		returnResultToClient(result);
//		System.out.println(result);
		
		resultFromServerRecog = "";
		detectedImagesBytes.clear();
		counter = 0;
		cleanDirContent();
	}
		
	
	private static void cleanDirContent(){
		File folder = new File(DETECTED_IMAGES_DIR);  
		if (folder.isDirectory()) {  
		    File[] sun = folder.listFiles();  
		    for (File toDelete : sun) {  
		        toDelete.delete();  
		    }  
		}  
	}

	
	private void fillCVRectList(String text) {
		cvRectList = new ArrayList<CvRect>();
		String[] tokens = text.split(";");
		String[] tokens2 = null;
		
		for (String string : tokens) {
			tokens2 = string.split(",");
			cvRectList.add(new CvRect(Integer.parseInt(tokens2[0]),Integer.parseInt(tokens2[1]),Integer.parseInt(tokens2[2]),Integer.parseInt(tokens2[3])));
		}
		
	}
	
	public void addResult(String result) {
		this.resultFromServerRecog += result;
		this.counter++;
	}
	
	public void returnResultToClient(String text) {
	    try {
	        OutputStream socketStream = connectionSocket.getOutputStream();
	        ObjectOutputStream objectOutput = new ObjectOutputStream(socketStream);
	        objectOutput.writeObject(text);

	        objectOutput.close();
	        socketStream.close();

	    } catch (Exception e) {
	    	System.out.println(e.getMessage());
	    }
	}

	public byte[] readBytesFromClient() throws IOException {
		InputStream in = connectionSocket.getInputStream();
		DataInputStream dis = new DataInputStream(in);

		int len = dis.readInt();
		byte[] dataToStoreTheImage = new byte[len];

		if (len > 0) {
			dis.readFully(dataToStoreTheImage);
		}

		return dataToStoreTheImage;
	}
	

	/**
	 * This method ping the list of registered servers and rank the round by delay and load percentage of them to get the fast server and
	 * then return its IP.
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 * @throws ConsumerCancelledException 
	 * @throws ShutdownSignalException 
	 */
	public void rankServers(String serverType) throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
		String returnedStatistics = "";
		double maxLoadPercentage = -1, minLoadPercentage = 10000000;
		double maxRtt = -1, minRtt = 10000000;
		
		int latency;
		double capacity;
		
		//CAPTURE THE STATISTICS LOAD AND DELAY TO FILL THE PINGRESULTS
		for (PingResult pingResult: this.returnedPingResultList) {
			returnedStatistics = getReturnTimeAndLoadPercentage(pingResult.getName(), serverType);
			String[] tokens = returnedStatistics.split(";");
			
			double loadPercentage = Double.parseDouble(tokens[0]);
			latency = Integer.parseInt(tokens[1]);
			capacity = Double.parseDouble(tokens[2]);
			
			pingResult.setLoadPercentage(loadPercentage);
			
			
			//Ação temporária para fixar a latencia, procurando atingir distribuição normal no experimento.
			pingResult.setLatency(3);
			pingResult.setCapacity(capacity);
			
			System.out.println("CLOUDLET - Server: " + pingResult.getName() +
					" has a latency: " +latency + 
					"Ms and load percentage: " + loadPercentage +
					" and capacity: " +capacity);

			if (pingResult.getLoadPercentage() < minLoadPercentage) {
				minLoadPercentage = pingResult.getLoadPercentage() == 0 ? 1 : pingResult.getLoadPercentage();
			}
			if (pingResult.getRtt() < minRtt) {
				minRtt = pingResult.getRtt() == 0 ? 1 : pingResult.getRtt();
			}
			
			if (pingResult.getLoadPercentage() > maxLoadPercentage){
				maxLoadPercentage = pingResult.getLoadPercentage() == 0 ? 1 : pingResult.getLoadPercentage();
			}
			if (pingResult.getRtt() > maxRtt){
				maxRtt = pingResult.getRtt() == 0 ? 1 : pingResult.getRtt();
			}
			
		}
		
		//FILL THE FIELD FITNESS 
		for (PingResult pingResult: this.returnedPingResultList) {
			double cost = CloudletUtils.calculateCost(
					pingResult,
					minLoadPercentage,
					maxLoadPercentage,
					minRtt,
					maxRtt);
			pingResult.setCost(cost);
		}
		
		//SORT THE LIST
		Collections.sort(this.returnedPingResultList);
	}

	/**
	 * Return time + Load Percentage 
	 * Example1: 43434;34
	 * Example2: 434;99
	 * @param nameServer
	 * @param serverType
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 * @throws ConsumerCancelledException 
	 * @throws ShutdownSignalException 
	 */
	public String getReturnTimeAndLoadPercentage(String nameServer, String serverType) throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
		rabbitMQUtils.connect();
	    
	    String replyQueueName = rabbitMQUtils.getChannel().queueDeclare().getQueue(); 
	    QueueingConsumer consumer = new QueueingConsumer(rabbitMQUtils.getChannel());
	    rabbitMQUtils.getChannel().basicConsume(replyQueueName, true, consumer);
	    
		String response = null;
	    String corrId = java.util.UUID.randomUUID().toString();

	    String requestQueueName = nameServer + "-ping";
	    BasicProperties props = new BasicProperties
	                                .Builder()
	                                .correlationId(corrId)
	                                .replyTo(replyQueueName)
	                                .build();

		long startTime = System.currentTimeMillis();

		rabbitMQUtils.getChannel().basicPublish("", requestQueueName, props, ".".getBytes());

	    while (true) {
	        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
	        if (delivery.getProperties().getCorrelationId().equals(corrId)) {
	            response = new String(delivery.getBody());
	            break;
	        }
	    }

		System.out.println("CLOUDLET - Pinging server " + nameServer + "...");
		long endTime = System.currentTimeMillis();

		if (response.contains("pong")) {
			String[] tokens = response.split(";");
			String cpuUsagePercentage = tokens[1];
			return cpuUsagePercentage +";"+String.valueOf(endTime - startTime) + ";" + Double.parseDouble(tokens[2]);
		}

		return "";
	}
	
	public Map<String, Integer> getPicturesNumberByTotalCost(int totalPictures) {
		double inverseCostSum = 0;
		int totalPicturesByName = 0;
		int totalRemaining = totalPictures;
		Map<String, Integer> result = new HashMap<String, Integer>();
		for(PingResult pingResult : this.returnedPingResultList){
			inverseCostSum = inverseCostSum + (1 - pingResult.getCost());
		}
		System.out.println("Total pictures: " + totalPictures);
		for(PingResult pingResult : this.returnedPingResultList){
			totalPicturesByName = (int) Math.round(((1 - pingResult.getCost()) * totalPictures) / inverseCostSum);
			if(totalPicturesByName > totalRemaining) {
				totalPicturesByName = totalRemaining;
			}
			result.put(pingResult.getName(), totalPicturesByName);
			totalRemaining = totalRemaining - totalPicturesByName;
			System.out.println("Total pictures for: " + pingResult.getName() + ": " + totalPicturesByName);
		}
		if(totalRemaining > 0){
			PingResult firstServer = this.returnedPingResultList.get(0);
			int currentPictures = result.get(firstServer.getName());
			int adjustmentPictures = currentPictures + totalRemaining;
			result.put(firstServer.getName(), adjustmentPictures);
			System.out.println("Adjustment for " + firstServer.getName() + ": " + adjustmentPictures);
		}
		

//		System.out.print("Total pictures for: " );
//		for(PingResult pingResult : this.returnedPingResultList){
//			System.out.println(pingResult.getName() + ": " + totalPicturesByName);
//		}
		
		return result;
	}
	
	//POG
	public Map<String, Integer> getPicturesNumberEqualy() {
		Map<String, Integer> result = new HashMap<String, Integer>();
		for(PingResult pingResult : this.returnedPingResultList){
			result.put(pingResult.getName(), 6);
		}
		return result;
	}
	
	public List<PingResult> getReturnedPingResultList() {
		return returnedPingResultList;
	}

	public void setReturnedPingResultList(List<PingResult> returnedPingResultList) {
		this.returnedPingResultList = returnedPingResultList;
	}
	
	
}
