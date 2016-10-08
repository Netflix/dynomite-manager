/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.sidecore.storage;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class ArdbRocksDbRedisCompatible {
    final static String DYNO_ARDB_CONF_PATH = "/apps/ardb/conf/rocksdb.conf";
    final static String ARDB_ROCKSDB_START_SCRIPT = "/apps/ardb/bin/launch_ardb.sh";
    final static String ARDB_ROCKSDB_STOP_SCRIPT = "/apps/ardb/bin/kill_ardb.sh";
    
    private static final Logger logger = LoggerFactory.getLogger(ArdbRocksDbRedisCompatible.class);
    
    public static void updateConfiguration(long storeMaxMem) throws IOException {
	
	/**
	 * write_buffer_size = 512MB;
         * max_write_buffer_number = 5;
         * 
         * We check if the memory is above 10GB and then allocate more max_write_buffer_number.
         * This approach is naive and should be optimized
         * 
	 */
	int max_write_buffer_number = 5;
	if (storeMaxMem > 10*1024*1024*1024) { // max storage memory
	    max_write_buffer_number = 20;
	}
	

  	String ardbRedisCompatibleMode = "^redis-compatible-mode \\s*[a-zA-Z]*";
  	String maxWriteBufferNumber = ".max_write_buffer_number \\s*[a-zA-Z]*";

  	logger.info("Updating ARDB/RocksDB conf: " + DYNO_ARDB_CONF_PATH);
  	Path confPath = Paths.get(DYNO_ARDB_CONF_PATH);
  	Path backupPath = Paths.get(DYNO_ARDB_CONF_PATH + ".bkp");
  	// backup the original baked in conf only and not subsequent updates
  	if (!Files.exists(backupPath)) {
  	    logger.info("Backing up baked in ARDB/RocksDB config at: " + backupPath);
  	    Files.copy(confPath, backupPath, COPY_ATTRIBUTES);
  	}

  	// Not using Properties file to load as we want to retain all comments,
  	// and for easy diffing with the ami baked version of the conf file.
  	List<String> lines = Files.readAllLines(confPath, Charsets.UTF_8);
  	for (int i = 0; i < lines.size(); i++) {
  	    String line = lines.get(i);
  	    if (line.startsWith("#")) {
  		continue;
  	    }
  	    if (line.matches(ardbRedisCompatibleMode)) {
  		String compatibable = "redis-compatible-mode yes";
  		logger.info("Updating ARDB property: " + compatibable);
  		lines.set(i, compatibable);
  	    }
  	    else if(line.matches(maxWriteBufferNumber)) {
  		String writeBufferNumber = "max_write_buffer_number=" + max_write_buffer_number + ";\\"; 
  		String padded = String.format("%-20s", writeBufferNumber);
  		logger.info("updatng options to: " + writeBufferNumber);
  		lines.set(i, padded);
  	    }
  	}

  	Files.write(confPath, lines, Charsets.UTF_8, WRITE, TRUNCATE_EXISTING);
      }


}
