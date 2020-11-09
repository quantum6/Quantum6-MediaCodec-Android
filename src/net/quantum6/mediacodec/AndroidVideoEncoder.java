package net.quantum6.mediacodec;

import java.util.Arrays;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

@SuppressLint("NewApi")
public final class AndroidVideoEncoder extends AndroidVideoCodec
{
    @SuppressWarnings("deprecation")
    private static int mColorFormat = 
            //MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

    private static int  ENCODE_OUTPUT_BUFFER_FLAG_SPS = 2;
    private static int  ENCODE_OUTPUT_BUFFER_FLAG_KEY = 1;

    public static final int VFPS = 15;
    public static final int VGOP = 30;
    private static final int DEFAULT_BIT_RATE = 1000*1000;

    private byte[] mConfigBuffer;
    private byte[] tempBuffer;
    //private AtomicInteger videoFrameCacheNumber = new AtomicInteger(0);
    private int mRotate;
    private byte[] rotateBuffer;

    public AndroidVideoEncoder(int width, int height, int rotate, int framerate, int bitrate)
    {
        //listCodec();
        //mFramerate = framerate;
        //mBitrate = bitrate;
        mRotate = rotate;
        if (mRotate == 90 || mRotate == 180)
        {
            int temp = height;
            height   = width;
            width    = temp;
        }
        super.initParams(null, width, height, rotate);
    }


    @Override
    public final boolean isEncoder()
    {
        return true;
    }

    @Override
    protected final MediaCodec getCodec()
    {
        try
        {
            return MediaCodec.createEncoderByType(MediaCodecKit.MIME_CODEC_H264);
            //return MediaCodec.createByCodecName("OMX.google.h264.encoder");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected final MediaFormat getMediaFormat()
    {
        MediaFormat mediaFormat = super.getMediaFormat();
        
        mColorFormat = MediaCodecKit.chooseVideoEncoderColor(MediaCodecKit.MIME_CODEC_H264);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, 	 mColorFormat);
        
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,   0);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,         DEFAULT_BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,       VFPS);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VGOP / VFPS);
        
        return mediaFormat;
    }

    private void rotateNV21(MediaCodecData inputData)
    {
        if (mRotate == 0)
        {
            return;
        }
        
        if (rotateBuffer == null || rotateBuffer.length != inputData.mDataSize)
        {
            rotateBuffer = new byte[inputData.mDataSize];
        }
        if (mRotate == 90)
        {
            MediaCodecKit.NV21_rotate_to_90(inputData.mDataArray, rotateBuffer, mHeight, mWidth);
        }
        else if (mRotate == 180)
        {
            MediaCodecKit.NV21_rotate_to_180(inputData.mDataArray, rotateBuffer, mWidth, mHeight);
        }
        else if (mRotate == 270)
        {
            MediaCodecKit.NV21_rotate_to_270(inputData.mDataArray, rotateBuffer, mHeight, mWidth);
        }
        else
        {
            return;
        }
        System.arraycopy(rotateBuffer, 0, inputData.mDataArray, 0, inputData.mDataSize);
    }
    
    @Override
    public int process(MediaCodecData inputData, MediaCodecData outputData)
    {
    	int inputSize = inputData.mDataSize;
    	if (inputSize > 0)
    	{
            if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
            {
                rotateNV21(inputData);
                MediaCodecKit.NV21_TO_YUV420SP(mWidth, mHeight, inputData.mDataArray);
            }
            else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
            {
                if (tempBuffer == null || tempBuffer.length != inputSize)
                {
                    tempBuffer = new byte[inputSize];
                    //inputData.mDataSize = inputSize;
                }
                //Log.d(TAG, "MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar="+inputSize);
                MediaCodecKit.NV21_TO_YUV420P(tempBuffer, inputData.mDataArray, mWidth, mHeight);
                System.arraycopy(tempBuffer, 0, inputData.mDataArray, 0, inputSize);
            }
    	}
        
        int ret = super.process(inputData, outputData);
        if (ret <= 0)
        {
        	return ret;
        }
        
        if (ENCODE_OUTPUT_BUFFER_FLAG_SPS == mBufferInfo.flags)
        {
        	mConfigBuffer = new byte[ret];
        	System.arraycopy(outputData.mDataArray, 0, mConfigBuffer, 0, ret);
        }
        else if (ENCODE_OUTPUT_BUFFER_FLAG_KEY == mBufferInfo.flags
        		&& mConfigBuffer != null)
        {
        	byte[] temp = new byte[ret];
        	System.arraycopy(outputData.mDataArray, 0, temp, 0, ret);
        	int sps = mConfigBuffer.length;
        	System.arraycopy(mConfigBuffer, 0, outputData.mDataArray, 0, sps);
        	System.arraycopy(temp, 0, outputData.mDataArray, sps, ret);
        	ret += sps;
        }
        return ret;
    }

}
