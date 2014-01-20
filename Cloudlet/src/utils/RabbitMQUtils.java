package utils;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQUtils {

	private ConnectionFactory factory = null;
	private Channel channel = null;
	private Connection connection = null;

	public Channel connect() throws IOException, InterruptedException {
		Configuration config = Configuration.getConfiguration();
		while (channel == null || !channel.isOpen()) {
			try{
				
			// Obtém acesso à conexão com o servidor
			factory = new ConnectionFactory();
			factory.setHost(config.getRabbitMQServer());
			factory.setPort(5672);
			factory.setPassword("guest");
			factory.setUsername("guest");
			connection = factory.newConnection();
			
			System.out.println("Accessing server '" + factory.getHost()
					+ "' on port '" + factory.getPort() + "'");
			System.out.println("User: '" + factory.getUsername()
					+ "' Password: '" + factory.getPassword() + "'");

			// Obtém um canal a partir da conexão
			channel = connection.createChannel();
//			channel.exchangeDeclare(JiTCollectorProperties.get(JiTCollectorProperties.EXCHANGE_NAME), "topic");
			}catch (Exception e) {
				System.err.print("[CONNECTION ERROR] ");
				System.err.println(e.getMessage());
				System.out.println("\nTrying to connect again ....");
				Thread.sleep(5000);
				continue;
			}
		}
		return channel;
	}

	public ConnectionFactory getFactory() {
		return factory;
	}

	public void setFactory(ConnectionFactory factory) {
		this.factory = factory;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	
	

}