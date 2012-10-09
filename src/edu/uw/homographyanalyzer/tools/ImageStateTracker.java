package edu.uw.homographyanalyzer.tools;

/**
 * Helper class to help track the state of this activity
 * @author mhotan
 */
public class ImageStateTracker {
	
	private boolean firstImageLoaded, secondImageLoaded;
	
	private ReadyToTransformListener mlistener;
	
	/**
	 * 
	 */
	public ImageStateTracker(){
		firstImageLoaded = false;
		secondImageLoaded = false;
	}
	
	/// Query methods
	
	/**
	 * @return whether this images are ready to be transformed
	 */
	public boolean isReadyForTranform(){
		return firstImageLoaded && secondImageLoaded;
	}
	
	/**
	 * Sets whether first image is loaded or not
	 * @param isLoaded indicates whether image is loaded or not
	 */
	public void setFirstImageIsLoaded(boolean isLoaded) {
		firstImageLoaded = isLoaded;
		if (isReadyForTranform() && mlistener != null){
			mlistener.OnReadyToTransform();
		} else
			mlistener.OnNotReadyToTransform();
	}
	
	/**
	 * Sets whether first image is loaded or not
	 * @param isLoaded indicates whether image is loaded or not
	 */
	public void setSecondImageIsLoaded(boolean isLoaded) {
		secondImageLoaded = isLoaded;
		if (isReadyForTranform() && mlistener != null){
			mlistener.OnReadyToTransform();
		}else
			mlistener.OnNotReadyToTransform();
	}
	
	/**
	 * To be implemented for 
	 * @author mhotan
	 */
	public interface ReadyToTransformListener {
		
		public void OnReadyToTransform();
		public void OnNotReadyToTransform();
	}
	
	/**
	 * Sets current On ready to transform listener
	 * @param listener null if listener should be disregarded.
	 */
	public void setOnReadyToTransformListener(ReadyToTransformListener listener){
		mlistener = listener;
	}
	
}
