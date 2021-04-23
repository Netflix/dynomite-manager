package com.netflix.dynomitemanager.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.nfsidecar.scheduler.SimpleTimer;
import com.netflix.nfsidecar.scheduler.Task;
import com.netflix.nfsidecar.scheduler.TaskTimer;

public class DynomiteManagerMetricsTask extends Task {
    private static final Logger Logger = LoggerFactory.getLogger(DynomiteManagerMetricsTask.class);
    public static final String TaskName = "Dynomite-Manager-Task";


	@Override
	public void execute() throws Exception {
		// TODO Auto-generated method stub
		
	}

    @Override
    public String getName() {
        return TaskName;
    }

    public static TaskTimer getTimer() {
        // run once every 30 seconds
        return new SimpleTimer(TaskName, 30 * 1000);
    }

}
