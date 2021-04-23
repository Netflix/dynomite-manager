package com.netflix.dynomitemanager.dynomite;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.netflix.dynomitemanager.config.FloridaConfig;

public class DynomiteRest {
    
    private static final Logger logger = LoggerFactory.getLogger(DynomiteRest.class);

    private FloridaConfig config;
    
    @Inject
    public DynomiteRest(FloridaConfig config) {
    	this.config = config;
    }

    
    public boolean sendCommand(String cmd) {
    	    	    			
        String url = config.getAdminUrl() + cmd;
        HttpClient client = new HttpClient();
        client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
                                        new DefaultHttpMethodRetryHandler());
        
        GetMethod get = new GetMethod(url);
        try {
            int statusCode = client.executeMethod(get);
            if (!(statusCode == 200)) {
                logger.error("Got non 200 status code from " + url);
                return false;
            }
            
            String response = get.getResponseBodyAsString();
            //logger.info("Received response from " + url + "\n" + response);
            
            if (!response.isEmpty()) {
                logger.info("Received response from " + url + "\n" + response);
            } else {
                logger.error("Cannot parse empty response from " + url);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to sendCommand and invoke url: " + url, e);
            return false;
        }
        logger.info("Dynomite REST completed succesfully: " + url);

        return true;
    }

  
}
