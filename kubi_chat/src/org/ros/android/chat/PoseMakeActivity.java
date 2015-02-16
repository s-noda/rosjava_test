package org.ros.android.chat;


import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import org.ros.android.chat.R;

public class PoseMakeActivity extends Activity{

	final private String TAG = "PoseMakeActivity";
	
	private EditText pose_title_edit;
	private ImageButton back_button, motion_record_button;
	
	private int motion_button_color ;
	private int active_color = Color.RED;
	private int negative_color = Color.TRANSPARENT;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pose_make_act);
		
		this.back_button = (ImageButton)findViewById(R.id.pose_craete_back_to_home_button);
		this.back_button.setOnClickListener( new OnClickListener(){
			@Override
			public void onClick(View v) {
				PoseMakeActivity.this.finish();
			}
		} );
		
		this.pose_title_edit = (EditText) findViewById(R.id.pose_title_edit);
		
		this.motion_button_color = this.negative_color;
		this.motion_record_button = (ImageButton) findViewById(R.id.pose_register_button);
		this.motion_record_button.setBackgroundColor(this.motion_button_color);
		this.motion_record_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//DemoMakeActivity.this.voice_text_button.setBackgroundColor(Color.GRAY);
				String pose_text = PoseMakeActivity.this.pose_title_edit.getText().toString();
				if ( pose_text.length() == 0 ){
					PoseMakeActivity.this.runOnUiThread(
							new Runnable(){
								@Override
								public void run(){
									Toast.makeText(PoseMakeActivity.this, "error: no title", Toast.LENGTH_LONG).show();
								}});	
				} else if ( PoseMakeActivity.this.motion_button_color == PoseMakeActivity.this.active_color ){
					PoseMakeActivity.this.motion_button_color = PoseMakeActivity.this.negative_color;
					if ( RobotBarActivity.rb_node != null && pose_text.length() > 0){
						RobotBarActivity.rb_node.publishStringStatus("motion-record-off:"+pose_text);
					}
				} else {
					PoseMakeActivity.this.motion_button_color = PoseMakeActivity.this.active_color;
					if ( RobotBarActivity.rb_node != null && pose_text.length() > 0){
						RobotBarActivity.rb_node.publishStringStatus("motion-record-on:"+pose_text);
					}
				}
				PoseMakeActivity.this.motion_record_button.setBackgroundColor(PoseMakeActivity.this.motion_button_color);
			}});
	}

	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy() ;
	}
	
	@Override
	public void finalize() throws Throwable{
		onDestroy();
		super.finalize();
	}
	
}
