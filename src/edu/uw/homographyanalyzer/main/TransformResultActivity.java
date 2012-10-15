package edu.uw.homographyanalyzer.main;

import com.example.homographyanalyzer.R;
import com.example.homographyanalyzer.R.layout;
import com.example.homographyanalyzer.R.menu;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

public class TransformResultActivity extends Activity {

	private final String TAG = "TransformResultActivity";
	
	//////////////////////////////////////////////////
	////  Activity Overridden Methods
	//////////////////////////////////////////////////
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transform_result);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_transform_result, menu);
        return true;
    }

    @Override
    protected void onStart(){
    	super.onStart();
    	
    	// Obtain the URIs from before
    	Intent resultIntent = getIntent();
    	if (resultIntent == null) {
    		// Activity started without valid results
    		// 
    		Log.e(TAG, "Started without uris for result");
    		finish();
    	}
    	
    	resultIntent.getParcelableExtra(name)
    		
    }
    
    @Override
    protected void onRestart(){
    	super.onRestart();
    } 
    
    @Override
    protected void onResume(){
    	super.onResume();
    } 
   
    @Override
    protected void onPause(){
    	super.onPause();
    } 
    
    @Override
    protected void onStop(){
    	super.onStop();
    } 
   
    @Override
    protected void onDestroy(){
    	super.onDestroy();
    } 
}
