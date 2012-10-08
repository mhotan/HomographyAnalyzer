package edu.uw.homographyanalyzer.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.example.homographyanalyzer.R;

import edu.uw.homographyanalyzer.camera.BaseImageTaker;
import edu.uw.homographyanalyzer.global.GlobalLogger;
import edu.uw.homographyanalyzer.global.LoggerInterface;
import edu.uw.homographyanalyzer.reusable.ComputerVision;
import edu.uw.homographyanalyzer.reusable.ComputerVisionCallback;

/*
 * Sample Activity meant to demonstrate how to use the implemented
 *  wrapper for related OpenCV library and many other building blocks. 
 *  
 * UI Description:
 * This is essentially an activity where we can take 2 pictures,
 * reference and target. 
 * 
 * We then can do a homography transformation between the two images
 * with a ransac_treshold value and other possible variables that we specify.
 * The application would call a file explorer intent to open up the 
 * folder where the images and results were placed. 
 * The application also generates a textual file that 
 * represent the number of keypoints, treshold, inliers, etc. 
 * 
 *   The UI looks as follow:
 *   When the application first runs, there is an option to create a
 *   new workspace. Workspace is a folder where images and the textual
 *   data are placed. The application would be able to parse the textual
 *   info and show the info on the UI.
 *   
 *   Another key design is that the folder would be able to be opened
 *   in the computer so we can investigate it easily. Also the data layout
 *   would be the same as our computer program, so we can easily investigate the result
 *   using our laptop. 
 *   
 *   It's also possible to create the workspace manually using a computer
 *   and put the input image manually then have the phone to parse it.
 *   (eg. we make an input folder and place the reference and other images there
 *   with the appropriate naming convention, then have the phoen to compute
 *   homography for each of the pictures and place it on the output folder.)
 *   
 *   Textual information:
 *   
 *   Folder layout:
 *   /mnt/sdcard/ApplianceReader/[WORKSPACE_NAME]
 *   	input/
 *   	output/
 *   	info.txt
 *   
 *   input -> REFERENCE.png, [NUMBER].png 
 *   where REFERENCE.png is the reference image and [NUMBER].png is 
 *   the target image
 *   
 *   output -> contains the result of each of the transformed image
 *   _[number of inliers]_[NUMBER]_[RESULT/ORIG_W_KP/MATCHED].png
 *   
 *   where: RESULT is the final homography image
 *   		ORIG_W_KP is the original image with keypoint drawn on it
 *   		MATCHED is the original and target put side by side with inliers 
 *   				feature points connect the 2 images.
 */
public class MainActivity extends Activity implements LoggerInterface,
		ComputerVisionCallback {

	// Logging tag
	private static final String TAG = "HomographyAnalyzer";
	// CV library ready to be used
	private boolean mCVLibraryInitialized = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_homography);

		// This needs to be done first because many other components
		// depend on this global logger
		new GlobalLogger(this);
		ComputerVision cv = new ComputerVision(this, this, this);
		cv.initializeService();

		/*
		 * logd("Calling camera intent"); Intent i = new Intent(this,
		 * ExternalApplication.class); startActivityForResult(i, 123);
		 */
	}

	// Called when a started intent returns
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case 123:
			logd("Camera returns!");
			if (resultCode == RESULT_OK)
				logd("Bitmap OKAY: "
						+ data.getExtras().getString(
								BaseImageTaker.INTENT_RESULT_IMAGE_PATH));
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_homography, menu);
		return true;
	}

	@Override
	public void logd(String msg) {
		Log.d(TAG, msg);
	}

	@Override
	public void loge(String msg) {
		Log.e(TAG, msg);
	}

	@Override
	public void onInitServiceFinished() {
		// TODO Auto-generated method stub
		logd("onInitServiceFinished()");
	}

	@Override
	public void onInitServiceFailed() {
		// TODO Auto-generated method stub
		logd("onInitServiceFailed()");
		mCVLibraryInitialized = true;
	}

	@Override
	public void cvLogd(String msg) {
		// TODO Auto-generated method stub
		logd("cvLogd()");
	}

	@Override
	public void cvLogd(String tag, String msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void cvLoge(String msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void cvLoge(String tag, String msg) {
		// TODO Auto-generated method stub

	}

}
