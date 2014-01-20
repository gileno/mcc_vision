package servers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import utils.Configuration;
import utils.FaceDetectionUtils;
import utils.RabbitMQUtils;
import utils.RandomString;
import utils.TCPServerUtils;
import utils.Utils;

import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class TCPServerDetection implements Runnable{
	protected Socket connectionSocket;
	private ServerSocket welcomeSocket;
	private static String ROOT_PATH = ""; //TO DEPLOY
	public static String RECEIVED_IMAGES_PATH = ROOT_PATH+"received_images/";
	public static String DETECTED_IMAGES_DIR = ROOT_PATH+"detected_images/";

	TCPServerUtils tcpServerUtils;
	protected String originalImageName;
	protected static List<String> imageNames = new ArrayList<String>();
	protected static List<CvRect> coordinationsList =  new ArrayList<CvRect>();
	
	private String name;
	private RabbitMQUtils rabbitMQUtils = new RabbitMQUtils();
	
	public TCPServerDetection(String name) {
		this.name = name + "-detection";
	}

	public void startServer() throws IOException, FileNotFoundException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
		System.out.println("DETECTION SERVER - Recognition Asynchronous Server started...");
		
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
		    
		    originalImageName = new RandomString(5).nextString() + ".jpg";
		    
			Utils.writeFileOnDisc(delivery.getBody(), RECEIVED_IMAGES_PATH, originalImageName);

			// DETECT, CROP AND SAVE IMAGES
			System.out.println("DETECTION SERVER - DETECTING...");
			
			coordinationsList.clear();
			coordinationsList = FaceDetectionUtils.dedectFacesOnly(originalImageName);
			
			String result = "";
			for (CvRect rect : coordinationsList) {
				result += ""+ rect.x() + "," + rect.y() + "," + rect.height() + "," + rect.width()  + ";";
			}
			
			rabbitMQUtils.getChannel().basicPublish( "", props.getReplyTo(), replyProps, result.getBytes());

			rabbitMQUtils.getChannel().basicAck(delivery.getEnvelope().getDeliveryTag(), false);
		}
		
	}

	@Override
	public void run() {
		try {
			startServer();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ShutdownSignalException e) {
			e.printStackTrace();
		} catch (ConsumerCancelledException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}