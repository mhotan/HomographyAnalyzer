package edu.uw.homographyanalyzer.reusable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import edu.uw.homographyanalyzer.quicktransform.Data;

import android.graphics.Bitmap;
import android.os.AsyncTask;
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

	// key Points
	private Point[] refKeyPoints = null;
	private Point[] otherKeyPoints = null;

	// Feature processors per image
	private FeatureProcessor refProcessor;
	private FeatureProcessor otherProcessor;
	
	TransformationStateListener mlistener;

	///////////////////////////////////////////////////////////////////
	// Vairables for build process

	//Images to warp between
	private Bitmap mReferenceBitmap, mOtherBitmap;
	
	//Following are user defined variables that can be changed
	private FeatureDetector mFeatureDetectorRef;
	private FeatureDetector mFeatureDetectorOther;
	
	//Name of feature detection type
	private String mFeatureDetectorName = null;
	
	//Name of homography method
	private String mHomographyMethod = null;
	
	private int ransacThreshhold;
	
	///////////////////////////////////////////////////////////////////
	// Constructor

	public TransformationBuilder(ComputerVision cv){
		if (cv == null)
			throw new IllegalArgumentException("ComputerVision not instantiated");
		mCV = cv;

		// Initialize feature detector to default
		setFeatureDetector(SIFT);
		
		//Initialize Homography to default
		setHomograhyMethod(RANSAC);
		ransacThreshhold = 1;
		
		refProcessor = new FeatureProcessor(mReferenceBitmap, mFeatureDetectorRef);
		otherProcessor = new FeatureProcessor(mOtherBitmap, mFeatureDetectorOther);
	}
	
	public void setTransformationStateListener(TransformationStateListener listener){
		mlistener = listener;
	}
	
	/**
	 * Attempts to build transformation if possible
	 * @return null if couldnt build or Data other wise
	 */
	public Data build(){
		if (refKeyPoints == null || 
				otherKeyPoints == null) {
			return null;
		}
		
		//Find Homography Matrix
		Mat homography = mCV.findHomography(refKeyPoints, otherKeyPoints, getCurrentMethod(), ransacThreshhold);
		
		// Do a transformation with non inverted map
		Mat refMat = new Mat();
		Utils.bitmapToMat(mReferenceBitmap, refMat);
		Size sz = refMat.size();
		Mat result = new Mat(sz, refMat.type());
		Imgproc.warpPerspective(refMat, result, homography, sz);
		Bitmap disp = Bitmap.createBitmap(result.cols(), result.rows(),
				mReferenceBitmap.getConfig());
		Utils.matToBitmap(result, disp);
	
		// Do a transformation with inverted map
		Mat refMatInv = new Mat();
		Utils.bitmapToMat(mReferenceBitmap, refMatInv);
		Size szInv = refMatInv.size();
		Mat resultInv = new Mat(szInv, refMatInv.type());
		Imgproc.warpPerspective(refMatInv, resultInv, homography, szInv, Imgproc.WARP_INVERSE_MAP);
		Bitmap dispInv = Bitmap.createBitmap(resultInv.cols(), result.rows(),
				mReferenceBitmap.getConfig());
		Utils.matToBitmap(resultInv, dispInv);

		Data data = new Data();
		data.addBitMap(disp);
		data.addBitMap(dispInv);
		return data;
	}
	
	///////////////////////////////////////////////////////////////////
	// Homography 
	
	public String getCurrentHomographyMethod(){
		return mHomographyMethod;
	}
	
	private int getCurrentMethod(){
		return mHomographyMethods.get(mHomographyMethod);
	}
	
	/**
	 * Sets the homography method to use
	 * @param method name of method
	 */
	public void setHomograhyMethod(String method){
		if (mHomographyMethod == null 
				|| !mHomographyMethod.equals(method)
				&& mHomographyMethods.containsKey(method)){
			mHomographyMethod = method;
			updateListeners();
		}
	}
	
	/**
	 * Sets threshold
	 * @param threshhold
	 */
	public void setRansacThreshold(int threshhold){
		if (threshhold == ransacThreshhold) return;
		int min = RANSAC_RANGE.first;
		int max = RANSAC_RANGE.second;
		int thresh = Math.max(min, threshhold);
		thresh = Math.min(max, thresh);
		ransacThreshhold = thresh;
		updateListeners();
	}
	
	/**
	 * @return set of all available method of homographies
	 */
	public static Set<String> getHomographyMethodNames(){
		return Collections.unmodifiableSet(mHomographyMethods.keySet());
	} 
	
	private static final String RANSAC = "RANSAC";
	private static final String REGULAR = "ALL POINTS";
	private static final String LMEDS = "LEAST MEDIAN";
	
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
		if (image == mReferenceBitmap) return;
		refKeyPoints = null;
		setImagePrivate(image, REF_IMG);
	}

	/**
	 * sets and preprocesses other image finding key points
	 * @param image Bitmap image to be other
	 */
	public void setOtherImage(Bitmap image){
		if (image == mOtherBitmap) return;
		otherKeyPoints = null;
		setImagePrivate(image, OTHER_IMG);
	}

	private static final int REF_IMG = 0;
	private static final int OTHER_IMG = 1;

	private void setImagePrivate(Bitmap image, int which){
		if (image == null)
			throw new IllegalArgumentException("NULL image");

		// cancel any asynchronous process before we starrt a new one
		switch (which){
		case REF_IMG:
			mReferenceBitmap = image;
			refProcessor.cancel(false);
			refProcessor = null;
			refProcessor = new FeatureProcessor(mReferenceBitmap, mFeatureDetectorRef);
			refProcessor.execute();
			break;
		case OTHER_IMG:
			mOtherBitmap = image;
			otherProcessor.cancel(false);
			otherProcessor = null;
			otherProcessor = new FeatureProcessor(mOtherBitmap, mFeatureDetectorOther);
			otherProcessor.execute();
			break;			
		}
	}

	///////////////////////////////////////////////////////////////////
	// Image Processing

	/**
	 * Assign Corresponding Point[] depending on BitMap image passed in
	 * @author mhotan
	 */
	private class FeatureProcessor extends AsyncTask<Void, Void, Point[]>{

		//Bitmap image to process for features
		private Bitmap image;

		// Feature detector to use
		private FeatureDetector fDetector;

		// Feature detector to use
		private Point[] keyPoints;

		public FeatureProcessor(Bitmap image, FeatureDetector detector){
			setImage(image, detector);
		}

		public void setImage(Bitmap image, FeatureDetector detector ){
			this.image =image;
			fDetector = detector;
			// feature detector for specific image
			// Reference key points per image
			if (image == mReferenceBitmap){
				fDetector = mFeatureDetectorRef;
				keyPoints = refKeyPoints;
			}
			else{
				fDetector = mFeatureDetectorOther;
				keyPoints = otherKeyPoints;
			}
		}

		@Override
		protected Point[] doInBackground(Void... arg0) {

			//If of ARGB_8888 configuration format set flag
			Bitmap.Config imgConfig = image.getConfig();
			boolean unPreMultiplyAlpha = false;
			switch (imgConfig) {
			case ARGB_8888:
				unPreMultiplyAlpha = true;
			case RGB_565:
				break;
			default: //Unsupported request
				return null;
			}

			Mat converted = new Mat();
			Utils.bitmapToMat(image, converted, unPreMultiplyAlpha);
			MatOfKeyPoint keyPoints = mCV.findKeyPoints(fDetector, converted);
			return mCV.convertMatOfKeyPointToPointArray(keyPoints);
		}

		@Override
		protected void onPostExecute(Point[] ret){
			// Only act if failed to get points
			if (ret == null) {
				Log.e(TAG, "Unable to process key points");
			} 
			keyPoints = ret;
		}

	}

	///////////////////////////////////////////////////////////////////
	// Feature detector

	/**
	 * Sets the feature detector of this transformation
	 * @param id id defined by org.opencv.features2d.FeatureDetector
	 */
	public void setFeatureDetector(String detectorType){
		if (mFeatureDetectorNames.containsKey(detectorType) && 
				!mFeatureDetectorName.equals(detectorType)){
			int id = mFeatureDetectorNames.get(detectorType);
			mFeatureDetectorRef = FeatureDetector.create(id);
			mFeatureDetectorOther = FeatureDetector.create(id);
			mFeatureDetectorName = detectorType;
			updateListeners();
		} else
			Log.e(TAG, "Illegal feature detector inputted");
	}

	public static Set<String> getSupportedFeatureDetectorNames(){
		return Collections.unmodifiableSet(mFeatureDetectorNames.keySet());
	}

	/**
	 * @return name of current feature detector
	 */
	public String getCurrentFeatureDetector(){
		return mFeatureDetectorName;
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
		mFeatureDetectorNames.put(SIFT, FeatureDetector.SIFT);
		mFeatureDetectorNames.put(SURF, FeatureDetector.SURF);
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
	
	private void updateListeners(){
		if (mlistener != null){
			if (isTransformable())
				mlistener.OnReadyToTransform();
			else
				mlistener.OnNotReadyToTransform();
		}
	}

	private boolean isTransformable(){
		return refKeyPoints != null && otherKeyPoints != null;
	}
	
	public interface TransformationStateListener {
		public void OnReadyToTransform();
		public void OnNotReadyToTransform();
	}


}
