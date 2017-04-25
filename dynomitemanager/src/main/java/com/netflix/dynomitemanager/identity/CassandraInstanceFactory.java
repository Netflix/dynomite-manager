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

package com.netflix.dynomitemanager.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;

/**
 * Factory to use Cassandra for managing instance data
 */
@Singleton
public class CassandraInstanceFactory implements IAppsInstanceFactory {
	private static final Logger logger = LoggerFactory.getLogger(CassandraInstanceFactory.class);

	IConfiguration config;
	InstanceDataDAOCassandra dao;

	@Inject
	public CassandraInstanceFactory(IConfiguration config, InstanceDataDAOCassandra dao) {
		this.config = config;
		this.dao = dao;
	}

	public List<AppsInstance> getAllIds(String appName) {
		List<AppsInstance> return_ = new ArrayList<AppsInstance>();
		for (AppsInstance instance : dao.getAllInstances(appName)) {
			return_.add(instance);
		}

		sort(return_);
		return return_;
	}

	public List<AppsInstance> getLocalDCIds(String appName, String region) {
		List<AppsInstance> return_ = new ArrayList<AppsInstance>();
		for (AppsInstance instance : dao.getLocalDCInstances(appName, region)) {
			return_.add(instance);
		}

		sort(return_);
		return return_;
	}

	public void sort(List<AppsInstance> return_) {
		Comparator<? super AppsInstance> comparator = new Comparator<AppsInstance>() {

			@Override
			public int compare(AppsInstance o1, AppsInstance o2) {
				String c1 = o1.getId();
				String c2 = o2.getId();
				return c1.compareTo(c2);
			}
		};
		Collections.sort(return_, comparator);
	}

	public AppsInstance create(String app, String id, String instanceID, String hostname, String ip, String zone,
			Map<String, Object> volumes, String payload, String rack) {
		try {
			Map<String, Object> v = (volumes == null) ? new HashMap<String, Object>() : volumes;
			AppsInstance ins = new AppsInstance();
			ins.setApp(app);
			ins.setZone(zone);
			ins.setRack(rack);
			ins.setHost(hostname);
			ins.setHostIP(ip);
			ins.setId(id);
			ins.setInstanceId(instanceID);
			ins.setDatacenter(config.getDataCenter());
			ins.setToken(payload);
			ins.setVolumes(v);

			dao.createInstanceEntry(ins);
			return ins;
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public void delete(AppsInstance inst) {
		try {
			dao.deleteInstanceEntry(inst);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void update(AppsInstance inst) {
		try {
			dao.createInstanceEntry(inst);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void attachVolumes(AppsInstance instance, String mountPath, String device) {
		throw new UnsupportedOperationException("Volumes not supported");
	}

	@Override
	public AppsInstance getInstance(String appName, String dc, String id) {
		return dao.getInstance(appName, dc, id);
	}
}
