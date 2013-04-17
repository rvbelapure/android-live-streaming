

package edu.gatech.rts.stream.app;

import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.gatech.rts.stream.rtsp.RtspServer;
import edu.gatech.rts.stream.streaming.video.H264Stream;
import edu.gatech.rts.stream.streaming.video.VideoQuality;

public class AndroidStreamingApp extends Activity  {
    
    static final public String TAG = "AndroidStreamingApp"; 
    

    private ImageView led;
    private PowerManager.WakeLock wl;
    private RtspServer rtspServer = null;
    private SurfaceHolder holder;
    private SurfaceView camera;
    private TextView console, status;
    private VideoQuality defaultVideoQuality = new VideoQuality();
    private Context context;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        //console = (TextView) findViewById(R.id.console);
        status = (TextView) findViewById(R.id.status);
        getWindowManager().getDefaultDisplay();
        context = this.getApplicationContext();
        led = (ImageView)findViewById(R.id.led);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        H264Stream.setPreferences(settings);
        defaultVideoQuality.resX = settings.getInt("video_resX", 640);
        defaultVideoQuality.resY = settings.getInt("video_resY", 480);
        defaultVideoQuality.frameRate = Integer.parseInt(settings.getString("video_framerate", "15"));
        defaultVideoQuality.bitRate = Integer.parseInt(settings.getString("video_bitrate", "500"))*1000; // 500 kb/s
       	
        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();
		
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "edu.gatech.rts.stream.app.wakelock");
        
        RtspSession.setSurfaceHolder(holder);
        RtspSession.setDefaultVideoQuality(defaultVideoQuality);
        RtspSession.setDefaultAudioEncoder(settings.getBoolean("stream_audio", true)?Integer.parseInt(settings.getString("audio_encoder", "1")):0);
        RtspSession.setDefaultVideoEncoder(settings.getBoolean("stream_video", true)?Integer.parseInt(settings.getString("video_encoder", "1")):0);
        
        if (settings.getBoolean("enable_rtsp", true)) rtspServer = new RtspServer(8086, handler);
        
    }
    
    public void onStart() {
    	super.onStart();
    	// Lock screen
    	wl.acquire();
    }
    	
    public void onStop() {
    	super.onStop();
    	wl.release();
    }
    
    public void onResume() {
    	super.onResume();
    	
    	// Determines if user is connected to a wireless network & displays ip 
    	displayIpAddress();
    	
    	startServers();
    	
    	registerReceiver(wifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    	//handler.postDelayed(logoAnimation, 7000);
    	
    }
    
    public void onPause() {
    	super.onPause();
    	stopServers();
    	unregisterReceiver(wifiStateReceiver);
    	//handler.removeCallbacks(logoAnimation);
    }
    
    private void stopServers() {
    	if (rtspServer != null) rtspServer.stop();
    }
    
    private void startServers() {
    	if (rtspServer != null) {
    		try {
    			rtspServer.start();
    		} catch (IOException e) {
    			log("RtspServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
    		}
    	}
    }
    
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		displayIpAddress();
        	}
        } 
    };
    
    private boolean streaming = false;
    
    // The Handler that gets information back from the RtspServer
    private final Handler handler = new Handler() {
    	
    	public void handleMessage(Message msg) { 
    		
    		switch (msg.what) {
    			
    		case RtspServer.MESSAGE_LOG:
    			Toast.makeText(context, (String)msg.obj, 500).show();
    			break;

    		case RtspServer.MESSAGE_ERROR:
    			Toast.makeText(context, (String)msg.obj, 1500).show();
    			break;
    			
    		case RtspSession.MESSAGE_START:
    			if (!streaming) handler.postDelayed(ledAnimation, 100);
    			streaming = true;
    			status.setText(R.string.streaming);
    			break;
    		case RtspSession.MESSAGE_STOP:
    			streaming = false;
    			handler.removeCallbacks(ledAnimation);
    			displayIpAddress();
    			break;

    		case RtspSession.MESSAGE_ERROR:
    			Toast.makeText(context, (String)msg.obj, 1000).show();
    			break;

    		}
    	}
    	
    };
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
    	
        switch (item.getItemId()) {
        case R.id.options:
            // Starts QualityListActivity where user can change the streaming quality
            intent = new Intent(this.getBaseContext(),VideoOptAct.class);
            startActivityForResult(intent, 0);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
    	if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	    	status.setText("rtsp://");
	    	status.append(String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff));
	    	status.append(":8086/");
	    	led.setImageResource(R.drawable.led_green);
    	} else {
    		led.setImageResource(R.drawable.led_red);
    		status.setText(R.string.warning);
    	}
    }
    
    public void log(String s) {
    	String t = console.getText().toString();
    	if (t.split("\n").length>8) {
    		console.setText(t.substring(t.indexOf("\n")+1, t.length()));
    	}
    	console.append(Html.fromHtml(s+"<br />"));
    }

	private boolean ledState = true; 
	
	private void toggleLed() {
		if (ledState) {
			ledState = false;
			led.setImageResource(R.drawable.led_green);
		} else {
			ledState = true;
			led.setImageResource(getResources().getColor(android.R.color.transparent));
		}
	}
	
	private Runnable ledAnimation = new Runnable() {
		public void run() {
			toggleLed();
			handler.postDelayed(this,900);
		}
	};
    
    
}