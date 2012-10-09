package edu.uw.homographyanalyzer.camera;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import edu.uw.homographyanalyzer.global.GlobalLogger;

/*
 * Base class of all picture-taker activities.
 * This class is meant to standardize the process so
 * a new implementation would agree to the same pattern.
 * 
 * This class is designed as an independent Activity
 * So a user would invoke it through startActivity()
 * and obtain the path of the picture taken through 
 * the corresponding construct. NOTE that returning the 
 * Bitmap immediately DOESN'T work due to Google's bug?
 * (I wasted some time just to figure this out!!)
 * 
 * The path of the resulting image is returned
 * via an Intent passed through onActivityResult()
 * with key name = "IMAGE_PATH"
 * (eg. intent.getExtras().getString("IMAGE_PATH") )
 * 
 */
public abstract class BaseImageTaker extends Activity {
	// Key name of the string that encapsulates the output path
	// to be parsed by onActivityResult()
	public static final String INTENT_RESULT_IMAGE_PATH = "IMAGE_PATH";
	public static final String INTENT_RESULT_IMAGE_URI = "IMAGE_URI";
	
	/*
	 * Quit the activity and returns the image path
	 */
	protected void finishAndReturnImagePath(String path) {
		GlobalLogger.getInstance().logd("Picture taken successfully!");
		Intent wrappedResult = new Intent();
		wrappedResult.putExtra(INTENT_RESULT_IMAGE_PATH, path);
		this.setResult(RESULT_OK, wrappedResult);
		this.finish();
	}
	
	protected void finishAndReturnImagePath(Uri uriPath){
		GlobalLogger.getInstance().logd("Picture taken successfully!");
		Intent wrappedResult = new Intent();
		wrappedResult.putExtra(INTENT_RESULT_IMAGE_URI, uriPath);
		this.setResult(RESULT_OK, wrappedResult);
		this.finish();
	}

	/*
	 * Quit the application but not returning the image path as the capture was
	 * failed
	 */
	protected void finishFail() {
		GlobalLogger.getInstance().logd("Picture taking failed!");
		this.setResult(RESULT_CANCELED);
		finish();
	}
}
