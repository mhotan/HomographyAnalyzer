package edu.uw.homographyanalyzer.quicktransform;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

/**
 * Represents the 5 distinct images of 
 * @author mhotan
 *
 */
public class Data {
	public final static int NUM_OF_BITMAP = 5;
	
	public String homographyMethod;
	public String featureDetection;
	
	//The singleton
	private static Data mInstance;
	public Bitmap bmp[], sbsBmp1, sbsBmp2;
	
	private List<Bitmap> bmpList;
	
	public Data(){
		bmp = new Bitmap[NUM_OF_BITMAP];
		bmpList = new ArrayList<Bitmap>();
	}
	
	public void addBitMap(Bitmap image){
		if (image == null) return;
		bmpList.add(image);
	}
	
	public static Data getInstance(){
		if(mInstance == null){
			mInstance = new Data();
		}
		
		return mInstance;
	}
	
}
