package edu.uw.homographyanalyzer.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import edu.uw.homographyanalyzer.global.GlobalLogger;

/*
 * Take a picture using an external application
 */
public class ExternalApplication extends BaseImageTaker {
	
	private String mTempFilePath = "/mnt/sdcard/download/temp.bmp";
	private final static int ACTIVITY_RESULT = 1;

	// Temporary place to store the captured image
	private File mTemporaryFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTempFilePath = getExternalCacheDir() + "temp.bmp";
		
		mTemporaryFile = new File(mTempFilePath);
		
		if (!(mTemporaryFile.canWrite() && mTemporaryFile.canRead())) {
			GlobalLogger.getInstance()
					.globalLoge("ExternalApplication.java: mTemporary file is unwriteable/unreadable!");
			throw new RuntimeException("ExternalApplication.java: mTemporary file is unwriteable/unreadable!");
		}
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
				// Set the return value
				setBitmap(Media.getBitmap(getContentResolver(),
						Uri.fromFile(mTemporaryFile)));
				// Return and quit
				finishAndReturnBitmap();
			} catch (FileNotFoundException e) {
				GlobalLogger.getInstance().globalLoge(
						"ExternalApplication.java: temporary file not found!");
				e.printStackTrace();
			} catch (IOException e) {
				GlobalLogger.getInstance().globalLoge(
						"ExternalApplication.java: IO Exception!");
				e.printStackTrace();
			}
		} else {
			finishFail();
		}
	}

}
