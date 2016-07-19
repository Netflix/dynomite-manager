package com.netflix.dynomitemanager.sidecore.backup;

import java.io.File;

import org.joda.time.DateTime;

import com.netflix.dynomitemanager.sidecore.ICredential;


public interface Backup {
      boolean upload(File file, DateTime todayStart);
}
