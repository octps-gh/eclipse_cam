package com.littlecheesecake.shadercam;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;

import android.widget.ImageButton;
import android.widget.Toast;

import com.littlecheesecake.shadercam.gl.CameraRenderer;
import com.littlecheesecake.shadercameraexample.R;



public class MainActivity extends Activity {
	private CameraRenderer mRenderer;
	ImageButton imgBtnVert;
	ImageButton imgBtnHori;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);
		mRenderer = (CameraRenderer)findViewById(R.id.renderer_view);
		
		addListenerOnButton();
	}

	public void addListenerOnButton() {

		imgBtnVert = (ImageButton) findViewById(R.id.ibtn_vert);
		imgBtnHori = (ImageButton) findViewById(R.id.ibtn_hori);
		imgBtnVert.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
			   Toast.makeText(MainActivity.this,"Vertical ImgButton is clicked!", Toast.LENGTH_SHORT).show();
			}
		});
		
		imgBtnHori.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
			   Toast.makeText(MainActivity.this,"Horizontal ImgButton is clicked!", Toast.LENGTH_SHORT).show();
			}
		});
		

	}
	
	@Override
	public void onStart(){
		super.onStart();

	}
	
	
	@Override
	public void onPause(){
		super.onPause();
		mRenderer.onDestroy();
		
	}
	
	@Override
	public void onResume(){
		super.onResume();
		mRenderer.onResume();
	}

}
