package com.netflix.dynomitemanager.dynomite;

import java.io.IOException;


/**
 * Interface to aid in starting and stopping Dynomite.
 *
 */
public interface IDynomiteProcess
{
    void start() throws IOException;

    void stop() throws IOException;
    
    boolean dynomiteCheck();
    
    boolean dynomiteProcessCheck();
}
