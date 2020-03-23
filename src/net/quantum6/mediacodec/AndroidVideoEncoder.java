package net.quantum6.mediacodec;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

@SuppressLint("NewApi")
public final class AndroidVideoEncoder extends AndroidVideoCodec
{
    private static int mColorFormat = 
            //MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

    private static int  ENCODE_OUTPUT_BUFFER_FLAG_SPS = 2;
    private static int  ENCODE_OUTPUT_BUFFER_FLAG_KEY = 1;

    private byte[] mConfigBuffer;

    
    public AndroidVideoEncoder(int width, int height, int framerate, int bitrate)
    {
        //listCodec();
        //mFramerate = framerate;
        //mBitrate = bitrate;
        super.initParams(null, width, height);
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
            return MediaCodec.createEncoderByType(MIME_CODEC);
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
        
        mColorFormat = chooseVideoEncoderColor(MIME_CODEC);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, 	 mColorFormat);
        
        return mediaFormat;
    }

    @Override
    public int process(MediaCodecData inputData, MediaCodecData outputData)
    {
    	int inputSize = inputData.mDataSize;
        if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
        {
            YUV420SP_UV_EXCHANGE(mWidth, mHeight, inputData.mDataArray);
        }
        else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
        {
            //Log.d(TAG, "MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar="+inputSize);
            byte[] dest = new byte[inputSize];
            NV21_2_yuv420p(dest, inputData.mDataArray, mWidth, mHeight);
            inputData.mDataArray = dest;
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
