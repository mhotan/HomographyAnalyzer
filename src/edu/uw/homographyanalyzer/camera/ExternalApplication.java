package edu.uw.homographyanalyzer.camera;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import edu.uw.homographyanalyzer.global.GlobalLogger;

/*
 * Take a picture using an external application.
 * This class extends BaseImageTaker. Take a look at the class description
 * to get an idea on how to use it
 */
public class ExternalApplication extends BaseImageTaker {

	private String mTempFilePath;
	private String mTempFileName;
	private final static int ACTIVITY_RESULT = 1;

	// Temporary place to store the captured image
	private File mTemporaryFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Default directory to store pictures
		mTempFilePath = getExternalFilesDir(null).getAbsolutePath();
		mTempFileName = System.currentTimeMillis()+ ".bmp";
		// Time stamp the image name
		mTemporaryFile = new File(mTempFilePath, mTempFileName);
		GlobalLogger.getInstance().logd(
				"Camera directory: " + mTemporaryFile.getAbsolutePath());
		// Start camera intent
		takePicture();
	}

	/*
	 * Runs an external intent to take a picture.
	 */
	private void takePicture() {
		Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTemporaryFile));
		startActivityForResult(i, ACTIVITY_RESULT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Picture is taken!
		if (resultCode == RESULT_OK) {
			try {
				if(!mTemporaryFile.exists())
					throw new FileNotFoundException();
				// Return and quit
				finishAndReturnImagePath(mTemporaryFile.getAbsolutePath());
			} catch (FileNotFoundException e) {
				GlobalLogger.getInstance().loge(
						"ExternalApplication.java: temporary file not found!");
				e.printStackTrace();
			}
		} else {
			finishFail();
		}
	}

}
