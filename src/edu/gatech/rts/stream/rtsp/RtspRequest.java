package edu.gatech.rts.stream.rtsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

class RtspRequest {
	
	// Parse method & uri
	public static final Pattern regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP",Pattern.CASE_INSENSITIVE);
	// Parse a request header
	public static final Pattern rexegHeader = Pattern.compile("(\\S+):(.+)",Pattern.CASE_INSENSITIVE);
	
	public String method;
	public String uri;
	public HashMap<String,String> headers = new HashMap<String,String>();
	
	/** Parse the method, uri & headers of a RTSP request */
	public static RtspRequest parseRequest(BufferedReader input) throws IOException, IllegalStateException, SocketException {
		RtspRequest rtspRequest = new RtspRequest();
		String line;
		Matcher matcher;

		// Parsing request method & uri
		if ((line = input.readLine())==null) throw new SocketException("Client disconnected");
		matcher = regexMethod.matcher(line);
		matcher.find();
		rtspRequest.method = matcher.group(1);
		rtspRequest.uri = matcher.group(2);

		// Parsing headers of the request
		while ( (line = input.readLine()) != null && line.length()>3 ) {
			matcher = rexegHeader.matcher(line);
			matcher.find();
			rtspRequest.headers.put(matcher.group(1),matcher.group(2));
		}
		if (line==null) throw new SocketException("Client disconnected");
		
		Log.e(RtspServer.TAG,rtspRequest.method+" "+rtspRequest.uri);
		
		return rtspRequest;
	}
}