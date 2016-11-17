/**
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.dynomitemanager.sidecore.aws;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.identity.AppsInstance;
import com.netflix.dynomitemanager.identity.IAppsInstanceFactory;
import com.netflix.dynomitemanager.identity.IMembership;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.sidecore.scheduler.SimpleTimer;
import com.netflix.dynomitemanager.sidecore.scheduler.Task;
import com.netflix.dynomitemanager.sidecore.scheduler.TaskTimer;

/**
 * Update the security access control list, such as the inbound connections list in an AWS Security Group.
 */
@Singleton
public class UpdateSecuritySettings extends Task {

	public static final String JOBNAME = "Update_SG";
	public static boolean firstTimeUpdated = false;

	private static final Random ran = new Random();
	private final IMembership membership;
	private final IAppsInstanceFactory factory;

	@Inject
	public UpdateSecuritySettings(IConfiguration config, IMembership membership, IAppsInstanceFactory factory) {
		super(config);
		this.membership = membership;
		this.factory = factory;
	}

	@Override
	public void execute() {
		// if seed does not execute.
		int port = config.getDynomitePeerPort();
		List<String> acls = membership.listACL(port, port);
		List<AppsInstance> instances = factory.getAllIds(config.getDynomiteClusterName());

		// iterate to add...
		List<String> add = Lists.newArrayList();
		for (AppsInstance instance : factory.getAllIds(config.getDynomiteClusterName())) {
			String range = instance.getHostIP() + "/32";
			if (!acls.contains(range))
				add.add(range);
		}
		if (add.size() > 0) {
			membership.addACL(add, port, port);
			firstTimeUpdated = true;
		}

		// just iterate to generate ranges.
		List<String> currentRanges = Lists.newArrayList();
		for (AppsInstance instance : instances) {
			String range = instance.getHostIP() + "/32";
			currentRanges.add(range);
		}

		// iterate to remove...
		List<String> remove = Lists.newArrayList();
		for (String acl : acls)
			if (!currentRanges.contains(acl)) // if not found then remove....
				remove.add(acl);
		if (remove.size() > 0) {
			membership.removeACL(remove, port, port);
			firstTimeUpdated = true;
		}
	}

	public static TaskTimer getTimer(InstanceIdentity id) {
		SimpleTimer return_;
		if (id.isSeed())
			return_ = new SimpleTimer(JOBNAME, 120 * 1000 + ran.nextInt(120 * 1000));
		else
			return_ = new SimpleTimer(JOBNAME);
		return return_;
	}

	@Override
	public String getName() {
		return JOBNAME;
	}

}
