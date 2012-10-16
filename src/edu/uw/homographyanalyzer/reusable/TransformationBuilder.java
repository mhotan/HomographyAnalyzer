package edu.uw.homographyanalyzer.reusable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;
import edu.uw.homographyanalyzer.quicktransform.Data;
import edu.uw.homographyanalyzer.quicktransform.TransformInfo;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.provider.CalendarContract.Attendees;
import android.util.Log;
import android.util.Pair;

/**
 * Class that is able to build a homography trasnformation between to images
 * 
 * @author mhotan
 */
public class TransformationBuilder {

	private static final String TAG = "HomographyBuilder";

	public static final int RANSAC_THRESHHOLD_MAX = 10;
	private static final Pair<Integer, Integer> RANSAC_RANGE = 
			new Pair<Integer, Integer>(1, RANSAC_THRESHHOLD_MAX);

	// Computer vision object to
	private ComputerVision mCV;

	// Asyncronous homography producer 
	private AsyncHomographyProcessor homographyProcesser;
	private AsyncFeatureDetector mRefFeatureDetector;
	private AsyncFeatureDetector mOtherFeatureDetector;
	
	//Listeners that listens for state changes 
	private TransformationStateListener mlistener;
	
	// Storage of Tranformation information
	TransformInfo storage;

	///////////////////////////////////////////////////////////////////
	// Vairables for build process

	private Bitmap mReferenceImage, mOtherImage;
	
	//Name of feature detection type to use
	private String mFeatureDetectorName = null;

	//Name of homography method to use
	// just store the name that way a feature detector can be created per thread
	private String mHomographyMethod = null;

	// threshold for feature matching
	private int mRansacThreshhold;

	///////////////////////////////////////////////////////////////////
	// Constructor

	/**
	 * Creates instance
	 * @param cv fully initialized Computer Vision instance
	 */
	public TransformationBuilder(ComputerVision cv){
		if (cv == null)
			throw new IllegalArgumentException("ComputerVision not instantiated");
		// Instantiated vision
		mCV = cv;
		storage = new TransformInfo();
		// Initialize feature detector to default
		setFeatureDetector(FAST);
		//Initialize Homography to default
		setHomograhyMethod(RANSAC);
		setRansacThreshhold(1);
	}

	
	public void setTransformationStateListener(TransformationStateListener listener){
		mlistener = listener;
		updateListeners(storage);
	}

	/**
	 * Attempts to build transformation and returns in paor
	 * pair.first = regular inversion
	 * pair.secod = inverse ivnersion 
	 * @return null if couldnt build or Data other wise
	 */
	public Pair<Bitmap, Bitmap> getWarpedImages(){
		if (!storage.isComplete()) return null;
		
		// Check if storage has a complete homography
		Mat homography = storage.getHomographyMatrix() ;
		
		// Do a transformation with non inverted map
		Mat refMat = storage.getReferenceMatrix();
		
		Mat result = ComputerVision.getWarpedImage(refMat, homography, false);
		Bitmap disp = Bitmap.createBitmap(result.cols(), result.rows(),
				Bitmap.Config.ARGB_8888); // Android uses ARGB_8888
		Utils.matToBitmap(result, disp);

		Mat resultInv = ComputerVision.getWarpedImage(refMat, homography, true);
		Bitmap dispInv = Bitmap.createBitmap(resultInv.cols(), result.rows(),
				Bitmap.Config.ARGB_8888); // Android uses ARGB_8888
		Utils.matToBitmap(resultInv, dispInv);

		return new Pair<Bitmap, Bitmap>(disp, dispInv);
	}

	///////////////////////////////////////////////////////////////////
	// Homography 

	public String getCurrentHomographyMethod(){
		return mHomographyMethod;
	}

	private int getHomographyCode(){
		return mHomographyMethods.get(mHomographyMethod);
	}

	/**
	 * Sets the homography method to use
	 * @param method name of method
	 */
	public void setHomograhyMethod(String method){
		// method is null or not in library exit
		if (method == null || !mHomographyMethods.containsKey(method)){
			Log.e(TAG, "Illegal Homography method: " + method);
			return;
		}

		// If initial or new method
		if (mHomographyMethod == null 
				|| !mHomographyMethod.equals(method)){
			mHomographyMethod = method;
			Log.i(TAG, "Set Homography Method set: " + mHomographyMethod);
			attemptToBuild();
		} 
	}

	/**
	 * Sets the threshold to input value if the value falls in between 
	 * 1 < max threshold value		
	 * @param threshhold
	 */
	public void setRansacThreshhold(int threshhold){
		mRansacThreshhold = Math.max(RANSAC_RANGE.first, //It is at least min value
				Math.min(RANSAC_RANGE.second, threshhold)); // atmost max value
		Log.i(TAG, "Ransac threshhold set: " + mRansacThreshhold);
		attemptToBuild();
	}

	/**
	 * @return set of all available method of homographies
	 */
	public static Set<String> getHomographyMethodNames(){
		return Collections.unmodifiableSet(mHomographyMethods.keySet());
	} 

	public static final String RANSAC = "RANSAC";
	public static final String REGULAR = "ALL POINTS";
	public static final String LMEDS = "LEAST MEDIAN";

	private static final HashMap<String, Integer> mHomographyMethods = new HashMap<String, Integer>();

	static{
		mHomographyMethods.put(RANSAC, Calib3d.RANSAC);
		mHomographyMethods.put(LMEDS, Calib3d.LMEDS);
		mHomographyMethods.put(REGULAR, 0);
	}

	///////////////////////////////////////////////////////////////////
	// Image Selection

	/**
	 * sets and preprocesses reference images finding key points
	 * @param image Bitmap image to be reference
	 */
	public void setReferenceImage(Bitmap image){
		if (image == mReferenceImage) return;
		setImagePrivate(image, REF_IMG);
	}

	/**
	 * sets and preprocesses other image finding key points
	 * @param image Bitmap image to be other
	 */
	public void setOtherImage(Bitmap image){
		if (image == mOtherImage) return;
		setImagePrivate(image, OTHER_IMG);
	}

	private static final int REF_IMG = 0;
	private static final int OTHER_IMG = 1;

	/**
	 * Note that Large BitMap will cause out of memory errors
	 * This 
	 * @param image 
	 * @param which
	 */
	private void setImagePrivate(Bitmap image, int which){
		if (image == null)
			throw new IllegalArgumentException("NULL image");

		Mat imgMat = new Mat();
		// cancel any asynchronous process before we starrt a new one
		switch (which){
		case REF_IMG:
			mReferenceImage = image;
			Utils.bitmapToMat(mReferenceImage, imgMat);

			// Cancel any feature finding thread
			if (mRefFeatureDetector != null){
				mRefFeatureDetector.cancel(true);
				mRefFeatureDetector = null;
			} // Start new feature detector
			mRefFeatureDetector = new AsyncFeatureDetector(imgMat, REF_IMG);
			mRefFeatureDetector.execute();
			break;
		case OTHER_IMG:
			mOtherImage = image;
			Utils.bitmapToMat(mOtherImage, imgMat);
			
			// Cancel any feature finding thread
			if (mOtherFeatureDetector != null){
				mOtherFeatureDetector.cancel(true);
				mOtherFeatureDetector = null;
			} // Start new feature detector
			mOtherFeatureDetector = new AsyncFeatureDetector(imgMat, OTHER_IMG);
			mOtherFeatureDetector.execute();
			break;			
		}
		
		// We know that one of the images must have changed threrefore we need 
		// to reset the storage of the transform and start from scratch
		attemptToBuild();
	}
	
	/**
	 * calls asynchronous method to process Images
	 */
	private void attemptToBuild(){
		if (storage.hasBothImages()){
			//Cancel any current running processes
			if (homographyProcesser != null){
				homographyProcesser.cancel(false);
				homographyProcesser = null;
			}
			homographyProcesser = new AsyncHomographyProcessor(storage);
			homographyProcesser.execute();
		}
	}

	///////////////////////////////////////////////////////////////////
	// Image Processing
	
	/**
	 * Runs feature detection in the background for specific image
	 * @author mhotan
	 */
	private class AsyncFeatureDetector extends AsyncTask<Void, Void, KeyPoint[]>{

		private FeatureDetector mFd;
		private int mWhichImg;
		private Mat mImg;
		
		public AsyncFeatureDetector(Mat img, int whichImg){
			// Create new instances of thesse object to be run in background thread
			mFd = getCurrentFeatureDetector();
			mImg = img.clone();
			mWhichImg = whichImg;
		}
		
		@Override
		protected KeyPoint[] doInBackground(Void... params) {
			return mCV.findKeyPoints(mFd, mImg).toArray();
		}
		//Runs on main thread
		@Override
		protected void onPostExecute(KeyPoint[] result){
			
			if (mWhichImg == REF_IMG){
				storage.setReferenceImage(mImg, result);
				mlistener.OnKeypointsFoundForReference(storage.getRefKeyPointImage());
				// because image changed must attempt to build again
				attemptToBuild();
			} else if (mWhichImg == OTHER_IMG) {
				storage.setOtherImage(mImg, result);
				mlistener.OnKeypointsFoundForOther(storage.getOtherKeyPointImage());
				// because image changed must attempt to build again
				attemptToBuild();
			}
		}
		
	}

	
	private class AsyncHomographyProcessor extends AsyncTask<Void, Void,Boolean>{
		
		private final TransformInfo tempStorage;
		private final FeatureDetector detector;
		private final int tranformMethod, threshhold;
		
		public AsyncHomographyProcessor(TransformInfo info){
			tempStorage = info.clone();
			detector = getCurrentFeatureDetector();
			tranformMethod = mHomographyMethods.get(mHomographyMethod);
			threshhold = mRansacThreshhold;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			// Process Homography
			
			Mat refMat = tempStorage.getReferenceMatrix();
			Mat otherMat = tempStorage.getOtherMatrix();
			
			// Data structures needed
			// The 2 matrices put to a list
			List<Mat> listMatrixes = new LinkedList<Mat>();
			listMatrixes.add(refMat);
			listMatrixes.add(otherMat);
			
			// List that holds the resulting keypoints
			List<Point[]> matchedPoints = mCV.findKeyPointMatches(
					detector, listMatrixes);
			
			Point[] refMatches = matchedPoints.get(0);
			Point[] otherMatches = matchedPoints.get(1);
			
			// Calculate the matched points
			// Store Corresponding matched points
			tempStorage.setPutativeMatches(refMatches, otherMatches);
			
			// Convert points to MAt for calculation
			// Find homography 
			Mat homography = mCV.findHomography(refMatches, 
					otherMatches, tranformMethod, threshhold);
			
			// Store Homography
			tempStorage.setHomographyMatrix(homography);
			return Boolean.TRUE;
		}
		
		@Override 
		protected void onPostExecute(Boolean result){
			if (result.booleanValue()){
				storage = tempStorage;
				updateListeners(storage);
			}
		}
		
	}

	///////////////////////////////////////////////////////////////////
	// Feature detector

	/**
	 * Sets the feature detector of this transformation
	 * @param id id defined by org.opencv.features2d.FeatureDetector
	 */
	public void setFeatureDetector(String detectorType){
		// Input cant be null and library must contain type
		if (detectorType == null || !mFeatureDetectorNames.containsKey(detectorType)) {
			Log.e(TAG, "Illegal feature detector inputted: " + detectorType);
			return;
		}

		// Only change if in initial state or new detection
		// type is different
		if (mFeatureDetectorName == null || 
				!mFeatureDetectorName.equals(detectorType)){
			mFeatureDetectorName = detectorType;
			Log.i(TAG, "Feature Detector set: " + mFeatureDetectorName);
			attemptToBuild();
		}	
	}

	public static Set<String> getSupportedFeatureDetectorNames(){
		return Collections.unmodifiableSet(mFeatureDetectorNames.keySet());
	}

	/**
	 * @return name of current feature detector
	 */
	public String getCurrentFeatureDetectorName(){
		return mFeatureDetectorName;
	}
	
	/**
	 * @return name of current feature detector
	 */
	public FeatureDetector getCurrentFeatureDetector(){
		return FeatureDetector.create(
				mFeatureDetectorNames.get(mFeatureDetectorName));
	}

	// Feature Detector library
	public static final String SIFT = "SIFT";
	public static final String SURF = "SURF";
	public static final String FAST = "FAST";
	private static final String DYNAMIC_PREFIX = "DYNAMIC ";
	public static final String DYNAMIC_SIFT = DYNAMIC_PREFIX+SIFT;
	public static final String DYNAMIC_SURF = DYNAMIC_PREFIX+SURF;
	public static final String DYNAMIC_FAST = DYNAMIC_PREFIX+FAST;
	private static final String GRID_PREFIX = "GRID ";
	public static final String GRID_SIFT = GRID_PREFIX+SIFT;
	public static final String GRID_SURF = GRID_PREFIX+SURF;
	public static final String GRID_FAST = GRID_PREFIX+FAST;
	private static final String PYRAMID_PREFIX = "PYRAMID ";
	public static final String PYRAMID_SIFT = PYRAMID_PREFIX+SIFT;
	public static final String PYRAMID_SURF = PYRAMID_PREFIX+SURF;
	public static final String PYRAMID_FAST = PYRAMID_PREFIX+FAST;
	// Add sift names

	private static final HashMap<String, Integer> mFeatureDetectorNames = new HashMap<String, Integer>();
	static {
//		mFeatureDetectorNames.put(SIFT, FeatureDetector.SIFT); //MH Causes fatal error 10/15/2012
//		mFeatureDetectorNames.put(SURF, FeatureDetector.SURF); //MH SIFT and SURF not in free OPENCV library 
		mFeatureDetectorNames.put(FAST, FeatureDetector.FAST);
		mFeatureDetectorNames.put(DYNAMIC_SIFT, FeatureDetector.DYNAMIC_SIFT);
		mFeatureDetectorNames.put(DYNAMIC_SURF, FeatureDetector.DYNAMIC_SURF);
		mFeatureDetectorNames.put(DYNAMIC_FAST, FeatureDetector.DYNAMIC_FAST);
		mFeatureDetectorNames.put(GRID_SIFT, FeatureDetector.GRID_SIFT);
		mFeatureDetectorNames.put(GRID_SURF, FeatureDetector.GRID_SURF);
		mFeatureDetectorNames.put(GRID_FAST, FeatureDetector.GRID_FAST);
		mFeatureDetectorNames.put(PYRAMID_SIFT, FeatureDetector.PYRAMID_SIFT);
		mFeatureDetectorNames.put(PYRAMID_SURF, FeatureDetector.PYRAMID_SURF);
		mFeatureDetectorNames.put(DYNAMIC_SIFT,FeatureDetector.DYNAMIC_SIFT);
		// Add sift names
	}

	private void updateListeners(TransformInfo storage){
		if (mlistener != null){
			if (storage != null && storage.isComplete())
				mlistener.OnHomographyStored(storage);
			else
				mlistener.OnNoHomographyFound();
		}
	}

	public interface TransformationStateListener {
		public void OnHomographyStored(TransformInfo storage);
		public void OnNoHomographyFound();
		public void OnKeypointsFoundForReference(Mat image);
		public void OnKeypointsFoundForOther(Mat image);
	}


}
