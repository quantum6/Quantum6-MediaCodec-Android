package net.quantum6.mediacodec;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;


@SuppressLint("NewApi")
public final class AndroidVideoDecoder extends AndroidVideoCodec
{
    //private final static String TAG = AndroidVideoDecoder.class.getCanonicalName();

	private static int mInstanceCount = 0;
	
    public final byte[] csd0 = 
        {
            0x0, 0x0, 0x0, 0x1, 0x67, 0x42, 0x0, 0x29, (byte)0x8d, (byte)0x8d, 0x40, 0x28, 0x2, (byte)0xdd, 0x0, (byte)0xf0, (byte)0x88, 0x45, 0x38,
            0x0, 0x0, 0x0, 0x1, 0x68, (byte)0xca, 0x43, (byte)0xc8 
        };
	
    public AndroidVideoDecoder(Surface surface, int width, int height, int rotate)
    {
        mInstanceCount ++;
    	setSurface(surface, width, height, rotate);
    }
    
    public void setSurface(Surface surface, int width, int height, int rotate)
    {
    	if (surface == null)
    	{
    		return;
    	}
    	
        boolean result = super.initParams(surface, width, height, rotate);
        if (result && mDisplaySurface != null)
        {
        	mMediaCodec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        }
    }
    
    public boolean isSameSurface(Surface surface)
    {
    	return (mDisplaySurface == surface);
    }
    
    @Override
    public final boolean isEncoder()
    {
        return false;
    }
    
    @Override
    protected final MediaFormat getMediaFormat()
    {
        MediaFormat mediaFormat = super.getMediaFormat();
        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));

        //KEY_PUSH_BLANK_BUFFERS_ON_STOP
        
        
        //mediaFormat.setInteger("priority", 0);

        /*
		byte[] header_sps = { 0, 0, 0, 1, 103, 100, 0, 40, -84, 52, -59, 1, -32, 17, 31, 120, 11, 80, 16, 16, 31, 0, 0, 3, 3, -23, 0, 0, -22, 96, -108 };
        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));

        byte[] header_pps = { 0, 0, 0, 1, 104, -18, 60, -128 };
        mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
        
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1920 * 1080);
        mediaFormat.setInteger("durationUs", 63446722);
		*/ 
        
        return mediaFormat;
    }

    
    @Override
    protected final MediaCodec getCodec()
    {
        try
        {
            return MediaCodec.createDecoderByType(MediaCodecKit.MIME_CODEC_H264);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void release()
    {
    	mInstanceCount--;
    	super.release();
    }
    
    public static void resetInstanceCount()
    {
    	mInstanceCount = 0;
    }
    
    public static int getInstanceCount()
    {
    	return mInstanceCount;
    }
}
