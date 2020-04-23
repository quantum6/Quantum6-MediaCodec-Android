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

    public final static void YUV420SP_UV_EXCHANGE(int width, int height, byte[] in)
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
    public final static int NV21_2_yuv420p(byte[] dst, byte[] src, int w, int h)
    {
        int ysize = w * h;
        int usize = w * h * 1 / 4;

        byte[] dsttmp = dst;

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

        dst = dsttmp;

        // _EF_TIME_DEBUG_END(0x000414141);

        return 0;
    }

    // {{{{{{{{{
    static int Table_fv1[] = {
        -180, -179, -177, -176, -174, -173, -172, -170,
        -169, -167, -166, -165, -163, -162, -160, -159,
        -158, -156, -155, -153, -152, -151, -149, -148,
        -146, -145, -144, -142, -141, -139, -138, -137,
        
        -135, -134, -132, -131, -130, -128, -127, -125,
        -124, -123, -121, -120, -118, -117, -115, -114,
        -113, -111, -110, -108, -107, -106, -104, -103,
        -101, -100,  -99,  -97,  -96,  -94,  -93,  -92,
        
         -90,  -89,  -87,  -86,  -85,  -83,  -82,  -80,
         -79,  -78,  -76,  -75,  -73,  -72,  -71,  -69,
         -68,  -66,  -65,  -64,  -62,  -61,  -59,  -58,
         -57,  -55,  -54,  -52,  -51,  -50,  -48,  -47,
         
         -45, -44, -43, -41, -40, -38, -37, -36,
         -34, -33, -31, -30, -29, -27, -26, -24,
         -23, -22, -20, -19, -17, -16, -15, -13,
         -12, -10,  -9,  -8,  -6,  -5,  -3,  -2,
         
         0, 1, 2, 4, 5, 7,
         8, 9, 11, 12, 14, 15,
         16, 18, 19, 21, 22, 23, 25, 26, 28, 29, 30,
         32, 33, 35, 36, 37, 39, 40, 42, 43, 44, 46,
         47, 49, 50, 51, 53, 54, 56, 57, 58, 60, 61,
         63, 64, 65, 67, 68, 70, 71, 72, 74, 75, 77, 78, 79,
         81, 82, 84, 85, 86, 88, 89, 91, 92, 93, 95, 96, 98, 99, 100,
         102, 103, 105, 106, 107, 109, 110, 112, 113, 114, 116, 117,
         119, 120, 122, 123, 124, 126, 127, 129, 130, 131, 133, 134,
         136, 137, 138, 140, 141, 143, 144, 145, 147, 148,
         150, 151, 152, 154, 155, 157, 158, 159, 161, 162,
         164, 165, 166, 168, 169, 171, 172, 173, 175, 176, 178 };
    static int Table_fv2[] = {
        -92, -91, -91, -90, -89, -88, -88, -87, -86, -86, -85,
        -84, -83, -83, -82, -81, -81, -80, -79, -78, -78, -77,
        -76, -76, -75, -74, -73, -73, -72, -71, -71, -70, -69,
        -68, -68, -67, -66, -66, -65, -64, -63, -63, -62,
        -61, -61, -60, -59, -58, -58, -57, -56, -56, -55,
        -54, -53, -53, -52, -51, -51, -50, -49, -48, -48, -47,
        -46, -46, -45, -44, -43, -43, -42, -41, -41, -40, -39, -38, -38,
        -37, -36, -36, -35, -34, -33, -33, -32, -31, -31, -30, -29,
        -28, -28, -27, -26, -26, -25, -24, -23, -23, -22, -21, -21, -20, -19,
        -18, -18, -17, -16, -16, -15, -14, -13, -13, -12, -11, -11, -10, -9,
        -8, -8, -7, -6, -6, -5, -4, -3, -3, -2, -1, 0, 0, 1, 2, 2, 3, 4, 5, 5,
        6, 7, 7, 8, 9, 10, 10, 11, 12, 12, 13, 14, 15, 15, 16, 17, 17, 18, 19,
        20, 20, 21, 22, 22, 23, 24, 25, 25, 26, 27, 27, 28, 29, 30, 30, 31,
        32, 32, 33, 34, 35, 35, 36, 37, 37, 38, 39, 40, 40, 41, 42, 42,
        43, 44, 45, 45, 46, 47, 47, 48, 49, 50, 50, 51, 52, 52, 53, 54, 55, 55,
        56, 57, 57, 58, 59, 60, 60, 61, 62, 62, 63, 64, 65, 65, 66, 67, 67, 68, 69,
        70, 70, 71, 72, 72, 73, 74, 75, 75, 76, 77, 77, 78, 79, 80, 80, 81, 82, 82,
        83, 84, 85, 85, 86, 87, 87, 88, 89, 90, 90 };
    static int Table_fu1[] = {
        -44, -44, -44, -43, -43, -43, -42, -42, -42,
        -41, -41, -41, -40, -40, -40, -39, -39, -39,
        -38, -38, -38, -37, -37, -37, -36, -36, -36,
        -35, -35, -35, -34, -34, -33, -33, -33,
        
        -32, -32, -32, -31, -31, -31, -30, -30, -30,
        -29, -29, -29, -28, -28, -28, -27, -27, -27,
        -26, -26, -26, -25, -25, -25, -24, -24, -24,
        -23, -23, -22, -22, -22, -21, -21, -21,
        
        -20, -20, -20, -19, -19, -19, -18, -18, -18,
        -17, -17, -17, -16, -16, -16, -15, -15, -15,
        -14, -14, -14, -13, -13, -13, -12, -12,
        
        -11, -11, -11, -10, -10, -10, -9, -9, -9,
        -8, -8, -8, -7, -7, -7, -6, -6, -6, -5,
        -5, -5, -4, -4, -4, -3, -3, -3,
        -2, -2, -2, -1, -1,
        
        0, 0, 0, 1, 1, 1, 2, 2, 2,
        3, 3, 3, 4, 4, 4, 5, 5, 5,
        6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10,
        11, 11, 11, 12, 12, 12, 13, 13, 13, 14, 14, 14,
        15, 15, 15, 16, 16, 16, 17, 17, 17, 18, 18, 18,
        19, 19, 19, 20, 20, 20, 21, 21, 22, 22, 22, 23, 23, 23,
        24, 24, 24, 25, 25, 25, 26, 26, 26, 27, 27, 27,
        28, 28, 28, 29, 29, 29, 30, 30, 30, 31, 31, 31, 32, 32,
        33, 33, 33, 34, 34, 34, 35, 35, 35, 36, 36, 36, 37, 37, 37,
        38, 38, 38, 39, 39, 39, 40, 40, 40, 41, 41, 41, 42, 42, 42,
        43, 43 };
    
    static int Table_fu2[] = {
        -227, -226, -224, -222, -220, -219, -217, -215, -213, -212,
        -210, -208, -206, -204, -203, -201, -199, -197, -196, -194,
        -192, -190, -188, -187, -185, -183, -181, -180, -178, -176,
        -174, -173, -171, -169, -167, -165, -164, -162, -160, -158,
        -157, -155, -153, -151, -149, -148, -146, -144, -142, -141,
        -139, -137, -135, -134, -132, -130, -128, -126, -125, -123,
        -121, -119, -118, -116, -114, -112, -110, -109, -107, -105,
        -103, -102, -100, -98, -96, -94, -93, -91, -89, -87, -86,
        -84, -82, -80, -79, -77, -75, -73, -71, -70, -68, -66,
        -64, -63, -61, -59, -57, -55, -54, -52, -50, -48, -47,
        -45, -43, -41, -40, -38, -36, -34, -32, -31, -29, -27,
        -25, -24, -22, -20, -18, -16, -15, -13, -11, -9, -8,
        -6, -4, -2, 0, 1, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19,
        21, 23, 24, 26, 28, 30, 31, 33, 35, 37, 39, 40, 42,
        44, 46, 47, 49, 51, 53, 54, 56, 58, 60, 62, 63, 65,
        67, 69, 70, 72, 74, 76, 78, 79, 81, 83, 85, 86, 88,
        90, 92, 93, 95, 97, 99, 101, 102, 104, 106, 108, 109,
        111, 113, 115, 117, 118, 120, 122, 124, 125, 127, 129,
        131, 133, 134, 136, 138, 140, 141, 143, 145, 147, 148,
        150, 152, 154, 156, 157, 159, 161, 163, 164, 166, 168,
        170, 172, 173, 175, 177, 179, 180, 182, 184, 186, 187,
        189, 191, 193, 195, 196, 198, 200, 202, 203, 205, 207,
        209, 211, 212, 214, 216, 218, 219, 221, 223, 225
        };

    public static boolean YV12ToBGRA_Table(ByteBuffer pYUV, ByteBuffer pBGR24, int width, int height)
    {
        pYUV.rewind();
        byte[] yuv = pYUV.array();
        
        pBGR24.rewind();
        byte[] rgb = pBGR24.array();
        YV12ToBGRA_Table(yuv, rgb, width, height);
        
        pBGR24.rewind();
        return true;
    }
    
    
    public static boolean YV12ToBGRA_Table(byte[] pYUV, byte[] pBGR24, int width, int height)
    {
        if (width < 1 || height < 1 || pYUV == null || pBGR24 == null)
        {
            return false;
        }
        
        Log.e(TAG, "size="+pYUV.length+", "+pBGR24.length+", "+width+", "+height);
        int color_depth = 4;
        int len = width * height;
        int yData = 0;
        int vData = len;
        int uData = len >> 2;

        int bgr[] = new int[color_depth];
        int yIdx,uIdx,vIdx,idx;
        int rdif,invgdif,bdif;
        for (int i = 0;i < height;i++){
            for (int j = 0;j < width;j++){
                yIdx = i * width + j;
                vIdx = (i/2) * (width/2) + (j/2);
                uIdx = vIdx;
                
                rdif    = Table_fv1[pYUV[vData+vIdx] & 0xFF];
                invgdif = Table_fu1[pYUV[uData+uIdx] & 0xFF] + Table_fv2[pYUV[vData+vIdx] & 0xFF];
                bdif    = Table_fu2[pYUV[uData+uIdx] & 0xFF];

                bgr[0] = (pYUV[yData+yIdx] & 0xFF) + bdif;    
                bgr[1] = (pYUV[yData+yIdx] & 0xFF) - invgdif;
                bgr[2] = (pYUV[yData+yIdx] & 0xFF) + rdif;

                for (int k = 0;k < color_depth;k++){
                    idx = (i * width + j) * color_depth + k;
                    if(bgr[k] >= 0 && bgr[k] <= 255)
                        pBGR24[idx] = (byte)  bgr[k];
                    else
                        pBGR24[idx] = (byte)((bgr[k] < 0)? 0: 255);
                }
            }
        }
        return true;
    }
    

    /**
     * no test
     * @param rgb
     * @param yuv420sp
     * @param width
     * @param height
     */
    public static void YUV420SPToRGB(int[] rgb, byte[] yuv420sp, int width, int height)
    {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++)
        {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++)
            {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0)
                {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    // }}}}}}}}}}
    
}
