package edu.uw.homographyanalyzer.main.ocr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Gallery;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.example.homographyanalyzer.R;

import edu.uw.homographyanalyzer.api.XMLTestImageSet;
import edu.uw.homographyanalyzer.main.ImageSelectionAdapter;
import edu.uw.homographyanalyzer.main.MainActivity;

public class DisplayReaderActivity extends Activity implements OnItemSelectedListener,
ViewFactory {

	private static final String TAG = "[DisplayReaderActivity]";

	public static final String DATA_PATH = Environment
			.getExternalStorageDirectory().toString() + "/DisplayReaderActivity/";
	
	// You should have the trained data file in assets folder
	// You can get them at:
	// http://code.google.com/p/tesseract-ocr/downloads/list
	public static final String lang = "eng";
	
	// UI elements for the layout
	private Gallery _gallery;
	private TextView _description, _ocrResult;
	private Spinner _featureChoices;
	private ImageSwitcher _imageSwitcher;

	// Create a map of known appliances to there xml
	private Map<String, Integer> _featureIds;

	// For image selection and representation
	private ImageSelectionAdapter _imgAdapter;
	// Bitmap images 
	private Bitmap[] _images;

	private XMLTestImageSet _thisFeatures;

	private List<Uri> _uris;

	@Override
	protected void onCreate (Bundle savedInstanceState){

		// Establish UI elements
		setContentView(R.layout.ocr_main);
		_gallery = (Gallery)findViewById(R.id.ocr_gallery);
		_description = (TextView) findViewById(R.id.ocr_label);
		_ocrResult = (TextView) findViewById(R.id.ocr_result);
		_featureChoices = (Spinner) findViewById(R.id.ocr_spinner);
		_imageSwitcher = (ImageSwitcher) findViewById(R.id.ocr_image_switcher);

		// Set the main display for this image
		// Make pretty fasde in and out effect
		_imageSwitcher.setFactory(this);
		_imageSwitcher.setInAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_in));
		_imageSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_out));

		// Keep track of Id
		_featureIds = new HashMap<String, Integer>();
		_featureIds.put("Frigidaire drye", R.xml.frigidaire_dryer_faqe7072lw);
		_featureIds.put("Mike's microwave", R.xml.mike_microwave);
		_featureIds.put("Bryan's stove", R.xml.russel_stove);
		_featureIds.put("Whirlpool washer", R.xml.frigidaire_dryer_faqe7072lw);
		//		_featureIds.put(<Canonical Name>, R.xml.<associated ID>);
		// TODO Manually add names of appliance to there xml files ...
		// TODO FIX this non automated thing

		// Initialize spinner to correct value
		List<String> list = new ArrayList<String>(_featureIds.keySet());
		ArrayAdapter<String> dataAdapter = 
				new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		_featureChoices.setAdapter(dataAdapter);
		_featureChoices.setOnItemSelectedListener(this);

		_imgAdapter = new ImageSelectionAdapter(this);

		// Add the uri of all the bitmaps
		Intent intent = getIntent();
		_uris = new ArrayList<Uri>();
		_uris.add((Uri)intent.getParcelableExtra(MainActivity.BASE_SOURCE_EXTRA));
		_uris.add((Uri)intent.getParcelableExtra(MainActivity.QUERY_SOURCE_EXTRA));
		_uris.add((Uri)intent.getParcelableExtra(MainActivity.WARPED_SOURCE_EXTRA));

		//Update the gallery with current images
		_gallery.setAdapter(_imgAdapter);
		// Have selections from gallery show in switcher
		_gallery.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> listView, View view,
					int position, long id) {
				//Update text and image
				_description.setText(getLabel(position));
				_imageSwitcher.setImageURI(_uris.get(position));
				return false;
			}
		});

		// Update the images from stored in the intent
		updateImages();

		_description.setText("Please select which image this is");
		_ocrResult.setText("...");

		// 2 Extra images for drawing box around
	}

	/**
	 * Based on the uris provided update the image
	 */
	private void updateImages(){
		_imgAdapter.reset();
		for (int i = 0; i < _uris.size(); ++i){
			Bitmap b = MainActivity.getBitmapFromURIviaInputStream(
					getContentResolver(), _uris.get(i));
			_images[i] = b;
			_imgAdapter.setImage(b, i);
		}
	}

	/**
	 * Returns the associated label of the index image
	 * @param pos position of the image in gallery
	 * @return the label for this image
	 */
	private String getLabel(int pos){
		switch (pos){
		case 0: return "Reference Image";
		case 1: return "Other Image";
		case 2: return "Warped Image";
		case 3: return "Reference Image with feature outlined";
		case 4: return "Warped Image with feature outlined";
		case 5: return "Warped Image outline close up";
		default: return "Unknown Request";
		}

	}

	@Override
	protected void onDestroy (){
		// Delete Warped Image
	}

	@Override
	public void onItemSelected(AdapterView<?> spinner, View arg1, int pos,
			long arg3) {
		// TODO Auto-generated method stub

		Object o = spinner.getItemAtPosition(pos);
		String request = (String)o;
		int id = _featureIds.get(request);

		// Obtain XML file
		try {
			_thisFeatures = new XMLTestImageSet(this, id);
		} catch (IOException e) {
			Log.e(TAG, "IOException occured");
			e.printStackTrace();
			return;
		} catch (XmlPullParserException e) {
			Log.e(TAG, "XmlPullParserException occured with Request: " + request);
			_description.setText("Error occured while reading XML file");
			return;
		}

		//Obtain the first feature and use it as a base to 
		List<String> features =  _thisFeatures.getFeatures();
		if (features.size() == 0) {
			Log.w(TAG, "No features found for request: " + request);
			_description.setText("No features annotated");
			return;
		}
		List<Point> shape = _thisFeatures.getShapePoints(features.get(0));
		if (shape == null || shape.size() == 0){
			Log.w(TAG, "Error with main feature: " + features.get(0));
			_description.setText("Error with main feature: " + features.get(0));
			return;
		}

		Mat origWBorder = new Mat();
		Mat warpWBorder = new Mat();

		Utils.bitmapToMat(_images[0], origWBorder);
		Utils.bitmapToMat(_images[2], warpWBorder);

		Bitmap origBdrBM = Bitmap.createBitmap(origWBorder.cols(), origWBorder.rows(),
				Bitmap.Config.ARGB_8888);
		Bitmap warpedBdrBM = Bitmap.createBitmap(warpWBorder.cols(), warpWBorder.rows(),
				Bitmap.Config.ARGB_8888);

		// Draw three images
		// 1. Original image with border drawn
		drawBorder(origWBorder, shape);
		Utils.matToBitmap(origWBorder, origBdrBM);
		// 2. Warp image with border drawn
		drawBorder(warpWBorder, shape);
		Utils.matToBitmap(warpWBorder, warpedBdrBM);

		String origBdrUrl = MediaStore.Images.Media.insertImage(getContentResolver(), 
				origBdrBM, "Original with main feature", "Original image with main feature drawn" +
				" it.");
		if (origBdrUrl == null){
			Log.e(TAG, "Unable to save original image with shape border");
			return;
		}

		String warpedBdrUrl = MediaStore.Images.Media.insertImage(getContentResolver(), 
				warpedBdrBM, "Warped image with main feature", "Warped image with main feature drawn" +
				" it.");
		if (warpedBdrUrl == null){
			Log.e(TAG, "Unable to save warped image with shape border");
			return;
		}



		int xMin = Integer.MAX_VALUE;
		int yMin = Integer.MAX_VALUE;
		int xMax = Integer.MIN_VALUE;
		int yMax = Integer.MIN_VALUE;

		for (int i = 0; i < shape.size(); ++i){
			Point p = shape.get(i);
			xMin = Math.min(xMin, (int)p.x);
			xMax = Math.max(xMax, (int)p.x);
			yMin = Math.min(yMin, (int)p.y);
			yMax = Math.max(yMax, (int)p.y);
		}

		xMin = Math.max(xMin, 0);
		yMin = Math.max(yMin, 0);
		xMax = Math.min(xMax, _images[2].getWidth());
		yMax = Math.min(yMax, _images[2].getHeight());

		Bitmap sectionImg = Bitmap.createBitmap(_images[2], xMin, xMin, 
				xMax-xMin, yMax-yMin, new Matrix(), false);
		// Convert to ARGB_8888, required by tess
		sectionImg = sectionImg.copy(Bitmap.Config.ARGB_8888, true);
		String sectionImgUrl = MediaStore.Images.Media.insertImage(getContentResolver(), 
				warpedBdrBM, "Section image with main feature", "Section image with main feature drawn" +
				" it.");
		if (sectionImgUrl == null){
			Log.e(TAG, "Unable to save section image with shape border");
			return;
		}
		
		// Add uris to known list
		_uris.add(Uri.parse(origBdrUrl));
		_uris.add(Uri.parse(warpedBdrUrl));
		_uris.add(Uri.parse(sectionImgUrl));

		// 3. Create a box around the image 
		updateImages();

		// TODO obtain warped image place box around original and warped image
		Log.v(TAG, "Before baseApi");

	//	TessBaseAPI baseApi = new TessBaseAPI();
	//	baseApi.setDebug(true);
		//baseApi.init(DATA_PATH, lang);
		//baseApi.setImage(_images[_images.length-1]);
		//String recognizedText = baseApi.getUTF8Text();
		//baseApi.end();

		// You now have the text in recognizedText var, you can do anything with it.
		// We will display a stripped out trimmed alpha-numeric version of it (if lang is eng)
		// so that garbage doesn't make it to the display.

//		Log.v(TAG, "OCRED TEXT: " + recognizedText);
//		// Draw to new images and place in images
//		
//		// You now have the text in recognizedText var, you can do anything with it.
//		// We will display a stripped out trimmed alpha-numeric version of it (if lang is eng)
//		// so that garbage doesn't make it to the display.
//
//		Log.v(TAG, "OCRED TEXT: " + recognizedText);
//
//		if ( lang.equalsIgnoreCase("eng") ) {
//			recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
//		}
//		
//		recognizedText = recognizedText.trim();
//		_ocrResult.setText(recognizedText);
	}

	private void drawBorder(Mat canvas, List<Point> points){
		Scalar lineColor = new Scalar(255,125,0);
		int thickness = 3;

		// Close the loop with the shape if it is not closed already
		// Ensure last element is the first element
		if (!points.get(0).equals(points.get(points.size()-1))){
			points.add(points.get(0));
		}

		// If not big enough to encompass the shape
		if (points.size() < 4)
			throw new IllegalArgumentException("[drawBorder] points size not less then 3 Size: " + (points.size()-1));

		// List of points to draw with
		for (int i = 1; i < points.size(); ++i){
			Core.line(canvas, points.get(i-1), points.get(i), lineColor, thickness);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		//		_featureChoices.
	}

	@Override
	public View makeView() {
		// Set the new paramters of this view group to expand 
		// fitting the entire space
		ImageView iView = new ImageView(this);
		iView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		iView.setLayoutParams(new
				ImageSwitcher.LayoutParams(
						LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
		iView.setBackgroundColor(0xFF000000);
		return iView;
	}

}
