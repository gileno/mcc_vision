package recognition;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import main.CloudletTransmitter;
import utils.CloudletUtils;
import utils.RabbitMQUtils;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class RecognitionClient implements Runnable{

	private final CountDownLatch startSignal;
	private final CountDownLatch doneSignal;
	
	private String ipServer;
	private List<byte[]> pictures;
	private CloudletUtils tcpClientCloudlet;
	private String result;
	private CloudletTransmitter cloudletTransmitterAbstract;
	RabbitMQUtils rabbitMQUtils = new RabbitMQUtils();
	
	private String requestQueueName;
	private String replyQueueName;
	private QueueingConsumer consumer;

	public RecognitionClient(){
		startSignal = null;
		doneSignal = null;
	}
	
	
	public RecognitionClient(List<byte[]> pictures, String nameServer, CloudletTransmitter cloudletTransmitterAbstract,
			CountDownLatch startSignal, CountDownLatch doneSignal) throws IOException, InterruptedException {
		super();
		this.ipServer = nameServer + "-recognition";
		this.requestQueueName = nameServer + "-recognition";
		this.tcpClientCloudlet = new CloudletUtils();
		this.pictures = pictures;
		this.cloudletTransmitterAbstract = cloudletTransmitterAbstract;
		this.startSignal = startSignal;
		this.doneSignal = doneSignal;
	    
		rabbitMQUtils.connect();

	    replyQueueName = rabbitMQUtils.getChannel().queueDeclare().getQueue(); 
	    consumer = new QueueingConsumer(rabbitMQUtils.getChannel());
	    rabbitMQUtils.getChannel().basicConsume(replyQueueName, true, consumer);
	}
	
	public String getResult(){
		return this.result;
	}
	
	public void sendPictureToServerRecognition() throws IOException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
		String response = null;
	    String corrId = java.util.UUID.randomUUID().toString();

	    BasicProperties props = new BasicProperties
	                                .Builder()
	                                .correlationId(corrId)
	                                .replyTo(replyQueueName)
	                                .build();
	    for(byte[] picture : this.pictures) {
	    	rabbitMQUtils.getChannel().basicPublish("", requestQueueName, props, picture);

		    while (true) {
		        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
		        if (delivery.getProperties().getCorrelationId().equals(corrId)) {
		            response = new String(delivery.getBody());
		            break;
		        }
		    }
		    this.result += response;
	    }
	}

	public void run() {
		try {
			startSignal.await(); 
			sendPictureToServerRecognition();
			doneSignal.countDown();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

}
