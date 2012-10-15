package edu.uw.homographyanalyzer.quicktransform;

import android.graphics.Bitmap;

/**
 * Represents the 5 distinct images of 
 * @author mhotan
 *
 */
public class Data {
	public final static int NUM_OF_BITMAP = 5;
	
	//The singleton
	private static Data mInstance;
	public Bitmap bmp[], sbsBmp1, sbsBmp2;
	
	public Data(){
		bmp = new Bitmap[NUM_OF_BITMAP];
	}
	
	
	public static Data getInstance(){
		if(mInstance == null){
			mInstance = new Data();
		}
		
		return mInstance;
	}
	
}
