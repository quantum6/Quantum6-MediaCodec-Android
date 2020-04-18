package net.quantum6.kit;

public final class Log
{
    public static boolean debug = true;

    private Log()
    {
    }

    public static int v(String tag, String msg) {
        return debug ? android.util.Log.v(tag, msg) : 0;
    }

    public static int v(String tag, String msg, Throwable tr)
    {
        return debug ? android.util.Log.v(tag, msg, tr) : 0;
    }

    public static int d(String tag, String msg)
    {
        return debug ? android.util.Log.d(tag, msg) : 0;
    }

    public static int d(String tag, String msg, Throwable tr)
    {
        return debug ? android.util.Log.d(tag, msg, tr) : 0;
    }

    public static int i(String tag, String msg)
    {
        return debug ? android.util.Log.i(tag, msg) : 0;
    }

    public static int i(String tag, String msg, Throwable tr)
    {
        return debug ? android.util.Log.i(tag, msg, tr) : 0;
    }

    public static int w(String tag, String msg)
    {
        return debug ? android.util.Log.w(tag, msg) : 0;
    }

    public static int w(String tag, String msg, Throwable tr)
    {
        return debug ? android.util.Log.w(tag, msg, tr) : 0;
    }

    public static int w(String tag, Throwable tr)
    {
        return debug ? android.util.Log.w(tag, tr) : 0;
    }

    public static int e(String tag, String msg)
    {
        return debug ? android.util.Log.e(tag, msg) : 0;
    }

    public static int e(String tag, String msg, Throwable tr)
    {
        return debug ? android.util.Log.e(tag, msg, tr) : 0;
    }
}
