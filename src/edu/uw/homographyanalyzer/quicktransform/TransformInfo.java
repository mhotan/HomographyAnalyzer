package edu.uw.homographyanalyzer.quicktransform;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.Features2d;

import android.graphics.Bitmap;

/**
 * Wrapper class that contains information to images
 * This is a general purpose ADT that contains all the pertinent
 * information regarding making transformations
 * 
 * Stores items in light weight fashion using Matrixes
 * Bitmaps should not be used because they are to memory costly
 * 
 * @author mhotan
 */
public class TransformInfo {

	/*
	 * Abstract Representation:
	 * 	
	 * Stores all information regarding a transformation process
	 * 	
	 * Abstract Function
	 * 	reference_image = Bitmap Representation of reference image
	 * 	other_image = Bitmap representation of other image
	 * 	reference_keyPoint = Key Points found by feature detector for reference image
	 * 	other_keyPoint = Key Points found by feature detector for other image
	 * 	reference_KPImage = Reference image with Key points labeled with circles
	 * 	other_KPImage = Other image with Key points labled with circles
	 *  reference_matched_points = Matched points on reference image
	 *  other_matched_points = Matched points on other image
	 *  homography = Homography matrix between two images
	 *  generalPhotos = List of general Bitmap images that relate the two images
	 *  				It is up to the client to decide the organization of these images
	 *  
	 *  Representation Invariant
	 *  	generalPhotos != null;
	 *  
	 */
	
//	private static final int NUM_IMAGES = 2;
	
	// Base reference images
	private Mat reference_image, other_image;
	
	// KeyPoint features for both images sizes are not equal
	private MatOfKeyPoint reference_keyPoint, other_keyPoint;
	
	// KeyPoint features for both images sizes are not equal
	private Mat reference_Descriptors, other_Descriptors;
	
	// Bitmaps of reference and other images with keypoints
//	private Mat reference_KPImage, other_KPImage;
	
	// Matches of
	private MatOfDMatch matches;
	
	//public Mat reference_mat, other_mat;
	private Mat homography;

	//List of general photos stored
	private List<Bitmap> generalPhotos;
	
	public TransformInfo(){
		generalPhotos = new ArrayList<Bitmap>();
	}
	
	/**
	 * Returns clone of this storage with all containing elements
	 * @param src to be copied
	 * @return copied element
	 */
	public TransformInfo clone(){
		TransformInfo clone = new TransformInfo();
		clone.reference_image = reference_image;
		clone.other_image = other_image;
		clone.reference_keyPoint = reference_keyPoint;
		clone.other_keyPoint = other_keyPoint;
//		clone.reference_KPImage = reference_KPImage;
//		clone.other_KPImage = other_KPImage;
		clone.matches = matches;
//		clone.other_matched_points = other_matched_points;
		clone.homography = homography;
		clone.generalPhotos.addAll(generalPhotos);
		clone.reference_Descriptors = reference_Descriptors;
		clone.other_Descriptors = other_Descriptors;
		return clone;
	}
	
	/**
	 * @return if two images exists to find homography
	 */
	public boolean hasBothImages(){
		return reference_image != null && other_image != null;
	}
	
	/**
	 * removes any memory of all artifacts and clears all knowledge of stored Bitmaps
	 */
	public void reset(){
		reference_image = null;
		other_image = null;
		homography = null;
		reference_keyPoint = null;
		other_keyPoint = null;
//		reference_KPImage = null;
//		other_KPImage = null;
		matches = null;
		homography = null;
		other_Descriptors = null;
		reference_Descriptors = null;
		generalPhotos.clear();
	}
	
	/**
	 * @return if storage contains a homography, reference image, and other image
	 */
	public boolean isComplete(){
		return (reference_image != null &&
				other_image != null &&
				homography != null);
	}

	public void addBitMap(Bitmap image) {
		generalPhotos.add(image);
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Mutating Methods
	
	/**
	 * Sets the reference image and keypoints
	 * 
	 * @requires ref != null and keyPoints != null
	 * @param ref reference Matrix
	 * @param keyPoints
	 */
	public void setReferenceImage(Mat ref, MatOfKeyPoint keyPoints, Mat descriptors){
		reference_image = ref;
		reference_keyPoint = keyPoints;
		reference_Descriptors = descriptors;
		// Store current Image with Circles
//		reference_KPImage = getMatWithKP(reference_image, reference_keyPoint);
	}

	/**
	 * Sets the other image and keypoints
	 * 
	 * @requires other != null and keyPoints != null
	 * @param other Other Matrix
	 * @param keyPoints
	 */
	public void setOtherImage(Mat other, MatOfKeyPoint keyPoints, Mat descriptors){
		other_image = other;
		other_keyPoint = keyPoints;
		other_Descriptors = descriptors;
		// Store current Image with circles
//		other_KPImage = getMatWithKP(other_image, other_keyPoint);
	}
	
	/**
	 * Store matches from reference to other matches
	 * @requires neither argument is null
	 * @param matchedPnts matches on referencs image
	 * @param matchedPnts2 matches on other image
	 */
	public void setPutativeMatches(MatOfDMatch matches){
		this.matches = matches;
	}
	
	/**
	 * Sets homography Matrix 
	 * @param homography
	 */
	public void setHomographyMatrix(Mat homography){
		this.homography = homography;
	}
	
	/**
	 * If b exists b is added to end
	 * if b is null it is not added
	 * @param b Bitmap to add
	 */
	public void addBitmap(Bitmap b){
		if (b == null) return;
		if (generalPhotos.contains(b)){
			generalPhotos.remove(b);
		} 
		generalPhotos.add(b);
	}
	
	/**
	 * Adds Bitmap to pos if pos is negative adds to
	 * beginning. if pos is >= size then it is appended to the end
	 * if Bitmap is contained in this already it is appended to pos
	 * if b is null it is not added
	 * @param b Bitmap to add at pos
	 * @param pos position to be added at
	 */
	public void addBitmap(Bitmap b, int pos){
		if (b == null) return;
		int idx = generalPhotos.indexOf(b);
		if (idx != -1){
			if (idx == pos)
				return;
			generalPhotos.remove(b);
		}
		int position = Math.max(0, Math.min(generalPhotos.size()-1, pos));
		generalPhotos.add(position, b);
	}
	
	/**
	 * Removes all Bitmaps from memory
	 */
	public void clearBitmaps(){
		generalPhotos.clear();
	}
	
	/**
	 * Remove all bit maps from pos (inclusive) and after
	 * @param pos 0 based index to remove to
	 */
	public void removeBitmapFrom(int pos){
		if (pos < 0 || pos >= generalPhotos.size()) return;
		generalPhotos.remove(pos);
	}
	
	/**
	 * Remove Bitmap if it exist
	 * @param b Bitmap to remove
	 */
	public void removeBitmapFrom(Bitmap b){
		generalPhotos.remove(b);
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Retrieving Methods
	// NOTE: To ensure Representation exposure and unwanted client manipulation
	// All getters are required to return copies
	
	/**
	 * @return null if there is no reference image, a copy of reference image otherwise
	 */
	public Mat getReferenceMatrix(){
		return reference_image == null ? null : reference_image.clone();
	}
	
	/**
	 * @return null if there is no other image, a copy of other image otherwise
	 */
	public Mat getOtherMatrix(){
		return other_image == null ? null : other_image.clone();
	}
	
	/**
	 * @return null if no KeyPoints exits, or a copy of KeyPoint array
	 */
	public MatOfKeyPoint getReferenceKeyPoints(){
		return reference_keyPoint == null ? null :
			new MatOfKeyPoint(reference_keyPoint);
	}
	
	/**
	 * @return null if no KeyPoints exits, or a copy of KeyPoint array
	 */
	public MatOfKeyPoint getOtherKeyPoints(){
		return other_keyPoint == null ? null :
			new MatOfKeyPoint(other_keyPoint);
	}
	
	/**
	 * Using the images stored in t
	 * @return null if no images exist or an image of two images of same appliance
	 */
	public Mat getMatchImage(){
		if (matches == null || reference_image == null || 
				other_image == null || reference_keyPoint == null 
				|| other_keyPoint == null)
		return null;
		
		Mat output = new Mat();
		// Need to copy 
		Features2d.drawMatches(reference_image, reference_keyPoint, other_image, other_keyPoint,
				matches, output);
		
//		List<MatOfDMatch> list = new LinkedList<MatOfDMatch>();
//		list.add(matches);
//		Features2d.drawMatches2(reference_image, reference_keyPoint, 
//				other_image, other_keyPoint, list, output);
		return output;
	}
	
	/**
	 * Returns a list of descriptors defined by current images
	 * Order proceed as the reference image first followed by other images
	 * 
	 * @return empty list if no reference image exist, or list containing descriptors
	 */
	public Mat[] getDescriptors(){
		if (reference_Descriptors == null) return new Mat[0];
		Mat[] m = new Mat[2];
		m[0] = reference_Descriptors.clone();
		m[1] = other_Descriptors.clone();
		return m;
	}

	/**
	 * @return null if there is no matrix, or homography other wise
	 */
	public Mat getHomographyMatrix(){
		return homography == null ? null : homography.clone();
	}
	
	/**
	 * Retrieve Bitmap images that are currently stored
	 * @return
	 */
	public List<Bitmap> getBitmaps(){
		return new LinkedList<Bitmap>(generalPhotos);
	}
	
}
