package edu.uw.homographyanalyzer.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SlidingDrawer;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.homographyanalyzer.R;

import edu.uw.homographyanalyzer.camera.BaseImageTaker;
import edu.uw.homographyanalyzer.camera.ExternalApplication;
import edu.uw.homographyanalyzer.global.GlobalLogger;
import edu.uw.homographyanalyzer.global.LoggerInterface;
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

	// For on activity result references
	private static int CNTR = 0;
	private static final int BASE_IMAGE = ++CNTR;
	private static final int QUERY_IMAGE = ++CNTR;

	public static final String EXTRA_POSITION = TAG + "_POSITION";

	public static final String BASE_URI_EXTRA = TAG + "BASE_URI";
	public static final String QUERY_URI_EXTRA = TAG + "QUERY_URI";

	// Context of this for inner class use
	private Context mContext;

	// adapter to display images
	private ImageSelectionAdapter mImageAdapter;

	// if equal 0 or 1 then represent reference and new image respectively
	// if OTHER_IMG_SELECT then other image is selected
	private static int OTHER_IMG_SELECT = -1;

	//Thumbnails of homography images
	private Gallery mGallery;
	//UI elements
	private SlidingDrawer drawer;
	private Button transformButton;
	private ImageButton searchButton;

	// Selectors for tranform paramaters
	private Spinner featureDetectorSpinner, homoMethodSpinner;
	private SeekBar threshhold;

	private TransformationBuilder tranBuilder;

	private ComputerVision mCV;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mContext = this;
		// This needs to be done first because many other components
		// depend on this global logger
		new GlobalLogger(this);
		mCV = new ComputerVision(this, this, this);
		mCV.initializeService();

		transformButton = (Button) findViewById(R.id.transformButton);
		transformButton.setOnClickListener(this);

		searchButton = (ImageButton) findViewById(R.id.imageRetrieverButton);
		searchButton.setOnClickListener(this);

		// List of features
		featureDetectorSpinner = (Spinner) findViewById(R.id.features);

		// list of methods
		homoMethodSpinner = (Spinner) findViewById(R.id.homography_methods);

		//Threshold chooser
		threshhold = (SeekBar) findViewById(R.id.threshold_seekbar);
		threshhold.setMax(TransformationBuilder.RANSAC_THRESHHOLD_MAX);
		threshhold.setOnSeekBarChangeListener(this);
		
		drawer = (SlidingDrawer) findViewById(R.id.slidingDrawer);

		// Adpater for managing images to be displayed in gallery
		mImageAdapter = new ImageSelectionAdapter(this);
		
		// Gallery for displaying images
		mGallery = (Gallery) findViewById(R.id.gallery);
		mGallery.setAdapter(mImageAdapter);

		mGallery.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> listView, View view,
					int position, long id) {

				int selectPosition;

				switch (position) {
				case 0:
				case 1:
					selectPosition = position;
					break;
				default: // Non reference image selected
					selectPosition = OTHER_IMG_SELECT;
				}

				//				CharSequence message = "position:" + selectPosition;
				//				Toast toast = Toast.makeText(This, message, Toast.LENGTH_SHORT);
				//				toast.show();
				//				Log.i(TAG, message.toString());

				// If the image has already been selected 
				// this requires another mechanism to search for images
				if (!mImageAdapter.isDefaultImage(selectPosition)){
					searchButton.setEnabled(true);
					searchButton.setVisibility(ImageButton.VISIBLE);
				} else {
					searchButton.setEnabled(false);
					searchButton.setVisibility(ImageButton.INVISIBLE);

					// If default image => Search needs to occur
					getImageForPosition(selectPosition);
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
		String detector = tranBuilder.getCurrentFeatureDetector();
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

		String imgIdentity = position == 0 
				? "Base Image" : "Other Image";

		//Check if file path or uri image source
		String filePath = data.getExtras().getString(
				BaseImageTaker.INTENT_RESULT_IMAGE_PATH);

		// Depending on how image was obtained,
		// obtain a Bitmap image of object
		Bitmap image = null;
		if (filePath != null) {
			message = imgIdentity + " Found! file: " + filePath;
			image = BitmapFactory.decodeFile(filePath);
		} else {
			Uri uri = data.getExtras().getParcelable(
					BaseImageTaker.INTENT_RESULT_IMAGE_URI);
			//			message = imgIdentity + " Found! URI: " + uri.getPath();
			//			image = BitmapFactory.decodeFile(uri.getPath());
			try {
				image = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
			} catch (FileNotFoundException e) {
				Log.e(TAG, "URI returned by image retreiver not found");
			} catch (IOException e) {
				Log.e(TAG, "IO Exception: " +e);
			}
		}

		if (image == null){
			message = "Null image cannot display";
			Log.e(TAG, message);
			return;
		} else
			mImageAdapter.setImage(image, position);

		Toast t = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		t.show();
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
	 * Allow user to perform transform
	 */
	@Override
	public void OnReadyToTransform() {
		boolean ready = mCVLibraryInitialized;
		transformButton.setEnabled(ready);
	}

	/**
	 * Adjust the User iterface to depict that it is not ready to transform
	 */
	@Override
	public void OnNotReadyToTransform() {
		transformButton.setEnabled(false);
	}

	/**
	 *Starts new Activity to display Homography transformation
	 */
	@Override
	public void onClick(View v) {
		if (v.getId() == transformButton.getId()){



			//preform transformation
			//			List<Uri> uris = mImageAdapter.getUris();
			//			if (uris != null) {
			//				Uri base = uris.get(0);
			//				Uri query = uris.get(1);
			//				
			//				Intent i = new Intent(this, TransformationDemoActivity.class);
			//				i.putExtra(BASE_URI_EXTRA, base);
			//				i.putExtra(QUERY_URI_EXTRA, query);
			//				startActivity(i);
			//			}

			// Start transformation process

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
			boolean fromUser) {
		int nThreshhold = Math.min(1, progress);
		tranBuilder.setRansacThreshold(nThreshhold);
		Toast t = Toast.makeText(this, "Toast threshhold: "  + nThreshhold, Toast.LENGTH_SHORT);
		t.show();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

}
