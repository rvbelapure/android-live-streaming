
package edu.gatech.rts.stream.streaming.audio;

import java.io.IOException;

import android.media.MediaRecorder;

import com.jboss.netty.AMRNBPacketizer;

import edu.gatech.rts.stream.streaming.MediaStream;


public class AMRNBStream extends MediaStream {

	public AMRNBStream() {
		super();
		
		this.packetizer = new AMRNBPacketizer();
		
	}

	public void prepare() throws IllegalStateException, IOException {
		setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		setAudioChannels(1);
		super.prepare();
	}
	
	public String generateSessionDescriptor() {
		return "m=audio "+String.valueOf(getDestinationPort())+" RTP/AVP 96\r\n" +
				   "b=AS:128\r\n" +
				   "b=RR:0\r\n" +
				   "a=rtpmap:96 AMR/8000\r\n" +
				   "a=fmtp:96 octet-align=1;\r\n";
	}
	
}
