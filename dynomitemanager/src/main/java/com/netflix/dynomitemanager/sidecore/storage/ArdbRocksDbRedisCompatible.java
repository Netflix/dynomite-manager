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

public class ArdbRocksDbRedisCompatible {

    final static String DYNO_ARDB = "ardb-rocksdb";
    final static String DYNO_ARDB_CONF_PATH = "/apps/ardb/conf/rocksdb.conf";
    final static String ARDB_ROCKSDB_START_SCRIPT = "/apps/ardb/bin/launch_ardb.sh";
    final static String ARDB_ROCKSDB_STOP_SCRIPT = "/apps/ardb/bin/kill_ardb.sh";

    private static final Logger logger = LoggerFactory.getLogger(ArdbRocksDbRedisCompatible.class);

    private int writeBufferSize;
    private int maxWriteBufferNumber;
    private int minWriteBufferToMerge;
    private long storeMaxMem;

    public ArdbRocksDbRedisCompatible(long storeMaxMem, int writeBufferSize, int maxWriteBufferNumber,
            int minWriteBufferToMerge) {
        this.writeBufferSize = writeBufferSize;
        this.maxWriteBufferNumber = maxWriteBufferNumber;
        this.minWriteBufferToMerge = minWriteBufferToMerge;
        this.storeMaxMem = storeMaxMem;
    }

    private String ConvertRocksDBOptions(String rocksDBOptions) {
        // split the arguments based on the ";"
        String[] allOptions  = rocksDBOptions.split(";");

        // String builder to put the properties back
        StringBuilder newProperties = new StringBuilder();

        // parse the properties and replace
        for (String pr : allOptions) {
            logger.info("Checking Property: '" + pr + "'");

            // change the properties to the updated values
            if (pr.contains("write_buffer_size")) {
                pr = "write_buffer_size=" + writeBufferSize + "MB";
                logger.info("Updating to: '" + pr + "'");
            } else if (pr.contains("max_write_buffer_number")) {
                pr = "max_write_buffer_number=" + maxWriteBufferNumber;
                logger.info("Updating to: '" + pr + "'");
            } else if (pr.contains("min_write_buffer_number_to_merge")) {
                pr = "min_write_buffer_number_to_merge=" + minWriteBufferToMerge;
                logger.info("Updating to: '" + pr + "'");
            }
            /*
             * reconstructing
             */
            if (pr.contains("\\")) {
                pr = pr.replace("\\", "");
                if (pr.length() > 0) {
                    pr += ";";
                }
                pr = pr + "\\";
                newProperties.append(pr);
            }
            else
                newProperties.append(pr + ";");
            logger.info("Appending Property: '" + pr +"'");
        }
        return newProperties.toString();

    }
    public void updateConfiguration(String confPathName) throws IOException {

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

        if (this.writeBufferSize * this.maxWriteBufferNumber > this.storeMaxMem) {
            logger.warn("There is not enough memory in the instance. Using writeBufferSize = 128MB");
            this.writeBufferSize = 128;
            if (this.writeBufferSize * this.maxWriteBufferNumber > this.storeMaxMem) {
                logger.warn("There is still not enough memory. Using maxWriteBufferNumber = 10");
                this.maxWriteBufferNumber = 10;
            }
        }

        String rocksdbOptions = "rocksdb.options";

        String ardbRedisCompatibleMode = "^redis-compatible-mode \\s*[a-zA-Z]*";

        String logLevel = "^loglevel \\s*[a-zA-Z]*";

        logger.info("Updating ARDB/RocksDB conf: " + confPathName);
        Path confPath = Paths.get(confPathName);
        Path backupPath = Paths.get(confPathName + ".bkp");
        // backup the original baked in conf only and not subsequent updates
        if (!Files.exists(backupPath)) {
            logger.info("Backing up baked in ARDB/RocksDB config at: " + backupPath);
            Files.copy(confPath, backupPath, COPY_ATTRIBUTES);
        }

        boolean rocksParse = false;

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
                String compatible = "redis-compatible-mode yes";
                logger.info("Updating ARDB property: " + compatible);
                newLines.add(compatible);
                continue;
            } else if (line.matches(logLevel)) {
                String logWarn = "loglevel warn";
                logger.info("Updating ARDB property: " + logWarn);
                newLines.add(logWarn);
                continue;
            } else if (line.contains(rocksdbOptions)) {
                logger.info("Found rocksdb option");
                rocksParse = true;
                String[] keyValue = line.split("\\s+");
                newLines.add(keyValue[0] + spaces(15) + ConvertRocksDBOptions(keyValue[1]));
                continue;
            } else if (rocksParse) {
                // we need this for multi-line options parsing
                if (!line.contains("\\")) {
                    rocksParse = false;
                }
                newLines.add(ConvertRocksDBOptions(line));
                continue;
            } else {
                newLines.add(line);
            }
        }

        Files.write(confPath, newLines, Charsets.UTF_8, WRITE, TRUNCATE_EXISTING);
    }

    private static String spaces(int numberOfSpaces) {
        // String builder is efficient at concatenating strings together
        StringBuilder sb = new StringBuilder();

        // Loop as many times as specified; each time add a space to the string
        for (int i = 0; i < numberOfSpaces; i++) {
            sb.append(" ");
        }

        // Return the string
        return sb.toString();
    }

}
