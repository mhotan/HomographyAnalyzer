package edu.uw.homographyanalyzer.main;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;

public class OrganizedImageSelectionAdapter extends ImageSelectionAdapter {

	private Bitmap mReference, mOther;
	private Bitmap mRefKP, mOtherKP;
	private Bitmap mPutativeMatchesWithOutLines;
	private Bitmap mPutativeMatchesWithLines;
	private Bitmap mRegularWarp, mInvWarp;
	
	private List<Bitmap> mDynamicList;
	
	public OrganizedImageSelectionAdapter(Context galleryContext) {
		super(galleryContext);
		mDynamicList = new ArrayList<Bitmap>(10);
	}

	/**
	 * Builds dynamic view depending on what client has added
	 */
	private void buildImageViews(){
		if (mReference != null)
			setImage(mReference, 0);
		if (mOther != null)
			setImage(mOther, 1);
		mDynamicList.clear();
		if (mRefKP != null)
			mDynamicList.add(mRefKP);
		if (mOtherKP != null)
			mDynamicList.add(mOtherKP);
		if (mPutativeMatchesWithOutLines != null)
			mDynamicList.add(mPutativeMatchesWithOutLines);
		if (mPutativeMatchesWithLines != null)
			mDynamicList.add(mPutativeMatchesWithLines);
		if (mRegularWarp != null)
			mDynamicList.add(mRegularWarp);
		if (mInvWarp != null)
			mDynamicList.add(mInvWarp);
		addAllImagesToEnd(mDynamicList);
	}
	
	public void setReferenceImage(Bitmap ref){
		mReference = ref;
		buildImageViews();
	}
	
	public void setOtherImage(Bitmap other){
		mOther = other;
		buildImageViews();
	}
	
	public void setReferenceKeyPointImage(Bitmap img){
		mRefKP = img;
		buildImageViews();
	}
	
	public void setOtherKeyPointImage(Bitmap img){
		mOtherKP = img;
		buildImageViews();
	}
	public void setPutativeImageWithoutLinesImage(Bitmap img){
		mPutativeMatchesWithOutLines = img;
		buildImageViews();
	}
	public void setPutativeImageWithLinesImage(Bitmap img){
		mPutativeMatchesWithLines = img;
		buildImageViews();
	}
	
	public void setWarpedImage(Bitmap img){
		mRegularWarp = img;
		buildImageViews();
	}
	
	public void setInvertedWarpedImage(Bitmap img){
		mInvWarp = img;
		buildImageViews();
	}
	
}
