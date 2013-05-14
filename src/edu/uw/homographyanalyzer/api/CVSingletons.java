package edu.uw.homographyanalyzer.api;

import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

/**
 * These are instance that are created at compile time for this particular 
 * application.  This reduces the run time work done by android.
 * @author mhotan
 */
public class CVSingletons {

	private static final FeatureDetector mFD_ = TransformationLibrary.getFeatureDetector(TransformationLibrary.ORB);;
	private static final DescriptorExtractor mDE_ = TransformationLibrary.getDescriptorExtractor(TransformationLibrary.ORB);;
	private static final DescriptorMatcher mDM_ = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_SL2);;
	private static final int mHM_ = TransformationLibrary.getHomographyIdentifier(TransformationLibrary.RANSAC);
	private static final int mRANSACTHRESH = 3;
	
	public static int getHomographyMethod(){
		return mHM_;
	}
	
	public static int getRansacThreshold(){
		return mRANSACTHRESH;
	}
	
	/**
	 * Not thread safe
	 * @return the single instance of the feature detector 
	 */
	public static FeatureDetector getFeatureDetector(){
		return mFD_;
	}
	
	/**
	 * Not thread safe
	 * @return the single instance of the Descriptor Extractor
	 */
	public static DescriptorExtractor getDescriptorExtractor(){
		return mDE_;
	}
	
	/**
	 * Not thread safe
	 * @return the single instance of the Descriptor Matcher
	 */
	public static DescriptorMatcher getDescriptorMatcher(){
		return mDM_;
	}
}
