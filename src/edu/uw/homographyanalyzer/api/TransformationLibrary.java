package edu.uw.homographyanalyzer.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;

/**
 * Class that contains all the feature descriptors, exctractor
 *  and Homography methods that are accessible
 * and usable.   
 * @author mhotan
 */
public class TransformationLibrary {

	/**
	 * @param name String name of the descriptor extracter
	 * @return corresponding Descriptor extractor 
	 */
	public static DescriptorExtractor getDescriptorExtractor(String name) {
		if (mFeatureExtractor.containsKey(name))
			return DescriptorExtractor.create(mFeatureExtractor.get(name));
		throw new IllegalArgumentException("DescriptorExtractor name: " + name + "" +
				" not supported, Use getSupportedDescriptorExtractors() for list of all possible" +
				"choices");
	}

	/**
	 * Returns a feature detector that correlates to name
	 * @param name supported name of feature detector
	 * @return Feature detector correlated to that name
	 */
	public static FeatureDetector getFeatureDetector(String name){
		if (mFeatureDetectorNames.containsKey(name))
			return FeatureDetector.create(mFeatureDetectorNames.get(name));
		throw new IllegalArgumentException("FeatureDetector name: " + name + "" +
				" not supported, Use getSupportedFeatureNames() for list of all possible" +
				"choices");
	}

	/**
	 * Returns integer representation as defined by org.opencv.calib3d.Calib3d
	 * then can use as "method" in Calib3d.findHomography(arg1, arg2, method, ransac_treshold)
	 * @param name name of homography method as defined by getSupportedHomographyMethods
	 * @return integer representation 
	 */
	public static int getHomographyIdentifier(String name) {
		if (mHomographyMethods.containsKey(name))
			return mHomographyMethods.get(name);
		throw new IllegalArgumentException("Homography name: " + name + " Not supported," +
				" use getSupportedHomographyMethods() for set of all methods");
	}

	/**
	 * @return Unmodifiable list of all supported methods of homography
	 */
	public static List<String> getSupportedHomographyMethods(){
		List<String> hNames = new ArrayList<String>(mHomographyMethods.keySet());
		Collections.sort(hNames);
		return Collections.unmodifiableList(hNames);
	}

	/**
	 * Provides the name of all the supported Feature Extractors
	 * @return the set of all supported Feature Extractors
	 */
	public static List<String> getSupportedFeatureExtractors(){
		List<String> feNames = new ArrayList<String>(mFeatureExtractor.keySet());
		Collections.sort(feNames);
		return Collections.unmodifiableList(feNames);
	} 

	/**
	 * Provides the name of all the supportted Feature detector
	 * @return the set of all supported Feature Detector Names
	 */
	public static List<String> getSupportedFeatureDetectorNames(){
		List<String> fdNames = new ArrayList<String>(mFeatureDetectorNames.keySet());
		Collections.sort(fdNames);
		return Collections.unmodifiableList(fdNames);
	}

	///////////////////////////////////////////////////////////////////
	// Feature detection Methods
	///////////////////////////////////////////////////////////////////

	public enum MATCH_PRUNING_METHOD {
		NONE("NONE"), 
		CROSS_MATCH("CROSS MATCH"), 
		KNNMATCH("KNN MATCH"), 
		LOCAL_MATCH("LOCAL MATCH"),
		KNN_AND_CROSSCHECK("KNN AND CROSS CHECK")
//		STANDARD_DISTANCE("STANDARD MIN DISTANCE")
		;

		private MATCH_PRUNING_METHOD(final String text){
			this.text = text;
		}

		private final String text;

		@Override
		public String toString(){
			return text;
		}
	}

	public static final MATCH_PRUNING_METHOD[] ALL_PRUNING_METHODS =
		{ 
//		MATCH_PRUNING_METHOD.STANDARD_DISTANCE,
		MATCH_PRUNING_METHOD.KNN_AND_CROSSCHECK,
		MATCH_PRUNING_METHOD.NONE, 
		MATCH_PRUNING_METHOD.CROSS_MATCH,
		MATCH_PRUNING_METHOD.KNNMATCH, 
		MATCH_PRUNING_METHOD.LOCAL_MATCH
		 
		};

	// Feature Detector Names
	public static final String SIFT = "SIFT";
	public static final String SURF = "SURF";
	public static final String FAST = "FAST";
	public static final String ORB = "ORB";
	private static final String DYNAMIC_PREFIX = "DYNAMIC ";
	public static final String DYNAMIC_SIFT = DYNAMIC_PREFIX+SIFT;
	public static final String DYNAMIC_SURF = DYNAMIC_PREFIX+SURF;
	public static final String DYNAMIC_FAST = DYNAMIC_PREFIX+FAST;
	public static final String DYNAMIC_ORB = DYNAMIC_PREFIX+ORB;
	private static final String GRID_PREFIX = "GRID ";
	public static final String GRID_SIFT = GRID_PREFIX+SIFT;
	public static final String GRID_SURF = GRID_PREFIX+SURF;
	public static final String GRID_FAST = GRID_PREFIX+FAST;
	public static final String GRID_ORB = GRID_PREFIX+ORB;
	private static final String PYRAMID_PREFIX = "PYRAMID ";
	public static final String PYRAMID_SIFT = PYRAMID_PREFIX+SIFT;
	public static final String PYRAMID_SURF = PYRAMID_PREFIX+SURF;
	public static final String PYRAMID_FAST = PYRAMID_PREFIX+FAST;
	public static final String PYRAMID_ORB = PYRAMID_PREFIX+ORB;

	// Feature Extractors
	public static final String BRIEF = "BRIEF";
	public static final String BRISK = "BRISK";
	public static final String FREAK = "FREAK";
	private static final String OPPONENT = "OPPONENT";
	public static final String OPPONENT_BRIEF = OPPONENT + "_" + BRIEF;
	public static final String OPPONENT_BRISK = OPPONENT + "_" + BRISK;
	public static final String OPPONENT_FREAK = OPPONENT + "_" + FREAK;
	public static final String OPPONENT_ORB = OPPONENT + "_" + ORB;
	public static final String OPPONENT_SIFT = OPPONENT + "_" + SIFT;
	public static final String OPPONENT_SURF = OPPONENT + "_" + SURF;

	// Homography projection matrices
	public static final String RANSAC = "RANSAC";
	public static final String REGULAR = "ALL POINTS";
	public static final String LMEDS = "LEAST MEDIAN";

	private static final HashMap<String, Integer> mHomographyMethods = new HashMap<String, Integer>();
	static{
		mHomographyMethods.put(RANSAC, Calib3d.RANSAC);
		mHomographyMethods.put(LMEDS, Calib3d.LMEDS);
		mHomographyMethods.put(REGULAR, 0);
	}

	private static final HashMap<String, Integer> mFeatureDetectorNames = new HashMap<String, Integer>();
	static {
		mFeatureDetectorNames.put(SIFT, FeatureDetector.SIFT); //MH Causes fatal error 10/15/2012
		mFeatureDetectorNames.put(SURF, FeatureDetector.SURF); //MH SIFT and SURF not in free OPENCV library 
		mFeatureDetectorNames.put(ORB, FeatureDetector.ORB);
		mFeatureDetectorNames.put(FAST, FeatureDetector.FAST);
		mFeatureDetectorNames.put(DYNAMIC_SIFT, FeatureDetector.DYNAMIC_SIFT);
		mFeatureDetectorNames.put(DYNAMIC_SURF, FeatureDetector.DYNAMIC_SURF);
		mFeatureDetectorNames.put(DYNAMIC_FAST, FeatureDetector.DYNAMIC_FAST);
		mFeatureDetectorNames.put(DYNAMIC_ORB, FeatureDetector.DYNAMIC_ORB);
		mFeatureDetectorNames.put(GRID_SIFT, FeatureDetector.GRID_SIFT);
		mFeatureDetectorNames.put(GRID_SURF, FeatureDetector.GRID_SURF);
		mFeatureDetectorNames.put(GRID_FAST, FeatureDetector.GRID_FAST);
		mFeatureDetectorNames.put(GRID_ORB, FeatureDetector.GRID_ORB);
		mFeatureDetectorNames.put(PYRAMID_SIFT, FeatureDetector.PYRAMID_SIFT);
		mFeatureDetectorNames.put(PYRAMID_SURF, FeatureDetector.PYRAMID_SURF);
		mFeatureDetectorNames.put(PYRAMID_FAST,FeatureDetector.PYRAMID_FAST);
		mFeatureDetectorNames.put(PYRAMID_ORB,FeatureDetector.PYRAMID_ORB);
		// Add sift names ...
	}

	private static final HashMap<String, Integer> mFeatureExtractor = new HashMap<String, Integer>();
	static {
		//For all feature ORB feature detectors 
		mFeatureExtractor.put(BRIEF, DescriptorExtractor.BRIEF);
		mFeatureExtractor.put(BRISK, DescriptorExtractor.BRISK);
		mFeatureExtractor.put(FREAK, DescriptorExtractor.FREAK);
		mFeatureExtractor.put(OPPONENT_BRIEF, DescriptorExtractor.OPPONENT_BRIEF);
		mFeatureExtractor.put(OPPONENT_BRISK, DescriptorExtractor.OPPONENT_BRISK);
		mFeatureExtractor.put(OPPONENT_FREAK, DescriptorExtractor.OPPONENT_FREAK);
		mFeatureExtractor.put(OPPONENT_ORB, DescriptorExtractor.OPPONENT_ORB);
		mFeatureExtractor.put(OPPONENT_SIFT, DescriptorExtractor.OPPONENT_SIFT);
		mFeatureExtractor.put(OPPONENT_SURF, DescriptorExtractor.OPPONENT_SURF);
		mFeatureExtractor.put(ORB, DescriptorExtractor.ORB);
		mFeatureExtractor.put(SIFT, DescriptorExtractor.ORB);
		mFeatureExtractor.put(SURF, DescriptorExtractor.SIFT);
	}

	/**
	 * Class the
	 */
	public static class PruningMethodParameters implements Cloneable {

		// Arbitrary Default values 
		public static final int DEFAULT_ZONES = 12;
		public static final int DEFAULT_K_NUMBERS = 2;
		public static final int DEFAULT_DISTANCE_THRESHHOLD = 80;
		public static final int MIN_ZONES = 1;
		public static final int MAX_ZONES = 200; // TODO find legitamite value for this
		public static final int MIN_K = 1;
		public static final int MAX_K = Integer.MAX_VALUE;
		public static final int MIN_DISTANCE_THRESHHOLD = 10;
		public static final int MAX_DISTANCE_THRESHHOLD = Integer.MAX_VALUE;

		// Number of zones for local feature extraction
		private int mZones;
		// K number of top matches
		private int mK;
		// Distance threshold
		private int mDistanceThreshold;

		/**
		 * Creates parameters with 
		 */
		public PruningMethodParameters(){
			mZones = DEFAULT_ZONES;
			mK = DEFAULT_K_NUMBERS;
			mDistanceThreshold = DEFAULT_DISTANCE_THRESHHOLD;
		}

		/**
		 * Sets the number of zones for local match pruning.  
		 * If this number is < 0 or greater then MAX_ZONES then it is automatically
		 * Incremented or decremented  
		 */
		public void setNumZones(int numZones){
			mZones =  Math.max(MIN_ZONES, Math.min(MAX_ZONES, numZones));
		}

		/**
		 * Sets the K value which is automatically adjusted with appropiate range
		 *  MIN_K and MAX_K
		 * @param newK K value
		 */
		public void setKNumber(int newK){
			mK = Math.max(MIN_K, Math.min(MAX_K, newK));;
		}

		/**
		 * Sets the distance threshhold
		 * @param threshhold distance threshhold to be set for matching
		 */
		public void setDistanceThreshhold(int threshhold) {
			mDistanceThreshold = Math.max(MIN_DISTANCE_THRESHHOLD, 
					Math.min(MAX_DISTANCE_THRESHHOLD, threshhold));
		}

		/**
		 * @return returns the current number of Zone
		 */
		public int getNumZones(){
			return mZones;
		}

		/**
		 * @return current K value
		 */
		public int getKValue(){
			return mK;
		}

		/**
		 * @return Returns distance threshold
		 */
		public int getDistanceThreshhold(){
			return mDistanceThreshold;
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			PruningMethodParameters clone = new PruningMethodParameters();
			clone.mDistanceThreshold = this.mDistanceThreshold;
			clone.mK = this.mK;
			clone.mZones = this.mZones;
			return clone;
		}
	}
}
