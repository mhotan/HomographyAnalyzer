package edu.uw.homographyanalyzer.main;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.example.homographyanalyzer.R;

import edu.uw.homographyanalyzer.camera.ExternalApplication;
import edu.uw.homographyanalyzer.global.GlobalLogger;
import edu.uw.homographyanalyzer.global.LoggerInterface;

public class MainActivity extends Activity implements LoggerInterface {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homography);
        
        // This needs to be done first because many other components
        // depend on this global logger
        new GlobalLogger(this);
        File n = new File("/mnt/sdcard/download/", "a.bmp");
        throw new RuntimeException(n.canWrite() + " " + n.canRead());
        
        /*
        Intent i = new Intent(this, ExternalApplication.class);
        startActivityForResult(i, 1);
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_homography, menu);
        return true;
    }

	@Override
	public void Logd(String msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void Loge(String msg) {
		// TODO Auto-generated method stub
		
	}

}
