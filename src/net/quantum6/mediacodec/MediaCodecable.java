package net.quantum6.mediacodec;

public interface MediaCodecable
{
	int initCodec();
	
	boolean isEncoder();
	
	int process(MediaCodecData inputData, MediaCodecData outputData);
	
	void release();
	
}
