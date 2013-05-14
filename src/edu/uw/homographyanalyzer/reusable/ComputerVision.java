package edu.uw.homographyanalyzer.reusable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import edu.uw.homographyanalyzer.api.CVSingletons;

/**
 * Helper class that wraps the OpenCV algorithm 
 * required for our purposes.
 * 
 * Note that some functions might take a while to return.
 * Threading might be needed to avoid ANR.
 */
public class ComputerVision {

	private final static String TAG = "ComputerVision.java";

	// Components needed for initializing cv
	private Context mContext;
	private Activity mActivity;
	private ComputerVisionCallback mCallback;

	// TODO: Re assess if this is really needed
	// or program should fall through and break
	private boolean initialized; // Failsafe to check if was initialized or not

	private final static boolean DEBUG = true;

	/**
	 * Creates an isntance of this ComputerVision helper
	 * <b>WARNING: Does not initialize Computer Vision
	 * @param ctx application context
	 * @param activity owning activity
	 * @param callback Callback to be notified when intialized
	 */
	public ComputerVision(Context ctx, Activity activity,
			ComputerVisionCallback callback) {
		mContext = ctx;
		mCallback = callback;
		mActivity = activity;
		initialized = false;
	}

	// Used to hook with the OpenCV service
	private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(
			mActivity) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
			}
			if(DEBUG) Logd("OpenCV Service initialization finished successfully!");
			initialized = true;
			if (mCallback != null) // If call back should exist
				mCallback.onInitServiceFinished();
			else
				Log.w(TAG, "OpenCV initialized no callback notified");
			break;
			default: {
				Loge("Service hook-up failed!");
				if (mCallback != null)
					mCallback.onInitServiceFailed();
				else 
					Log.w(TAG, "OpenCV initialization failed but no callback");
			}
			break;
			}
		}
	};

	/**
	 * Asynchronous OpenCV service loader.
	 * 
	 * The first thing to do before using any other functions Try to connect
	 * with the OpenCV service.
	 * 
	 * mCallback.onInitServiceFinished() would be invoked once the
	 * initialization is done
	 */
	public void initializeService() {
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3,
				mContext, mOpenCVCallBack)) {
			Loge("Couldn't load OpenCV Engine!");
		}
		if(DEBUG) Logd("OpenCV Engine loaded");
	}

	/**
	 * @return whether this instance of computer vision is initialized
	 */
	public boolean isInitialized(){
		return initialized;
	}

	/*
	 * Find the keypoints of a matrix
	 * featureDetector can be obtained from the FeatureDetector class
	 * (eg. FeatureDetector.FAST)
	 */
	public synchronized MatOfKeyPoint findFeatures(FeatureDetector detector, Mat images){
		if (!initialized)
			throw new IllegalStateException("CV not initialized");
		MatOfKeyPoint results = new MatOfKeyPoint();	
		detector.detect(images, results);
		return results;
	}

	/**
	 * Returns descrtiptions of features of keypoints found in image
	 * @param extractor Extractor to ue to extract description of features
	 * @param image which hold the features
	 * @param keypoints Matrix of keypoints to identify
	 * @return matrix of descriptors describing the features
	 */
	public synchronized Mat computeDescriptors(DescriptorExtractor extractor, Mat image,
			MatOfKeyPoint keypoints){
		if (!initialized)
			throw new IllegalStateException("CV not initialized");
		Mat descriptors = new Mat();
		extractor.compute(image, keypoints, descriptors);
		return descriptors;
	}

	/**
	 * Given two descriptors, compute the matches
	 */
	public synchronized MatOfDMatch getMatchingCorrespondences(Mat queryDescriptors,
			Mat trainDescriptors) {
		return privateGetMatchingCorrespondences(queryDescriptors, trainDescriptors);
	}
	
	/**
	 * Unsyncronized helper
	 * @param srcDescriptors
	 * @param destDescriptors
	 * @return
	 */
	private MatOfDMatch privateGetMatchingCorrespondences(Mat queryDescriptors,
			Mat trainDescriptors){
		if (!initialized)
			throw new IllegalStateException("CV not initialized");
		// Holds the result
		MatOfDMatch matches = new MatOfDMatch();
		// Flann-based descriptor
		DescriptorMatcher dm = CVSingletons.getDescriptorMatcher();
		// Compute matches
		dm.match(queryDescriptors, trainDescriptors, matches);
		return matches;
	}

	/**
	 * Precondition: Every argument except for matchesMask cannot be null
	 * @param img1 Mat of image 1
	 * @param keypoints1 Keypoints of image 1
	 * @param img2 Mat of Image 2
	 * @param keypoints2 Keypoints of image 2
	 * @param matches1to2 matches from 1 to 2
	 * @param matchColor The color that match lines shall be drawn
	 * @param singlePointColor Color of all keypoints without any matches
	 * @param matchesMask null or mask describing which matches to draw
	 * @return image with matches
	 */
	public synchronized Mat getMatchesImage(Mat img1, MatOfKeyPoint keypoints1, 
			Mat img2, MatOfKeyPoint keypoints2, MatOfDMatch matches1to2, Scalar matchColor, 
			Scalar singlePointColor, MatOfByte matchesMask){
		if (img1 == null || keypoints1 == null || 
				img2 == null || keypoints2 == null 
				|| matches1to2 == null)
			throw new IllegalArgumentException("[getMatchesImage] Null Input argument");

		// Default flag == 0;
		// Output image matrix will be created (Mat::create),
		// i.e. existing memory of output image may be reused.
		// Two source image, matches and single keypoints
		// will be drawn.
		// For each keypoint only the center point will be
		// drawn (without the circle around keypoint with
		// keypoint size and orientation).
		int flags = 0; 

		// Check if no mask is specified
		if (matchesMask == null)
			matchesMask = new MatOfByte();

		if (matchColor == null)
			matchColor = Scalar.all(-1);
		if (singlePointColor == null)
			singlePointColor = Scalar.all(-1);

		Mat RGBAOutput = new Mat();
		// RGB values
		Mat RGBOutput  = new Mat();
		Mat RGBImg1 = new Mat();
		Mat RGBImg2 = new Mat();

		// Convert to correct color format for cvtColor 
		Imgproc.cvtColor(img1, RGBImg1, Imgproc.COLOR_RGBA2RGB);
		Imgproc.cvtColor(img2, RGBImg2, Imgproc.COLOR_RGBA2RGB);

		Features2d.drawMatches(RGBImg1, keypoints1, RGBImg2, keypoints2, matches1to2, RGBOutput, 
				matchColor, singlePointColor, matchesMask, flags);

		Imgproc.cvtColor(RGBOutput, RGBAOutput, Imgproc.COLOR_RGB2RGBA);
		return RGBAOutput;
	}

	/**
	 * 
	 * Return image with keypoints drawn on
	 * @param src Source matrix to clone
	 * @param mKeypoints array of keypoints to label on image
	 * @return new matrix with key points labeled by circles
	 */
	public synchronized Mat getMatWithKeyPointsDrawn(Mat src, MatOfKeyPoint matKeyPoints) {
		// Have to handle stupid OpenCV problem of not being able to draw 
		Mat rgb = new Mat();
		Mat output = new Mat();
		Imgproc.cvtColor(src, rgb, Imgproc.COLOR_RGBA2RGB);
		Features2d.drawKeypoints(rgb, matKeyPoints, rgb, new Scalar(255,0,0), 0); 
		Imgproc.cvtColor(rgb, output, Imgproc.COLOR_RGB2RGBA);
		return output;
	}

	/*
	 * Given a feature descriptor, a MatOfDmatch, which describes the reference
	 * and target image and also MatOfKeyPoint for the reference and the target
	 * image, this method returns MatOfPoints2f for the reference and target
	 * image to be used for homography computation
	 * 
	 * Return: [0] = reference
	 *         [1] = target
	 */
	public MatOfPoint2f[] getCorrespondences(MatOfDMatch descriptors,
			MatOfKeyPoint ref_kp, MatOfKeyPoint tgt_kp) {

		// The source of computation
		DMatch[] descriptors_array = descriptors.toArray();
		KeyPoint[] ref_kp_array = ref_kp.toArray();
		KeyPoint[] tgt_kp_array = tgt_kp.toArray();

		// The result
		Point[] ref_pts_array = new Point[descriptors_array.length];
		Point[] tgt_pts_array = new Point[descriptors_array.length];

		for (int i = 0; i < descriptors_array.length; i++) {
			ref_pts_array[i] = ref_kp_array[descriptors_array[i].trainIdx].pt;
			tgt_pts_array[i] = tgt_kp_array[descriptors_array[i].queryIdx].pt;
		}
		
		MatOfPoint2f ref_pts = new MatOfPoint2f(ref_pts_array);
		MatOfPoint2f tgt_pts = new MatOfPoint2f(tgt_pts_array);
		
		MatOfPoint2f[] results = new MatOfPoint2f[2];
		results[0] = ref_pts;
		results[1] = tgt_pts;
		return results;
	}

	/**
	 * Gets all cross matches
	 * @param matches12
	 * @param matches21
	 * @return
	 */
	public synchronized MatOfDMatch getCrossMatches(MatOfDMatch matches12, 
			MatOfDMatch matches21) {
		
		List<DMatch> filtered = new LinkedList<DMatch>();
		DMatch[] m12Array = matches12.toArray();
		DMatch[] m21Array = matches21.toArray();
		
//		if (m12Array.length != m21Array.length)
//			throw new IllegalArgumentException("Matched do not have equal lengths");
	
		for (int i = 0; i < m12Array.length; i++) {
			DMatch forward = m12Array[i];
			DMatch backward = m21Array[forward.trainIdx];
			if (backward.trainIdx == forward.queryIdx)
				filtered.add(forward);
		}
		
		MatOfDMatch filteredMat = new MatOfDMatch();
		filteredMat.fromList(filtered);
		return filteredMat;
	}
	
	/*
	 * Given forward and backward MatOfDMatch, training MatOfKeyPoint and 
	 * query MatOfKeyPoint to compute cross matches.
	 * 
	 * Return: reduced MatOfDMatch by cross check
	 */
	public synchronized MatOfDMatch getCrossMatchesAndDistance(MatOfDMatch matches12, 
			MatOfDMatch matches21, MatOfKeyPoint train_kp, MatOfKeyPoint query_kp) {


		DMatch[] matches12_array = matches12.toArray();
		DMatch[] matches21_array = matches21.toArray();

		KeyPoint[] train_kp_array = train_kp.toArray();
		KeyPoint[] query_kp_array = query_kp.toArray();


		MatOfDMatch new_matches = new MatOfDMatch();
		List<DMatch> new_matchesList = new ArrayList<DMatch>();

		MatOfKeyPoint new_train_kp = new MatOfKeyPoint();
		List<KeyPoint> new_train_kpList = new ArrayList<KeyPoint>();

		MatOfKeyPoint new_query_kp = new MatOfKeyPoint();
		List<KeyPoint> new_query_kpList = new ArrayList<KeyPoint>();

		int count = 0;
		//matches12_array.length

		float distance =  matches12_array[0].distance;
		float distanceMin = distance;
		float distanceMax = 0;

		Point ptQuery = query_kp_array[matches12_array[0].queryIdx].pt;
		logi("HomoTrans::: CrossCheck :: Distance: " + distance);
		logi("HomoTrans::: CrossCheck :: Point: " + ptQuery);


		logi("matches12 queryIdx: " + matches12_array[0]);
		logi("matches12 queryIdx's point: " + query_kp_array[matches12_array[0].queryIdx]);


		for (int i = 0 ; i < matches12_array.length; i++) {

			distance =  matches12_array[i].distance;
			ptQuery = query_kp_array[matches12_array[i].queryIdx].pt;
			//			logi("HomoTrans::: CrossCheck :: Distance: " + distance);
			//			logi("HomoTrans::: CrossCheck :: Point: " + ptQuery);

			if (distance < distanceMin) {
				distanceMin = distance;
			}else if (distance > distanceMax) {
				distanceMax = distance;
			}

			if (matches12_array[i].queryIdx == matches21_array[matches12_array[i].trainIdx].trainIdx 
					&& matches12_array[i].trainIdx == matches21_array[matches12_array[i].trainIdx].queryIdx 
					&& distance < 100000) {

				new_matchesList.add(matches12_array[i]);		
				new_query_kpList.add(query_kp_array[matches12_array[i].queryIdx]);			
				new_train_kpList.add(train_kp_array[matches12_array[i].trainIdx]);			
				count = count + 1;
			}

			//			logi("HomoTrans::: CrossCheck :: crossMatch count: " + count);
		}

		logi("HomoTrans::: CrossCheck :: maximum distance: " + distanceMax);
		logi("HomoTrans::: CrossCheck :: minimum distance: " + distanceMin);

		new_matches.fromList(new_matchesList);

		new_train_kp.fromList(new_train_kpList);

		new_query_kp.fromList(new_query_kpList);

		return new_matches;

	}


	/**
	 * Given MatOfDMatch, training MatOfKeyPoint, query MatOfKeyPoint, and chessBoard zones
	 * to compute cross matches.
	 * 
	 * @return reduced MatOfDMatch gathered from each zone
	 */
	public synchronized MatOfDMatch getLocalMatches(MatOfDMatch matches, MatOfKeyPoint train_kp, 
			MatOfKeyPoint query_kp, int zones, int imgHeight, int imgWidth) {

		MatOfDMatch newMatches = new MatOfDMatch();
		DMatch[] localMatchesArray = new DMatch[zones * zones];
		List<DMatch> newMatchesList = new ArrayList<DMatch>();
		DMatch[] matchesArray = matches.toArray();


		KeyPoint[] train_kp_array = train_kp.toArray();
		KeyPoint[] query_kp_array = query_kp.toArray();


		logi("length: " + matchesArray.length);
		logi("height: " + imgHeight);
		logi("width: " + imgWidth);


		for (int i = 0; i < matchesArray.length; i++) {
			//			logi("i= " + i + " matches12 queryIdx's point: " + query_kp_array[matchesArray[i].queryIdx].pt.toString());
			//			logi("i= " + i + " matches12 trainIdx's point: " + train_kp_array[matchesArray[i].trainIdx].pt.toString());
			//			logi("i= " + i + " matches12 distance: " + matchesArray[i].distance);


			double intervalX = imgWidth / (double) zones;
			double intervalY = imgHeight / (double) zones;

			double chessX = Math.floor( query_kp_array[matchesArray[i].queryIdx].pt.x / intervalX );
			double chessY = Math.floor( query_kp_array[matchesArray[i].queryIdx].pt.y / intervalY );

			//			double chessX = (int) query_kp_array[matchesArray[i].queryIdx].pt.x / intervalX;
			//			double chessY = (int) query_kp_array[matchesArray[i].queryIdx].pt.y / intervalY;



			int index = (int) (chessX + chessY * zones);

			//			logi("intervalX= " + intervalX + ", intervalY= " + intervalY);
			//			logi("chessX= " + chessX + ", chessY= " + chessY);
			//			logi("index= " + index);

			if (localMatchesArray[index] == null || matchesArray[i].distance < localMatchesArray[index].distance) {
				localMatchesArray[index] = matchesArray[i];
			}
		}

		for (int i = 0; i < localMatchesArray.length; i++) {

			//			logi("location: (" + i % zones + ", " + i / zones + ")");
			if (localMatchesArray[i] != null) {
				newMatchesList.add(localMatchesArray[i]);
				//				logi("i= " + i + " localMatchesArray queryIdx's point: " + query_kp_array[localMatchesArray[i].queryIdx].pt.toString());
				//				logi("i= " + i + " localMatchesArray trainIdx's point: " + train_kp_array[localMatchesArray[i].trainIdx].pt.toString());
				//				logi("i= " + i + " localMatchesArray distance: " + localMatchesArray[i].distance);
			}
		}

		logi("local check count: " + newMatchesList.size());
		newMatches.fromList(newMatchesList);
		return newMatches;
	}

	/**
	 * KNNMatch works by selecting the top K matched rated fof distance for a given vector
	 * Given two descriptors, compute the matchesList
	 * 
	 * Return: List of MatOfDMatch
	 */
	public synchronized List<MatOfDMatch> getKnnMatchList(Mat queryDescriptors, Mat trainDescriptors, int numberOfMatches) {
		return privateGetKnnMatchList(queryDescriptors, trainDescriptors, numberOfMatches);
	}
	
	/**
	 * Private helper get numberOfMatches or less that best represent
	 * @param queryDescriptors
	 * @param trainDescriptors
	 * @param numberOfMatches
	 * @return
	 */
	private List<MatOfDMatch> privateGetKnnMatchList(Mat queryDescriptors, Mat trainDescriptors, int numberOfMatches) {
		List<MatOfDMatch> matchesList = new ArrayList<MatOfDMatch>();
		// Flann-based descriptor
		DescriptorMatcher dm = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_SL2);
		logi("HomoTrans::: find knnMatches :: number of matches: " + numberOfMatches);
		dm.knnMatch(queryDescriptors, trainDescriptors, matchesList, numberOfMatches);
		return matchesList;
	}


	/*
	 * Given List of MatOfDMatch from knnMatches, training MatOfKeyPoint and 
	 * query MatOfKeyPoint to compute good matches.
	 * 
	 * Return: reduced MatOfDMatch by distance check
	 */
	public synchronized MatOfDMatch getDistanceMatches(List<MatOfDMatch> knnMatchesList, MatOfKeyPoint trainMatOfKeyPoint, MatOfKeyPoint queryMatOfKeyPoint, int n, int threshold){

		logi("HomoTrans::: DistanceCheck :: matchesList size " + knnMatchesList.size());
		logi("HomoTrans::: DistanceCheck :: matchesList depth " + knnMatchesList.get(0).size());

		MatOfDMatch newMatches = new MatOfDMatch();
		List<DMatch> newMatchesList = new ArrayList<DMatch>();

		double sum = 0;
		int count = 0;
		for (int i = 0; i < knnMatchesList.size(); i++) {
			//    		logi("HomoTrans::: DistanceCheck :: matchesList 1.1 " + knnMatchesList.get(i).toList().get(0).toString());
			//        	logi("HomoTrans::: DistanceCheck :: matchesList 1.2 " + knnMatchesList.get(i).toList().get(1).toString());
			double diffDistance = Math.abs(knnMatchesList.get(i).toList().get(1).distance - knnMatchesList.get(0).toList().get(0).distance);
			//        	logi("HomoTrans::: DistanceCheck :: matchesList Distance:  " + diffDistance);
			sum = sum + diffDistance;
			if (diffDistance > threshold) {
				newMatchesList.add(knnMatchesList.get(i).toList().get(0));
				count++;
			}
		}

		logi("HomoTrans::: DistanceCheck :: count: " + count);
		logi("HomoTrans::: DistanceCheck :: distance average: " + sum/knnMatchesList.size());
		newMatches.fromList(newMatchesList);		

		return newMatches;
	}

	/**
	 * 
	 * @param m Only allowed RGB image
	 * @return Grey scale version of this image 
	 */
	public synchronized Mat RGBToGrey(Mat m) {
		Mat grey = new Mat();
		Imgproc.cvtColor(m, grey, Imgproc.COLOR_RGBA2GRAY); // TODO Verify right color scale
		Log.i(TAG, "HomoTrans::: Mat converted to grey scale: " + grey.toString());
		return grey;
	}

	/**
	 * Returns the hsitogram equalized image of m
	 * @param m original grey scaled image
	 * @return 
	 */
	public synchronized Mat toEqualizedHistogram(Mat m) {
		Mat equalized = new Mat();
		Imgproc.equalizeHist(m, equalized);
		Log.i(TAG, "HomoTrans::: Mat histogram Equalized: " + equalized.toString());
		return equalized;
	}

	/**
	 * Computes crosschecking via knn top match list.
	 * @param descriptors1
	 * @param descriptors2
	 * @param knn
	 * @return
	 */
	public synchronized MatOfDMatch getKnnWithCrossCheckingMatches(Mat descriptors1, Mat descriptors2,
			int knn){
		//TODO Implement
		//HardCode a descriptor Matcher
		// Sticking with brute force for simplicity
		Mat knnCrossed = new Mat();
		List<DMatch> filteredMatches = new ArrayList<DMatch>();

		List<MatOfDMatch> matches1to2 = privateGetKnnMatchList(descriptors1, descriptors2, knn);
		List<MatOfDMatch> matches2to1 = privateGetKnnMatchList(descriptors2, descriptors1, knn);
		
		for (int m = 0; m < matches1to2.size(); ++m) {
			boolean findCrossCheck = false;
			DMatch[] match1to2 = matches1to2.get(m).toArray();
			
			// For every forward match in from 1 to 2
			for (int fk = 0; fk < match1to2.length; fk++) {
				DMatch forward = match1to2[fk];
							
				DMatch[] matchFromForward = matches2to1.get(forward.trainIdx).toArray();
				// For every backward match that forward knows about
				// Verify that 
				for (int bk = 0; bk < matchFromForward.length; ++bk) {
					DMatch backward = matchFromForward[bk];
					if (backward.trainIdx == forward.queryIdx){
						filteredMatches.add(forward);
						findCrossCheck = true;
						break;
					}
				}
				if (findCrossCheck) break;
			}
				
		}
		
		// SOURCE Code copied from internet
		// http://stackoverflow.com/questions/5937264/using-opencv-descriptor-matches-with-findfundamentalmat
		//		filteredMatches12.clear();
		//	    vector<vector<DMatch> > matches12, matches21;
		//	    descriptorMatcher->knnMatch( descriptors1, descriptors2, matches12, knn );
		//	    descriptorMatcher->knnMatch( descriptors2, descriptors1, matches21, knn );
		//	    for( size_t m = 0; m < matches12.size(); m++ )
		//	    {
		//	        bool findCrossCheck = false;
		//	        for( size_t fk = 0; fk < matches12[m].size(); fk++ )
		//	        {
		//	            DMatch forward = matches12[m][fk];
		//
		//	            for( size_t bk = 0; bk < matches21[forward.trainIdx].size(); bk++ )
		//	            {
		//	                DMatch backward = matches21[forward.trainIdx][bk];
		//	                if( backward.trainIdx == forward.queryIdx )
		//	                {
		//	                    filteredMatches12.push_back(forward);
		//	                    findCrossCheck = true;
		//	                    break;
		//	                }
		//	            }
		//	            if( findCrossCheck ) break;
		//	        }
		//	    }
		Log.i(TAG, "HomoTrans::: Mat KNN Cross checked: " + knnCrossed.toString() +
				" Num matches: " + filteredMatches.size());
		MatOfDMatch good_matches = new MatOfDMatch();
		good_matches.fromList(filteredMatches);
		return good_matches;
	}

	/**
	 * MH: Found this does not work very well
	 * 
	 * Implement a standard distance check.  Prunes out good matches by comparing 3 times the
	 * minimum found distance.  If the min distance is 0 it automatically compare against 3.
	 * @param matches1to2
	 * @param numDescriptors
	 * @return
	 */
	public synchronized MatOfDMatch getStandardDistanceCheck(MatOfDMatch matches1to2){
		// TODO Implement
		float min = Float.MAX_VALUE;
		float max = 0;
		DMatch[] matchArr = matches1to2.toArray();
		for (DMatch match : matchArr) {
			float dist = match.distance;
			if (dist < min) min = dist;
			if (dist > max) max = dist;
		}
		Log.i(TAG, "HomoTrans::: Distances found for distance check min: " + min + " max: " + max);
		
		if (min <= 0) min = 1; // Check for min == 0
		
		List<DMatch> goodMatches = new LinkedList<DMatch>();
		for (DMatch match : matchArr) {
			if (match.distance < 3*min)
				goodMatches.add(match);
		}
		
		Log.i(TAG, "HomoTrans::: Number of good matches after standard distance check");
		MatOfDMatch good_matches = new MatOfDMatch();
		good_matches.fromList(goodMatches);
		return good_matches;
	}

	private void logi(String string) {
		Log.i(TAG, string);
	}

	/**
	 * Given the reference points and the other keypoints
	 * Returns the homography matrix to transform the other to be 
	 * of the same perspective as the reference.
	 * RANSAC method is used.
	 */
	public synchronized Mat findHomography(MatOfPoint2f tgt, MatOfPoint2f dst,
			int method, int ransac_treshold){
		Log.i(TAG, "HomoTrans::: Find Homography called");
		return privateHomographyFinder(tgt, dst,
				method, ransac_treshold);
	}

	/**
	 * Unsyncronized method
	 */
	private Mat privateHomographyFinder(MatOfPoint2f tgt, MatOfPoint2f dst,
			int method, int ransac_treshold){
		return Calib3d.findHomography(tgt, dst,
				method, ransac_treshold);
	}

	/**
	 * Calculates perspective warp of a reference matrix with homography and returns 
	 * resutls
	 * 
	 * @param img image to be used in transformation
	 * @param homography 3x3 transfromation matrix for transform operations
	 * @param invert true if findHomography finds inverse matrix with using Imgproc.WARP_INVERSE_MAP, 
	 * 			false if normal transformation 
	 * @return transformed matrix
	 */
	public Mat getWarpedImage(Mat img, Mat homography, Size tgtSize, boolean invert){
		Mat result = new Mat();
		if (invert)
			Imgproc.warpPerspective(img, result, homography, tgtSize,Imgproc.WARP_INVERSE_MAP);
		else
			Imgproc.warpPerspective(img, result, homography, tgtSize);
		return result;
	}
	
	/**
	 * perspective warps one set a points using a homography and returns the
	 * @param originalPoints
	 * @param Homography Matrix for transformation
	 * @return transformed Points
	 */
	public List<Point> getWarpedPoints(List<Point> originalPoints, Mat Homography) {
		Mat pointsMat = Converters.vector_Point2f_to_Mat(originalPoints);
		Mat trans = new Mat(pointsMat.size(), pointsMat.type());
		Core.perspectiveTransform(pointsMat, trans, Homography);
		List<Point> transPoints = new ArrayList<Point>();
		Converters.Mat_to_vector_Point2f(trans, transPoints);
		return transPoints;
	}
	
	public void drawRect(Rect rect, Mat image) {
		Core.rectangle(image, rect.br(), rect.tl(), new Scalar(255, 0, 0), 
				1);
	}
	
	/**
	 * Draw a line from two points in an image
	 * @param src Source point of the line
	 * @param dest Destination point of the line
	 * @param image Image to which to draw on
	 */
	public void drawLine(Point src, Point dest, Mat image){
		Scalar red = new Scalar(255,0,0);
		Core.line(image, src,dest, red);
	}

	// Logging function that propagates to the callback
	public void Logd(String msg) {
		mCallback.cvLogd(TAG, msg);
	}

	public void Loge(String msg) {
		mCallback.cvLoge(TAG, msg);
	}
	
	/*
	 * Given an input image, draws the keypoints on it and produce an output mat
	 * 
	 * This function wraps Features2d.drawKeypoints that takes only RGB image so
	 * that it'd take RGBA (which is what most of our mat would be of)
	 */
	public void drawKeypoints_RGBA(Mat src, Mat dst,
			MatOfKeyPoint keypoints) {
		Mat src_rgb = new Mat();
		Mat dst_rgb = new Mat();
		Imgproc.cvtColor(src, src_rgb, Imgproc.COLOR_RGBA2RGB);
		Features2d.drawKeypoints(src_rgb, keypoints, dst_rgb);
		Imgproc.cvtColor(dst_rgb, dst, Imgproc.COLOR_RGB2RGBA);
		// Imgproc.cvtColor(src_rgb, dst, Imgproc.COLOR_RGB2RGBA);
	}
}





/*
 * Find the keypoints between to patrices and returns a list the same size
 * as number of images.  Each array is of the same size
 * featureDetector can be obtained from the FeatureDetector class
 * (eg. FeatureDetector.FAST)
 */
//public synchronized List<Point[]> findKeyPointMatches(FeatureDetector detector, List<Mat> images){
//	List<MatOfKeyPoint> results = new ArrayList<MatOfKeyPoint>();
//	detector.detect(images, results);
//	
//	//Key points per image
//	KeyPoint[] referenceKeyPoints = results.get(0).toArray();
//	KeyPoint[] otherKeyPoints = results.get(1).toArray();
//	
//	int minNumOfPoints = Math.min(referenceKeyPoints.length, otherKeyPoints.length);
//	Point[] refPts = new Point[minNumOfPoints];
//	Point[] otherPts = new Point[minNumOfPoints];
//
//	for (int i = 0; i < minNumOfPoints; i++) {
//		refPts[i] = referenceKeyPoints[i].pt;
//		otherPts[i] = otherKeyPoints[i].pt;
//	}
//	
//	//TODO to change so points are not being removed
//	
//	List<Point[]> matchedPoints = new ArrayList<Point[]>(2); 
//	matchedPoints.add(refPts);
//	matchedPoints.add(otherPts);
//	return matchedPoints;
//}

///*
// * Given a MatOfKeyPoint return Point[] which is only the
// * x and y coordinates of the keypoints.  
// * 
// */
//public synchronized Point[] convertMatOfKeyPointToPointArray(MatOfKeyPoint source){
//	KeyPoint[] keyPointArray = source.toArray();
//	Point[] result = new Point[keyPointArray.length];
//	for(int i = 0 ; i < keyPointArray.length ; i++){
//		result[i] = keyPointArray[i].pt;
//	}
//	return result;
//}

/*
 * Given a feature descriptor, a MatOfDmatch, which describes the reference
 * and target image and also MatOfKeyPoint for the reference and the target
 * image, this method returns MatOfPoints2f for the reference and target
 * image to be used for homography computation
 * 
 * Return: [0] = reference
 *         [1] = target
 */
//public synchronized MatOfPoint2f[] getCorrespondences(MatOfDMatch descriptors,
//		MatOfKeyPoint ref_kp, MatOfKeyPoint tgt_kp) {
//
//	// The source of computation
//	DMatch[] descriptors_array = descriptors.toArray();
//	KeyPoint[] ref_kp_array = ref_kp.toArray();
//	KeyPoint[] tgt_kp_array = tgt_kp.toArray();
//
//	// The result
//	Point[] ref_pts_array = new Point[descriptors_array.length];
//	Point[] tgt_pts_array = new Point[descriptors_array.length];
//
//	for (int i = 0; i < descriptors_array.length; i++) {
//		ref_pts_array[i] = ref_kp_array[descriptors_array[i].trainIdx].pt;
//		tgt_pts_array[i] = tgt_kp_array[descriptors_array[i].queryIdx].pt;
//	}
//	
//	MatOfPoint2f ref_pts = new MatOfPoint2f(ref_pts_array);
//	MatOfPoint2f tgt_pts = new MatOfPoint2f(tgt_pts_array);
//	
//	MatOfPoint2f[] results = new MatOfPoint2f[2];
//	results[0] = ref_pts;
//	results[1] = tgt_pts;
//	return results;
//}
