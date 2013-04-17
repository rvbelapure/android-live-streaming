package edu.gatech.rts.stream.rtsp;

import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

class RtspResponse {
	
	// Status code definitions
	public static final String STATUS_OK = "200 OK";
	public static final String STATUS_BAD_REQUEST = "400 Bad RtspRequest";
	public static final String STATUS_NOT_FOUND = "404 Not Found";
	public static final String STATUS_INTERNAL_SERVER_ERROR = "500 Internal Server Error";
	
	public String status = STATUS_OK;
	public String content = "";
	public String attributes = "";
	private final RtspRequest rtspRequest;
	
	public RtspResponse(RtspRequest rtspRequest) {
		this.rtspRequest = rtspRequest;
	}
	
	public void send(OutputStream output) throws IOException {
		int seqid = -1;
		
		try {
			seqid = Integer.parseInt(rtspRequest.headers.get("Cseq"));
		} catch (Exception ignore) {}
		
		String response = 	"RTSP/1.0 "+status+"\r\n" +
				"Server: MajorKernelPanic RTSP Server\r\n" +
				(seqid>=0?("Cseq: " + seqid + "\r\n"):"") +
				"Content-Length: " + content.length() + "\r\n" +
				attributes +
				"\r\n" + 
				content;
		
		Log.d(RtspServer.TAG,response);
		
		output.write(response.getBytes());
	}
}