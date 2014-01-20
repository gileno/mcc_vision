package servers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import utils.FaceDetectionUtils;
import utils.FaceRecognitionUtils;
import utils.RandomString;
import utils.RecognitionResult;
import utils.TCPServerUtils;
import utils.Utils;

public class TCPServerDetectionAndRecognition implements Runnable {

	protected String IMAGES_LIST_FOR_RECOGNITION_FILE = DETECTED_IMAGES_DIR + "imagesList.txt";
	protected String ImageNameAllPath;
	FaceRecognitionUtils faceRecognition;
	public static int IMG_WIDTH = 200;
	public static int IMG_HEIGHT = 200;

	protected Socket connectionSocket;
	private ServerSocket welcomeSocket;
	private static String ROOT_PATH = ""; // TO DEPLOY
	public static String RECEIVED_IMAGES_PATH = ROOT_PATH + "received_images/";
	public static String DETECTED_IMAGES_DIR = ROOT_PATH + "detected_images/";

	TCPServerUtils tcpServerUtils;
	protected String originalImageName;
	protected static List<String> imageNames = new ArrayList<String>();

	static int PORT_SERVER = 123;

	public static void main(String[] args) {
		TCPServerDetectionAndRecognition c = new TCPServerDetectionAndRecognition(PORT_SERVER);
		new Thread(c).start();
	}


	public TCPServerDetectionAndRecognition(){}
	
	public TCPServerDetectionAndRecognition(int port) {
		try {
			welcomeSocket = new ServerSocket(port);
			tcpServerUtils = new TCPServerUtils();
			faceRecognition = new FaceRecognitionUtils();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		System.out.println("DETECTION SERVER - Detection Server started...");
		System.out.println("SERVER_RECOG - Recognition Server started...");

		List<String> imagesNamesList = new ArrayList<String>();

		while (true) {

			try {
				connectionSocket = welcomeSocket.accept();
				tcpServerUtils.setClientSocket(connectionSocket);
				System.out.println("Connection accepted...");

				// RECEIVE IMAGES AND SAVE.....
				byte[] bytesFromClient = Utils.readBytes(connectionSocket);
				System.out.println("DETECTION SERVER - Bytes received: " + bytesFromClient);
				String originalImageName = new RandomString(5).nextString() + ".jpg";
				Utils.writeFileOnDisc(bytesFromClient, RECEIVED_IMAGES_PATH, originalImageName);

				// DETECT, CROP AND SAVE IMAGES
				System.out.println("DETECTION SERVER - DETECTING...");
				imagesNamesList = FaceDetectionUtils.dedectFacesCropAndSave(RECEIVED_IMAGES_PATH + originalImageName);

				// RECOG
				createFileListOfImagesDetected(imagesNamesList);
				recognizeImages();

				imagesNamesList.clear();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	// NÃO CONTE PRA NINGUÉM QUE COPIEI TODOS OS MÉTODOS ABAIXO DAS OUTRAS CLASSES¹¹¹ KKK

	private void sendData(List<RecognitionResult> list) {
		try {
			OutputStream socketStream = connectionSocket.getOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(socketStream);
			String string ="";
			for (RecognitionResult rr : list) {
				string += new String("Id=" + rr.getFaceIndex() + "\nLsd=" + rr.getLeast_squared_distance() + "\nConf.=" + rr.getConfidence() + "\n\n");
			}
			
			objectOutput.writeObject(string);

			objectOutput.close();
			socketStream.close();

		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	protected void recognizeImages() {
		System.out.println("SERVER_RECOG - RECOGNIZING...");
		List<RecognitionResult> recognitionResult = faceRecognition.recognizeFileList(IMAGES_LIST_FOR_RECOGNITION_FILE);

		sendData(recognitionResult);

		System.out.println("SERVER_RECOG - Sending result of recognition to cloudlet...");
	}

	protected void createFileListOfImagesDetected(List<String> imagesList) throws IOException, FileNotFoundException {

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

		int i = 0;
		for (String image : imagesList) {
			out.write((i + " " + image + " " + DETECTED_IMAGES_DIR + image + "\n").getBytes());
			i++;
		}

		out.flush();
		out.close();
	}

}
