package net.quantum6.mediacodec;

import java.nio.ByteBuffer;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import net.quantum6.kit.Log;


public final class MediaCodecKit
{
	private final static String TAG = MediaCodecKit.class.getCanonicalName();

    public final static String MIME_CODEC_H264 = "video/avc";

    public static boolean hasH264Encoder()
    {
        return (chooseCodec(MIME_CODEC_H264, true) != null);
    }
    
    public static boolean hasH264Decoder()
    {
        return (chooseCodec(MIME_CODEC_H264, false) != null);
    }

    public static int check()
    {
        return 0;
    }
    

    //{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{
    
    private final static void listColor(MediaCodecInfo codecInfo, String mime)
    {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mime);
        for (int i = 0; i < capabilities.colorFormats.length; i++)
        {
            int format = capabilities.colorFormats[i];
            Log.e(TAG, "        color["+i+"]=" + format + ", hex=0x" + Integer.toHexString(format));
            switch (format)
            {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                    Log.e(TAG, "        YUV color ");
                    break;
            }
        }
    }

    public final static void listCodec()
    {
        /*
         * int colorFormat = 0; MediaCodecInfo.CodecCapabilities capabilities =
         * MediaCodecInfo.getCapabilitiesForType(MIME_AVC); for (int i = 0; i <
         * capabilities.colorFormats.length && colorFormat == 0; i++) { int
         * format = capabilities.colorFormats[i]; Log.e(TAG,
         * "Using color format " + format); }
         */
        int numCodecs = MediaCodecList.getCodecCount();
        Log.e(TAG, "listCodec="+numCodecs);
        for (int i = 0; i < numCodecs; i++)
        {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            Log.e(TAG, "codec["+i+"]="+codecInfo.getName());
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++)
            {
                Log.e(TAG, "    types[" + j + "]=" + types[j]);
                listColor(codecInfo, types[j]);
            }
        }
        //
    }

    //}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}
    
    
    //{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{
    
    private final static MediaCodecInfo chooseCodec(String codecName, final boolean isEncoder)
    {
        int nbCodecs = MediaCodecList.getCodecCount();
        Log.e(TAG, "getCodecCount()="+nbCodecs);
        String type = (isEncoder ? "encoder" : "decoder");
        for (int i = 0; i < nbCodecs; i++)
        {
            MediaCodecInfo mci = MediaCodecList.getCodecInfoAt(i);
            if (mci.isEncoder() == isEncoder)
            {
                continue;
            }
            String[] types = mci.getSupportedTypes();
            for (int j = 0; j < types.length; j++)
            {
                Log.e(TAG,type+"="+ types[j]);
                if (types[j].equalsIgnoreCase(codecName))
                {
                    Log.e(TAG, String.format("%s %s types: %s", type, mci.getName(), types[j]));
                    return mci;
                }
            }
        }
        return null;
    }

    protected final static int chooseVideoEncoderColor(String codecName)
    {
        MediaCodecInfo vmci = chooseCodec(codecName, true);
        if (vmci == null)
        {
            return 0;
        }

        int matchedColorFormat = 0;
        MediaCodecInfo.CodecCapabilities cc = vmci.getCapabilitiesForType(MediaCodecKit.MIME_CODEC_H264);
        for (int i = 0; i < cc.colorFormats.length; i++)
        {
            int cf = cc.colorFormats[i];

            Log.i(TAG, String.format("vencoder %s supports color fomart 0x%x(%d)", vmci.getName(), cf, cf));

            // choose YUV for h.264, prefer the bigger one.
            // corresponding to the color space transform in onPreviewFrame
            if ((cf >= MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                    && cf <= MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar))
            {
                if (cf > matchedColorFormat)
                {
                    matchedColorFormat = cf;
                }
            }
        }
        for (int i = 0; i < cc.profileLevels.length; i++)
        {
            //MediaCodecInfo.CodecProfileLevel pl = cc.profileLevels[i];
            //Log.i(TAG, String.format("vencoder %s support profile %d, level %d", vmci.getName(), pl.profile, pl.level));
        }
        //Log.i(TAG, String.format("vencoder %s choose color format 0x%x(%d)", vmci.getName(), matchedColorFormat, matchedColorFormat));
        return matchedColorFormat;
    }
    
    //}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}

    public final static void NV21_TO_YUV420SP(int width, int height, byte[] in)
    {
        int pixels = width * height;
        byte s;
        int count = (pixels / 2);
        for (int i = 0; i < count; i += 2)
        {
            s = in[pixels + i];
            in[pixels + i] = in[pixels + i + 1];
            in[pixels + i + 1] = s;
        }
    }

    /*
     * NV21: YYYYYYYY VUVU =>YUV420SP I420: YYYYYYYY UU VV =>YUV420P
     */
    public final static int NV21_TO_YUV420P(byte[] dst, byte[] src, int w, int h)
    {
        int ysize = w * h;
        int usize = ysize >>  2;

        // y
        System.arraycopy(src, 0, dst, 0, ysize);

        // u, 1/4
        int srcPointer = ysize;
        int dstPointer = ysize;
        int count = usize;
        while (count > 0)
        {
            srcPointer++;
            dst[dstPointer] = src[srcPointer];
            dstPointer++;
            srcPointer++;
            count--;
        }

        // v, 1/4
        srcPointer = ysize;

        count = usize;
        while (count > 0)
        {
            dst[dstPointer] = src[srcPointer];
            dstPointer++;
            srcPointer += 2;
            count--;
        }

        return 0;
    }

    /**
     * OK !
     * Performance is bad.
     * 
     * @param input
     * @param width
     * @param height
     * @param output
     * @param isRGB
     */
    public static void NV21ToRGBA(byte[] input, int width, int height, byte[] output, boolean isRGB)
    {
        int depth = 4;
        int nvOff = width * height ;
        int  i, j, yIndex = 0;
        int y, u, v;
        int r, g, b, nvIndex = 0;
        for(i = 0; i < height; i++){
            for(j = 0; j < width; j ++,++yIndex){
                nvIndex = (i / 2)  * width + j - j % 2;
                y = input[yIndex] & 0xff;
                u = input[nvOff + nvIndex ] & 0xff;
                v = input[nvOff + nvIndex + 1] & 0xff;

                // yuv to rgb
                r = y + ((351 * (v-128))>>8);  //r
                g = y - ((179 * (v-128) + 86 * (u-128))>>8); //g
                b = y + ((443 * (u-128))>>8); //b
                
                r = ((r>255) ?255 :(r<0)?0:r); 
                g = ((g>255) ?255 :(g<0)?0:g);
                b = ((b>255) ?255 :(b<0)?0:b);
                if(isRGB){
                    output[yIndex*depth + 0] = (byte) b;
                    output[yIndex*depth + 1] = (byte) g;
                    output[yIndex*depth + 2] = (byte) r;
                }else{
                    output[yIndex*depth + 0] = (byte) r;
                    output[yIndex*depth + 1] = (byte) g;
                    output[yIndex*depth + 2] = (byte) b;
                }
            }
        }
    }
    
    
}
