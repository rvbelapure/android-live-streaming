
package edu.gatech.rts.stream.streaming.video;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaRecorder;
import android.util.Log;

import com.jcodec.mp4.MP4Data;

public class H264Stream extends VideoStream {

	static private SharedPreferences settings = null;
	
	private Semaphore lock = new Semaphore(0);
	private MP4Data mp4Config;
	
	public H264Stream(int cameraId) {
		super(cameraId);
		setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		this.packetizer = new com.jboss.netty.H264Packetizer();
	}
	
	static public void setPreferences(SharedPreferences prefs) {
		settings = prefs;
	}
	
	// Should not be called by the UI thread
	private MP4Data testH264() throws IllegalStateException, IOException {
		if (!qualityHasChanged && mp4Config!=null) return mp4Config;
		
		final String TESTFILE = "/sdcard/test.mp4";
		
		Log.i(TAG,"Testing H264 support...");
		
		// Save flash state & set it to false so that led remains off while testing h264
		boolean savedFlashState = flashState;
		flashState = false;
		
		// That means the H264Stream will behave as a regular MediaRecorder object
		// it will not start the packetizer thread and can be used to save the video
		// in a file
		setMode(MODE_DEFAULT);
		
		setOutputFile(TESTFILE);
		
		// Start recording
		prepare();
		start();
		
		// We wait a little and stop recording
		this.setOnInfoListener(new MediaRecorder.OnInfoListener() {
			public void onInfo(MediaRecorder mr, int what, int extra) {
				Log.d(TAG,"MediaRecorder callback called !");
				if (what==MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
					Log.d(TAG,"MediaRecorder: MAX_DURATION_REACHED");
				} else if (what==MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
					Log.d(TAG,"MediaRecorder: MAX_FILESIZE_REACHED");
				} else if (what==MEDIA_RECORDER_INFO_UNKNOWN) {
					Log.d(TAG,"MediaRecorder: INFO_UNKNOWN");
				} else {
					Log.d(TAG,"WTF ?");
				}
				lock.release();
			}
		});
		
		try {
			if (lock.tryAcquire(6,TimeUnit.SECONDS)) {
				Log.d(TAG,"MediaRecorder callback was called :)");
				Thread.sleep(400);
			} else {
				Log.d(TAG,"MediaRecorder callback was not called :(");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			stop();
		}
		
		
		// Retrieve SPS & PPS & ProfileId with MP4Config
		mp4Config = new MP4Data(TESTFILE);

		// Delete dummy video
		File file = new File(TESTFILE);
		if (!file.delete()) Log.e(TAG,"Temp file could not be erased");
		
		// Back to streaming mode & prepare
		setMode(MODE_STREAMING);
		
		// Restore flash state
		flashState = savedFlashState;
		
		Log.i(TAG,"H264 Test succeded...");
		
		// Save test result
		if (settings != null) {
			Editor editor = settings.edit();
			editor.putString(quality.frameRate+","+quality.resX+","+quality.resY, mp4Config.getProfileLevel()+","+mp4Config.getB64SPS()+","+mp4Config.getB64PPS());
			editor.commit();
		}
		return mp4Config;
		
	}
	
	public String generateSessionDescriptor() throws IllegalStateException, IOException {
		String profile,sps,pps;
		
		if (settings != null) {
			if (!settings.contains(quality.frameRate+","+quality.resX+","+quality.resY)) {
				testH264();
				profile = mp4Config.getProfileLevel();
				pps = mp4Config.getB64PPS();
				sps = mp4Config.getB64PPS();
			} else {
				String[] s = settings.getString(quality.frameRate+","+quality.resX+","+quality.resY, "").split(",");
				profile = s[0];
				sps = s[1];
				pps = s[2];
			}
		} else {
			testH264();
			profile = mp4Config.getProfileLevel();
			pps = mp4Config.getB64PPS();
			sps = mp4Config.getB64PPS();
		}

		return "m=video "+String.valueOf(getDestinationPort())+" RTP/AVP 96\r\n" +
				   "b=RR:0\r\n" +
				   "a=rtpmap:96 H264/90000\r\n" +
				   "a=fmtp:96 packetization-mode=1;profile-level-id="+profile+";sprop-parameter-sets="+sps+","+pps+";\r\n";
	}
	
}
