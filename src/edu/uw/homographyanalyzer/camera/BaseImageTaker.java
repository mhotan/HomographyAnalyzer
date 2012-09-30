package edu.uw.homographyanalyzer.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;

/*
 * Base class of all picture-taker activities.
 * This class is meant to standardize the process so
 * a new implementation would agree to the same pattern.
 * 
 * This class is designed as an independent Activity
 * So a user would invoke it through startActivity()
 * and obtain the Bitmap result through the corresponding construct.
 * 
 * The resulting Bitmap is returned via an Intent
 * as a parcelable bitmap keyword-ed RESULT_BITMAP
 * 
 */
public abstract class BaseImageTaker extends Activity {
	// The resulting bitmap of the picture taken
	private Bitmap mResult;
	public static final String RESULT_BITMAP = "RESULT_BITMAP";

	protected Bitmap getBitmap() {
		if (mResult == null) {
			throw new RuntimeException("The returned bitmap is null!");
		}
		return mResult;
	}

	protected void setBitmap(Bitmap bmp) {
		mResult = bmp;
	}
	
	/*
	 * Quit the activity and returns the bitmap
	 */
	protected void finishAndReturnBitmap(){
		// Return the bitmap via intent
		Intent wrappedResult = new Intent();
		wrappedResult.putExtra(RESULT_BITMAP, getBitmap());
		setResult(RESULT_OK, wrappedResult);
		finish();
	}
	
	/*
	 * Quit the application but not returning the bitmap
	 * as the capture was failed
	 */
	protected void finishFail(){
		setResult(RESULT_CANCELED);
		finish();
	}
}
