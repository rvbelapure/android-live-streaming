package edu.gatech.rts.stream.rtsp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

import android.os.Handler;
import android.util.Log;

public class RequestDispatcher extends Thread implements Runnable {
	
	final ServerSocket server;
	private final Handler handler;
	
	public RequestDispatcher(final int port, final Handler handler) throws IOException {
		this.server = new ServerSocket(port);
		this.handler = handler;
	}
	
	public void run() {
		Log.i(RtspServer.TAG,"Listening on port "+server.getLocalPort());
		while (!Thread.interrupted()) {
			try {
				new WorkerThread(server.accept(), handler).start();
			} catch (SocketException e) {
				break;
			} catch (IOException e) {
				Log.e(RtspServer.TAG,e.getMessage());
				continue;
			}
		}
		Log.i(RtspServer.TAG,"RequestListener stopped !");
	}
	
}