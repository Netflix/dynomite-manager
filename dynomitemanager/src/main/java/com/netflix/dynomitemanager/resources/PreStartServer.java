/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.resources;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PreStartServer implements ServletContextListener {
	private static final Logger logger = LoggerFactory.getLogger(PreStartServer.class);
	static final String bootPropFileName = "/etc/dynomitemanager.properties";
	static final String APP_NAME = deriveAppName();

	static String deriveAppName() {
		final String name = System.getenv("NETFLIX_APP");
		System.setProperty("netflix.appinfo.name", name);
		return name;
	}

	public void contextInitialized(ServletContextEvent servletContextEvent) {
		/*
            OK, this is messy as we need to tell platform not to use platform service for
            properties if this is a cassandra instance that holds the tokens.
         */
		if (new File(bootPropFileName).exists()) {
			logger.info("found /etc/dynomitemanager.properties file; reading those props for override");
			try {
				Properties p = new Properties();
				p.load(new FileReader(bootPropFileName));
				for (String s : p.stringPropertyNames()) {
					System.setProperty(s, p.get(s).toString().trim());
				}
			} catch (Exception e) {
				throw new RuntimeException("failed to load " + bootPropFileName, e);
			}
		} else {
			logger.info("did not find /etc/dynomitemanager.properties file; normal reading of properties will occur.");
		}
	}

	public void contextDestroyed(ServletContextEvent servletContextEvent) {

	}
}
