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
package com.netflix.dynomitemanager.identity;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.sidecore.utils.ITokenManager;
import com.netflix.dynomitemanager.sidecore.utils.RetryableCallable;
import com.netflix.dynomitemanager.sidecore.utils.Sleeper;
import com.netflix.dynomitemanager.defaultimpl.IConfiguration;
import com.netflix.dynomitemanager.identity.AppsInstance;
import com.netflix.dynomitemanager.identity.IAppsInstanceFactory;
import com.netflix.dynomitemanager.identity.IMembership;
import com.netflix.dynomitemanager.identity.InstanceEnvIdentity;
import com.netflix.dynomitemanager.identity.InstanceIdentity;

/**
 * This class provides the central place to create and consume the identity of
 * the instance - token, seeds etc.
 *
 */

@Singleton
public class InstanceIdentity {
    private static final Logger logger = LoggerFactory.getLogger(InstanceIdentity.class);
    private static final String DUMMY_INSTANCE_ID = "new_slot";

    private final ListMultimap<String, AppsInstance> locMap = Multimaps
	    .newListMultimap(new HashMap<String, Collection<AppsInstance>>(), new Supplier<List<AppsInstance>>() {
		public List<AppsInstance> get() {
		    return Lists.newArrayList();
		}
	    });
    private final IAppsInstanceFactory factory;
    private final IMembership membership;
    private final IConfiguration config;
    private final Sleeper sleeper;
    private final ITokenManager tokenManager;
    private final InstanceEnvIdentity insEnvIdentity;

    private final Predicate<AppsInstance> differentHostPredicate = new Predicate<AppsInstance>() {
	@Override
	public boolean apply(AppsInstance instance) {
	    return (!instance.getInstanceId().equalsIgnoreCase(DUMMY_INSTANCE_ID)
		    && !instance.getHostName().equals(myInstance.getHostName()));
	}
    };

    private AppsInstance myInstance;
    private boolean isReplace = false;
    private boolean isTokenPregenerated = false;
    private String replacedIp = "";

    @Inject
    public InstanceIdentity(IAppsInstanceFactory factory, IMembership membership, IConfiguration config,
	    Sleeper sleeper, ITokenManager tokenManager, InstanceEnvIdentity insEnvIdentity) throws Exception {
	this.factory = factory;
	this.membership = membership;
	this.config = config;
	this.sleeper = sleeper;
	this.tokenManager = tokenManager;
	this.insEnvIdentity = insEnvIdentity;
	init();
    }

    public AppsInstance getInstance() {
	return myInstance;
    }

    public void init() throws Exception {
	// try to grab the token which was already assigned
	myInstance = new RetryableCallable<AppsInstance>() {
	    @Override
	    public AppsInstance retriableCall() throws Exception {
		// Check if this node is decommissioned
		for (AppsInstance ins : factory.getAllIds(config.getDynomiteClusterName() + "-dead")) {
		    logger.debug(String.format("[Dead] Iterating though the hosts: %s", ins.getInstanceId()));
		    if (ins.getInstanceId().equals(config.getInstanceName())) {
			ins.setOutOfService(true);
			return ins;
		    }
		}
		for (AppsInstance ins : factory.getAllIds(config.getDynomiteClusterName())) {
		    logger.debug(String.format("[Alive] Iterating though the hosts: %s My id = [%s]",
			    ins.getInstanceId(), ins.getId()));
		    if (ins.getInstanceId().equals(config.getInstanceName()))
			return ins;
		}
		return null;
	    }
	}.call();
	// Grab a dead token
	if (null == myInstance)
	    myInstance = new GetDeadToken().call();

	// Grab a pre-generated token if there is such one
	if (null == myInstance)
	    myInstance = new GetPregeneratedToken().call();

	// Grab a new token
	if (null == myInstance) {
	    GetNewToken newToken = new GetNewToken();
	    newToken.set(100, 100);
	    myInstance = newToken.call();
	}
	logger.info("My token: " + myInstance.getToken());

    }

    private void populateRacMap() {
	locMap.clear();
	for (AppsInstance ins : factory.getAllIds(config.getDynomiteClusterName())) {
	    locMap.put(ins.getZone(), ins);
	}
    }

    private List<String> getDualAccountRacMembership(List<String> asgInstances) {
	logger.info("Dual Account cluster");

	List<String> crossAccountAsgInstances = membership.getCrossAccountRacMembership();

	if (insEnvIdentity.isClassic()) {
	    logger.info("EC2 classic instances (local ASG): " + Arrays.toString(asgInstances.toArray()));
	    logger.info("VPC Account (cross-account ASG): " + Arrays.toString(crossAccountAsgInstances.toArray()));
	} else {
	    logger.info("VPC Account (local ASG): " + Arrays.toString(asgInstances.toArray()));
	    logger.info("EC2 classic instances (cross-account ASG): "
		    + Arrays.toString(crossAccountAsgInstances.toArray()));
	}

	// Remove duplicates (probably there are not)
	asgInstances.removeAll(crossAccountAsgInstances);

	// Merge the two lists
	asgInstances.addAll(crossAccountAsgInstances);
	logger.info("Combined Instances in the AZ: " + asgInstances);

	return asgInstances;
    }

    public class GetDeadToken extends RetryableCallable<AppsInstance> {
	@Override
	public AppsInstance retriableCall() throws Exception {
	    final List<AppsInstance> allIds = factory.getAllIds(config.getDynomiteClusterName());
	    List<String> asgInstances = membership.getRacMembership();
	    if (config.isDualAccount()) {
		asgInstances = getDualAccountRacMembership(asgInstances);
	    } else {
		logger.info("Single Account cluster");
	    }

	    // Sleep random interval - upto 15 sec
	    sleeper.sleep(new Random().nextInt(5000) + 10000);
	    for (AppsInstance dead : allIds) {
		// test same dc and is it is alive.
		if (!dead.getRack().equals(config.getRack()) || asgInstances.contains(dead.getInstanceId()))
		    continue;
		logger.info("Found dead instances: " + dead.getInstanceId());
		// AppsInstance markAsDead = factory.create(dead.getApp() +
		// "-dead", dead.getId(), dead.getInstanceId(),
		// dead.getHostName(), dead.getHostIP(), dead.getZone(),
		// dead.getVolumes(), dead.getToken(), dead.getRack());
		// remove it as we marked it down...
		// factory.delete(dead);
		isReplace = true;
		replacedIp = dead.getHostIP();
		String payLoad = dead.getToken();
		logger.info("Trying to grab slot {} with availability zone {}", dead.getId(), dead.getZone());
		return factory.create(config.getDynomiteClusterName(), dead.getId(), config.getInstanceName(), config.getHostname(),
			config.getHostIP(), config.getZone(), dead.getVolumes(), payLoad, config.getRack());
	    }
	    return null;
	}

	public void forEachExecution() {
	    populateRacMap();
	}
    }

    public class GetPregeneratedToken extends RetryableCallable<AppsInstance> {
	@Override
	public AppsInstance retriableCall() throws Exception {
	    logger.info("Looking for any pre-generated token");
	    final List<AppsInstance> allIds = factory.getAllIds(config.getDynomiteClusterName());
	    List<String> asgInstances = membership.getRacMembership();
	    // Sleep random interval - upto 15 sec
	    sleeper.sleep(new Random().nextInt(5000) + 10000);
	    for (AppsInstance dead : allIds) {
		// test same zone and is it is alive.
		if (!dead.getRack().equals(config.getRack()) || asgInstances.contains(dead.getInstanceId())
			|| !isInstanceDummy(dead))
		    continue;
		logger.info("Found pre-generated token: " + dead.getToken());
		// AppsInstance markAsDead = factory.create(dead.getApp() +
		// "-dead", dead.getId(), dead.getInstanceId(),
		// dead.getHostName(), dead.getHostIP(), dead.getRack(),
		// dead.getVolumes(),
		// dead.getToken());
		// remove it as we marked it down...
		factory.delete(dead);
		isTokenPregenerated = true;

		String payLoad = dead.getToken();
		logger.info("Trying to grab slot {} with availability zone {}", dead.getId(), dead.getRack());
		return factory.create(config.getDynomiteClusterName(), dead.getId(), config.getInstanceName(), config.getHostname(),
			config.getHostIP(), config.getZone(), dead.getVolumes(), payLoad, config.getRack());
	    }
	    return null;
	}

	public void forEachExecution() {
	    populateRacMap();
	}
    }

    public class GetNewToken extends RetryableCallable<AppsInstance> {

	public AppsInstance retriableCall() throws Exception {
	    // Sleep random interval - upto 15 sec
	    sleeper.sleep(new Random().nextInt(15000));
	    int hash = tokenManager.regionOffset(config.getRack());
	    // use this hash so that the nodes are spred far away from the other
	    // regions.
	    String myInstanceId = config.getInstanceName();
	    List<String> asgInstanceIds = membership.getRacMembership();

	    logger.info("My Instance Id: " + myInstanceId);

	    for (String instanceId : asgInstanceIds) {
		logger.info("InstanceId in ASG: " + instanceId);
	    }

	    int my_slot = asgInstanceIds.indexOf(myInstanceId);
	    logger.info("my_slot ::: " + my_slot);

            int rackMembershipSize;
            if (config.isDualAccount()){
        	rackMembershipSize = membership.getRacMembershipSize() + membership.getCrossAccountRacMembershipSize();
            }
            else {
        	rackMembershipSize = membership.getRacMembershipSize();
            }

	    logger.info(String.format(
		    "Trying to createToken with slot %d with rac count %d with rac membership size %d with dc %s",
		    my_slot, membership.getRacCount(), rackMembershipSize, config.getDataCenter()));
	    // String payload = tokenManager.createToken(my_slot,
	    // membership.getRacCount(), membership.getRacMembershipSize(),
	    // config.getDataCenter());
	    String payload = tokenManager.createToken(my_slot, rackMembershipSize, config.getRack());
	    return factory.create(config.getDynomiteClusterName(), my_slot + hash, config.getInstanceName(), config.getHostname(),
		    config.getHostIP(), config.getZone(), null, payload, config.getRack());
	}

	public void forEachExecution() {
	    populateRacMap();
	}
    }

    /*
     * public List<String> getSeeds1() throws UnknownHostException {
     * populateRacMap(); List<String> seeds = new LinkedList<String>(); //
     * Handle single zone deployment if (config.getRacs().size() == 1) { //
     * Return empty list if all nodes are not up if
     * (membership.getRacMembershipSize() !=
     * locMap.get(myInstance.getRac()).size()) return seeds; // If seed node,
     * return the next node in the list //if
     * (locMap.get(myInstance.getRac()).size() > 1 &&
     * locMap.get(myInstance.getRac()).get(0).getHostIP().equals(myInstance.
     * getHostIP())) //{
     * //seeds.add(locMap.get(myInstance.getRac()).get(1).getHostName());
     * //seedp.add(seed + ":" + config.getDynomitePeerPort() + ":" +
     * config.getDataCenter() + ":5622637");
     * seeds.add(locMap.get(myInstance.getRac()).get(1).getHostName() + ":" +
     * config.getDynomitePeerPort() + ":" + config.getDataCenter() + ":" +
     * locMap.get(myInstance.getRac()).get(1).getToken()); //} } for (String loc
     * : locMap.keySet()) { AppsInstance instance =
     * Iterables.tryFind(locMap.get(loc), differentHostPredicate).orNull(); if
     * (instance != null) { //seeds.add(instance.getHostName());
     * seeds.add(instance.getHostName() + ":" + config.getDynomitePeerPort() +
     * ":" + config.getDataCenter() + ":" + instance.getToken()); } } return
     * seeds; }
     */

    public List<String> getSeeds() throws UnknownHostException {
	// populateRacMap();
	List<String> seeds = new LinkedList<String>();

	for (AppsInstance ins : factory.getAllIds(config.getDynomiteClusterName())) {
	    if (!ins.getInstanceId().equals(myInstance.getInstanceId())) {
		logger.debug("Adding node: " + ins.getInstanceId());
		seeds.add(ins.getHostName() + ":" + config.getDynomitePeerPort() + ":" + ins.getRack() + ":"
			+ ins.getDatacenter() + ":" + ins.getToken());
	    }
	}

	return seeds;
    }

    public List<String> getClusterInfo() throws UnknownHostException {
	List<String> nodes = new LinkedList<String>();

	for (AppsInstance ins : factory.getAllIds(config.getDynomiteClusterName())) {
	    logger.debug("Adding node: " + ins.getInstanceId());
	    nodes.add("{" + "\"token\":" + "\"" + ins.getToken() + "\"," + "\"hostname\":" + "\"" + ins.getHostName()
		    + "\"," + "\"rack\":" + "\"" + ins.getRack() + "\"," + "\"ip\":" + "\"" + ins.getHostIP() + "\","
		    + "\"zone\":" + "\"" + ins.getZone() + "\"," + "\"dc\":" + "\"" + ins.getDatacenter() + "\"" + "}");

	}

	return nodes;
    }

    public boolean isSeed() {
	populateRacMap();
	String ip = locMap.get(myInstance.getZone()).get(0).getHostName();
	return myInstance.getHostName().equals(ip);
    }

    public boolean isReplace() {
	return isReplace;
    }

    public boolean isTokenPregenerated() {
	return isTokenPregenerated;
    }

    public String getReplacedIp() {
	return replacedIp;
    }

    public String getTokens() {
	return myInstance.getToken();
    }

    private boolean isInstanceDummy(AppsInstance instance) {
	return instance.getInstanceId().equals(DUMMY_INSTANCE_ID);
    }
}
