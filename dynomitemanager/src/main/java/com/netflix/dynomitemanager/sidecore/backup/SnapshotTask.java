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
package com.netflix.dynomitemanager.sidecore.backup;

import static com.netflix.dynomitemanager.defaultimpl.DynomiteManagerConfiguration.LOCAL_ADDRESS;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.sidecore.ICredential;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.sidecore.scheduler.SimpleTimer;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskTimer;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;
import com.netflix.dynomitemanager.sidecore.utils.ThreadSleeper;
import com.netflix.dynomitemanager.sidecore.scheduler.CronTimer;
import com.netflix.dynomitemanager.sidecore.scheduler.CronTimer.DayOfWeek;

/**
 * Task for creating backup snapshots.
 */
@Singleton
public class SnapshotTask extends Task {

	public static final String TaskName = "SnapshotTask";
	private static final Logger logger = LoggerFactory.getLogger(SnapshotTask.class);
	private final ThreadSleeper sleeper = new ThreadSleeper();
	private final ICredential cred;
	private final InstanceIdentity iid;
	private final InstanceState state;
	private final IStorageProxy storageProxy;
	private final Backup backup;

	private final int storageRetries = 5;

	@Inject
	public SnapshotTask(IConfiguration config, InstanceIdentity id, ICredential cred, InstanceState state,
			IStorageProxy storageProxy, Backup backup) {
		super(config);
		this.cred = cred;
		this.iid = id;
		this.state = state;
		this.storageProxy = storageProxy;
		this.backup = backup;
	}

	public void execute() throws Exception {
		this.state.setFirstBackup(false);
		if (!state.isRestoring() && !state.isBootstrapping()) {
			/** Iterate five times until storage (Redis) is ready.
			 *  We need storage to be ready to dumb the data,
			 *  otherwise we may backup older data. Another case,
			 *  is that the thread that starts Dynomite has not
			 *  started Redis yet.
			 */
			int i = 0;
			for (i = 0; i < this.storageRetries; i++) {
				if (!this.state.isStorageAlive()) {
					//sleep 2 seconds to make sure Dynomite process is up, Storage process is up.
					sleeper.sleepQuietly(2000);
				} else {
					this.state.setBackingup(true);
					/**
					 * Set the status of the backup to false every time we start a backup.
					 * This will ensure that prior to backup we recapture the status of the backup.
					 */
					this.state.setBackUpStatus(false);

					// the storage proxy takes a snapshot or compacts data
					boolean snapshot = this.storageProxy.takeSnapshot();
					File file = null;
					if (config.isRedisAofEnabled()) {
						file = new File(config.getRedisDataDir() + "/appendonly.aof");
					} else {
						file = new File(config.getRedisDataDir() + "/nfredis.rdb");
					}
					// upload the data to S3
					if (file.length() > 0 && snapshot == true) {
						DateTime now = DateTime.now();
						DateTime todayStart = now.withTimeAtStartOfDay();
						this.state.setBackupTime(todayStart);

						if (this.backup.upload(file, todayStart)) {
							this.state.setBackUpStatus(true);
							logger.info("S3 backup status: Completed!");
						} else {
							logger.error("S3 backup status: Failed!");
						}
					} else {
						logger.warn("S3 backup: Redis AOF file length is zero - nothing to backup");
					}
					break;
				}
			}

			if (i == this.storageRetries) {
				logger.error("S3 backup Failed: Redis was not up after " + this.storageRetries
						+ " retries");
			}
			this.state.setBackingup(false);
		} else {
			logger.error("S3 backup Failed: Restore is happening");
		}

	}

	@Override
	public String getName() {
		return TaskName;
	}

	/**
	 * Returns a timer that enables this task to run on a scheduling basis defined by FP
	 * if the BackupSchedule == week, it runs on Monday
	 * if the BackupSchedule == day, it runs everyday.
	 * @return TaskTimer
	 */
	public static TaskTimer getTimer(IConfiguration config) {
		int hour = config.getBackupHour();
		if (config.getBackupSchedule().equals("week")) {
			return new CronTimer(DayOfWeek.MON, hour, 1, 0);
		}
		return new CronTimer(hour, 1, 0);

	}

}

