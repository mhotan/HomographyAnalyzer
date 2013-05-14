package edu.uw.homographyanalyzer.reusable;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import android.os.AsyncTask;
import android.util.Log;
import edu.uw.homographyanalyzer.api.CVSingletons;

public class AsyncFeatureDetector extends AsyncTask<Mat, Void, ImageInformation> {

	private static final String TAG = AsyncFeatureDetector.class.getSimpleName();
	
	private final ComputerVision mCV_;
	
	private FeatureDetectionListener mListener;
	
	/**
	 * Given an initialized 
	 * @param cv
	 */
	public AsyncFeatureDetector(ComputerVision cv){
		mCV_ = cv;
		checkRep();
	}
	
	@Override
	protected ImageInformation doInBackground(Mat... args) {
		Mat image = args[0];
		if (image == null)
			return null;
		
		// Compute keypoints of the reference image
		MatOfKeyPoint kp = mCV_.findFeatures(CVSingletons.getFeatureDetector(), image);
		
		if (kp.empty()){
			return null;
		}
		
		// Compute the descriptor
		Mat descriptor = mCV_.computeDescriptors(CVSingletons.getDescriptorExtractor(), image, kp);
		
		return new ImageInformation(image, kp, descriptor);
	}
	
	/**
	 * 
	 * @param listener
	 */
	public void setFeatureDetectionListener(FeatureDetectionListener listener){
		mListener = listener;
	}
	
	@Override 
	protected void onPostExecute(ImageInformation info){
		if (mListener == null) {
			Log.d(TAG, "no listener attached upon notification");
			return;
		}
		if (info == null){
			Log.d(TAG, "Failed to retrieve features from image");
			mListener.onFailedToExtractFeatures();
			return;
		}
		mListener.onExtractedFeatures(info);
	}
	
	public interface FeatureDetectionListener {
		public void onFailedToExtractFeatures();
		public void onExtractedFeatures(ImageInformation info);
	}

	private void checkRep() {
		if (mCV_ == null)
			throw new RuntimeException(TAG+": Null Computer Vision argument in initialization");
		if (!mCV_.isInitialized())
			throw new RuntimeException(TAG+": Computer Vision argument not initialized");
	}
}
