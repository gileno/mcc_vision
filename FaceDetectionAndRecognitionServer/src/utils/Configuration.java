package utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {

	private Properties properties;
	private String filename = "config-server.properties";
	private static Configuration configuration = null;
	
	private Configuration() throws FileNotFoundException, IOException{
		this.properties = new Properties();
		InputStream in = new FileInputStream("./"+this.filename);
		this.properties.load(in);
	}
	
	public static Configuration getConfiguration() throws FileNotFoundException, IOException{
		if(configuration == null) {
			configuration = new Configuration();
		}
		return configuration;
	}
	
	public String getProperty(String key){
		return this.properties.getProperty(key);
	}
	
	public String getProperty(String key, String defaultValue){
		return this.properties.getProperty(key, defaultValue);
	}
	
	public double getServerCapacity() {
		return Double.parseDouble(this.getProperty("serverCapacity"));
	}
	
	public double getServerLoad() {
		return Double.parseDouble(this.getProperty("serverLoad"));
	}
	
	public String getServerName() {
		return this.getProperty("serverName");
	}
	
	public String getRabbitMQServer() {
		return this.getProperty("rabbitmq-server");
	}
	
	public static void main(String[] args) {
		try {
			Configuration configuration = Configuration.getConfiguration();
			System.out.println(configuration.getProperty("testKey"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
