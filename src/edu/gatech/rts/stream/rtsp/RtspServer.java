
package edu.gatech.rts.stream.rtsp;

import java.io.IOException;


import android.os.Handler;
import android.util.Log;


public class RtspServer {
	
	final static String TAG = "RtspServer";

	// Message types for UI thread
	public static final int MESSAGE_LOG = 2;
	public static final int MESSAGE_ERROR = 6;

	private final Handler handler;
	private final int port;
	private RequestDispatcher listenerThread;

	public RtspServer(int port, Handler handler) {
		this.handler = handler;
		this.port = port;
	}
	
	public void start() throws IOException {
		listenerThread = new RequestDispatcher(port,handler);
		listenerThread.start();
	}
	
	public void stop() {
		try {
			listenerThread.server.close();
		} catch (IOException e) {
			Log.e(TAG,"Error when close was called on serversocket: "+e.getMessage());
		}
	}
		
	
	
}
