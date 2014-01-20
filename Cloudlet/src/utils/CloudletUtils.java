package utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;

import main.CloudletTransmitter;

public class CloudletUtils {
	private static Socket serverSocket;
	private static OutputStream outToServer;

	private String host;
	private int port;
	
	public CloudletUtils(){
		super();
	}
	
	public static void cleanDirContent(String dir){
		File folder = new File(dir);  
		if (folder.isDirectory()) {  
		    File[] sun = folder.listFiles();  
		    for (File toDelete : sun) {  
		        toDelete.delete();  
		    }  
		}  
	}
			
	public synchronized void connect(String host, int port) throws UnknownHostException, IOException{
		serverSocket = new Socket();
		serverSocket.connect(new InetSocketAddress(host, port), 4000);
		this.host = host;
	}

    public String sendBytesForDetectionReturnText(byte[] myByteArray) throws IOException {
    	String result = sendBytesReturnText(myByteArray, 0, myByteArray.length);
    	System.out.println("CLOUDLET - The result from server DETECTION "+host+" is: " + result);
		return result;
	}
	
    public synchronized String sendBytesForRecognition(byte[] myByteArray) throws IOException {
	    return this.sendBytesForRecognition(myByteArray, serverSocket);
	}
    
    public synchronized String sendBytesForRecognition(byte[] myByteArray, Socket serverSocket) throws IOException {
	    String result = sendBytesReturnText(myByteArray, 0, myByteArray.length, serverSocket);
	    System.out.println("CLOUDLET - The result from server RECOGNITION "+host+" is: \n" + result);
		return result;
	}
    
    public String sendBytesForPing(byte[] myByteArray, String serverType) throws IOException {
	    String result = sendBytesReturnText(myByteArray, 0, myByteArray.length);
	    System.out.println("CLOUDLET - The result from server "+serverType +" " +host+" is: " + result);
		return result;
	}

	private String sendBytesReturnText(byte[] myByteArray, int start, int len) throws IOException {
		return this.sendBytesReturnText(myByteArray, start, len, serverSocket);
	}
	
	private String sendBytesReturnText(byte[] myByteArray, int start, int len, Socket serverSocket) throws IOException {
		String answer;
		if (len < 0)
	        throw new IllegalArgumentException("Negative length not allowed");
	    if (start < 0 || start >= myByteArray.length)
	        throw new IndexOutOfBoundsException("Out of bounds: " + start);
	    
	    if(!serverSocket.isConnected()){
	    	serverSocket.connect(new InetSocketAddress(host, port), 4000);
	    }
	    OutputStream outToServer = serverSocket.getOutputStream(); 
	    DataOutputStream dos = new DataOutputStream(outToServer);

	    dos.writeInt(len);
	    if (len > 0) {
	        dos.write(myByteArray, start, len);
	    }

	    answer = readDataAsText();
	    return answer;
	}
	
	
	
	private byte[] sendBytesReturnBytes(byte[] myByteArray, int start, int len) throws IOException {
		byte[] answer;
		if (len < 0)
	        throw new IllegalArgumentException("Negative length not allowed");
	    if (start < 0 || start >= myByteArray.length)
	        throw new IndexOutOfBoundsException("Out of bounds: " + start);
	    
	    outToServer = serverSocket.getOutputStream(); 
	    DataOutputStream dos = new DataOutputStream(outToServer);

	    dos.writeInt(len);
	    if (len > 0) {
	        dos.write(myByteArray, start, len);
	    }

	    answer = readBytes();
	    System.out.println("CLOUDLET - The result from server DETECTION "+host+" is: " + answer);
	    
	    return answer;
	}
	
	public static String cropAndSaveImage(String directoryOrigin, String directoryDestiny, String originalImage, String type, int x,
			int y, int w, int h) {
		String imageName = null;
		Image orig = null;

		File diretorio = new File(directoryOrigin);   
		if (!diretorio.exists()) {  
		   diretorio.mkdirs(); 
		} 
		
		try {
			orig = ImageIO.read(new File(directoryOrigin + originalImage));
		} catch (IOException e) {
			e.printStackTrace();
		}

		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		bi.getGraphics().drawImage(orig, 0, 0, w, h, x, y, x + w, y + h, null);

		try {
			imageName = new RandomString(5).nextString()+"."+type;
			int typeInt = bi.getType() == 0? BufferedImage.TYPE_INT_ARGB : bi.getType();
			BufferedImage biResizeImagePng = resizeImage(bi, typeInt);
			ImageIO.write(biResizeImagePng, "png", new File(directoryDestiny + imageName));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return imageName;
	}
	
	
	
	public static String cropAndSaveImage(String directory, String originalImage, String type, int x, int y, int w, int h) {
		String imageName = null;
		Image orig = null;

		File diretorio = new File(directory);
		if (!diretorio.exists()) {
			diretorio.mkdirs();
		}

		try {
			orig = ImageIO.read(new File(originalImage));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		bi.getGraphics().drawImage(orig, 0, 0, w, h, x, y, x + w, y + h, null);

		try {
			imageName = new RandomString(5).nextString() + "." + type;
			int typeInt = bi.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : bi.getType();
			BufferedImage biResizeImagePng = resizeImage(bi, typeInt);
			ImageIO.write(biResizeImagePng, "png", new File(directory + imageName));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return imageName;
	}
	
	public static byte[] imageToBytes(String inFile) throws IOException{   
        InputStream is = null;    
        byte[] buffer = null;   
        is = new FileInputStream(inFile);   
        buffer = new byte[is.available()];   
        is.read(buffer);   
        is.close();   
        return buffer;  
 }
	
	public static String writeFileOnDisc(String dir, byte[] readBytes)
			throws FileNotFoundException, IOException {
		
		File directory = new File(dir);
		if (!directory.exists()) {
			directory.mkdirs();
		}
		
		String imageName = new RandomString(5).nextString() + ".jpg";
		File file = new File(dir + imageName);
		FileOutputStream out = new FileOutputStream(file, true);
		out.write(readBytes);
		out.flush();
		out.close();
		
		return imageName;
	}
	
	private static BufferedImage resizeImage(BufferedImage originalImage, int type){
		BufferedImage resizedImage = new BufferedImage(CloudletTransmitter.IMG_WIDTH, CloudletTransmitter.IMG_HEIGHT, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, CloudletTransmitter.IMG_WIDTH, CloudletTransmitter.IMG_HEIGHT, null);
		g.dispose();
	 
		return resizedImage;
	 }
	
	public String readDataAsText() {
	    return this.readDataAsText(serverSocket);
	}
	
	public String readDataAsText(Socket socket) {
	    try {
	    InputStream is = socket.getInputStream();
	    ObjectInputStream ois = new ObjectInputStream(is);
	     return (String)ois.readObject();
	    } catch(Exception e) {
	    	e.printStackTrace();
	        return null;
	    }
	}
	
	public byte[] readBytes() throws IOException {
		InputStream in = serverSocket.getInputStream();
		DataInputStream dis = new DataInputStream(in);

		int len = dis.readInt();
		byte[] dataToStoreTheImage = new byte[len];

		if (len > 0) {
			dis.readFully(dataToStoreTheImage);
		}

		return dataToStoreTheImage;
	}
	
	public static double calculateCost(PingResult pingResult, double minLoad, double maxLoad,  double minRtt, double maxRtt) throws FileNotFoundException, IOException{
		double normalizedLoad = (maxLoad - minLoad) == 0 ? 1 : (pingResult.getLoadPercentage() - minLoad) / (maxLoad - minLoad);
		double normalizedRtt =  (maxRtt - minRtt) == 0 ? 1 : (pingResult.getRtt() - minRtt) / (maxRtt - minRtt);
		Configuration config = Configuration.getConfiguration();
		
		return  ((config.getWeightLoad() * normalizedLoad) + (config.getWeightRTT() * normalizedRtt));
	}
	
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Socket getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(Socket serverSocket) {
		CloudletUtils.serverSocket = serverSocket;
	}  

	
}