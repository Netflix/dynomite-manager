package com.netflix.florida.utils.test;

import java.io.File;
import java.util.Scanner;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.florida.sidecore.storage.ArdbRocksDbRedisCompatible;

public class ArdbConfParserTest {
    @Test
    public void test() throws Exception {

        /**
         * some random values
         */
        int writeBufferSize = 128;
        int maxWriteBufferNumber = 16;
        int minWriteBufferToMerge = 4;
        long storeMaxMem = 10000000;

        String configPathName = "./src/test/resources/rocksdb.conf";
        ArdbRocksDbRedisCompatible checkConf = new ArdbRocksDbRedisCompatible(storeMaxMem, writeBufferSize,
                maxWriteBufferNumber, minWriteBufferToMerge);
        checkConf.updateConfiguration(configPathName);

        String conf = new Scanner(new File(configPathName)).useDelimiter("\\Z").next();
        
        final String bufSize = "write_buffer_size=" + writeBufferSize + "M;";
        int occurrences = 0;
        int index = 0;
        while (index < conf.length() && (index = conf.indexOf(bufSize, index)) >= 0) {
            occurrences++;
            index += bufSize.length();
        }
        Assert.assertTrue(occurrences == 1);

        final String bufNum = "max_write_buffer_number=" + maxWriteBufferNumber;
        occurrences = 0;
        index = 0;
        while (index < conf.length() && (index = conf.indexOf(bufNum, index)) >= 0) {
            occurrences++;
            index += bufNum.length();
        }
        Assert.assertTrue(occurrences == 1);
        
        final String bufMerge = "min_write_buffer_number_to_merge=" + minWriteBufferToMerge;;
        occurrences = 0;
        index = 0;
        while (index < conf.length() && (index = conf.indexOf(bufMerge, index)) >= 0) {
            occurrences++;
            index += bufMerge.length();
        }
        Assert.assertTrue(occurrences == 1);
        
        
    }
}
