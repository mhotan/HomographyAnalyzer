package edu.uw.homographyanalyzer.global;

/*
 * Global logging utility
 * This class makes it able to log easily from any other classes
 * 
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
	
	public static GlobalLogger getInstance(){
		if(mInstance == null){
			throw new RuntimeException("Logger needs to be initialized!");
		}
		return mInstance;
	}
	
	public void globalLogd(String msg){
		mInterface.Logd(msg);
	}
	
	public void globalLoge(String msg){
		mInterface.Loge(msg);
	}
}
