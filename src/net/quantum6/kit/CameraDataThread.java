package net.quantum6.kit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;

import android.hardware.Camera;

public abstract class CameraDataThread implements Runnable, Camera.PreviewCallback
{
    private final static String TAG = CameraDataThread.class.getCanonicalName();
    
    private final static int DEFAULT_FPS = 30;
    
    private List<byte[]>  mCameraDataList = Collections.synchronizedList(new LinkedList<byte[]>());
    private List<byte[]>  mEmptyDataList  = Collections.synchronizedList(new LinkedList<byte[]>());

    private boolean threadRunning;
    private Camera mCamera;

    public abstract void onCameraDataArrived(final byte[] data, Camera camera);
    
    public void stop()
    {
        threadRunning = false;
        mCameraDataList.clear();
        mEmptyDataList.clear();
    }
    
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera)
    {
        if (!threadRunning)
        {
            return;
        }
        
        mCamera = camera;
        
        byte[] buffer = null;
        if (mEmptyDataList.size() > 0)
        {
            buffer = mEmptyDataList.remove(0);
        }
        if (buffer == null)
        {
            buffer = new byte[data.length];
        }
        if ( buffer.length != data.length)
        {
            mEmptyDataList.clear();
            buffer = new byte[data.length];
        }
        System.arraycopy(data, 0, buffer, 0, data.length);
       
        mCameraDataList.add(buffer);

        if (camera != null)
        {
            camera.addCallbackBuffer(data);
        }
    }
    
    @Override
    public void run()
    {
        threadRunning = true;
        long unit = 1000/DEFAULT_FPS;
        Log.e(TAG, "run()"+threadRunning);
        
        while (threadRunning)
        {
            if (mCameraDataList.size() < 2)
            {
                SystemKit.sleep(unit);
                continue;
            }

            long startTime = System.currentTimeMillis();
            
            byte[] buffer = mCameraDataList.get(0);
            
            onCameraDataArrived(buffer, mCamera);
            mCameraDataList.remove(buffer);
            
            mEmptyDataList.add(buffer);
            SystemKit.sleep(System.currentTimeMillis()-startTime);
        }
        
        mCameraDataList.clear();
        mEmptyDataList.clear();
    }
}
