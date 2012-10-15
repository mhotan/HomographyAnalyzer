package edu.uw.homographyanalyzer.quicktransform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import com.example.homographyanalyzer.R;

import edu.uw.homographyanalyzer.main.MainActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class TransformationDemoActivity extends Activity implements
		View.OnClickListener {

	private static final String TAG = "TransDemo";

	private final String OUT_PATH = "/mnt/sdcard/download/";

	private File ref_file_, other_file_;

	private final static int CAMERA_INTENT_REFERENCE = 123;
	private final static int CAMERA_INTENT_OTHER = 234;
	private Bitmap referenceImage_ = null;
	private Bitmap otherImage_ = null;

	private Uri reference_image_uri_, other_image_uri_;

	private FeatureDetector fd_;

	private Button btnTakeRefImg, btnTakeImg;

	// Singleton to pass data between activities/classes
	private Data data_;
	
	private boolean initialized;
	StartOnCreate starter;
	static {
		if (!OpenCVLoader.initDebug()) {
			Log.d(TAG, "Init failed!");
		}

	}

	// Used to hook with the OpenCV service
	private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				initializes();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		initialized = false;
		Intent sender = getIntent();
		Uri base = (Uri) sender.getExtras().get(MainActivity.BASE_URI_EXTRA);
		Uri other = (Uri) sender.getExtras().get(MainActivity.QUERY_URI_EXTRA);
		
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack)){
        	Log.e(TAG, "Couldn't load OpenCV Engine!");
        	return;
        }
        
        starter = new StartOnCreate(base, other);
        starter.execute();
	}
	
	private class StartOnCreate extends AsyncTask<Void, Void, Void> {

		private boolean success;
		private Uri base, other;
		
		public StartOnCreate(Uri base, Uri other) {
			this.base = base;
			this.other = other;
			success = false;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			while (!initialized) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					return null;
				}
			}
			success = true;
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result){
			if (success){
				startTransformProcess(base, other);
			}
		}
	}

	public void initializes() {

		fd_ = FeatureDetector.create(FeatureDetector.FAST);

		ref_file_ = new File(OUT_PATH, "reference.bmp");
		other_file_ = new File(OUT_PATH, "other.bmp");

		InitWidgets();

		data_ = Data.getInstance();
		Toast t = Toast.makeText(this, "Ready!", Toast.LENGTH_SHORT);
		t.show();
		initialized = true;
	}

	public void onClick(View v) {
		logd("onClick!");
		// TODO Auto-generated method stub
//		switch (v.getId()) {
//		case R.id.btnTakeRef:
//			logd("btnTakeRef clicekd!");
//			TakeReferenceImage();
//			break;
//
//		case R.id.btnTakeImg:
//			logd("btnTakeImg clicekd!");
//			TakeOtherImage();
//			break;
//		}

	}

	private void InitWidgets() {
		logd("InitWidgets()");
//		btnTakeRefImg = (Button) findViewById(R.id.btnTakeRef);
//		btnTakeImg = (Button) findViewById(R.id.btnTakeImg);

		
		btnTakeRefImg.setOnClickListener(this);
		btnTakeImg.setOnClickListener(this);
	}

	private void TakeReferenceImage() {
		Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		reference_image_uri_ = Uri.fromFile(ref_file_);
		cam.putExtra(MediaStore.EXTRA_OUTPUT, reference_image_uri_);
		startActivityForResult(cam, CAMERA_INTENT_REFERENCE);
	}

	private void TakeOtherImage() {
		Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		other_image_uri_ = Uri.fromFile(other_file_);
		cam.putExtra(MediaStore.EXTRA_OUTPUT, other_image_uri_);
		startActivityForResult(cam, CAMERA_INTENT_OTHER);
	}

	private void DisplayKP(Bitmap bmp) {
		data_.bmp[0] = GetBmpWithKP(bmp);
		Intent i = new Intent(this, Display.class);
		logd("putting image index to intent");
		i.putExtra("bitmapIdx", 0);
		logd("starting dispay activity");
		startActivity(i);
	}

	private Bitmap GetBmpWithKP(Bitmap bmp) {
		Mat image = new Mat();
		MatOfKeyPoint keypoints = new MatOfKeyPoint();
		KeyPoint[] mKeypoints;
		Point[] points;

		Utils.bitmapToMat(bmp, image);
		logd("doing feature detection");
		fd_.detect(image, keypoints);

		logd("drawing keypoints");
		mKeypoints = keypoints.toArray();
		logd("number of features: " + mKeypoints.length);
		points = new Point[mKeypoints.length];
		for (int i = 0; i < points.length; i++) {
			points[i] = mKeypoints[i].pt;
			Core.circle(image, points[i], (int) mKeypoints[i].size, new Scalar(
					255, 0, 0));
		}

		Bitmap result = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(image, result);

		return result;
	}

	private void DisplaySideBySide() {
		Intent i = new Intent(this, SideBySideDisplay.class);
		startActivity(i);
	}

	private void Transform() {
		// Original Matrix
		Mat refMat = new Mat();
		Mat otherMat = new Mat();
		// KeyPoints
		MatOfKeyPoint refMkp, otherMkp;
		// Point coordinate for the keypoints
		Point[] refPts, otherPts;

		// Data structures needed
		// The 2 matrices put to a list
		List<Mat> listM = new LinkedList<Mat>();
		listM.add(refMat);
		listM.add(otherMat);
		// List that holds the resulting keypoints
		List<MatOfKeyPoint> listMkp = new LinkedList<MatOfKeyPoint>();

		// Convert bitmap to Matrix
		Utils.bitmapToMat(referenceImage_, refMat);
		Utils.bitmapToMat(otherImage_, otherMat);
		//Utils.bitmapToMat(referenceImage_, otherMat);
		
		// Find keypoints
		fd_.detect(listM, listMkp);
		refMkp = listMkp.get(0);
		otherMkp = listMkp.get(1);

		KeyPoint[] refKp = refMkp.toArray();
		KeyPoint[] otherKp = otherMkp.toArray();

		int numKP = Math.min(refKp.length, otherKp.length);
		refPts = new Point[numKP];
		otherPts = new Point[numKP];

		for (int i = 0; i < numKP; i++) {
			refPts[i] = refKp[i].pt;
			otherPts[i] = otherKp[i].pt;
		}

		MatOfPoint2f refMatPt = new MatOfPoint2f(refPts);
		MatOfPoint2f baseMatPt = new MatOfPoint2f(otherPts);

		Mat m = Calib3d.findHomography(refMatPt, baseMatPt, Calib3d.RANSAC, 4);
		logd("Homography matrix size: " + m.size());

		Size sz = refMat.size();
		Mat result = new Mat(sz, refMat.type());
		Imgproc.warpPerspective(refMat, result, m, sz);

		Bitmap disp = Bitmap.createBitmap(result.cols(), result.rows(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(result, disp);

		data_.bmp[0] = disp;

		logd("invoking new activity");
		Intent i = new Intent(this, Display.class);
		i.putExtra(Display.DATA_BITMAP_INDEX, 0);
		startActivity(i);

		/*
		 * for(int i=0;i<kp.length;i++){ Core.circle(otherMat, kp[i].pt, (int)
		 * kp[i].size, new Scalar(255,0,0)); } Bitmap disp =
		 * Bitmap.createBitmap(otherMat.cols(), otherMat.rows(),
		 * Bitmap.Config.ARGB_8888); Utils.matToBitmap(otherMat, disp); Intent i
		 * = new Intent(this, Display.class); i.putExtra("image", disp);
		 * startActivity(i);
		 */
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		logd("onActivityResult()");
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case CAMERA_INTENT_REFERENCE:
			if (resultCode == RESULT_OK) {
				logd("Camera activity returns OK");
				// get the camera data
				try {
					referenceImage_ = Media.getBitmap(getContentResolver(),
							reference_image_uri_);
				} catch (FileNotFoundException e) {
					loge("reference image not found!");
					e.printStackTrace();
				} catch (IOException e) {
					loge("problem on reading the image file");
					e.printStackTrace();
				}

				// logd("referenceImage size: " + referenceImage_.getDensity());
				// (Bitmap) data.getExtras().get("data");

				// DisplayKP(referenceImage_);
			} else {
				logd("Camera activity returns CANCELLED");
			}

			break;
		case CAMERA_INTENT_OTHER:
			if (resultCode == RESULT_OK) {
				if (referenceImage_ == null) {
					loge("Take a reference image first!");
					return;
				}

				logd("Camera activity returns OK");
				// get the camera data
//				try {
//					otherImage_ = Media.getBitmap(getContentResolver(),
//							other_image_uri_);
//				} catch (FileNotFoundException e) {
//					loge("other image not found!");
//					e.printStackTrace();
//				} catch (IOException e) {
//					loge("problem on reading the image file");
//					e.printStackTrace();
//				}
//				logd("Camera activity returns OK");
//
//				data_.sbsBmp1 = GetBmpWithKP(referenceImage_);
//				data_.sbsBmp2 = GetBmpWithKP(otherImage_);
//				DisplaySideBySide();
//
//				// DisplayKP(otherImage_);
//				Transform();
				startTransformProcess(reference_image_uri_, other_image_uri_);
			} else {
				logd("Camera activity returns CANCELLED");
			}

			break;
		default:

			break;
		}
	}
	
	private boolean startTransformProcess(Uri reference, Uri other){
		
		if (reference == null || other == null)
			throw new IllegalArgumentException("Null URIs");
		
		try {
			reference_image_uri_ = reference;
			referenceImage_ = Media.getBitmap(getContentResolver(),
					reference_image_uri_);
		} catch (FileNotFoundException e) {
			loge("reference image not found!");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			loge("problem on reading the image file");
			e.printStackTrace();
			return false;
		}
		
		try {
			other_image_uri_ = other;
			otherImage_ = Media.getBitmap(getContentResolver(),
					other_image_uri_);
		} catch (FileNotFoundException e) {
			loge("other image not found!");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			loge("problem on reading the image file");
			e.printStackTrace();
			return false;
		}
		logd("Camera activity returns OK");

		data_.sbsBmp1 = GetBmpWithKP(referenceImage_);
		data_.sbsBmp2 = GetBmpWithKP(otherImage_);
		DisplaySideBySide();

		// DisplayKP(otherImage_);
		Transform();
		
		return true;
	}

	private void loge(String str) {
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
		Log.e(TAG, str);
	}

	private void logd(String str) {
		Log.d(TAG, str);
	}

}