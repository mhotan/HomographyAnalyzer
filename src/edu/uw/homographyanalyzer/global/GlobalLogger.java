package edu.uw.homographyanalyzer.global;

/*
 * Global logging utility
 * This class makes it able to log easily from any other classes
 * Implemented as a singleton that would need to be initialized
 * from the main activity once before any other use.
 */
public class GlobalLogger {
	// A singleton
	private static GlobalLogger mInstance;
	private LoggerInterface mInterface;
	
	// Initialization should only be done once on the main activity 
	public GlobalLogger(LoggerInterface loggerInterface){
		mInstance = this;
		mInterface = loggerInterface;
	}
	
	// Get the singleton
	public static GlobalLogger getInstance(){
		if(mInstance == null){
			throw new RuntimeException("Logger needs to be initialized!");
		}
		return mInstance;
	}
	
	// Just like Android's Log class
	public void logd(String msg){
		mInterface.logd(msg);
	}
	
	public void loge(String msg){
		mInterface.loge(msg);
	}
}
