package edu.uw.homographyanalyzer.reusable;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import edu.uw.homographyanalyzer.tools.Utility;

import android.app.Activity;
import android.content.Context;

/*
 * Helper class that wraps the OpenCV algorithm 
 * required for our purposes.
 * 
 * Note that some functions might take a while to return.
 * Threading might be needed to avoid ANR.
 */
public class ComputerVision {
	private Context mContext;
	private Activity mActivity;
	private ComputerVisionCallback mCallback;

	private final static boolean DEBUG = true;
	private final static String TAG = "ComputerVision.java";

	public ComputerVision(Context ctx, Activity activity,
			ComputerVisionCallback callback) {
		mContext = ctx;
		mCallback = callback;
		mActivity = activity;
	}

	// Used to hook with the OpenCV service
	private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(
			mActivity) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
			}
				if(DEBUG) Logd("Service hook-up finished successfully!");
				mCallback.onInitServiceFinished();
				break;
			default: {
				Loge("Service hook-up failed!");
				mCallback.onInitServiceFailed();
			}
				break;
			}
		}
	};

	/*
	 * Asynchronous OpenCV service loader.
	 * 
	 * The first thing to do before using any other functions Try to connect
	 * with the OpenCV service.
	 * 
	 * mCallback.onInitServiceFinished() would be invoked once the
	 * initialization is done
	 */
	public void initializeService() {
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2,
				mContext, mOpenCVCallBack)) {
			Loge("Couldn't load OpenCV Engine!");
		}
		if(DEBUG) Logd("OpenCV Engine loaded");
	}
	
	/*
	 * Find the keypoints of a matrix
	 * featureDetector can be obtained from the FeatureDetector class
	 * (eg. FeatureDetector.FAST)
	 */
	public synchronized MatOfKeyPoint findKeyPoints(FeatureDetector detector, Mat images){
		MatOfKeyPoint results = new MatOfKeyPoint();	
		detector.detect(images, results);
		return results;
	}
	
	/*
	 * Find the keypoints between to patrices and returns a list the same size
	 * as number of images.  Each array is of the same size
	 * featureDetector can be obtained from the FeatureDetector class
	 * (eg. FeatureDetector.FAST)
	 */
	public synchronized List<Point[]> findKeyPointMatches(FeatureDetector detector, List<Mat> images){
		List<MatOfKeyPoint> results = new ArrayList<MatOfKeyPoint>();
		detector.detect(images, results);
		
		//Key points per image
		KeyPoint[] referenceKeyPoints = results.get(0).toArray();
		KeyPoint[] otherKeyPoints = results.get(1).toArray();
		
		int minNumOfPoints = Math.min(referenceKeyPoints.length, otherKeyPoints.length);
		Point[] refPts = new Point[minNumOfPoints];
		Point[] otherPts = new Point[minNumOfPoints];

		for (int i = 0; i < minNumOfPoints; i++) {
			refPts[i] = referenceKeyPoints[i].pt;
			otherPts[i] = otherKeyPoints[i].pt;
		}
		
		//TODO to change so points are not being removed
		
		List<Point[]> matchedPoints = new ArrayList<Point[]>(2); 
		matchedPoints.add(refPts);
		matchedPoints.add(otherPts);
		return matchedPoints;
	}
	
	/*
	 * Given a MatOfKeyPoint return Point[] which is only the
	 * x and y coordinates of the keypoints.  
	 * 
	 */
	public synchronized Point[] convertMatOfKeyPointToPointArray(MatOfKeyPoint source){
		KeyPoint[] keyPointArray = source.toArray();
		Point[] result = new Point[keyPointArray.length];
		for(int i = 0 ; i < keyPointArray.length ; i++){
			result[i] = keyPointArray[i].pt;
		}
		return result;
	}
	
	/*
	 * Given the reference points and the other keypoints
	 * Returns the homography matrix to transform the other to be 
	 * of the same perspective as the reference.
	 * RANSAC method is used.
	 */
	public Mat findHomography(MatOfKeyPoint referenceKeyPoints, MatOfKeyPoint otherKeyPoint,
								Mat refDescriptor, Mat otherDescriptor
								,int ransac_treshold){
		// Intermediate data structures expected by the findHomography function
		// provided by the library
		Point[] referencePoints, otherPoints;
		MatOfPoint2f matReference, matOther;
		Mat result;
		
		MatOfDMatch matches = Utility.getMatchingCorrespondences(
				refDescriptor, otherDescriptor); 
		
		
		
		referencePoints = convertMatOfKeyPointToPointArray(referenceKeyPoints);
		otherPoints = convertMatOfKeyPointToPointArray(otherKeyPoint);
		matReference = new MatOfPoint2f(referencePoints);
		matOther = new MatOfPoint2f(otherPoints);
		
		result = Calib3d.findHomography(matReference, matOther, Calib3d.RANSAC, ransac_treshold);
		
		return result;
	}
	
	/*
	 * Given the keypoints, compute the feature descriptors
	 */
	private Mat computeDescriptors(Mat img,
			MatOfKeyPoint kp) {
		Mat desc = new Mat();
		// Feature extractor
		DescriptorExtractor de = DescriptorExtractor
				.create(DescriptorExtractor.ORB);
		
		de.compute(img, kp, desc);
		
		return desc;
	}
	
	/*
	 * Given the reference points and the other keypoints
	 * Returns the homography matrix to transform the other to be 
	 * of the same perspective as the reference.
	 * RANSAC method is used.
	 */
	public Mat findHomography(Point[] referenceKeyPoints, Point[] otherKeyPoints, int method,
								int ransac_treshold){
		// Intermediate data structures expected by the findHomography function
		// provided by the library
		MatOfPoint2f matReference, matOther;
		Mat result;
		matReference = new MatOfPoint2f(referenceKeyPoints);
		matOther = new MatOfPoint2f(otherKeyPoints);
		
		result = Calib3d.findHomography(matReference, matOther, method, ransac_treshold);
		
		return result;
	}
	
	/**
	 * Calculates perspective warp of a reference matrix with homography and returns 
	 * resutls
	 * 
	 * @param refMatInv Reference image to be used in transformation
	 * @param homography 3x3 transfromation matrix for transform operations
	 * @param invert true if findHomography finds inverse matrix with using Imgproc.WARP_INVERSE_MAP, 
	 * 			false if normal transformation 
	 * @return transformed matrix
	 */
	public static Mat getWarpedImage(Mat refImage, Mat homography, boolean invert){
		Mat result = new Mat(refImage.size(), refImage.type());
		if (invert)
			Imgproc.warpPerspective(refImage, result, homography, refImage.size(),Imgproc.WARP_INVERSE_MAP);
		else
			Imgproc.warpPerspective(refImage, result, homography, refImage.size());
		return result;
	}
	
	// Logging function that propagates to the callback
	public void Logd(String msg) {
		mCallback.cvLogd(TAG, msg);
	}

	public void Loge(String msg) {
		mCallback.cvLoge(TAG, msg);
	}
}
