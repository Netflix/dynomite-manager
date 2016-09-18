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

public class ArdbStorageProxy implements IStorageProxy {

    public static final String DYNO_ARDB_ROCKSDB = "ardb-rocksdb";
    public static final String DYNO_ARDB_CONF_PATH = "/apps/ardb/conf/rocksdb.conf";
    public static final int ARDB_PORT = 22122;
    public static final String ARDB_ADDRESS = "127.0.0.1";

    private final String DEFAULT_ARDB_ROCKSDB_START_SCRIPT = "/apps/ardb/bin/launch_ardb.sh";
    private final String DEFAULT_ARDB_ROCKSDB_STOP_SCRIPT = "/apps/nfredis/bin/kill_ardb.sh";

    private static final Logger logger = LoggerFactory.getLogger(ArdbStorageProxy.class);

    @Override
    public boolean isAlive() {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public long getUptime() {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public Bootstrap warmUpStorage(String[] peers) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public boolean resetStorage() {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean takeSnapshot() {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean loadingData() {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public void stopPeerSync() {
	// TODO Auto-generated method stub

    }

    @Override
    public String getEngine() {
	return DYNO_ARDB_ROCKSDB;
    }

    @Override
    public int getEngineNumber() {
	return 3;
    }

    /**
     * Generate ardb.conf.
     * 
     * @throws IOException
     */
    public void updateConfiguration() throws IOException {

	String ardbRedisCompatibleMode = "^redis-compatible-mode \\s*[0-9][0-9]*[a-zA-Z]*";

	logger.info("Updating RocksDB conf: " + DYNO_ARDB_CONF_PATH);
	Path confPath = Paths.get(DYNO_ARDB_CONF_PATH);
	Path backupPath = Paths.get(DYNO_ARDB_CONF_PATH + ".bkp");
	// backup the original baked in conf only and not subsequent updates
	if (!Files.exists(backupPath)) {
	    logger.info("Backing up baked in Redis config at: " + backupPath);
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
		String compatibable = "yes";
		logger.info("Updating ARDB property: " + ardbRedisCompatibleMode);
		lines.set(i, compatibable);
	    }
	}

	Files.write(confPath, lines, Charsets.UTF_8, WRITE, TRUNCATE_EXISTING);
    }

    @Override
    public String getStartupScript() {
	return DEFAULT_ARDB_ROCKSDB_START_SCRIPT;
    }

    @Override
    public String getStopScript() {
	return DEFAULT_ARDB_ROCKSDB_STOP_SCRIPT;
    }
    
    @Override
    public String getIpAddress() {
	return ARDB_ADDRESS;
    }

    @Override
    public int storagePort() {
	return ARDB_PORT;
    }

}
