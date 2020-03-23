package net.quantum6.fps;

public final class FpsCounter extends FpsAbstract
{
    
    public int getFps()
    {
    	return mFpsInCurrent;
    }
    
    public int getFpsAndClear()
    {
    	int fps = mFpsInCurrent;
    	mFpsInCurrent = 0;
    	return fps;
    }

    public void count()
    {
    	long currentTime = System.currentTimeMillis();
        int diff = (int)(currentTime - mStartTime);
        if (0 == mStartTime || diff >= TIME_ONE_SECOND)
        {
            mStartTime = currentTime;
            
            mFpsInCurrent  = mFpsInCounter;
            mFpsInCounter  = 1;
            return;
        }
        
        mFpsInCounter++;
    }

}
