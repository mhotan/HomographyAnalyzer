package edu.uw.homographyanalyzer.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.Toast;

import com.example.homographyanalyzer.R;

import edu.uw.homographyanalyzer.camera.BaseImageTaker;
import edu.uw.homographyanalyzer.camera.ExternalApplication;
import edu.uw.homographyanalyzer.global.GlobalLogger;
import edu.uw.homographyanalyzer.global.LoggerInterface;
import edu.uw.homographyanalyzer.quicktransform.TransformationDemoActivity;
import edu.uw.homographyanalyzer.reusable.ComputerVision;
import edu.uw.homographyanalyzer.reusable.ComputerVisionCallback;
import edu.uw.homographyanalyzer.tools.ImageStateTracker;

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
ComputerVisionCallback, ImageStateTracker.ReadyToTransformListener, OnClickListener {

	// Logging tag
	private static final String TAG = "HomographyAnalyzer";
	// CV library ready to be used
	private boolean mCVLibraryInitialized = false;
	
	// For on activity result references
	private static int CNTR = 0;
	private static final int BASE_IMAGE = ++CNTR;
	private static final int QUERY_IMAGE = ++CNTR;
	
	public static final String BASE_URI_EXTRA = TAG + "BASE_URI";
	public static final String QUERY_URI_EXTRA = TAG + "QUERY_URI";
	
	// Context of this for inner class use
	private Context mContext;
	
	//image tracker to track what image are currently available for transform
	private ImageStateTracker imageTracker;
	
	// adapter to display images
	private ImageAdapter mImageAdapter;
	
	//UI elements
	SlidingDrawer drawer;
	Button transformButton;
	//TODO fix this for set Alpha in slider to support lower APis
	@TargetApi(11)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_selector);
		mContext = this;
		// This needs to be done first because many other components
		// depend on this global logger
		new GlobalLogger(this);
		ComputerVision cv = new ComputerVision(this, this, this);
		cv.initializeService();
		
		imageTracker = new ImageStateTracker();
		imageTracker.setOnReadyToTransformListener(this);
		
		transformButton = (Button) findViewById(R.id.transformButton);
		transformButton.setOnClickListener(this);
		
		GridView gridview = (GridView) findViewById(R.id.imagegrid);
		mImageAdapter = new ImageAdapter(this);
		gridview.setAdapter(mImageAdapter);
		
		//Make sure proper reaction are made per item select
		gridview.setOnItemClickListener(new ItemSelectListener());
		
		drawer = (SlidingDrawer) findViewById(R.id.slidingDrawer);
		drawer.setAlpha(1); // Make opaque
	}
	
	/**
	 * Starts activity to obtain image for further processing
	 * Img address is set to 
	 * @param id
	 */
	private void getImageForID(int id){
		logd("Calling camera intent"); Intent i = new Intent(this,
				ExternalApplication.class); startActivityForResult(i, id);
	} 

	// Called when a started intent returns
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		String message;
		if (resultCode != RESULT_OK) {
			Toast t = Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT);
			t.show();
			return;
		}
		
		// Assign the view to change based on who sent the request
		// There are two positions to the that can be found
		int position = 0;
		if (requestCode == BASE_IMAGE) {
			position = ImageAdapter.POSITION_BASE;
		} else if (requestCode == QUERY_IMAGE) {
			position = ImageAdapter.POSITION_QUERY;
		}
		
		//Check if file path or uri image source
		String filePath = data.getExtras().getString(
				BaseImageTaker.INTENT_RESULT_IMAGE_PATH);
		if (filePath != null) {
			message = "Base Image : Bitmap OKAY: " + filePath;
			mImageAdapter.changeImage(filePath, position);
		} else {
			Uri uri = data.getExtras().getParcelable(
					BaseImageTaker.INTENT_RESULT_IMAGE_URI);
			message = "Base Image : Bitmap OKAY: " + uri.getPath();
//			ViewToChange.setImageURI(uri);
			mImageAdapter.changeImage(uri, position);
		}
		
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
	
	
	/**
	 * 
	 * @author mhotan
	 *
	 */
	private class ItemSelectListener implements AdapterView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			//This indicates the user wants to change the image before producing the transform
			switch (position) {
			case 0:
				getImageForID(BASE_IMAGE);
				break;
			case 1:
				getImageForID(QUERY_IMAGE);
				break;
			default:
				Toast t = Toast.makeText(mContext, "Illegal position selected", Toast.LENGTH_SHORT);
				t.show();
			}
		}
		
	}
	
	/**
	 * Image adapter to show image contents stored on camera
	 * @author mhotan
	 */
	private class ImageAdapter extends BaseAdapter {
		private Context mContext;
		public static final int POSITION_BASE = 0;
		public static final int POSITION_QUERY = 1;
		HashMap<Integer, ImageView> toShowMap;
		HashMap<Integer, Uri> uriMap;
		
		public ImageAdapter(Context c) {
			mContext = c;
			toShowMap =  new HashMap<Integer, ImageView>();
			uriMap = new HashMap<Integer, Uri>();
			resetShowingIDs();
		}

		/**
		 * Resets the image thumbnails to show the search button
		 */
		private void resetShowingIDs(){
			for (int i = 0 ; i < toShowIDs.length; ++i) {
				toShowIDs[i] = mSearchThumbIDs[i];
			}
			toShowMap.put(POSITION_BASE, null);
			toShowMap.put(POSITION_QUERY, null);
			uriMap.put(POSITION_BASE, null);
			uriMap.put(POSITION_QUERY, null);
			
			if (imageTracker == null) return;
			imageTracker.setFirstImageIsLoaded(false);
			imageTracker.setSecondImageIsLoaded(false);
		}
		
		@Override
		public int getCount() {
			return mSearchThumbIDs.length;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			if (position >= toShowIDs.length || position < 0)
				return -1;
			return toShowIDs[position];
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			boolean createNewImageView = false;
			ImageView imageView = null;
			if (position == POSITION_BASE) {
				ImageView base = toShowMap.get(POSITION_BASE);
				if (base == null) 
					createNewImageView = true;
				else 
					imageView = base;
			} else if (position == POSITION_QUERY) {
				ImageView query = toShowMap.get(POSITION_QUERY);
				if (query == null) 
					createNewImageView = true;
				else
					imageView = query;
			} else {
				Toast t = Toast.makeText(mContext, "Illegal view selected", Toast.LENGTH_SHORT);
				t.show();
			}
			
			if (createNewImageView){
				if (convertView == null) {  // if it's not recycled, initialize some attributes
					imageView = new ImageView(mContext);
					imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
					imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
					imageView.setPadding(8, 8, 8, 8);
				} else {
					imageView = (ImageView) convertView;
				}
				imageView.setImageResource(toShowIDs[position]);
			}
	        return imageView;
		}
		
		/**
		 * Changes image to image stored at uri
		 * if position is not recognized everything is reset
		 * @param uri stores image
		 * @param position either POSTION_BASE or POSITION_QUERY
		 */
		public void changeImage(Uri uri, int position){
			ImageView imageView = new ImageView(mContext);
			imageView.setLayoutParams(new GridView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
			imageView.setScaleType(ImageView.ScaleType.CENTER);
			imageView.setPadding(8, 8, 8, 8);
			
			if (position == POSITION_BASE ||
					position == POSITION_QUERY && 
					uri != null) {
				imageView.setImageURI(uri);
				
				toShowMap.put(position, imageView);
				// Store the uris
				uriMap.put(position, uri);
				
				//Update the tracker
				if (position == POSITION_BASE)
					imageTracker.setFirstImageIsLoaded(true);
				else 
					imageTracker.setSecondImageIsLoaded(true);
				
				// Update the GUI
				notifyDataSetChanged();
			} else // Do nothing if position is wrong
				imageView = null;
			
		}
		
		/**
		 * Changes image to image stored at filePath
		 * @param filePath stores image
		 * @param position either POSTION_BASE or POSITION_QUERY
		 */
		public void changeImage(String filePath, int position){
			File imgFile = new File(filePath);
			changeImage(Uri.fromFile(imgFile), position);
		}
		
		private Integer[] toShowIDs = new Integer[2];
		
		private Integer[] mSearchThumbIDs = {R.drawable.ic_action_search_dark, 
				R.drawable.ic_action_search_dark};

		/**
		 * 
		 * @return arraylist of base and query image uri with base first position
		 * 		return null on failure
		 */
		public List<Uri> getUris() {
			ArrayList<Uri> list = new ArrayList<Uri>();
			list.add(uriMap.get(POSITION_QUERY));
			list.add(0, uriMap.get(POSITION_BASE));
			if (list.size() != 2)
				return null;
			return Collections.unmodifiableList(list);
		}
	}

	/**
	 * 
	 */
	@Override
	public void OnReadyToTransform() {
		transformButton.setEnabled(true);
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
			List<Uri> uris = mImageAdapter.getUris();
			if (uris != null) {
				Uri base = uris.get(0);
				Uri query = uris.get(1);
				
				Intent i = new Intent(this, TransformationDemoActivity.class);
				i.putExtra(BASE_URI_EXTRA, base);
				i.putExtra(QUERY_URI_EXTRA, query);
				startActivity(i);
			}
		}
	}
	
}
