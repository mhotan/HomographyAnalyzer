package edu.uw.homographyanalyzer.main;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;

public class OrganizedImageSelectionAdapter extends ImageSelectionAdapter {

	private static final String REFERENCE = "Reference Image";
	private static final String OTHER = "Other Image";
	private static final String REFERENCE_KEY_POINT = "Reference Image with Key Points";
	private static final String OTHER_KEY_POINT = "Other Image with Key Points";
	private static final String PUTATIVE_MATCHES_NO_LINES = "Putative Matches";
	private static final String PUTATIVE_MATCHES_LINES = "Putative Matches with Lines";
	private static final String REGULAR_WARP = "Regular Warp";
	private static final String INVERT_WARP = "Inverse Warp";

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
		//Strip away extra images
		removeAllExtraImages();

		//Add the list of images
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

	public List<Bitmap> getCurrentImages(){
		List<Bitmap> list = new ArrayList<Bitmap>();
		if (mReference != null)
			list.add(mReference);
		if (mOther != null)
			list.add(mOther);
		list.addAll(mDynamicList);
		return list;
	}

	public String getTitle(Bitmap b){
		if (mReference == b) return REFERENCE;
		if (mOther == b) return OTHER;
		if (mRefKP == b) return REFERENCE_KEY_POINT;
		if (mOtherKP == b) return OTHER_KEY_POINT;
		if (mPutativeMatchesWithOutLines == b) return PUTATIVE_MATCHES_NO_LINES;
		if (mPutativeMatchesWithLines == b) return PUTATIVE_MATCHES_LINES;
		if (mRegularWarp == b) return REGULAR_WARP;
		if (mInvWarp == b) return INVERT_WARP;
		else return "Error: No Title";
	}

	public void setReferenceImage(Bitmap ref){
		if (ref == null) return;
		mReference = ref;
		buildImageViews();
	}

	public void setOtherImage(Bitmap other){
		if (other == null) return;
		mOther = other;
		buildImageViews();
	}

	public void setReferenceKeyPointImage(Bitmap img){
		if (img == null) return;
		mRefKP = img;
		buildImageViews();
	}

	public void setOtherKeyPointImage(Bitmap img){
		if (img == null) return;
		mOtherKP = img;
		buildImageViews();
	}
	public void setPutativeImageWithoutLinesImage(Bitmap img){
		if (img == null) return;
		mPutativeMatchesWithOutLines = img;
		buildImageViews();
	}
	public void setPutativeImageWithLinesImage(Bitmap img){
		if (img == null) return;
		mPutativeMatchesWithLines = img;
		buildImageViews();
	}

	public void setWarpedImage(Bitmap img){
		if (img == null) return;
		mRegularWarp = img;
		buildImageViews();
	}

	public void setInvertedWarpedImage(Bitmap img){
		if (img == null) return;
		mInvWarp = img;
		buildImageViews();
	}

	@Override
	public void reset(){
		super.reset();

		// The first two images set to search boxes
		for (int i = 0 ; i < 2/*Default size*/; ++i) {
			mBitMaps.add(mPlaceHolder);
		}
	}

}
