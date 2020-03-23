package net.quantum6.mediacodec;

import java.nio.ByteBuffer;


public final class MediaCodecData
{
	private final static int INFO_SIZE 			= 4;
	
	public  final static int INDEX_WIDTH 		= 0;
	public  final static int INDEX_HEIGHT		= 1;
	public  final static int INDEX_CHANGED 		= 2;
	public  final static int INDEX_SIZE			= 3;
	
	
	public int[] getInfo()
	{
		if (null == mInfo)
		{
			mInfo = new int[INFO_SIZE];
		}
		return mInfo;
	}
	
	public void setData(byte[] data)
	{
		setData(data, data.length);
	}
	
	public void setData(byte[] data, int size)
	{
		mDataArray = data;
		mCapacity  = data.length; 
		mDataSize  = size > 0 ? size : mCapacity;
	}
	
	public void release()
	{
		if (mDataArray != null)
		{
			mDataArray = null;
		}
		if (mDataBuffer != null)
		{
			mDataBuffer = null;
		}
		
		mInfo = null;
	}
	
	public int 			mDataSize;
	
	public int 			mCapacity;
	
	public byte[] 	  	mDataArray;
	
	public ByteBuffer 	mDataBuffer;
	
	private int[] 		mInfo;

}
