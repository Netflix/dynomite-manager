package com.netflix.nfsidecar.backup;

import java.io.File;

import org.joda.time.DateTime;

public interface Backup {
      boolean upload(File file, DateTime todayStart);
}
