package servers;

import java.io.FileNotFoundException;
import java.io.IOException;

import load_capturer.CentralMeter;

import org.hyperic.sigar.SigarException;

import utils.Configuration;
import utils.RabbitMQUtils;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class TCPServerForPing implements Runnable {

	private double capacity;
	private String name;
	private double load;
	private RabbitMQUtils rabbitMQUtils = new RabbitMQUtils();
	
	public TCPServerForPing(String name, double capacity, double load) {
		this.capacity = capacity;
		this.name = name + "-ping";
		this.load = load;
	}

	public void startServer() throws IOException, FileNotFoundException, SigarException, ShutdownSignalException, ConsumerCancelledException, InterruptedException {
		System.out.println("PING SERVER - Ping Server started...");

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
		    
//		    String cpuUsage = centralMeter.getCPUPercentageUsage();
		    
		    String result = "pong;" + this.load + ";" + this.capacity;
			
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
		} catch (SigarException e) {
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
}
