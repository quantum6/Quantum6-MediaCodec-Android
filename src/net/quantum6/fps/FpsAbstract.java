package net.quantum6.fps;

abstract class FpsAbstract
{
    protected final static long TIME_ONE_SECOND = 1000;

    protected int mFpsInCurrent;
    protected int mFpsInCounter;

    protected long mStartTime;

    public void reset()
    {
        mFpsInCurrent = 0;
        
        mFpsInCounter = 0;

        mStartTime = 0;
    }
    
    public void release()
    {
        //
    }
}
