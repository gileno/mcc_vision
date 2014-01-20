package com.main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.communication.TCPClientAndroid;
import com.example.R;
import com.utils.ManageFile;

/**
 * Unique activity class of the app, it just load an image and send it to the server. 
 * @author airton
 *
 */
public class MainImageActivity extends Activity {
	private ImageView iv;
	private Button b;
	private TextView tv;
	private ManageFile manageFile;
	private String PATH = Environment.getExternalStorageDirectory().toString();
	private String IMAGE_DIR = "imagesDB";
	private String IMAGE_NAME = "eu.jpg";
	private String IMAGE_WITH_ALL_PATH = PATH + "/" + IMAGE_DIR + "/"
			+ IMAGE_NAME;
	private long startTime;
	private long endTime;
	private long totalTime;
	private String HOST = "192.168.20.4";//notebook // Do not forget to update the ip at the CloudletTransmitterAbstract too
	//	private String HOST = "192.168.25.31";//notebook // Do not forget to update the ip at the CloudletTransmitterAbstract too
//	private String HOST = "localhost";
//	private String HOST = "54.221.10.143";//amazon
	static int PORT_CLOUDLET_SERVER = 6790; // versao que tem o rabbit
//	static int PORT_CLOUDLET_SERVER = 123; // versao que unica
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_image);

		manageFile = new ManageFile(null);
		// get the ImageView
		iv = (ImageView) findViewById(R.id.myimage);
		b = (Button) findViewById(R.id.button1);
		tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());
		// display the image

		// iv.setBackgroundDrawable(getResources().getDrawable(R.drawable.group));
		iv.setBackgroundDrawable(Drawable.createFromPath(IMAGE_WITH_ALL_PATH));
		
		manageFile.clearFile();

		
		sendConstantly();
		
		
//		sendOncePerTime();

	}

	private void sendOncePerTime() {
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				b.setClickable(false);
				tv.setText("");
				new DownloadAndSaveTask().execute("");
			}
		});
	}

	private void sendConstantly() {
		for (int i = 0; i < 50; i++) {
			new DownloadAndSaveTask().execute("");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private class DownloadAndSaveTask extends AsyncTask<String, Void, String> {
		private final ProgressDialog d = new ProgressDialog(
				MainImageActivity.this);

		@Override
		protected void onPreExecute() {

			super.onPreExecute();
			d.setMessage("Uploading image...Please wait!!");
			d.setCancelable(true);
			d.show();
		}

		@Override
		protected String doInBackground(String... params) {
			String result = "No result";
			try {
				startTime = System.currentTimeMillis();

				TCPClientAndroid tcpClient = new TCPClientAndroid(HOST, PORT_CLOUDLET_SERVER);
		
				result = tcpClient.sendPicture(IMAGE_WITH_ALL_PATH);
				
				endTime = System.currentTimeMillis();
				
				totalTime = endTime - startTime;
				manageFile.WriteFile(String.valueOf(totalTime));
			
			} catch (UnknownHostException e) {
				return e.getMessage();
				// e.printStackTrace();
			} catch (IOException e) {
				return e.getMessage();
				// e.printStackTrace();
			} catch (Exception e) {
				return e.getMessage();
				// e.printStackTrace();
			}
			return String.valueOf(result);
		}

		@Override
		protected void onPostExecute(final String result) {
			super.onPostExecute(result);

			d.dismiss();
			b.setClickable(true);

			tv.setText("Result: \nTime: "+totalTime +"Ms\n\n"+result +"\n");

		} // end postexecute method
		
		
		

		public Uri getImageUri(Context inContext, Bitmap inImage) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
			String path = Images.Media.insertImage(
					inContext.getContentResolver(), inImage, "Title", null);
			return Uri.parse(path);
		}

		public String getRealPathFromURI(Uri uri) {
			Cursor cursor = getContentResolver().query(uri, null, null, null,
					null);
			cursor.moveToFirst();
			int idx = cursor
					.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
			return cursor.getString(idx);
		}
	} // end asynctask class
} // end class