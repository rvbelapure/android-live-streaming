package edu.gatech.rts.stream.streaming.video;

public class VideoQuality {

	/** Default video stream quality */
	public final static VideoQuality defaultVideoQualiy = new VideoQuality(640,480,15,500000);
	
	public VideoQuality() {}
	
	public VideoQuality(int resX, int resY, int frameRate, int bitRate) {
		
		this.frameRate = frameRate;
		this.bitRate = bitRate;
		this.resX = resX;
		this.resY = resY;
		
	}
	
	public int frameRate = 0;
	public int bitRate = 0;
	public int resX = 0;
	public int resY = 0;
	public int orientation = 90;
	
	public boolean equals(VideoQuality quality) {
		if (quality==null) return false;
		return (quality.resX == this.resX 				&
				 quality.resY == this.resY 				&
				 quality.frameRate == this.frameRate	&
				 quality.bitRate == this.bitRate 		);
	}
	
	public VideoQuality clone() {
		return new VideoQuality(resX,resY,frameRate,bitRate);
	}
	
	public static VideoQuality parseQuality(String str) {
		VideoQuality quality = new VideoQuality(0,0,0,0);
		if (str != null) {
			String[] config = str.split("-");
			try {
				quality.bitRate = Integer.parseInt(config[0])*1000; // conversion to bit/s
				quality.frameRate = Integer.parseInt(config[1]);
				quality.resX = Integer.parseInt(config[2]);
				quality.resY = Integer.parseInt(config[3]);
			}
			catch (IndexOutOfBoundsException ignore) {}
		}
		return quality;
	}

	public static void merge(VideoQuality videoQuality, VideoQuality defaultVideoQuality) {
		if (videoQuality.resX==0) videoQuality.resX = defaultVideoQuality.resX;
		if (videoQuality.resY==0) videoQuality.resY = defaultVideoQuality.resY;
		if (videoQuality.frameRate==0) videoQuality.frameRate = defaultVideoQuality.frameRate;
		if (videoQuality.bitRate==0) videoQuality.bitRate = defaultVideoQuality.bitRate;
	}
	
}
