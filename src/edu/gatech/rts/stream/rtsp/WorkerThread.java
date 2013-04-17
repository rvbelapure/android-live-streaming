package edu.gatech.rts.stream.rtsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Handler;
import android.util.Log;
import edu.gatech.rts.stream.app.RtspSession;
import edu.gatech.rts.stream.app.UriParser;

// One thread per client
class WorkerThread extends Thread implements Runnable {
	
	private final Socket client;
	private final OutputStream output;
	private final BufferedReader input;
	private final Handler handler;
	
	// Each client has an associated rtspSession
	private RtspSession rtspSession;
	
	public WorkerThread(final Socket client, final Handler handler) throws IOException {
		this.input = new BufferedReader(new InputStreamReader(client.getInputStream()));
		this.output = client.getOutputStream();
		this.rtspSession = new RtspSession(client.getInetAddress(), handler);
		this.client = client;
		this.handler = handler;
	}
	
	public void run() {
		RtspRequest rtspRequest;
		RtspResponse rtspResponse;
		
		log("Connection from "+client.getInetAddress().getHostAddress());

		while (!Thread.interrupted()) {
			try {
				// Parse the request
				rtspRequest = RtspRequest.parseRequest(input);
				// Do something accordingly
				rtspResponse = processRequest(rtspRequest);
				// Send response
				rtspResponse.send(output);
			} catch (IllegalStateException e1) {
				loge("Client sent a bad request !");
			} catch (SocketException e) {
				// Client left
				break;
			} catch (IOException e) {
				continue;
			}
		}

		// Streaming stops when client disconnects
		rtspSession.stopAll();
		rtspSession.flush();

		try {
			client.close();
		} catch (IOException ignore) {}
		
		log("Client disconnected");
		
	}
	
	public RtspResponse processRequest(RtspRequest rtspRequest) throws IllegalStateException, IOException{
		RtspResponse rtspResponse = new RtspResponse(rtspRequest);
		
		/* ********************************************************************************** */
		/* ********************************* Method DESCRIBE ******************************** */
		/* ********************************************************************************** */
		if (rtspRequest.method.toUpperCase().equals("DESCRIBE")) {
			
			// Parse the requested URI and configure the rtspSession
			UriParser.parse(rtspRequest.uri,rtspSession);
			
			String requestContent = rtspSession.getSessionDescriptor();
			String requestAttributes = 
					"Content-Base: "+client.getLocalAddress().getHostAddress()+":"+client.getLocalPort()+"/\r\n" +
					"Content-Type: application/sdp\r\n";
			
			rtspResponse.status = RtspResponse.STATUS_OK;
			rtspResponse.attributes = requestAttributes;
			rtspResponse.content = requestContent;
			
		}
		
		/* ********************************************************************************** */
		/* ********************************* Method OPTIONS ********************************* */
		/* ********************************************************************************** */
		else if (rtspRequest.method.toUpperCase().equals("OPTIONS")) {
			rtspResponse.status = RtspResponse.STATUS_OK;
			rtspResponse.attributes = "Public: DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE\r\n";
		}

		/* ********************************************************************************** */
		/* ********************************** Method SETUP ********************************** */
		/* ********************************************************************************** */
		else if (rtspRequest.method.toUpperCase().equals("SETUP")) {
			Pattern p; Matcher m;
			int p2, p1, ssrc, trackId, src;
			
			p = Pattern.compile("trackID=(\\w+)",Pattern.CASE_INSENSITIVE);
			m = p.matcher(rtspRequest.uri);
			
			if (!m.find()) {
				rtspResponse.status = RtspResponse.STATUS_BAD_REQUEST;
				return rtspResponse;
			} 
			
			trackId = Integer.parseInt(m.group(1));
			
			if (!rtspSession.trackExists(trackId)) {
				rtspResponse.status = RtspResponse.STATUS_NOT_FOUND;
				return rtspResponse;
			}
			
			p = Pattern.compile("client_port=(\\d+)-(\\d+)",Pattern.CASE_INSENSITIVE);
			m = p.matcher(rtspRequest.headers.get("Transport"));
			
			if (!m.find()) {
				int port = rtspSession.getTrackDestinationPort(trackId);
				p1 = port;
				p2 = port+1;
			}
			else {
				p1 = Integer.parseInt(m.group(1)); 
				p2 = Integer.parseInt(m.group(2));
			}
			
			ssrc = rtspSession.getTrackSSRC(trackId);
			src = rtspSession.getTrackLocalPort(trackId);
			rtspSession.setTrackDestinationPort(trackId, p1);
			
			try {
				rtspSession.start(trackId);
				rtspResponse.attributes = "Transport: RTP/AVP/UDP;unicast;client_port="+p1+"-"+p2+";server_port="+src+"-"+(src+1)+";ssrc="+Integer.toHexString(ssrc)+";mode=play\r\n" +
						"RtspSession: "+ "1185d20035702ca" + "\r\n" +
						"Cache-Control: no-cache\r\n";
				rtspResponse.status = RtspResponse.STATUS_OK;
			} catch (RuntimeException e) {
				rtspResponse.status = RtspResponse.STATUS_INTERNAL_SERVER_ERROR;
				throw new RuntimeException("Could not start stream, configuration probably not supported by phone");
			}
			
		}

		/* ********************************************************************************** */
		/* ********************************** Method PLAY *********************************** */
		/* ********************************************************************************** */
		else if (rtspRequest.method.toUpperCase().equals("PLAY")) {
			String requestAttributes = "RTP-Info: ";
			if (rtspSession.trackExists(0)) requestAttributes += "url=rtsp://"+client.getLocalAddress()+":"+client.getLocalPort()+"/trackID="+0+";seq=0,";
			if (rtspSession.trackExists(1)) requestAttributes += "url=rtsp://"+client.getLocalAddress()+":"+client.getLocalPort()+"/trackID="+1+";seq=0,";
			requestAttributes = requestAttributes.substring(0, requestAttributes.length()-1) + "\r\nSession: 1185d20035702ca\r\n";
			
			rtspResponse.status = RtspResponse.STATUS_OK;
			rtspResponse.attributes = requestAttributes;
		}


		/* ********************************************************************************** */
		/* ********************************** Method PAUSE ********************************** */
		/* ********************************************************************************** */
		else if (rtspRequest.method.toUpperCase().equals("PAUSE")) {
			rtspResponse.status = RtspResponse.STATUS_OK;
		}

		/* ********************************************************************************** */
		/* ********************************* Method TEARDOWN ******************************** */
		/* ********************************************************************************** */
		else if (rtspRequest.method.toUpperCase().equals("TEARDOWN")) {
			rtspResponse.status = RtspResponse.STATUS_OK;
		}
		
		/* Method Unknown */
		else {
			Log.e(RtspServer.TAG,"Command unknown: "+rtspRequest);
			rtspResponse.status = RtspResponse.STATUS_BAD_REQUEST;
		}
		
		return rtspResponse;
		
	}
	
	private void log(String message) {
		handler.obtainMessage(RtspServer.MESSAGE_LOG, message).sendToTarget();
		Log.v(RtspServer.TAG,message);
	}
	
	// Display an error on user interface
	private void loge(String error) {
		handler.obtainMessage(RtspServer.MESSAGE_LOG, error).sendToTarget();
		Log.e(RtspServer.TAG,error);
	}

}