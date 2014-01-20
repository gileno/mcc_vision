package utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.imageio.ImageIO;

import servers.TCPServerRecognition;

import com.googlecode.javacv.cpp.opencv_core.CvRect;

public class Utils {

	static CropImage ci = new CropImage();
	static Graphics2D bGr;

	/**
	 * Converts a given Image into a BufferedImage
	 * 
	 * @param img
	 *            The Image to be converted
	 * @return The converted BufferedImage
	 */
	public static BufferedImage toBufferedImage(Image img, int w, int h) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

		// Draw the image on to the buffered image
		bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
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

	private static BufferedImage resizeImage(BufferedImage originalImage, int type) {
		BufferedImage resizedImage = new BufferedImage(TCPServerRecognition.IMG_WIDTH, TCPServerRecognition.IMG_HEIGHT, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, TCPServerRecognition.IMG_WIDTH, TCPServerRecognition.IMG_HEIGHT, null);
		g.dispose();

		return resizedImage;
	}

	public static void writeFileOnDisc(byte[] readBytes, String dir, String originalImage) throws FileNotFoundException, IOException {

		File diretorio = new File(dir);
		if (!diretorio.exists()) {
			diretorio.mkdirs();
		}

		File file = new File(dir + originalImage);
		FileOutputStream out = new FileOutputStream(file, true);
		out.write(readBytes);
		out.flush();
		out.close();
	}

	public static byte[] readBytes(Socket connectionSocket) throws IOException {
		InputStream in = connectionSocket.getInputStream();
		DataInputStream dis = new DataInputStream(in);

		int len = dis.readInt();
		byte[] dataToStoreTheImage = new byte[len];

		if (len > 0) {
			dis.readFully(dataToStoreTheImage);
		}

		return dataToStoreTheImage;
	}

	public static void sendDataBack(String text, Socket connectionSocket) {
		try {
			OutputStream socketStream = connectionSocket.getOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(socketStream);
			objectOutput.writeObject(text);

			objectOutput.close();

			socketStream.close();

		} catch (Exception e) {
			System.out.println("SERVER DETEC OR RECOG" + e.toString());
		}
	}

}
