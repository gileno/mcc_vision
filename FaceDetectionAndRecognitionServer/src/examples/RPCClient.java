package examples;

import com.rabbitmq.client.AMQP.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class RPCClient {
	private Connection connection;
	private Channel channel;
	private String requestQueueName = "rpc_queue";
	private String replyQueueName;
	private QueueingConsumer consumer;

	public RPCClient() throws Exception {
	    ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost("localhost");
	    connection = factory.newConnection();
	    channel = connection.createChannel();

	    replyQueueName = channel.queueDeclare().getQueue(); 
	    consumer = new QueueingConsumer(channel);
	    channel.basicConsume(replyQueueName, true, consumer);
	}

	public String call(String message) throws Exception {     
	    String response = null;
	    String corrId = java.util.UUID.randomUUID().toString();

	    BasicProperties props = new BasicProperties
	                                .Builder()
	                                .correlationId(corrId)
	                                .replyTo(replyQueueName)
	                                .build();

	    channel.basicPublish("", requestQueueName, props, message.getBytes());

	    while (true) {
	        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
	        if (delivery.getProperties().getCorrelationId().equals(corrId)) {
	            response = new String(delivery.getBody());
	            break;
	        }
	    }

	    return response; 
	}

	public void close() throws Exception {
	    connection.close();
	}
	
	public static void main(String[] args) throws Exception {
		RPCClient fibonacciRpc = new RPCClient();

		System.out.println(" [x] Requesting fib(30)");   
		String response = fibonacciRpc.call("30");
		System.out.println(" [.] Got '" + response + "'");

		fibonacciRpc.close();
	}
}
