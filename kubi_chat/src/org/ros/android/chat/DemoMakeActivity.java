package org.ros.android.chat;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import org.ros.android.chat.R;
import org.ros.android.chat.RobotBarNode.TaggedIcon;

public class DemoMakeActivity extends Activity {

	final private String TAG = "DemoMakeActivity";

	private EditText demo_title_edit, voice_text_edit;
	private ImageButton back_button;
	private Button move_to_pose_button;
	private ImageButton register_button, voice_text_button;

	private LinearLayout tagged_motion_button_layout;
	// private LinearLayout selected_motion_layout;
	private View selected_motion_view;
	public static ArrayList<TaggedIcon> demo_icons;
	//private Drawable demo_bg_icon, voice_text_bg_icon;

	private ProgressDialog pDialog;
	
	private int voice_text_button_color ;
	private int active_color = Color.RED;
	private int negative_color = Color.TRANSPARENT;
	
	private boolean rosparam_loading;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.demo_make_act);

		this.rosparam_loading = false;
		
		this.pDialog = new ProgressDialog(this);
		this.pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		this.pDialog.setIndeterminate(false);
		this.pDialog.setCancelable(true);

		this.tagged_motion_button_layout = (LinearLayout) this
				.findViewById(R.id.taged_pose_image_buttons);
		// this.selected_motion_layout = (LinearLayout) this
		// .findViewById(R.id.selected_pose_image_view);
		this.selected_motion_view = this.tagged_motion_button_layout
				.getChildAt(0);
		//this.demo_bg_icon = this.selected_motion_view.getBackground();
		this.selected_motion_view.setBackgroundColor(Color.GREEN);

		this.back_button = (ImageButton) findViewById(R.id.demo_craete_back_to_home_button);
		this.back_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DemoMakeActivity.this.finish();
			}
		});

		this.move_to_pose_button = (Button) findViewById(R.id.pose_craete_move_button);
		this.move_to_pose_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(DemoMakeActivity.this
						.getApplicationContext(), PoseMakeActivity.class);
				DemoMakeActivity.this.startActivity(i);
			}
		});
		
		this.demo_title_edit = (EditText) findViewById(R.id.demo_title_edit);
		this.voice_text_edit = (EditText) findViewById(R.id.voice_text_edit);
		this.register_button = (ImageButton) findViewById(R.id.demo_register_button);
		this.register_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DemoMakeActivity.this.pDialog.show();
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (RobotBarActivity.rb_node != null
								&& DemoMakeActivity.this.demo_title_edit
										.getText().length() > 0) {
							byte[] icon = null;
							try {
								ImageButton ib = (ImageButton) DemoMakeActivity.this.selected_motion_view;
								BitmapDrawable bd = (BitmapDrawable) ib
										.getDrawable();
								Bitmap bm = bd.getBitmap();
								String txt = DemoMakeActivity.this.demo_title_edit.getText().toString();
								if ( txt.length() > 0 ){
									Bitmap mutableBitmap = bm.copy(Config.ARGB_8888, true);
									bm.recycle();
									bm = mutableBitmap;
									Canvas cv = new Canvas(bm);  
									Paint paint = new Paint();
									paint.setColor(Color.BLACK);
									setTextSizeForWidth(paint, bm.getWidth(), txt + "  ");
									cv.drawText(txt, 1, bm.getHeight() - 1, paint);  
								}
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								bm.compress(CompressFormat.PNG, 50, baos);
								icon = baos.toByteArray();
							} catch (Exception e) {
								e.printStackTrace();
							}
							String voice_text = DemoMakeActivity.this.voice_text_edit
									.getText().toString();
							boolean error = true;
							if (voice_text.length() > 0) {
								error = error & RobotBarActivity.rb_node.registerSound(
										DemoMakeActivity.this.demo_title_edit
												.getText().toString(),
										voice_text, null, null);
							}
							error = error & RobotBarActivity.rb_node.registerDemo(
									DemoMakeActivity.this.demo_title_edit
											.getText().toString(), icon,
									DemoMakeActivity.this.selected_motion_view
											.getTag().toString(),
									DemoMakeActivity.this.demo_title_edit
											.getText().toString());
							if ( ! error ){
								DemoMakeActivity.this.runOnUiThread(
										new Runnable(){
											@Override
											public void run(){
												Toast.makeText(DemoMakeActivity.this, "error: server missing", Toast.LENGTH_LONG).show();
											}});
							} else {
								DemoMakeActivity.this.runOnUiThread(
										new Runnable(){
											@Override
											public void run(){
												Toast.makeText(DemoMakeActivity.this, "registered", Toast.LENGTH_LONG).show();
											}});
							}
						} else {
							DemoMakeActivity.this.runOnUiThread(
									new Runnable(){
										@Override
										public void run(){
											Toast.makeText(DemoMakeActivity.this, "error: no title", Toast.LENGTH_LONG).show();
										}});	
						}
						DemoMakeActivity.this.pDialog.dismiss();
					}
				}).start();
			}
		});
		
		this.voice_text_button_color = this.negative_color;
		this.voice_text_button = (ImageButton) findViewById(R.id.voice_text_button);
		this.voice_text_button.setBackgroundColor(this.voice_text_button_color);
		this.voice_text_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//DemoMakeActivity.this.voice_text_button.setBackgroundColor(Color.GRAY);
				String voice_text = DemoMakeActivity.this.voice_text_edit.getText().toString();
				if ( DemoMakeActivity.this.voice_text_button_color == DemoMakeActivity.this.active_color ){
					DemoMakeActivity.this.voice_text_button_color = DemoMakeActivity.this.negative_color;
					if ( RobotBarActivity.rb_node != null && voice_text.length() > 0){
						RobotBarActivity.rb_node.publishStringStatus("sound-record-off:"+voice_text);
					}
				} else {
					DemoMakeActivity.this.voice_text_button_color = DemoMakeActivity.this.active_color;
					if ( RobotBarActivity.rb_node != null && voice_text.length() > 0){
						RobotBarActivity.rb_node.publishStringStatus("sound-record-on:"+voice_text);
					}
				}
				DemoMakeActivity.this.voice_text_button.setBackgroundColor(DemoMakeActivity.this.voice_text_button_color);
			}});
		
		if ( demo_icons == null ) demo_icons = new ArrayList<TaggedIcon>();
		if ( demo_icons.size() > 0 ) registerIcons(demo_icons.size());

	}
	
	public void registerIcons(int cnt){
		for (int i = 0; i < cnt; i++) {
			TaggedIcon ic = demo_icons.get(i);
			if (ic.icon != null) {
				final ImageButton imageButton = new ImageButton(this);
				imageButton.setScaleType(ScaleType.FIT_XY);
				imageButton.setAdjustViewBounds(true);
				imageButton.setImageBitmap(ic.icon);
				imageButton.setTag(ic.tag);
				imageButton
						.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								onClickMotionIcon(v);
							}
						});
				DemoMakeActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						DemoMakeActivity.this.tagged_motion_button_layout
								.addView(imageButton);
					}
				});
			} else {
				final Button bt = new Button(this);
				bt.setTag(ic.tag);
				bt.setText(ic.name);
				bt.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						onClickMotionIcon(v);
					}
				});
				DemoMakeActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						DemoMakeActivity.this.tagged_motion_button_layout
								.addView(bt);
					}
				});
			}
		}
	}
	
	public void setTextSizeForWidth(Paint paint, float desiredWidth,
	        String text) {
	    final float testTextSize = 48f;
	    paint.setTextSize(testTextSize);
	    Rect bounds = new Rect();
	    paint.getTextBounds(text, 0, text.length(), bounds);
	    float desiredTextSize = testTextSize * desiredWidth / bounds.width();
	    paint.setTextSize(desiredTextSize);
	}

	public void updateMotionIcons() {
		if (RobotBarActivity.rb_node != null && ! this.rosparam_loading ) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					DemoMakeActivity.this.rosparam_loading = true;
					int cnt = RobotBarActivity.rb_node.getNewDemos(
							DemoMakeActivity.demo_icons,
							RobotBarNode.motion_head_string);
					if ( cnt < 0 ){
						DemoMakeActivity.this.runOnUiThread(
								new Runnable(){
									@Override
									public void run(){
										Toast.makeText(DemoMakeActivity.this, "error: server missing", Toast.LENGTH_LONG).show();
									}});
					} else {
						DemoMakeActivity.this.registerIcons(cnt);
					}
					DemoMakeActivity.this.rosparam_loading = false;
				}
			}).start();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateMotionIcons();
	}

	public void onClickMotionIcon(final View v) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				DemoMakeActivity.this.selected_motion_view//.setBackground(DemoMakeActivity.this.demo_bg_icon);
						.setBackgroundColor(Color.GRAY);
				DemoMakeActivity.this.selected_motion_view = v;
				//DemoMakeActivity.this.demo_bg_icon = DemoMakeActivity.this.selected_motion_view.getBackground();
				DemoMakeActivity.this.selected_motion_view
						.setBackgroundColor(Color.GREEN);
				// DemoMakeActivity.this.selected_motion_layout.removeAllViews();
				// DemoMakeActivity.this.selected_motion_layout.addView(v);
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void finalize() throws Throwable {
		onDestroy();
		super.finalize();
	}

}
