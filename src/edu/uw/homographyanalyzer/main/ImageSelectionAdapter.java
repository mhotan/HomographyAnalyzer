package edu.uw.homographyanalyzer.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import com.example.homographyanalyzer.R;

import edu.uw.homographyanalyzer.global.GlobalLogger;
import edu.uw.homographyanalyzer.tools.ImageSelectionStateListener;

/**
 * Adapter used to cycle through images added to by client services
 * @author mhotan
 *
 */
public class ImageSelectionAdapter extends BaseAdapter {

	private static final String TAG = "ImageSelectionAdapter";
	
	// Owning context of this adapter
	private Context mContext;
	
	// 
	public static final int POSITION_BASE = 0;
	public static final int POSITION_QUERY = 1;
	
	
	HashMap<Integer, ImageView> toShowMap;
	HashMap<Integer, Uri> uriMap;
	
	// Resource IDs 
//	private List<Integer> toShowIDs;
	
	private List<Bitmap> mBitMaps;
	
	private ImageSelectionStateListener mListener;
	
	//Default Search Image
	private final Bitmap mPlaceHolder;
	
	private static int default_search_id = R.drawable.ic_action_search;
	private int defaultItemBackground;
	
	/**
	 * Adpater for managing images for Homography
	 * @param galleryContext Context that contains gallery of images
	 */
	public ImageSelectionAdapter(Context galleryContext) {
		mContext = galleryContext;
//		toShowMap =  new HashMap<Integer, ImageView>();
//		uriMap = new HashMap<Integer, Uri>();
		
		mBitMaps = new ArrayList<Bitmap>(2);
		mPlaceHolder = BitmapFactory.decodeResource(mContext.getResources(), default_search_id);
		
		// Set the gallery item backgrounf image
		TypedArray styleAttrs = mContext.obtainStyledAttributes(R.styleable.PicGallery);
		// Get background Resource
		defaultItemBackground = styleAttrs.getResourceId(
				R.styleable.PicGallery_android_galleryItemBackground, 0);
		//Recycle Attribute
		styleAttrs.recycle();
		
		// Add default images
		reset();
	}
	
	/**
	 * Will notify listener on set and at next change in transform capable state
	 * @param listener null to clear otherwise listener for callback
	 */
	public void setImageSelectionStateListener(ImageSelectionStateListener listener) {
		mListener = listener;
		if (mListener == null) return;
		
		//Do initial notification
		if (isReadyToTransform())
			mListener.OnReadyToTransform();
		else
			mListener.OnNotReadyToTransform();
	}
	
	@Override
	public int getCount() {
		return mBitMaps.size();
	}

	/**
	 * Returns the BIT Map associated with 
	 */
	@Override
	public Object getItem(int position) {
		if (position < 0 || position >= mBitMaps.size())
			return null;
		else 
			return mBitMaps.get(position);
	}

	/**
	 * Returns Integer values of resource ID of image
	 */
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//create the view
	    ImageView imageView = new ImageView(mContext);
	    //specify the bitmap at this position in the array
	    imageView.setImageBitmap(mBitMaps.get(position));
	    //set layout options
	    imageView.setLayoutParams(new Gallery.LayoutParams(300, 200));
	    //scale type within view area
	    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
	    //set default gallery item background
	    imageView.setBackgroundResource(defaultItemBackground);
	    //return the view
	    return imageView;
	}

	/**
	 * Sets the image at position
	 * @param image Image to show 
	 * @param position position to show image at
	 */
	public void setImage(Bitmap image, int position) {
		if (image == null){
			GlobalLogger.getInstance().loge(TAG + "[setImage] NULL Image attempted");
			return;
		}
		
		// Because default size of images is 2 it possible to remove and 
		// replace image
		// If position requested is outside of bounds then append to end
		if (position >= mBitMaps.size())
			mBitMaps.add(image);
		// If position index is negative append to the end
		else if (position < 0)
			mBitMaps.add(0,image);
		// Within bounds remove image
		else {
			mBitMaps.remove(position);
			mBitMaps.add(position, image);
		}
		
		// If there are two initial images
		// notify transform is ready
		if (mBitMaps.size() >= 2 && mListener != null) 
			mListener.OnReadyToTransform();
		
		notifyDataSetChanged();
	}
	
	/**
	 * Resets the image thumbnails to show the search button
	 */
	private void reset(){
		//
		mBitMaps.clear();
		
		// The first two images set to search boxes
		for (int i = 0 ; i < 2/*Default size*/; ++i) {
			mBitMaps.add(mPlaceHolder);
		}
		
		if (mListener != null)
			mListener.OnNotReadyToTransform();
	}
	
	/**
	 * 
	 * @return if there are two valid images to transform
	 */
	public boolean isReadyToTransform(){
		// If the first two images are not the basic search image
		boolean ready = !isDefaultImage(0) && !isDefaultImage(1);
		return ready;
	}
	
	public boolean isDefaultImage(int pos){
		if (pos < 0 || pos >= mBitMaps.size())
			return false;
		else 
			return mBitMaps.get(pos) == mPlaceHolder;
	}
		
//		boolean createNewImageView = false;
//		ImageView imageView = null;
//		if (position == POSITION_BASE) {
//			ImageView base = toShowMap.get(POSITION_BASE);
//			if (base == null) 
//				createNewImageView = true;
//			else 
//				imageView = base;
//		} else if (position == POSITION_QUERY) {
//			ImageView query = toShowMap.get(POSITION_QUERY);
//			if (query == null) 
//				createNewImageView = true;
//			else
//				imageView = query;
//		} else {
//			Toast t = Toast.makeText(mContext, "Illegal view selected", Toast.LENGTH_SHORT);
//			t.show();
//		}
//		
//		if (createNewImageView){
//			if (convertView == null) {  // if it's not recycled, initialize some attributes
//				imageView = new ImageView(mContext);
//				imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
//				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
//				imageView.setPadding(8, 8, 8, 8);
//			} else {
//				imageView = (ImageView) convertView;
//			}
//			imageView.setImageResource(Images[position]);
//		}
//        return imageView;
	
	
//	/**
//	 * Changes image to image stored at uri
//	 * if position is not recognized everything is reset
//	 * @param uri stores image
//	 * @param position either POSTION_BASE or POSITION_QUERY
//	 */
//	public void changeImage(Uri uri, int position){
//		ImageView imageView = new ImageView(mContext);
//		imageView.setLayoutParams(new GridView.LayoutParams(
//				ViewGroup.LayoutParams.MATCH_PARENT,
//				ViewGroup.LayoutParams.MATCH_PARENT));
//		imageView.setScaleType(ImageView.ScaleType.CENTER);
//		imageView.setPadding(8, 8, 8, 8);
//		
//		if (position == POSITION_BASE ||
//				position == POSITION_QUERY && 
//				uri != null) {
//			imageView.setImageURI(uri);
//			
//			toShowMap.put(position, imageView);
//			// Store the uris
//			uriMap.put(position, uri);
//			
//			//Update the tracker
//			if (position == POSITION_BASE)
//				imageTracker.setFirstImageIsLoaded(true);
//			else 
//				imageTracker.setSecondImageIsLoaded(true);
//			
//			// Update the GUI
//			notifyDataSetChanged();
//		} else // Do nothing if position is wrong
//			imageView = null;
//		
//	}
//	
//	/**
//	 * Changes image to image stored at filePath
//	 * @param filePath stores image
//	 * @param position either POSTION_BASE or POSITION_QUERY
//	 */
//	public void changeImage(String filePath, int position){
//		File imgFile = new File(filePath);
//		changeImage(Uri.fromFile(imgFile), position);
//	}
//	
//	
//
//	/**
//	 * 
//	 * @return arraylist of base and query image uri with base first position
//	 * 		return null on failure
//	 */
//	public List<Uri> getUris() {
//		ArrayList<Uri> list = new ArrayList<Uri>();
//		list.add(uriMap.get(POSITION_QUERY));
//		list.add(0, uriMap.get(POSITION_BASE));
//		if (list.size() != 2)
//			return null;
//		return Collections.unmodifiableList(list);
//	}

}
