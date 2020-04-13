package net.quantum6.kit;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.hardware.Camera;

public final class CameraKit
{
    private final static String TAG = CameraKit.class.getCanonicalName();
    private static final float MIN_SCREEN_RATIO         = 1.5f;
    
    private CameraKit()
    {
        //
    }

    private static boolean setCameraFocus(Camera.Parameters parameters, String selected)
    {
        List<String> modes = parameters.getSupportedFocusModes();
        try
        {
            for (String mode : modes)
            {
                //优先使用这个对焦方式。
                if (null != mode && mode.equals(selected))
                {
                    parameters.setFocusMode(mode);
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
    
    public static void setCameraFocus(Camera.Parameters parameters)
    {
        String[] selectedModes =
            {
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            };
        for (String mode : selectedModes)
        {
            if (setCameraFocus(parameters, mode))
            {
                return;
            }
        }
    }
    
    private static class SizeComparator implements Comparator<Camera.Size>
    {
        @Override
        public int compare(Camera.Size arg0, Camera.Size arg1)
        {
            if (arg0.width > arg1.width)
            {
                return 1;
            }
            if (arg0.width < arg1.width)
            {
                return -1;
            }
            if (arg0.height == arg1.height)
            {
                return 0;
            }
            return (arg0.height > arg1.height) ? 1 : -1;
        }
    }
    
    
    /**
     * 1, 从大到小。
     * ２，优先宽度。宽度相同第一优先；若无相同宽度，则选择低于且最接近的一个。
     * ３、相同分辨率，选择比例最接近的。
     */
    private static Camera.Size selectCameraSize(List<Camera.Size> previewSizes, int settingsWidth, int settingsHeight, boolean isWide)
    {
        final boolean setWide = ((1f * settingsWidth / settingsHeight) >= MIN_SCREEN_RATIO);
        for (int i = previewSizes.size()-1; i >=0; i--)
        {
            Camera.Size size = previewSizes.get(i);
            Log.d(TAG, "selectPictureSize() i="+i+ " ("+size.width+", "+size.height+")"+settingsWidth+", "+settingsHeight+", "+isWide);
            if (size.width <= settingsWidth && size.height <= settingsHeight)
            {
                //非宽模式
                if (!isWide || !setWide)
                {
                    Log.d(TAG, "selectPictureSize() setting=("+settingsWidth+", "+settingsHeight+") normal=("+size.width+", "+size.height+")");
                    return size;
                }
                //宽模式
                if ((1f * size.width / size.height) >= MIN_SCREEN_RATIO)
                {
                    Log.d(TAG, "selectPictureSize() setting=("+settingsWidth+", "+settingsHeight+") wide=("+size.width+", "+size.height+")");
                    return size;
                }
            }
        }
        return null;
    }
    
    public static Camera.Size getCameraBestPreviewSize(final Camera.Parameters parameters, final int width, final int height){
        
        List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
        //little to big.
        Collections.sort(supportedSizes, new SizeComparator());

        //first wide screen.
        Camera.Size bestSize = selectCameraSize(supportedSizes, width, height, true);
        if (null == bestSize)
        {
            bestSize  = selectCameraSize(supportedSizes, width, height, false);
        }
    
        // V3上的方式没有能找到合适的，那就用这里原来的选择分辨率方案。
        if (null == bestSize)
        {
            Camera.Size minSize  = null;
            int  minScore = Integer.MAX_VALUE;
            for (Camera.Size size : supportedSizes)
            {
                final int score = Math.abs(size.width - width) + Math.abs(size.height - height);
                if (minScore > score)
                {
                    minScore = score;
                    minSize = size;
                }
            }
            bestSize = minSize;
        }
    
        return bestSize;
    }

}
