package net.quantum6.fps;

public final class FpsController extends FpsAbstract
{
    //private final static String TAG = FpsController.class.getCanonicalName();

    private int mFpsWant;

    protected int mFpsOutCurrent;
    protected int mFpsOutCounter;

    protected int mUnit1;
    protected int mUnit2;
    

    public FpsController(int fps)
    {
        mFpsWant = fps;
    }
    
    public int[] getFps()
    {
    	return new int[]{mFpsInCurrent, mFpsOutCurrent};
    }

    public int[] getFpsAndClear()
    {
    	int[] fps = getFps();
    	
    	mFpsInCurrent = 0;
    	mFpsOutCurrent= 0;
    	return fps;
    }

    public boolean control()
    {
    	long currentTime = System.currentTimeMillis();
        int diff = (int)(currentTime - mStartTime);
        if (0 == mStartTime || diff >= TIME_ONE_SECOND)
        {
            mStartTime = currentTime;
            
            mFpsInCurrent  = mFpsInCounter;
            mFpsInCounter  = 1;
            mFpsOutCurrent = mFpsOutCounter;
            mFpsOutCounter = 1;
            
            //Log.d(TAG, "mFpsInCurrent="+mFpsInCurrent+", mFpsOutCurrent="+mFpsOutCurrent);
            
            mUnit1 = 0;
            mUnit2 = 0;
            if (mFpsWant > 0 && mFpsInCurrent > mFpsWant)
            {
                int left1 = (mFpsInCurrent - mFpsWant);
                mUnit1 = mFpsInCurrent / left1;
                if (mUnit1 * left1 < mFpsInCurrent)
                {
                    mUnit1 ++;
                    int left2 = left1 - (mFpsInCurrent / mUnit1);
                    if (left2 > 0)
                    {
                        mUnit2 = mFpsInCurrent/left2;
                    }
                }
            }
            return false;
        }
        
        mFpsInCounter ++;
    	if (mFpsWant <= 0)
    	{
    		return false;
    	}
    	
    	mFpsOutCounter++;
        if (mFpsInCurrent <= mFpsWant)
        {
            return false;
        }
        if (mUnit1 > 0 && mFpsInCounter % mUnit1 == 0
                || mUnit2 > 0 && mFpsInCounter % mUnit2 == 0)
        {
            mFpsOutCounter--;
            return true;
        }
        return false;
    }
    
    public void setFps(int fps)
    {
    	mFpsWant = fps;
    	
        mFpsOutCurrent = 0;
        mFpsOutCounter = 0;

    	reset();
    }
}
