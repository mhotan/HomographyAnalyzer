package edu.uw.homographyanalyzer.quicktransform;

import com.example.homographyanalyzer.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class SideBySideDisplay extends Activity implements View.OnClickListener{
	private final String TAG = "TransDemo";
	private ImageView[] img;
	private Button btnToggle;
	//toggle between first and second image
	private boolean img1shown = true;
	
	private Data data_;
	
	private void initWidgets(){
		img = new ImageView[2];
		
		img[0] = (ImageView) findViewById(R.id.imgView1);
		img[1] = (ImageView) findViewById(R.id.imgView2);
		
		btnToggle = (Button) findViewById(R.id.btnToggle);
		btnToggle.setOnClickListener(this);
		/*
		for(int i=0;i<2;i++){
			img[i].setOnTouchListener(this);
		}
		*/
		
		img[0].setVisibility(View.VISIBLE);
		img[1].setVisibility(View.GONE);
		
		data_ = Data.getInstance();
	}
	
	@Override
	public void onCreate(Bundle bdl){
		super.onCreate(bdl);
		setContentView(R.layout.sidebysidedisplay);
		initWidgets();
		img[0].setImageBitmap(data_.sbsBmp1);
		img[1].setImageBitmap(data_.sbsBmp2);
		
	}
	
	public void toggleVisible(){
		if(img1shown){
			img[0].setVisibility(View.GONE);
			img[1].setVisibility(View.VISIBLE);
		}
		else{
			img[1].setVisibility(View.GONE);
			img[0].setVisibility(View.VISIBLE);
		}
		img1shown = !img1shown;
	}

	//@Override
	public boolean onTouch(View v, MotionEvent event) {
		logd("on touch");
		toggleVisible();
		return false;
		
		
		//TODO: Implement swipe gestures
		//return gest
		
		/*
		switch(v.getId()){
		case  R.id.imgView1:
			
			break;
				
		case R.id.imgView2:
			
			break;
		}
		*/
	}
	
	private class GestureController extends GestureDetector.SimpleOnGestureListener {
		/*
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			
		}
	*/	
		
	}
	
	
    
	private void loge(String str){
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
		Log.e(TAG, str);
	}
	
	private void logd(String str){
		Log.d(TAG, str);
	}

	public void onClick(View v) {
		logd("on click");
		toggleVisible();
		// TODO Auto-generated method stub
		
	}
}
