package servers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import utils.Configuration;
import utils.FaceRecognitionUtils;
import utils.RabbitMQUtils;
import utils.RandomString;
import utils.RecognitionResult;
import utils.Utils;

public class TCPServerRecognition implements Runnable {

	private static String ROOT_PATH = ""; // TO DEPLOY
	public static String RECEIVED_IMAGES_PATH = ROOT_PATH + "received_images/";
	public static String PATTERN_DATA_PATH = ROOT_PATH + "pattern_data/";
	protected String IMAGES_LIST_FOR_RECOGNITION_FILE = DETECTED_IMAGES_DIR + "imagesList.txt";
	public static String DETECTED_IMAGES_DIR = ROOT_PATH + "detected_images/";
	public static String TRAINING_DATABASE_IMAGE_LIST = PATTERN_DATA_PATH + "facedata.xml";
	protected String ImageNameAllPath;
	FaceRecognitionUtils faceRecognition;
	public static int IMG_WIDTH = 200;
	public static int IMG_HEIGHT = 200;
	private RabbitMQUtils rabbitMQUtils = new RabbitMQUtils();
	
	private String name;

	public TCPServerRecognition(String name) {
		faceRecognition = new FaceRecognitionUtils();
		this.name = name + "-recognition";
	}
	
	private void startRPCServer() throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException{
		System.out.println("SERVER_RECOG - Recognition Asynchronous Server started...");
		
		rabbitMQUtils.connect();

		rabbitMQUtils.getChannel().queueDeclare(name, false, false, false, null);

		rabbitMQUtils.getChannel().basicQos(1);

		QueueingConsumer consumer = new QueueingConsumer(rabbitMQUtils.getChannel());
		rabbitMQUtils.getChannel().basicConsume(name, false, consumer);
		
		while(true) {
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();

		    BasicProperties props = delivery.getProperties();
		    BasicProperties replyProps = new BasicProperties
		                                     .Builder()
		                                     .correlationId(props.getCorrelationId())
		                                     .build();

		    String imageName = new RandomString(5).nextString() + ".jpg";
			ImageNameAllPath = imageName;
			Utils.writeFileOnDisc(delivery.getBody(), RECEIVED_IMAGES_PATH, ImageNameAllPath);

			createFileListOfImagesDetected(imageName);

			List<RecognitionResult> results = faceRecognition.recognizeFileList(IMAGES_LIST_FOR_RECOGNITION_FILE);
			
			RecognitionResult recognitionResult = results.get(0);
			
			String response = new String("Id=" + recognitionResult.getFaceIndex()
					+ "\nLsd=" + recognitionResult.getLeast_squared_distance() + "\nConf.="
					+ recognitionResult.getConfidence() + "\n\n");
			
			rabbitMQUtils.getChannel().basicPublish( "", props.getReplyTo(), replyProps, response.getBytes());

			rabbitMQUtils.getChannel().basicAck(delivery.getEnvelope().getDeliveryTag(), false);
			
			cleanDirContent();
		}
	}
	
	private static void cleanDirContent(){
		File folder = new File(TCPServerRecognition.RECEIVED_IMAGES_PATH);  
		if (folder.isDirectory()) {  
		    File[] sun = folder.listFiles();  
		    for (File toDelete : sun) {  
		        toDelete.delete();  
		    }  
		}  
	}

	public static void main(String[] args) {

	}

	// For while putting only one file
	private void createFileListOfImagesDetected(String pictureName)
			throws IOException, FileNotFoundException {
		if (!pictureName.isEmpty()) {

			File dir = new File(DETECTED_IMAGES_DIR);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			File file = new File(IMAGES_LIST_FOR_RECOGNITION_FILE);
			if (!file.exists()) {
				file.createNewFile();
			} else {
				new RandomAccessFile(file, "rws").setLength(0);
			}
			FileOutputStream out = new FileOutputStream(file, true);

			out.write((1 + " " + pictureName + " " + RECEIVED_IMAGES_PATH + pictureName + "\n").getBytes());

			out.flush();
			out.close();
		}
	}

	@Override
	public void run() {
//		startServer();
		try {
			startRPCServer();
		} catch (ShutdownSignalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConsumerCancelledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
