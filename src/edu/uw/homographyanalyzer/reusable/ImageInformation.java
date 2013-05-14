package edu.uw.homographyanalyzer.reusable;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public class ImageInformation {
	
	private static final String TAG = ImageInformation.class.getSimpleName();
	
	public final Mat mImage;
	public final MatOfKeyPoint mFeatureKeyPts;
	public final Mat mFeatureDescriptors;
	
	public ImageInformation(Mat image, MatOfKeyPoint keyPoints, Mat descriptors){
		mImage = image;
		mFeatureKeyPts = keyPoints;
		mFeatureDescriptors = descriptors;
		checkRep();
	}

	private void checkRep() {
		if (mImage == null)
			throw new RuntimeException(TAG + ": Image input is null");
		if (mFeatureKeyPts == null)
			throw new RuntimeException(TAG + ": Key Feature input is null");
		if (mFeatureDescriptors == null)
			throw new RuntimeException(TAG + ": Descriptor input is null");
	}
	
}
