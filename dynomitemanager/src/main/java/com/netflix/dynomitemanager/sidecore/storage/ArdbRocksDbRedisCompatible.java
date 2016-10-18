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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;

public class ArdbRocksDbRedisCompatible {
    final static String DYNO_ARDB_ROCKSDB = "ardb-rocksdb";
    final static String DYNO_ARDB_CONF_PATH = "/apps/ardb/conf/rocksdb.conf";
    final static String ARDB_ROCKSDB_START_SCRIPT = "/apps/ardb/bin/launch_ardb.sh";
    final static String ARDB_ROCKSDB_STOP_SCRIPT = "/apps/ardb/bin/kill_ardb.sh";
    
    private static final Logger logger = LoggerFactory.getLogger(ArdbRocksDbRedisCompatible.class);
    
    public static void updateConfiguration(long storeMaxMem, IConfiguration config) throws IOException {

	/**
	 * --- ARDB configuration ----
	 * 
	 * rocksdb.options
	 * write_buffer_size=512M;max_write_buffer_number=5;min_write_buffer_number_to_merge=2;compression=kSnappyCompression;\
	 * bloom_locality=1;memtable_prefix_bloom_bits=100000000;memtable_prefix_bloom_probes=6;\
	 * block_based_table_factory={block_cache=512M;filter_policy=bloomfilter:10:true};\
	 * create_if_missing=true;max_open_files=10000;rate_limiter_bytes_per_sec=50M
	 * 
	 * write_buffer_size = 512MB; max_write_buffer_number = 5;
	 * 
	 * We check if the memory is above 10GB and then allocate more
	 * max_write_buffer_number. This approach is naive and should be
	 * optimized
	 * 
	 */
	
	int writeBufferSize = config.getWriteBufferSize();
	int maxWriteBufferNumber = config.getMaxWriteBufferNumber();
	if (writeBufferSize * maxWriteBufferNumber > storeMaxMem) {
	    logger.warn("There is not enough memory in the instance. Using writeBufferSize = 128MB");
	    writeBufferSize = 128;
	    if (writeBufferSize * maxWriteBufferNumber > storeMaxMem) {
		logger.warn("There is still not enough memory. Using maxWriteBufferNumber = 10");
		maxWriteBufferNumber = 10;
	    }
	}
	int minWriteBufferToMerge = config.getMinWriteBufferToMerge();

	String rocksdbOptions = "rocksdb.options";

	String ardbRedisCompatibleMode = "^redis-compatible-mode \\s*[a-zA-Z]*";

	logger.info("Updating ARDB/RocksDB conf: " + DYNO_ARDB_CONF_PATH);
	Path confPath = Paths.get(DYNO_ARDB_CONF_PATH);
	Path backupPath = Paths.get(DYNO_ARDB_CONF_PATH + ".bkp");
	// backup the original baked in conf only and not subsequent updates
	if (!Files.exists(backupPath)) {
	    logger.info("Backing up baked in ARDB/RocksDB config at: " + backupPath);
	    Files.copy(confPath, backupPath, COPY_ATTRIBUTES);
	}

	boolean rocksParse = false;
	StringBuilder rocksProperties = new StringBuilder();

	// Not using Properties file to load as we want to retain all comments,
	// and for easy diffing with the ami baked version of the conf file.
	List<String> lines = Files.readAllLines(confPath, Charsets.UTF_8);
	
	// Create a new list to write back the file.
	List<String> newLines = new ArrayList<String>();

	for (int i = 0; i < lines.size(); i++) {
	    String line = lines.get(i);
	    if (line.startsWith("#")) {
		newLines.add(line);
		continue;
	    }
	    if (line.matches(ardbRedisCompatibleMode)) {
		String compatibable = "redis-compatible-mode yes";
		logger.info("Updating ARDB property: " + compatibable);
		newLines.add(compatibable);
	    } else if (line.contains(rocksdbOptions)) {
		rocksParse = true;
		/*** KEY = rocksdb.options ***/
		// split key and value pair of this line
		String[] keyValue = line.split("\\s+");
		rocksProperties.append(keyValue[1]);
	    } else if (rocksParse) { // we need this for multi-line options parsing
		// remove the empty space in the front and then append the value
		rocksProperties.append(line.replaceAll("\\s", ""));

		// last line of parsing rocksdb.options will not have ";\"
		if (!line.contains("\\")) {
		    // split the arguments based on the ";"
		    String[] allProperties = rocksProperties.toString().split(";");

		    // String builder to put the properties back
		    StringBuilder newProperties = new StringBuilder();

		    // parse the properties and replace
		    for (String pr : allProperties) {
			
			boolean newLine = false;
			// removing any prepending "\" in the properties
			if (pr.contains("\\")) {
			    pr = pr.replace("\\", "");
			    newLine = true;
			}
			logger.info(pr);
			// change the properties to the updated values
			if (pr.contains("write_buffer_size")) {
			    pr = "write_buffer_size=" + writeBufferSize + "MB";
			    logger.info("property: " + pr);
			} else if (pr.contains("max_write_buffer_number")) {
			    pr = "max_write_buffer_number=" + maxWriteBufferNumber;
			    logger.info("property: " + pr);
			} else if (pr.contains("min_write_buffer_number_to_merge")) {
			    pr = "min_write_buffer_number_to_merge=" + minWriteBufferToMerge;
			    logger.info("updating property: " + pr);
			}
			/*
			 * reconstructing 
			 */
			if (newLine == false) {
			    newProperties.append(pr + ";");
			}
			else {
			    newProperties.append(pr + ";\\" + System.lineSeparator() + spaces(30));
			}
		    }
		    newLines.add("rocksdb.options" + spaces(15) + newProperties.toString());
		    rocksParse = false;
		}
	    }
	    else {
		newLines.add(line);
	    }
	}

	Files.write(confPath, newLines, Charsets.UTF_8, WRITE, TRUNCATE_EXISTING);
    }
    
    private static String spaces(int numberOfSpaces)
    {
        //String builder is efficient at concatenating strings together
        StringBuilder sb = new StringBuilder();

        //Loop as many times as specified; each time add a space to the string
        for(int i=0; i < numberOfSpaces; i++)
        {
            sb.append(" ");
        }

        //Return the string
        return sb.toString();
    }

}
