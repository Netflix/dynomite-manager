/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.dynomitemanager.dynomite;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Query the Dynomite REST API.
 */
public class DynomiteAPI {

    private static final Logger logger = LoggerFactory.getLogger(DynomiteAPI.class);

    /**
     * Query the Dynomite REST API.
     *
     * @param url full URL to the Dynomite REST API endpoint
     * @return true if the request was successful or false if it failed
     */
    public static boolean sendCommand(String url) {
        HttpClient client = new HttpClient();
        client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());

        logger.info("Dynomite REST with url: " + url);
        GetMethod get = new GetMethod(url);
        try {
            int statusCode = client.executeMethod(get);
            if (!(statusCode == 200)) {
                logger.error("Got non 200 status code from " + url);
                return false;
            }

            String response = get.getResponseBodyAsString();
            // logger.info("Received response from " + url + "\n" + response);

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
        logger.info("Dynomite REST completed successfully: " + url);

        return true;
    }

}
