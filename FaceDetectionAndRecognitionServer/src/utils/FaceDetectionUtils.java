package utils;

// FaceDetection.java
// Andrew Davison, July 2013, ad@fivedots.psu.ac.th

/* Use JavaCV to detect faces in an image, and save a marked-faces
 version of the image to OUT_FILE.

 JavaCV location: http://code.google.com/p/javacv/

 Usage:
 run FaceDetection lena.jpg
 */

import static com.googlecode.javacv.cpp.opencv_core.CV_AA;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvLoad;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvRectangle;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_INTER_LINEAR;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvEqualizeHist;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;
import static com.googlecode.javacv.cpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static com.googlecode.javacv.cpp.opencv_objdetect.cvHaarDetectObjects;

import java.util.ArrayList;
import java.util.List;

import servers.TCPServerDetection;
import servers.TCPServerRecognition;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_objdetect;
import com.googlecode.javacv.cpp.opencv_objdetect.CvHaarClassifierCascade;

public class FaceDetectionUtils {
	private static final int SCALE = 2;
	// scaling factor to reduce size of input image

	// cascade definition for face detection
	private static String ROOT_PATH = "./"; //LOCAL
//	private static String ROOT_PATH = ""; //TO DEPLOY
	private static final String CASCADE_FILE = ROOT_PATH+TCPServerRecognition.PATTERN_DATA_PATH+"haarcascade_frontalface_alt.xml";

//	private static final String OUT_FILE = "markedFaces.jpg";
	static String originalImageTest = ROOT_PATH+"group.jpg";
	
	static List<String> imageNames = new ArrayList<String>();
	private static List<CvRect> coordinationsList =  new ArrayList<CvRect>();
	
//	public static void main(String[] args) {
//		dedectFaces(originalImageTest);
//	} 
//	  
	
	private static void detectAndEnframeFaces() {
		int SCALE = 2;
		String CASCADE_FILE ="pattern_data/haarcascade_frontalface_alt.xml";
		String OUT_FILE = "markedFaces.jpg";
		
		IplImage origImg = cvLoadImage("E:/WORKSPACES/mcc_repository/FaceDetectionAndRecognitionServer/modecs.png", 1);
		//IplImage origImg = cvLoadImage(args[0]);
		
		// convert to grayscale
		IplImage grayImg = IplImage.create(origImg.width(),origImg.height(), IPL_DEPTH_8U, 1);
		cvCvtColor(origImg, grayImg, CV_BGR2GRAY);
		
		// scale the grayscale (to speed up face detection)
		IplImage smallImg = IplImage.create(grayImg.width()/SCALE,grayImg.height()/SCALE, IPL_DEPTH_8U, 1);
		cvResize(grayImg, smallImg, CV_INTER_LINEAR);

		// equalize the small grayscale
		IplImage equImg = IplImage.create(smallImg.width(),smallImg.height(), IPL_DEPTH_8U, 1);
		cvEqualizeHist(smallImg, equImg);

		// create temp storage, used during object detection
		CvMemStorage storage = CvMemStorage.create();

		// instantiate a classifier cascade for face detection

		CvHaarClassifierCascade cascade =new CvHaarClassifierCascade(cvLoad(CASCADE_FILE));
		System.out.println("Detecting faces...");

		CvSeq faces = cvHaarDetectObjects(equImg, cascade, storage,1.1, 3, CV_HAAR_DO_CANNY_PRUNING);

		cvClearMemStorage(storage);

		// draw thick yellow rectangles around all the faces
		int total = faces.total();
		System.out.println("Found " + total + " face(s)");

		for (int i = 0; i < total; i++) {

		        CvRect r = new CvRect(cvGetSeqElem(faces, i));
		        cvRectangle(origImg, cvPoint( r.x()*SCALE, r.y()*SCALE ),cvPoint( (r.x() + r.width())*SCALE,(r.y() + r.height())*SCALE ),CvScalar.YELLOW, 2, CV_AA, 0);

		        String strRect = String.format("CvRect(%d,%d,%d,%d)", r.x(), r.y(), r.width(), r.height());
		        
		        System.out.println(strRect);
		        //undo image scaling when calculating rect coordinates
		}
		
		if (total > 0) {
		        System.out.println("Saving marked-faces version of " + " in " + OUT_FILE);

		        cvSaveImage(OUT_FILE, origImg);
		}
	}
	
	
	public static List<String> dedectFacesCropAndSave(String originalImage) {

		// preload the opencv_objdetect module to work around a known bug
		Loader.load(opencv_objdetect.class);

		// load an image
		System.out.println("Loading image from: " + originalImage);
		// IplImage origImg = cvLoadImage(args[0]);

		IplImage origImg = cvLoadImage(originalImage);

		// convert to grayscale
		CvSize cvGetSize = cvGetSize(origImg);
		IplImage grayImg = cvCreateImage(cvGetSize, IPL_DEPTH_8U, 1);
		cvCvtColor(origImg, grayImg, CV_BGR2GRAY);

		// scale the grayscale (to speed up face detection)
		IplImage smallImg = IplImage.create(grayImg.width() / SCALE,
				grayImg.height() / SCALE, IPL_DEPTH_8U, 1);
		cvResize(grayImg, smallImg, CV_INTER_LINEAR);

		// equalize the small grayscale
		cvEqualizeHist(smallImg, smallImg);

		// create temp storage, used during object detection
		CvMemStorage storage = CvMemStorage.create();

		// instantiate a classifier cascade for face detection
		
		System.out.println("CASCADE_FILE " + CASCADE_FILE);
		CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(
				cvLoad(CASCADE_FILE));
		System.out.println("Detecting faces...");
		CvSeq faces = cvHaarDetectObjects(smallImg, cascade, storage, 1.1, 3,
				CV_HAAR_DO_CANNY_PRUNING);
		// CV_HAAR_DO_ROUGH_SEARCH);
		// 0);
		cvClearMemStorage(storage);

		// iterate over the faces and draw yellow rectangles around them
		int total = faces.total();
		System.out.println("Found " + total + " face(s)");
		
		if (total == 0) {
			return imageNames;
		}
		
		imageNames.clear();
		
		//FIXANDO A LARGURA E ALTURA DO RETANGULO
		int WIDTH = 0;
		int HEIGHT = 0;
		
		CvRect r =null;
		for (int i = 0; i < total; i++) {
			r = new CvRect(cvGetSeqElem(faces, i));
			if (r.width() > WIDTH) {
				WIDTH = r.width();
			}
			if (r.height() > HEIGHT) {
				HEIGHT = r.height();
			}
		}
		
		for (int i = 0; i < total; i++) {
			r = new CvRect(cvGetSeqElem(faces, i));
			int pointX = (r.x() + WIDTH) * SCALE;
			int pointY = (r.y() + HEIGHT) * SCALE;

			cvRectangle(origImg, cvPoint(r.x() * SCALE, r.y() * SCALE),
					cvPoint(pointX, pointY), CvScalar.YELLOW, 6, CV_AA, 0);

			imageNames.add(
					Utils.cropAndSaveImage(TCPServerRecognition.DETECTED_IMAGES_DIR, originalImage, "jpg", r.x()
					* SCALE, r.y() * SCALE, WIDTH * SCALE, HEIGHT
					* SCALE)
					);
		}
		
		return imageNames;
	}
	
	public static List<CvRect> dedectFacesOnly(String originalImage) {

		// preload the opencv_objdetect module to work around a known bug
		Loader.load(opencv_objdetect.class);

		// load an image
		System.out.println("Loading image from: " + originalImage);
		// IplImage origImg = cvLoadImage(args[0]);

		IplImage origImg = cvLoadImage(TCPServerDetection.RECEIVED_IMAGES_PATH + originalImage);

		// convert to grayscale
		CvSize cvGetSize = cvGetSize(origImg);
		IplImage grayImg = cvCreateImage(cvGetSize, IPL_DEPTH_8U, 1);
		cvCvtColor(origImg, grayImg, CV_BGR2GRAY);

		// scale the grayscale (to speed up face detection)
		IplImage smallImg = IplImage.create(grayImg.width() / SCALE,
				grayImg.height() / SCALE, IPL_DEPTH_8U, 1);
		cvResize(grayImg, smallImg, CV_INTER_LINEAR);

		// equalize the small grayscale
		cvEqualizeHist(smallImg, smallImg);

		// create temp storage, used during object detection
		CvMemStorage storage = CvMemStorage.create();

		// instantiate a classifier cascade for face detection
		
		System.out.println("CASCADE_FILE " + CASCADE_FILE);
		CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(
				cvLoad(CASCADE_FILE));
		System.out.println("Detecting faces...");
		CvSeq faces = cvHaarDetectObjects(smallImg, cascade, storage, 1.1, 3,
				CV_HAAR_DO_CANNY_PRUNING);
		// CV_HAAR_DO_ROUGH_SEARCH);
		// 0);
		cvClearMemStorage(storage);

		// iterate over the faces and draw yellow rectangles around them
		int total = faces.total();
		System.out.println("Found " + total + " face(s)");
		
		if (total == 0) {
			return coordinationsList;
		}
		
		coordinationsList.clear();
		
		//FIXANDO A LARGURA E ALTURA DO RETANGULO
		int WIDTH = 0;
		int HEIGHT = 0;
		
		CvRect r =null;
		for (int i = 0; i < total; i++) {
			r = new CvRect(cvGetSeqElem(faces, i));
			if (r.width() > WIDTH) {
				WIDTH = r.width();
			}
			if (r.height() > HEIGHT) {
				HEIGHT = r.height();
			}
		}
		
		for (int i = 0; i < total; i++) {
			r = new CvRect(cvGetSeqElem(faces, i));
			coordinationsList.add(r);
		}
		
		return coordinationsList;
	}

} // end of FaceDetection class
