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
package com.netflix.dynomitemanager.resources;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.netflix.dynomitemanager.InstanceState;
import com.netflix.dynomitemanager.defaultimpl.StorageProcessManager;
import com.netflix.dynomitemanager.dynomite.IDynomiteProcess;
import com.netflix.dynomitemanager.identity.InstanceIdentity;
import com.netflix.dynomitemanager.sidecore.backup.RestoreTask;
import com.netflix.dynomitemanager.sidecore.backup.SnapshotTask;
import com.netflix.dynomitemanager.resources.DynomiteAdmin;
import com.netflix.dynomitemanager.sidecore.storage.IStorageProxy;
import com.netflix.dynomitemanager.sidecore.storage.Bootstrap;

/**
 *  DM's REST end-point 
 */
@Path("/v1/admin")
@Produces(MediaType.APPLICATION_JSON)
public class DynomiteAdmin {

    private static final String REST_HEADER_TOKEN = "token";
    private static final String REST_SUCCESS = "[\"ok\"]";
    private static final Logger logger = LoggerFactory.getLogger(DynomiteAdmin.class);
    private IDynomiteProcess dynoProcess;
    private InstanceIdentity ii;
    private final InstanceState instanceState;
    private SnapshotTask snapshotBackup;
    private RestoreTask restoreBackup;
    private IStorageProxy storage;

    @Inject
    private StorageProcessManager storageProcessMgr;

    @Inject
    public DynomiteAdmin(IDynomiteProcess dynoProcess, InstanceIdentity ii, InstanceState instanceState,
	    SnapshotTask snapshotBackup, RestoreTask restoreBackup, IStorageProxy storage) {
	this.dynoProcess = dynoProcess;
	this.ii = ii;
	this.instanceState = instanceState;
	this.snapshotBackup = snapshotBackup;
	this.restoreBackup = restoreBackup;
	this.storage = storage;
    }

    @GET
    @Path("/start")
    public Response dynoStart() throws IOException, InterruptedException, JSONException {
	instanceState.setIsProcessMonitoringSuspended(false);
	// Let the ProcessMonitorTask take over the job of starting the process
	// correctly.
	dynoProcess.start();
	return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/stop")
    public Response dynoStop() throws IOException, InterruptedException, JSONException {
	instanceState.setIsProcessMonitoringSuspended(true);
	dynoProcess.stop();
	return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/startstorageprocess")
    public Response storageProcessStart() throws IOException, InterruptedException, JSONException {
	storageProcessMgr.start();
	return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/stopstorageprocess")
    public Response storageProcessStop() throws IOException, InterruptedException, JSONException {
	storageProcessMgr.stop();
	return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/info")
    public Response dynoInfo() throws IOException, InterruptedException, JSONException {
	logger.info("REST interface for INFO - not implemented");
	// NodeProbe probe = new NodeProbe();
	// return Response.ok(probe.info(), MediaType.APPLICATION_JSON).build();
	return null;
    }

    @GET
    @Path("/ring/{id}")
    public Response dynoRing(@PathParam("id") String keyspace) throws IOException, InterruptedException, JSONException {
	logger.info("REST interface for RING - not implemented");
	// NodeProbe probe = new NodeProbe();
	// logger.debug("node tool ring being called");
	// return Response.ok(probe.ring(keyspace),
	// MediaType.APPLICATION_JSON).build();
	return null;
    }

    @GET
    @Path("/repair")
    public Response dynoRepair(@QueryParam("sequential") boolean isSequential,
	    @QueryParam("localDC") boolean localDCOnly,
	    @DefaultValue("false") @QueryParam("primaryRange") boolean primaryRange)
	    throws IOException, ExecutionException, InterruptedException {
	return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/version")
    public Response version() throws IOException, ExecutionException, InterruptedException {
	logger.info("REST: version");
	return Response.ok(new JSONArray().put("1.0.0"), MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/move")
    public Response moveToken(@QueryParam(REST_HEADER_TOKEN) String newToken)
	    throws IOException, ExecutionException, InterruptedException, ConfigurationException {
	return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/drain")
    public Response dynoDrain() throws IOException, ExecutionException, InterruptedException {
	return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/get_seeds")
    public Response getSeeds() {
	try {
	    final List<String> seeds = ii.getSeeds();
	    if (!seeds.isEmpty())
		return Response.ok(StringUtils.join(seeds, '|')).build();
	    logger.error("Cannot find the Seeds");
	} catch (Exception e) {
	    logger.error("Error while executing get_seeds", e);
	    return Response.serverError().build();
	}
	return Response.status(500).build();
    }

    @GET
    @Path("/cluster_describe")
    public Response getClusterDescribe() {
	try {
	    final List<String> nodes = ii.getClusterInfo();
	    if (!nodes.isEmpty())
		return Response.ok("[" + StringUtils.join(nodes, ',') + "]").build();
	    logger.error("Cannot find the nodes");
	} catch (Exception e) {
	    logger.error("Error while executing cluster_describe", e);
	    return Response.serverError().build();
	}
	return Response.status(500).build();
    }

    @GET
    @Path("/backup")
    public Response doBackup() {
	try {
	    logger.info("REST call: backups");
	    this.snapshotBackup.execute();
	    return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
	} catch (Exception e) {
	    logger.error("Error while executing backups from REST call", e);
	    return Response.serverError().build();
	}
    }

    @GET
    @Path("/restore")
    public Response doRestore() {
	try {
	    logger.info("REST call: restore");
	    this.restoreBackup.execute();
	    return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
	} catch (Exception e) {
	    logger.error("Error while executing restores from REST call", e);
	    return Response.serverError().build();
	}
    }

    @GET
    @Path("/takesnapshot")
    public Response takeSnapshot() {
	try {
	    logger.info("REST call: Persisting Data to Disk");
	    this.storage.takeSnapshot();
	    return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
	} catch (Exception e) {
	    logger.error("Error executing data persistence from REST call", e);
	    return Response.serverError().build();
	}
    }

    @GET
    @Path("/Status")
    public Response floridaStatus() {
	try {
	    JSONObject statusJson = new JSONObject();

	    /* Warm up status */
	    JSONObject warmupJson = new JSONObject();
	    if (!this.instanceState.firstBootstrap()) {
		if (this.instanceState.isBootstrapping()) {
		    warmupJson.put("status", "pending");
		} else {
		    Bootstrap bootstrap = this.instanceState.isBootstrapStatus();
		    switch (bootstrap) {
		    case CANNOT_CONNECT_FAIL:
			warmupJson.put("status", "failed: cannot connect");
			break;
		    case WARMUP_ERROR_FAIL:
			warmupJson.put("status", "failed: error in warmup");
			break;
		    case RETRIES_FAIL:
			warmupJson.put("status", "failed: too fast to warmup - retries");
			break;
		    case EXPIRED_BOOTSTRAPTIME_FAIL:
			warmupJson.put("status", "failed: too fast to warmup - expired bootstrap time");
			break;
		    case IN_SYNC_SUCCESS:
			warmupJson.put("status", "completed");
			break;
		    default:
			warmupJson.put("status", "unknown");
			break;
		    }
		}
		warmupJson.put("time", this.instanceState.getBootstrapTime());
	    } else {
		warmupJson.put("status", "not started");
	    }
	    statusJson.put("warmup", warmupJson);

	    /* backup status */
	    JSONObject backupJson = new JSONObject();
	    if (!this.instanceState.firstBackup()) {
		if (this.instanceState.isBackingup()) {
		    backupJson.put("status", "pending");
		} else if (!this.instanceState.isBackingup() && !this.instanceState.isBackupSuccessful()) {
		    backupJson.put("status", "unsuccessful");
		} else if (!this.instanceState.isBackingup() && this.instanceState.isBackupSuccessful()) {
		    backupJson.put("status", "completed");
		}
		backupJson.put("time", this.instanceState.getBackupTime());
	    } else {
		backupJson.put("status", "not started");
	    }
	    statusJson.put("backup", backupJson);

	    /* restore status */
	    JSONObject restoreJson = new JSONObject();
	    if (!this.instanceState.firstRestore()) {
		if (this.instanceState.isRestoring()) {
		    restoreJson.put("status", "pending");
		} else if (!this.instanceState.isRestoring() && !this.instanceState.isRestoreSuccessful()) {
		    restoreJson.put("status", "unsuccessful");
		} else if (!this.instanceState.isRestoring() && this.instanceState.isRestoreSuccessful()) {
		    restoreJson.put("status", "completed");
		}
		restoreJson.put("time", this.instanceState.getRestoreTime());

	    } else {
		restoreJson.put("status", "not started");
	    }
	    statusJson.put("restore", restoreJson);

	    /* Dynomite status */
	    statusJson.put("dynomiteAlive", this.instanceState.isStorageProxyProcessAlive() ? true : false);

	    /* Redis status */
	    statusJson.put("storageAlive", this.instanceState.isStorageAlive() ? true : false);

	    /* Overall status */
	    statusJson.put("healthy", this.instanceState.isHealthy() ? true : false);

	    /* My token */
	    statusJson.put("tokens", this.ii.getTokens());

	    logger.info("REST call: Florida Status");
	    return Response.ok(statusJson, MediaType.APPLICATION_JSON).build();

	} catch (Exception e) {
	    logger.error("Error requesting Florida status from REST call", e);
	    return Response.serverError().build();
	}

    }

}
