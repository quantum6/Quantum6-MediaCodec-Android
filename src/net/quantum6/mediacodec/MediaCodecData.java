package net.quantum6.mediacodec;

import java.nio.ByteBuffer;


public final class MediaCodecData
{
	private final static int INFO_SIZE 			= 4;
	
	public  final static int INDEX_WIDTH 		= 0;
	public  final static int INDEX_HEIGHT		= 1;
	public  final static int INDEX_CHANGED 		= 2;
	public  final static int INDEX_SIZE			= 3;
	
    public MediaCodecData(int width, int height)
    {
        int size = width*height*2;
        if (size < 128*1024)
        {
            size = 128*1024;
        }
        setData(new byte[size]);
        
        mInfo = new int[INFO_SIZE];
        mInfo[INDEX_WIDTH]  = width;
        mInfo[INDEX_HEIGHT] = height;
    }
    	
	public int[] getInfo()
	{
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
	
    public void setData(ByteBuffer byteBuffer, int size)
    {
        if (mDataArray == null || mDataArray.length < size)
        {
            mDataArray = new byte[size];
        }
        byteBuffer.get(mDataArray, byteBuffer.position(), size);
    }
    
    public void clearData()
    {
        mDataSize = 0;
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
