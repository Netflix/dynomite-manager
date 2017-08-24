package com.netflix.nfsidecar.guice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

/**
 * A holder class around the Governator Guice {@code Injector}
 */
public class GuiceContext {
    private static final Logger logger = LoggerFactory.getLogger(GuiceContext.class);
    private static final GuiceContext INSTANCE = new GuiceContext();
    private Injector injector;
    
    private GuiceContext(){}
    
    /*
     * IMPORTANT:  must be invoked when the web app starts (@see PriamLifecycleListener.initialize())
     */
    public static void setInjector(Injector val) {
        if (INSTANCE.injector == null) {
            synchronized(GuiceContext.class) {
                if (INSTANCE.injector == null) {
                    INSTANCE.injector = val;                    
                }
            }
        }

    }

    public static Injector getInjector()
    {
        if (INSTANCE.injector == null) {
            throw new IllegalStateException("The injector is null.  It should have been set when the web app starts (in some listener such as PriamLifecycleListener.initialize()");
        }
        
        logger.info("The injector provided has id: " + INSTANCE.injector.hashCode());
        
        return INSTANCE.injector;
    }    
}