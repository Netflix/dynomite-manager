package com.netflix.dynomitemanager.resources;

import java.io.IOException;
import java.util.LinkedList;
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

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.netflix.dynomitemanager.backup.RestoreTask;
import com.netflix.dynomitemanager.backup.SnapshotTask;
import com.netflix.dynomitemanager.config.FloridaConfig;
import com.netflix.dynomitemanager.config.InstanceState;
import com.netflix.dynomitemanager.dynomite.IDynomiteProcess;
import com.netflix.dynomitemanager.storage.Bootstrap;
import com.netflix.dynomitemanager.storage.StorageProcessManager;
import com.netflix.dynomitemanager.storage.StorageProxy;
import com.netflix.nfsidecar.identity.AppsInstance;
import com.netflix.nfsidecar.identity.InstanceIdentity;

@Path("/v1/admin")
@Produces(MediaType.APPLICATION_JSON)
public class DynomiteAdmin {

	private static final String REST_SUCCESS = "[\"ok\"]";
	private static final Logger logger = LoggerFactory.getLogger(DynomiteAdmin.class);
	private IDynomiteProcess dynoProcess;
	private InstanceIdentity ii;
	private InstanceState instanceState;
	private SnapshotTask snapshotBackup;
	private RestoreTask restoreBackup;
	private StorageProxy storage;
	private StorageProcessManager storageProcessMgr;
	private FloridaConfig config;

	@Inject
	public DynomiteAdmin(FloridaConfig config, IDynomiteProcess dynoProcess, InstanceIdentity ii,
			InstanceState instanceState, SnapshotTask snapshotBackup, RestoreTask restoreBackup, StorageProxy storage,
			StorageProcessManager storageProcessMgr) {
		this.config = config;
		this.dynoProcess = dynoProcess;
		this.ii = ii;
		this.instanceState = instanceState;
		this.snapshotBackup = snapshotBackup;
		this.restoreBackup = restoreBackup;
		this.storage = storage;
		this.storageProcessMgr = storageProcessMgr;

	}

	@GET
	@Path("/{start : (?i)start}")
	public Response dynoStart() throws IOException, InterruptedException, JSONException {
		logger.info("REST call: Starting Dynomite");

		instanceState.setIsProcessMonitoringSuspended(false);
		// Let the ProcessMonitorTask take over the job of starting the process
		// correctly.
		dynoProcess.start();
		return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/{stop : (?i)stop}")
	public Response dynoStop() throws IOException, InterruptedException, JSONException {
		logger.info("REST call: Stopping Dynomite");

		instanceState.setIsProcessMonitoringSuspended(true);
		dynoProcess.stop();
		return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/{startstorageprocess : (?i)startstorageprocess}")
	public Response storageProcessStart() throws IOException, InterruptedException, JSONException {
		storageProcessMgr.start();
		return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/{stopstorageprocess : (?i)stopstorageprocess}")
	public Response storageProcessStop() throws IOException, InterruptedException, JSONException {
		storageProcessMgr.stop();
		return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/{info : (?i)info}")
	public Response dynoInfo() throws IOException, InterruptedException, JSONException {
		logger.info("REST interface for INFO - not implemented");
		// NodeProbe probe = new NodeProbe();
		// return Response.ok(probe.info(), MediaType.APPLICATION_JSON).build();
		return null;
	}

	@GET
	@Path("/{ring : (?i)ring}/{id}")
	public Response dynoRing(@PathParam("id") String keyspace) throws IOException, InterruptedException, JSONException {
		logger.info("REST interface for RING - not implemented");
		// NodeProbe probe = new NodeProbe();
		// logger.debug("node tool ring being called");
		// return Response.ok(probe.ring(keyspace),
		// MediaType.APPLICATION_JSON).build();
		return null;
	}

	@GET
	@Path("/{repair : (?i)repair}")
	public Response dynoRepair(@QueryParam("sequential") boolean isSequential,
			@QueryParam("localDC") boolean localDCOnly,
			@DefaultValue("false") @QueryParam("primaryRange") boolean primaryRange)
			throws IOException, ExecutionException, InterruptedException {
		return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/{version : (?i)version}")
	public Response version() throws IOException, ExecutionException, InterruptedException {
		logger.info("REST: version");
		return Response.ok(new JSONArray().put("1.0.0"), MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/{drain : (?i)drain}")
	public Response dynoDrain() throws IOException, ExecutionException, InterruptedException {
		logger.info("REST interface for DRAIN - not implemented");

		return Response.ok(REST_SUCCESS, MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/{get_seeds : (?i)get_seeds}")
	public Response getSeeds() {
		try {
			final List<String> seeds = ii.getSeeds();
			return Response.ok(StringUtils.join(seeds, '|')).build();
		} catch (Exception e) {
			logger.error("Error while executing get_seeds", e);
			return Response.serverError().build();
		}
	}

	@GET
	@Path("/{cluster_describe : (?i)cluster_describe}")
	public Response getClusterDescribe() {
		try {
			List<String> nodes = new LinkedList<String>();

			for (AppsInstance ins : ii.getClusterInfo()) {
				logger.debug("Adding node: " + ins.getInstanceId());
				String node = "\"token\":" + "\"" + ins.getToken() + "\","
						+ "\"hostname\":" + "\"" + ins.getHostName() + "\","
						+ "\"port\":" + "\"" + Integer.toString(ins.getDynomitePort()) + "\","
						+ "\"secure_port\":" + "\"" + Integer.toString(ins.getDynomiteSecurePort()) + "\","
						+ "\"secure_storage_port\":" + "\"" + Integer.toString(ins.getDynomiteSecureStoragePort()) + "\","
						+ "\"peer_port\":" + "\"" + Integer.toString(ins.getPeerPort()) + "\","
						+ "\"rack\":" + "\"" + ins.getRack() + "\","
						+ "\"ip\":" + "\"" + ins.getHostIP() + "\","
						+ "\"zone\":" + "\"" + ins.getZone() + "\","
						+ "\"dc\":" + "\"" + ins.getDatacenter() + "\"";

				if (config.getDynomiteHashtag().isEmpty()) {
					nodes.add("{" + node + "}");
				} else {
					nodes.add("{" + node + ",\"hashtag\":" + "\"" + config.getDynomiteHashtag() + "\"" + "}");
				}
			}

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
	@Path("/{backup : (?i)backup}")
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
	@Path("/{restore : (?i)restore}")
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
	@Path("/{takesnapshot : (?i)takesnapshot}")
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
	@Path("/{consistency : (?i)consistency}")
	public Response getConsistency() {
		try {
			logger.info("REST call: Get Consistency");
			JSONObject consistencyJson = new JSONObject();
			consistencyJson.put("read_consistency", config.getDynomiteReadConsistency());
			consistencyJson.put("write_consistency", config.getDynomiteWriteConsistency());
			return Response.ok(consistencyJson, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			logger.error("Error getting consistency from REST call", e);
			return Response.serverError().build();
		}
	}

	@GET
	@Path("/{hashtag : (?i)hashtag}")
	public Response getHashtag() {
		try {
			JSONObject hashtagJson = new JSONObject();
			if (!config.getDynomiteHashtag().isEmpty()) {
				hashtagJson.put("hashtag", config.getDynomiteHashtag());
			} else {
				hashtagJson.put("hashtag", "none");
			}
			return Response.ok(hashtagJson, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			logger.error("Error getting the hashtag from REST call", e);
			return Response.serverError().build();
		}

	}

	@GET
	@Path("/{status : (?i)status}")
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

			/* Dynomite Consistency */
			JSONObject consistencyJson = new JSONObject();
			consistencyJson.put("read", config.getDynomiteReadConsistency());
			consistencyJson.put("write", config.getDynomiteWriteConsistency());
			statusJson.put("consistency", consistencyJson);

			JSONObject healthJson = new JSONObject();
			/* Dynomite status */
			healthJson.put("dynomiteAlive", this.instanceState.isStorageProxyProcessAlive() ? true : false);

			/* Redis status */
			healthJson.put("storageAlive", this.instanceState.isStorageAlive() ? true : false);

			/* Overall status */
			healthJson.put("Overall", this.instanceState.isHealthy() ? true : false);
			statusJson.put("health", healthJson);

			/* My token */
			statusJson.put("tokens", this.ii.getTokens());

			/* Storage Engine */
			statusJson.put("storage engine", config.getRedisCompatibleEngine());

			if (config.getConnectionPoolEnabled()) {
				JSONObject connectionsJson = new JSONObject();
				connectionsJson.put("Storage", config.getDatastoreConnections());
				connectionsJson.put("Local Peer", config.getLocalPeerConnections());
				connectionsJson.put("Remote Peer", config.getRemotePeerConnections());
				statusJson.put("Number Connections", connectionsJson);
			}

			/* Hashtag */
			if (!config.getDynomiteHashtag().isEmpty()) {
				statusJson.put("hashtag", config.getDynomiteHashtag());
			}

			logger.info("REST call: Florida Status");
			return Response.ok(statusJson, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			logger.error("Error requesting Florida status from REST call", e);
			return Response.serverError().build();
		}
	}

}
