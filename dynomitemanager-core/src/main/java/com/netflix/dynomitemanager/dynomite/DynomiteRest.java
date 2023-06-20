package com.netflix.dynomitemanager.dynomite;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

public class DynomiteRest {
    
    private static final Logger logger = LoggerFactory.getLogger(DynomiteRest.class);

    
    public static boolean sendCommand(String cmd) {
    	DynamicStringProperty adminUrl = 
                DynamicPropertyFactory.getInstance().getStringProperty("florida.metrics.url", "http://localhost:22222");
    	
        String url = adminUrl.get() + cmd;
        OkHttpClient client = new OkHttpClient();

        
        GetMethod get = new GetMethod(url);
        try {
            Request request = new Request.Builder()
                    .url(url).build();
            Response httpResponse = client.newCall(request).execute();
            int statusCode = httpResponse.code();
            if (!(statusCode == 200)) {
                logger.error("Got non 200 status code from " + url);
                return false;
            }
            
            String response = httpResponse.body().string();
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
