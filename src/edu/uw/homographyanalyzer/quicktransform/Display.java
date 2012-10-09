package edu.uw.homographyanalyzer.quicktransform;

import com.example.homographyanalyzer.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

public class Display extends Activity {
	private Bitmap mDisplayImage = null;
	private static final String TAG = "DISPLAY_ACTIVITY";
	
	public static final String DATA_BITMAP_INDEX = "bitmapIdx";
	
	private ImageView mImageView;
	//Identify which bitmap to parse from Data singleton
	private int mBitmapIndex;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.display);
		
		Data data = Data.getInstance();
		mBitmapIndex = getIntent().getIntExtra(DATA_BITMAP_INDEX, -1);
		
		if(mBitmapIndex == -1){
			loge("Must passed bitmap index to Display activity!");
			finish();
		}
		
		mDisplayImage = data.bmp[mBitmapIndex];
		if(mDisplayImage == null){
			loge("No image passed!");
			finish();
		}
		else{
			initWidgets();
			//logd("Image density: " + mDisplayImage.getDensity());
			DisplayImage();
		}
	}
	
	private void DisplayImage(){
		mImageView.setImageBitmap(mDisplayImage);
	}
	
	private void initWidgets(){
		mImageView = (ImageView) findViewById(R.id.imgDisplay);
	}
	
	private void logd(String str){
		Log.d(TAG,str);
	}
	
	private void loge(String str){
		Log.e(TAG, str);
	}
}
