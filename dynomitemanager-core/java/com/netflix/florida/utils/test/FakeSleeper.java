package com.netflix.florida.utils.test;

import com.netflix.florida.sidecore.utils.Sleeper;

public class FakeSleeper implements Sleeper
{
    @Override
    public void sleep(long waitTimeMs) throws InterruptedException
    {
        // no-op
    }

    public void sleepQuietly(long waitTimeMs)
    {
        //no-op
    }
}
