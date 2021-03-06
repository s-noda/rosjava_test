package org.ros.android.chat;


import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ros.android.RosDialogActivity;
import org.ros.android.chat.RosChatNode.RosFloatVectorCallback;
import org.ros.android.chat.RosChatNode.RosStringCallback;
import org.ros.android.chat.R;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Subscriber;

import std_msgs.Float32MultiArray;

public class RosChatActivity extends RosDialogActivity implements SurfaceHolder.Callback, Runnable{

	final private String TAG = "ChatActivity";
	
	private CompressedImageView image_view ;
	private CompressedImageView image_view_small ;
	private TextView bottom_notf ;
	private EditText edit_text;
	
	private RosChatNode chatnode;
	private Camera camera ;
	private SurfaceView surf ;
	private int camera_width=-1, camera_height=-1 ;
	
	private ImagePublishNode image_publisher ;
	private AudioPubSubNode audio_node;
	private AndroidPosePubNode pose_node;
	private KubiControlNode kubi_node;
	private TextPubNode text_node;
	
	private Thread chat_observer ;
	private boolean image_publishing ;
	
	private static boolean client_p = true;
	public static String node_name = "kubi_chat" ;
	static{
		if ( client_p ) node_name = "ros_chat";
	}
	
	public RosChatActivity() {
		super(node_name, node_name, node_name);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		this.image_publishing = false ;
		
		this.surf = (SurfaceView) findViewById(R.id.camera_surface) ;
		SurfaceHolder holder = this.surf.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		// replace tagged button images
		LinearLayout tagged_button = (LinearLayout) findViewById(R.id.taged_image_buttons) ;
		tagged_button.removeAllViews();
		if ( client_p ){
			// replace tagged button images
			String[] tagNames = new String[]{"fuza1","fuza2","my1","my2","my3","my4"};
			for (String imageName : tagNames) {
				R.drawable rDrawable = new R.drawable();
				Field field;
				int resId;
				try {
					field = rDrawable.getClass().getField(imageName);
					resId = field.getInt(rDrawable);
					BitmapFactory.Options options = new  BitmapFactory.Options();
					// options.inMutable = true;
					Bitmap image = BitmapFactory.decodeResource(getResources(),
							resId,options);
					//
					ImageButton imageButton = new ImageButton(this);
					imageButton.setScaleType(ScaleType.FIT_XY);
					imageButton.setAdjustViewBounds(true);
					imageButton.setImageBitmap(image);
					imageButton.setTag(imageName);
					imageButton.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View v) {
							RosChatActivity.this.chatnode.publishStringStatus("tag:" + v.getTag());
						}
					});
					tagged_button.addView(imageButton);
				} catch (Exception e) {
					System.out.println("[place tagged image] " + imageName + " fail!! ");
					e.printStackTrace();
				}
			}
		} else {
			tagged_button.setWeightSum(0);
		}
	}
	
	public void initializeNodes(){
		node_name = getNodename();
		this.chatnode = new RosChatNode(this.getApplicationContext());

		this.image_view = (CompressedImageView) findViewById(R.id.compressed_image_view) ;
		this.image_view.setTopicName( node_name + "/request/" + "image/raw/compressed") ;
		this.image_view.setMessageType(sensor_msgs.CompressedImage._TYPE);
		this.image_view.setTalker(this.chatnode) ;
		
		this.image_view_small = (CompressedImageView) findViewById(R.id.compressed_image_view_small) ;
		this.image_view_small.setTopicName( node_name + "/request/" + "image/raw/small/compressed") ;
		this.image_view_small.setMessageType(sensor_msgs.CompressedImage._TYPE);
		this.image_view_small.setNodeName(node_name + "/compressed_image_view_small");
		
		this.audio_node = new AudioPubSubNode(node_name);
		
		this.bottom_notf = (TextView) findViewById(R.id.bottom_notification_text) ;
		
		this.edit_text = (EditText) findViewById(R.id.editabletextbox);
		
		//this.connect() ;
		
		this.chat_observer = new Thread(this) ;
		this.chat_observer.start() ;
		
		this.image_view.setChat(this.chatnode);
		this.image_publisher = new ImagePublishNode() ;
		
		this.chatnode.setStringCallback( new RosStringCallback(){
			@Override
			public void messageArrive(String topic, String msg) {
				final String tp = topic ;
				final String mg = msg ;
				Log.d(TAG, "[Message arrive] " + mg + " at " + tp);
				RosChatActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						RosChatActivity.this.bottom_notf.setText("[Message arrive] " + mg + " at " + tp ) ;
					}
				});
			}
		} ) ;
		this.chatnode.setFloatVectorCallback( new RosFloatVectorCallback(){
			@Override
			public void messageArrive(String topic, float[] msg) {
				final String tp = topic ;
				final String mg = msg[0] + " " + msg[1] ;
				Log.d(TAG, "[Message arrive] " + mg + " at " + tp);
				RosChatActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						RosChatActivity.this.bottom_notf.setText("[Message arrive] " + mg + " at " + tp ) ;
					}
				});
			}
		} ) ;
		
		this.pose_node = new AndroidPosePubNode(node_name, (SensorManager)getSystemService(SENSOR_SERVICE));
		this.pose_node.onResume();

		this.text_node = new TextPubNode(this.nodename_org, this.edit_text);
		
		if ( ! client_p ){
			this.kubi_node = new KubiControlNode(this, node_name);
		}
	}
	
	
	@Override
	protected void init(NodeMainExecutor nodeMainExecutor) {
		
		initializeNodes();
		
		NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
				getHostname(), getMasterUri());
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				RosChatActivity.this.bottom_notf.setText(
						RosChatActivity.this.bottom_notf.getText()
						+ "/ROS: " + getHostname() + " --[connect]--> " + getMasterUri());
			}
		});
		nodeMainExecutor.execute(this.image_view, nodeConfiguration);
		nodeMainExecutor.execute(this.image_view_small, nodeConfiguration);
		nodeMainExecutor.execute(this.image_publisher, nodeConfiguration);
		nodeMainExecutor.execute(this.chatnode, nodeConfiguration);
		nodeMainExecutor.execute(this.audio_node, nodeConfiguration);
		nodeMainExecutor.execute(this.pose_node, nodeConfiguration);
		nodeMainExecutor.execute(this.text_node, nodeConfiguration);
		if ( this.kubi_node != null ) nodeMainExecutor.execute(this.kubi_node, nodeConfiguration);
		
		// vibration
		nodeMainExecutor.execute( new AbstractNodeMain(){
	        private Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
	        
			@Override
			public GraphName getDefaultNodeName() {
				return GraphName.of( RosChatActivity.node_name + "/vibrate_node");
			}
			@Override
			public void onStart(final ConnectedNode connectedNode) {
				Subscriber<std_msgs.Int64> subscriber1 = connectedNode
						.newSubscriber(RosChatActivity.node_name + "/request/vibrate", std_msgs.Int64._TYPE) ;
				subscriber1
						.addMessageListener(new MessageListener<std_msgs.Int64>() {
							@Override
							public void onNewMessage(std_msgs.Int64 tm) {
								vibrator.vibrate(tm.getData());
							}
						}, 1) ;
			}
		}, nodeConfiguration);
		
		if ( this.camera != null && ! this.image_publishing) {
			this.image_publishing = true ;
			Camera.Parameters param = this.camera.getParameters() ;
			this.image_publisher.startImagePublisher(this.camera, param.getPreviewSize().width, param.getPreviewSize().height) ;
		}
			
	}
	
//	private void openCamera(SurfaceHolder holder) throws IOException {
//		if (myCamera == null) {
//			// myCamera = Camera.open();
//			// if (myCamera == null) {
//			// myCamera = Camera.open(0);
//			// }
//			try {
//				myCamera = Camera.open(this.cameraId);
//			} catch (NoSuchMethodError e) {
//				e.printStackTrace();
//				myCamera = Camera.open();
//			}
//			if (myCamera == null) {
//				throw new IOException();
//			}
//		}
//		myCamera.setPreviewDisplay(holder);
//		// if (gl == null) {
//		myCamera.setOneShotPreviewCallback(this);
//		myCamera.startPreview();
//		// }
//		// myCamera.setOneShotPreviewCallback(this) ;
//	}
	
	private void setupCamera(){
		int cameraId = 0;
		Camera.CameraInfo info = new Camera.CameraInfo() ;
		for ( int i = 0; i< Camera.getNumberOfCameras() ; i++ ){
			Camera.getCameraInfo(i, info) ;
			if ( info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
				cameraId = i ;
			}
		}
		//
		try {
			if ( this.camera == null ){
				this.camera = Camera.open(cameraId) ;
			}
		} catch ( Exception e ){
			e.printStackTrace();
			this.camera = null;
			return ;
		}
		if (this.camera_height < 0 || this.camera_width < 0) {
			int width, height ;
			width = height = 300 ;
			double min = 1e+30 ;
			double target_size = width*height ;

			Camera.Parameters param = this.camera.getParameters();
			List<Camera.Size> sizes = param.getSupportedPictureSizes();
			for (Camera.Size size : sizes) {
				Log.d(TAG, "camera size = " + size.width + "x" + size.height);
				if (Math.abs(target_size - size.width * size.height) < min) {
					try {
						Log.d(TAG, "set camera size = " + size.width + "x"
								+ size.height);
						param.setPreviewSize(size.width, size.height);
						this.camera.setParameters(param);
						min = Math.abs(target_size - size.width * size.height);
						width = size.width;
						height = size.height;
					} catch (Exception e) {
						Log.d(TAG, "?? unsupported " + size.width + "x"
								+ size.height);
					}
				}
			}
			this.camera_width = width ;
			this.camera_height = height;

//			this.runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					float view_rate = (float) Math.min(
//							300.0 / ChatActivity.this.camera_width,
//							300.0 / ChatActivity.this.camera_height);
//					// ViewGroup.LayoutParams view_param =
//					// this.surf.getLayoutParams();
//					FrameLayout.LayoutParams view_param = new FrameLayout.LayoutParams(
//							300, 300);
//					view_param.width = (int) (view_rate * ChatActivity.this.camera_width);
//					view_param.height = (int) (view_rate * ChatActivity.this.camera_height);
//					ChatActivity.this.surf.setLayoutParams(view_param);
//				}
//			});
		}

		int rotation = getWindowManager().getDefaultDisplay()
	             .getRotation();
	     int degrees = 0;
	     switch (rotation) {
	         case Surface.ROTATION_0: degrees = 0; break;
	         case Surface.ROTATION_90: degrees = 1; break;
	         case Surface.ROTATION_180: degrees = 2; break;
	         case Surface.ROTATION_270: degrees = 3; break;
	     }
	     // this.image_publisher.setRotateCnt(degrees);
	     degrees = degrees*90 ;
	     int result;
	     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	         result = (info.orientation + degrees) % 360;
	         result = (360 - result) % 360;
	     } else { 
	         result = (info.orientation - degrees + 360) % 360;
	     }
	     this.camera.setDisplayOrientation(result);
		//this.camera.startPreview();
	}
	
//	private long last_connect_trial ;
//	public void connect(){
//		Log.e("chatActivity"," try to connect") ;
//		if ( ! this.isDestroyed() && ! this.isFinishing() && (this.last_connect_trial + 3000 < System.currentTimeMillis())){
//			this.last_connect_trial = System.currentTimeMillis() ;
//		}
//	}
	
	@Override
	public void onResume() {
		super.onResume();
		if ( this.pose_node != null ){
			this.pose_node.onResume();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if ( this.pose_node != null ){
			this.pose_node.onPause();
		}
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy() ;
		if ( this.camera != null ){
			this.image_publisher.stopImagePublisher() ;
			this.camera.stopPreview();
			this.camera.release() ;
			this.camera = null ;
		}
		this.chatnode.onDestroy();
		this.chat_observer = null ;
		this.audio_node.onDestroy();
		if ( this.kubi_node != null ) this.kubi_node.onDestroy();
	}
	
	@Override
	public void finalize() throws Throwable{
		onDestroy();
		super.finalize();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		System.out.println("-- RosChatActiivty surfaceChanged called");
		setupCamera() ;
		try {
           if ( this.camera != null ){
        	   this.camera.setPreviewDisplay(holder);
        	   this.camera.startPreview();
           }
        } catch (Exception e) {
            e.printStackTrace();
        }
		if ( ! this.image_publishing && this.camera != null && this.image_publisher != null ){
			this.image_publishing = true;
			Camera.Parameters param = this.camera.getParameters() ;
			this.image_publisher.startImagePublisher(this.camera, param.getPreviewSize().width, param.getPreviewSize().height) ;
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		System.out.println("-- RosChatActiivty surfaceCreated called");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		System.out.println("-- RosChatActiivty surfaceDestroyed called");
		if ( this.camera != null && this.image_publishing){
			this.image_publishing = false;
			this.image_publisher.stopImagePublisher() ;
			this.camera.stopPreview();
			this.camera.release() ;
			this.camera = null ;
		}
	}

	@Override
	public void run() {
		while ( this.chat_observer != null ){
			try {
				Thread.sleep(300) ;
				if ( this.pose_node != null){
					//this.pose_node.pubTwist();
					this.pose_node.pubPose();
				}
				if ( this.kubi_node != null ){
					this.kubi_node.pantltPublish();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}
	
}
