package edu.uw.homographyanalyzer.reusable;

/*
 * The main class that uses ComputerVision.java should implement this.
 * OpenCV on Android is implemented as a service, so there are
 * occasions when we need to know about the state of the service hook-up.
 * 
 * This class is also planned to be the debugging bridge between
 * the OpenCV library and the Android application. 
 * This is so the CV algo would be independent of the UI and
 * hence we'll be able to re-use it easily knowing that we'll
 * prototype a lot.
 * 
 */

public interface ComputerVisionCallback {
	
	/*
	 * Called when the service initialization has finished.
	 * No call to ComputerVision.java should be done before this.
	 */
	public void onInitServiceFinished();
	public void onInitServiceFailed();
	
	public void cvLogd(String msg);
	public void cvLogd(String tag, String msg);
	public void cvLoge(String msg);
	public void cvLoge(String tag, String msg);
}
