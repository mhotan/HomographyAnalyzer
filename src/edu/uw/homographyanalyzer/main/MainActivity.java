package edu.uw.homographyanalyzer.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SlidingDrawer;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.homographyanalyzer.R;

import edu.uw.homographyanalyzer.camera.BaseImageTaker;
import edu.uw.homographyanalyzer.camera.ExternalApplication;
import edu.uw.homographyanalyzer.global.GlobalLogger;
import edu.uw.homographyanalyzer.global.LoggerInterface;
import edu.uw.homographyanalyzer.quicktransform.TransformInfo;
import edu.uw.homographyanalyzer.quicktransform.TransformationDemoActivity;
import edu.uw.homographyanalyzer.reusable.ComputerVision;
import edu.uw.homographyanalyzer.reusable.ComputerVisionCallback;
import edu.uw.homographyanalyzer.reusable.TransformationBuilder;

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
ComputerVisionCallback, TransformationBuilder.TransformationStateListener, OnClickListener, 
OnItemSelectedListener, OnSeekBarChangeListener {

	private final Context This = this;

	// Logging tag
	private static final String TAG = "HomographyAnalyzer";
	// CV library ready to be used
	private boolean mCVLibraryInitialized = false;

	public static final String EXTRA_POSITION = TAG + "_POSITION";
	public static final String BASE_URI_EXTRA = TAG + "BASE_URI";
	public static final String QUERY_URI_EXTRA = TAG + "QUERY_URI";

	// adapter to display images
	private OrganizedImageSelectionAdapter mImageAdapter;

	//Thumbnails of homography images
	private Gallery mGallery;
	private Button transformButton;
	private ImageButton searchButton;

	// Selectors for tranform paramaters
	private Spinner featureDetectorSpinner, homoMethodSpinner;
	private SeekBar threshhold;

	// Transformation Builder
	private TransformationBuilder tranBuilder;

	//Computer vision
	private ComputerVision mCV;

	// text to presented above seekbar
	private TextView mSeekbarText;
	private TextView mExpandedImageText;

	private ImageView expandedImage;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// This needs to be done first because many other components
		// depend on this global logger
		new GlobalLogger(this);
		mCV = new ComputerVision(this, this, this);
		mCV.initializeService();

		expandedImage = (ImageView) findViewById(R.id.exp_image);
		expandedImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

		transformButton = (Button) findViewById(R.id.transformButton);
		transformButton.setEnabled(false);
		transformButton.setOnClickListener(this);

		searchButton = (ImageButton) findViewById(R.id.imageRetrieverButton);
		searchButton.setOnClickListener(this);

		mSeekbarText = (TextView) findViewById(R.id.threshhold_seekbar_textview);
		mExpandedImageText = (TextView) findViewById(R.id.exp_image_text);
		// List of features
		featureDetectorSpinner = (Spinner) findViewById(R.id.features);

		// list of methods
		homoMethodSpinner = (Spinner) findViewById(R.id.homography_methods);

		//Threshold chooser
		threshhold = (SeekBar) findViewById(R.id.threshold_seekbar);
		threshhold.setMax(TransformationBuilder.RANSAC_THRESHHOLD_MAX);
		threshhold.setOnSeekBarChangeListener(this);

		//		(SlidingDrawer) findViewById(R.id.slidingDrawer);

		// Adpater for managing images to be displayed in gallery
		mImageAdapter = new OrganizedImageSelectionAdapter(this);

		// Gallery for displaying images
		mGallery = (Gallery) findViewById(R.id.gallery);
		mGallery.setAdapter(mImageAdapter);

		mGallery.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> listView, View view,
					int position, long id) {

				boolean isRefOROther = position == 1 || position ==0;
				final int pos = position;
				// If the position is 0,1 or reference or other
				// and if default image
				// Ask to query
				if (isRefOROther && mImageAdapter.isDefaultImage(position)){
					searchButton.setEnabled(false);
					searchButton.setVisibility(ImageButton.INVISIBLE);
					// If default image => Search needs to occur
					getImageForPosition(pos);
				} 

				else
				{ 
					// if reference or other image but currently
					// has another image inside it
					if (isRefOROther){
						searchButton.setEnabled(true);
						searchButton.setVisibility(ImageButton.VISIBLE);
						searchButton.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								getImageForPosition(pos);
							}
						});
					} // Now we need to bring up the image in expanded view
					Bitmap image = 	mImageAdapter.getImage(position);
					if (image != null){
						expandedImage.setImageBitmap(image);
						String message = mImageAdapter.getTitle(image);
						mExpandedImageText.setText(message);
					}
				}
				return false;
			}
		});
	}

	private void initializeFeatures(Spinner s){
		List<String> list = new ArrayList<String>(TransformationBuilder.getSupportedFeatureDetectorNames());
		ArrayAdapter<String> dataAdapter = 
				new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(dataAdapter);

		//Set the default value
		String detector = tranBuilder.getCurrentFeatureDetectorName();
		int num = s.getCount();
		for (int i = 0; i < num; ++i){
			if (s.getItemAtPosition(i).equals(detector)) {
				s.setSelection(i);
				break;
			}
		}

		s.setOnItemSelectedListener(this);
	}

	private void initializeMethods(Spinner s){
		List<String> list = new ArrayList<String>(TransformationBuilder.getHomographyMethodNames());
		ArrayAdapter<String> dataAdapter = 
				new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(dataAdapter);

		//Set default value
		String method = tranBuilder.getCurrentHomographyMethod();
		int num = s.getCount();
		for (int i = 0; i < num; ++i){
			if (s.getItemAtPosition(i).equals(method)) {
				s.setSelection(i);
				break;
			}
		}

		s.setOnItemSelectedListener(this);
	}

	/**
	 * Starts activity to obtain image for further processing
	 * Img address is set to 
	 * @param id
	 */
	private void getImageForPosition(int pos){
		logd("Calling camera intent"); 
		Intent i = new Intent(this, ExternalApplication.class); 
		startActivityForResult(i, pos);
	} 

	// Called when a started intent returns
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		String message = null;
		if (resultCode != RESULT_OK) {
			Toast t = Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT);
			t.show();
			return;
		}

		// Assign the view to change based on who sent the request
		// There are two positions to the that can be found
		int position = requestCode;

		//Check if file path or uri image source
		String filePath = data.getExtras().getString(
				BaseImageTaker.INTENT_RESULT_IMAGE_PATH);

		// Depending on how image was obtained,
		// obtain a Bitmap image of object
		// Getting source of the image

		Bitmap image = null;
		//Decode File path
		if (filePath == null) {
			//query the data
			Uri pickedUri = data.getExtras().getParcelable(
					BaseImageTaker.INTENT_RESULT_IMAGE_URI);
			image = getBitmapFromURIviaInputStream(pickedUri);

		} else{
			int targetWidth = 600;
			int targetHeight = 400;

			BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
			bmpOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filePath, bmpOptions);
			int currHeight = bmpOptions.outHeight;
			int currWidth = bmpOptions.outWidth;

			int sampleSize = 1;
			{
				//use either width or height
				if (currWidth>currHeight)
					sampleSize = Math.round((float)currHeight/(float)targetHeight);
				else
					sampleSize = Math.round((float)currWidth/(float)targetWidth);
			}

			bmpOptions.inSampleSize = sampleSize;
			bmpOptions.inJustDecodeBounds = false;
			//decode the file with restricted sizee
			image = BitmapFactory.decodeFile(filePath, bmpOptions);
		}

		//Update the adapter and transform builder
		if (image == null){
			message = "Null image cannot display";
			Log.e(TAG, message);
			return;
		} else {
			mImageAdapter.setImage(image, position);
			if (position == 0)
				tranBuilder.setReferenceImage(image);
			else if (position == 1)
				tranBuilder.setOtherImage(image);
		}
	}

	/**
	 * 
	 * @param uri
	 * @return
	 */
	private Bitmap getBitmapFromURIviaInputStream(Uri uri){

		Bitmap image = null;
		try {
			int targetWidth = 600;
			int targetHeight = 400;

			BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
			bmpOptions.inJustDecodeBounds = true;
			InputStream is = getContentResolver().openInputStream(uri);
			BitmapFactory.decodeStream(is,null, bmpOptions);
			int currHeight = bmpOptions.outHeight;
			int currWidth = bmpOptions.outWidth;

			is.close();
			InputStream is2 = getContentResolver().openInputStream(uri);

			int sampleSize = 1;
			{
				//use either width or height
				if (currWidth>currHeight)
					sampleSize = Math.round((float)currHeight/(float)targetHeight);
				else
					sampleSize = Math.round((float)currWidth/(float)targetWidth);
			}

			bmpOptions.inSampleSize = sampleSize;
			bmpOptions.inJustDecodeBounds = false;
			//decode the file with restricted sizee
			image = BitmapFactory.decodeStream(is2, null, bmpOptions);
			is2.close();
			return image;
		} catch (IOException e) {
			Log.e(TAG, "Exception when reading: "+ e);
			return image;
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
		tranBuilder = new TransformationBuilder(mCV);
		initializeFeatures(featureDetectorSpinner);
		initializeMethods(homoMethodSpinner);
		tranBuilder.setTransformationStateListener(this);
		mCVLibraryInitialized = true;
	}

	@Override
	public void onInitServiceFailed() {
		// TODO Auto-generated method stub
		logd("onInitServiceFailed()");
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

	/**
	 *Starts new Activity to display Homography transformation
	 */
	@Override
	public void onClick(View v) {
		if (v.getId() == transformButton.getId()){
			transformButton.setEnabled(false);

			// Remove all but the two base imagess
			Pair<Bitmap, Bitmap> imagesToAdd = tranBuilder.getWarpedImages();
			if (imagesToAdd == null){
				//Notify User
				mExpandedImageText.setText("Tranformation is not ready just Yet");
				return;
			} else
				mExpandedImageText.setText(R.string.show_exp_image);

			mImageAdapter.setWarpedImage(imagesToAdd.first);
			mImageAdapter.setInvertedWarpedImage(imagesToAdd.second);

			// Start transformation process
			transformButton.setEnabled(true);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> spinner, View arg1, int pos,
			long arg3) {

		Object o = spinner.getItemAtPosition(pos);
		String request = (String)o;

		String message = "NOTHING";
		if (spinner == featureDetectorSpinner) {
			message = "Feature Spinner";
			tranBuilder.setFeatureDetector(request);
		} else if (spinner == homoMethodSpinner) {
			message = "Homography Method Spinner";
			tranBuilder.setHomograhyMethod(request);
		}

		Log.i(TAG, "Spinner item selected " + message + " with item " + request);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		int resourceId = arg0.getId();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		int nThreshhold = Math.max(1, seekBar.getProgress());
		Log.i(TAG, "STop tracking at pos: " + nThreshhold);
		mSeekbarText.setText("Threshhold: " + nThreshhold);
		tranBuilder.setRansacThreshhold(nThreshhold);
	}

	@Override
	public void OnHomographyStored(TransformInfo storage) {
		// Ready to show display reset text
		mExpandedImageText.setText(R.string.show_exp_image);
		boolean ready = mCVLibraryInitialized;
		transformButton.setEnabled(ready);
	}

	@Override
	public void OnNoHomographyFound() {
		transformButton.setEnabled(false);
	}

	@Override
	public void OnKeypointsFoundForReference(Mat image) {
		Bitmap disp = Bitmap.createBitmap(image.cols(), image.rows(),
				Bitmap.Config.ARGB_8888); // Android uses ARGB_8888
		Utils.matToBitmap(image, disp);
		mImageAdapter.setReferenceKeyPointImage(disp);
	}

	@Override
	public void OnKeypointsFoundForOther(Mat image) {
		Bitmap disp = Bitmap.createBitmap(image.cols(), image.rows(),
				Bitmap.Config.ARGB_8888); // Android uses ARGB_8888
		Utils.matToBitmap(image, disp);
		mImageAdapter.setOtherKeyPointImage(disp);
	}
}
