package net.quantum6.mediacodec;

import java.nio.ByteBuffer;

import net.quantum6.kit.Log;
import net.quantum6.kit.SystemKit;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;

import net.quantum6.fps.FpsCounter;

@SuppressLint("NewApi")
public abstract class AndroidVideoCodec implements MediaCodecable
{
    private final static String TAG = AndroidVideoCodec.class.getCanonicalName();
    public  final static int FPS_CONTROLLED           = 15;
    public  final static int DEFAULT_I_FRAME_INTERVAL = 50;
    
    private final static int DEFAULT_BIT_RATE         = 500*1000;
    public  final static int MAX_ERROR_COUNT          = 100;
    
    protected Surface mDisplaySurface;

    protected MediaCodec mMediaCodec;
    protected int mWidth;
    protected int mHeight;
    public final static int CODEC_TIME_OUT_US = 10;
    protected boolean debugFlag;
    private int errorCount = 0;
    
    private boolean isInitedOK;
    
    private FpsCounter mFpsCounter;

    private long mPresentTimeUs;
    protected MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    
    //{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{
    
    protected abstract MediaCodec getCodec();

    //}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}
    
    protected final boolean initParams(Surface surface, int width, int height)
    {
        mDisplaySurface = surface;
        mWidth = width;
        mHeight = height;

        try
        {
            mMediaCodec = getCodec();
            if (mMediaCodec == null)
            {
                return false;
            }
            //Log.e(TAG, "getName() = "+mMediaCodec.getName()+", "+mMediaCodec.getCodecInfo().getName());
            
            MediaFormat mediaFormat = getMediaFormat();
            mMediaCodec.configure(mediaFormat, mDisplaySurface, null, isEncoder() ? 1 : 0);
            mMediaCodec.start();
            mPresentTimeUs = System.nanoTime() / 1000; 
            isInitedOK = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return isInitedOK;
    }

    public boolean isInited()
    {
        return isInitedOK;
    }
    
    //{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{ MediaCodecable
    
    @Override
    public int initCodec()
    {
        return 0;
    }
    
    @Override
    public int process(MediaCodecData inputData, MediaCodecData outputData)
    {
        if (null == mMediaCodec)
        {
            return -1;
        }
        
        if (null != mFpsCounter)
        {
            mFpsCounter.count();
        }
        
        if (outputData.mDataArray == null)
        {
            byte[] data = new byte[isEncoder() ? SystemKit.getEncodedBufferSize(mWidth, mHeight) : SystemKit.getDecodedBufferSize(mWidth, mHeight)];
            outputData.setData(data);
        }

        int inputSize = inputData.mDataSize;
        //Log.d(TAG, "inputSize="+inputData.mDataSize+", "+inputData.mDataArray.length);
        boolean isSdk19 = (Build.VERSION.SDK_INT <= 19);
        int inputBufferIndex = -1;
        try
        {
            inputBufferIndex = mMediaCodec.dequeueInputBuffer(CODEC_TIME_OUT_US);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //inputBufferIndex = -1;
            //errorCount++;
            return -1;
        }
        
        if (inputSize > 0 && inputBufferIndex >= 0)
        {
            ByteBuffer inputBuffer = isSdk19 ? getInputBuffer19(inputBufferIndex, inputSize) : getInputBuffer21(inputBufferIndex);
            inputBuffer.clear();
            //inputBuffer.limit(inputSize) also exception.
            if (inputBuffer.limit() >= inputSize)
            {
                inputBuffer.put(inputData.mDataArray, 0, inputSize);
                long pts = System.nanoTime() / 1000 - mPresentTimeUs;
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, inputSize, pts, 0);
            }
        }

        mBufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = -1;
        try
        {
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, CODEC_TIME_OUT_US); 
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //outputBufferIndex = -1;
            //errorCount++;
            return -1;
        }
        /*Log.d(TAG, "Encoder="+this.isEncoder()
                +", inputBufferIndex="+inputBufferIndex
                +", outputBufferIndex="+outputBufferIndex
                +", bufferInfo.size="+mBufferInfo.size
                +", "+mDisplaySurface
                );*/
        int outputLen = 0;
        if (outputBufferIndex >= 0)
        {
            if (null == mDisplaySurface || !mDisplaySurface.isValid())
            {
                ByteBuffer outputBuffer = isSdk19 ? getOutputBuffer19(outputBufferIndex) : getOutputBuffer21(outputBufferIndex);
                if (outputData != null && outputBuffer != null && mBufferInfo.size > 0)
                {
                    outputLen = mBufferInfo.size;
                    outputData.setData(outputBuffer, outputLen);
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
            else
            {
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            }
        }
        else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
        {
            //outputBuffers = mMediaCodec.getOutputBuffers();
        }
        else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
        {
            MediaFormat format = mMediaCodec.getOutputFormat();
            Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED="+format);
            mWidth = format.getInteger(MediaFormat.KEY_WIDTH);
            mHeight= format.getInteger(MediaFormat.KEY_HEIGHT);
            inputData.getInfo()[MediaCodecData.INDEX_WIDTH]   = mWidth;
            inputData.getInfo()[MediaCodecData.INDEX_HEIGHT]  = mHeight;
            inputData.getInfo()[MediaCodecData.INDEX_CHANGED] = 1;
            
            if (!isEncoder() && Build.VERSION.SDK_INT >=19)
            {
                Bundle bundle = new Bundle();
                bundle.putInt(MediaFormat.KEY_WIDTH,  format.getInteger(MediaFormat.KEY_WIDTH));
                bundle.putInt(MediaFormat.KEY_HEIGHT, format.getInteger(MediaFormat.KEY_HEIGHT));
                
                //byte[] header_sps = { 0, 0, 0, 1, 103, 100, 0, 40, -84, 52, -59, 1, -32, 17, 31, 120, 11, 80, 16, 16, 31, 0, 0, 3, 3, -23, 0, 0, -22, 96, -108 };
                //byte[] header_pps = { 0, 0, 0, 1, 104, -18, 60, -128 };
                ByteBuffer buf = format.getByteBuffer("csd-0");
                if (null != buf)
                {
                    bundle.putByteArray("csd-0", buf.array());
                }
                buf = format.getByteBuffer("csd-1");
                if (null != buf)
                {
                    bundle.putByteArray("csd-1", buf.array());
                }
                bundle.putInt(MediaFormat.KEY_MAX_INPUT_SIZE, 1920 * 1080);
                bundle.putInt("durationUs", 63446722);
                try
                {
                    mMediaCodec.setParameters(bundle);
                }
                catch (Exception e)
                {
                    //
                }
            }
        }
        else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
        {
            //
        }
        
        if (errorCount >= MAX_ERROR_COUNT)
        {
            return -1;
        }
        return outputLen;
    }

    @Override
    public void release()
    {
        if (mMediaCodec == null)
        {
            return;
        }
        final MediaCodec codec = mMediaCodec;
        mMediaCodec = null;
        
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    codec.stop();
                    codec.release();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}} MediaCodecable
    
    
    protected MediaFormat getMediaFormat()
    {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaCodecKit.MIME_CODEC_H264, mWidth, mHeight);

        //30K BLACK SCRN, 40K OK
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,       FPS_CONTROLLED);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,         DEFAULT_BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_I_FRAME_INTERVAL);

        //KEY_MAX_WIDTH
        //KEY_MAX_HEIGHT
        //KEY_MAX_INPUT_SIZE
        //KEY_DURATION
        
        //KEY_CAPTURE_RATE鎱㈠姩浣滐紵
        
        /*
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, 2);
        mediaFormat.setInteger("prepend-sps-pps-to-idr-frames", 1);
        int mbs = (((mWidth + 15) / 16) * ((mWidth + 15) / 16) * 10) / 100;
        mediaFormat.setInteger("intra-refresh-CIR-mbs", mbs);
        mediaFormat.setInteger("intra-refresh-mode", 0);
        
        byte[] header_sps = { 0, 0, 0, 1, 103, 100, 0, 40, -84, 52, -59, 1, -32, 17, 31, 120, 11, 80, 16, 16, 31, 0, 0, 3, 3, -23, 0, 0, -22, 96, -108 };
        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));

        byte[] header_pps = { 0, 0, 0, 1, 104, -18, 60, -128 };
        mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
        */

        return mediaFormat;
    }


    //{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{

    @TargetApi(21)
    private ByteBuffer getInputBuffer21(int index)
    {
        return mMediaCodec.getInputBuffers()[index];
        //return mMediaCodec.getInputBuffer(index);
    }
    
    @TargetApi(21)
    private ByteBuffer getOutputBuffer21(int index)
    {
        return mMediaCodec.getOutputBuffers()[index];
        //return mMediaCodec.getOutputBuffer(index);
    }
    
    @TargetApi(19)
    private ByteBuffer getInputBuffer19(int index, int count)
    {
        ByteBuffer[]  inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer inputBuffer = inputBuffers[index];
        if (inputBuffer.capacity() < count)
        {
            inputBuffer = ByteBuffer.allocate(count);
            inputBuffers[index] = inputBuffer;
        }
        return inputBuffer;
    }
    
    @TargetApi(19)
    private ByteBuffer getOutputBuffer19(int index)
    {
        ByteBuffer[]  outputBuffers = mMediaCodec.getOutputBuffers();
        return outputBuffers[index];
    }

    //}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}

    public final int getWidth()
    {
        return mWidth;
    }
    
    public final int getHeight()
    {
        return mHeight;
    }
    
    public final int getFps()
    {
        if (mFpsCounter != null)
        {
            return mFpsCounter.getFps();
        }
        return 0;
    }
    
}
