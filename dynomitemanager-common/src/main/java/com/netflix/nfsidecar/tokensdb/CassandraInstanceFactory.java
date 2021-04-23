package com.netflix.nfsidecar.tokensdb;

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
import com.netflix.nfsidecar.config.CommonConfig;
import com.netflix.nfsidecar.identity.AppsInstance;
import com.netflix.nfsidecar.resources.env.IEnvVariables;

/**
 * Factory to use Cassandra for managing instance data 
 */

@Singleton
public class CassandraInstanceFactory implements IAppsInstanceFactory
{
    private static final Logger logger = LoggerFactory.getLogger(CassandraInstanceFactory.class);

    CommonConfig config;
    InstanceDataDAOCassandra dao;
    IEnvVariables envVariables;

    @Inject
    public CassandraInstanceFactory(CommonConfig config, InstanceDataDAOCassandra dao, IEnvVariables envVariables) {
    	this.config = config;
    	this.dao = dao;
    	this.envVariables = envVariables;
    }
    
    public List<AppsInstance> getAllIds(String appName)
    {
        List<AppsInstance> return_ = new ArrayList<AppsInstance>();
        for (AppsInstance instance : dao.getAllInstances(appName)) {
            return_.add(instance);
        }

        sort(return_);
        return return_;
    }
    
    public List<AppsInstance> getLocalDCIds(String appName, String region)
    {
        List<AppsInstance> return_ = new ArrayList<AppsInstance>();
        for (AppsInstance instance : dao.getLocalDCInstances(appName, region)) {
            return_.add(instance);
        }

        sort(return_);
        return return_;
    }

    public void sort(List<AppsInstance> return_)
    {
        Comparator<? super AppsInstance> comparator = new Comparator<AppsInstance>()
        {

            @Override
            public int compare(AppsInstance o1, AppsInstance o2)
            {
                Integer c1 = o1.getId();
                Integer c2 = o2.getId();
                return c1.compareTo(c2);
            }
        };
        Collections.sort(return_, comparator);
    }

    public AppsInstance create(String app, int id, String instanceID, String hostname, int dynomitePort, int dynomiteSecurePort, int dynomiteSecureStoragePort, int peerPort, String ip, String zone, Map<String, Object> volumes, String payload, String rack)
    {
        try {
            Map<String, Object> v = (volumes == null) ? new HashMap<String, Object>() : volumes;
            AppsInstance ins = new AppsInstance();
            ins.setApp(app);
            ins.setZone(zone);
            ins.setRack(rack);
            ins.setHost(hostname);
            ins.setDynomitePort(dynomitePort);
            ins.setDynomiteSecurePort(dynomiteSecurePort);
            ins.setDynomiteSecureStoragePort(dynomiteSecureStoragePort);
            ins.setPeerPort(peerPort);
            ins.setHostIP(ip);
            ins.setId(id);
            ins.setInstanceId(instanceID);
            ins.setDatacenter(envVariables.getRegion());
            ins.setToken(payload);
            ins.setVolumes(v);

            // remove old data node which are dead.
            //if (app.endsWith("-dead")) {
            //       AppsInstance oldData = dao.getInstance(app, ins.getRack(), id);
                   // clean up a very old data...
                   //if (null != oldData)
                   //     dao.deleteInstanceEntry(oldData);
            //}
            dao.createInstanceEntry(ins);
            return ins;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void delete(AppsInstance inst)
    {
        try {
            dao.deleteInstanceEntry(inst);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void update(AppsInstance inst)
    {
        try {
            dao.updateInstanceEntry(inst);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void attachVolumes(AppsInstance instance, String mountPath, String device)
    {
        throw new UnsupportedOperationException("Volumes not supported");
    }

    @Override
    public AppsInstance getInstance(String appName, String dc, int id)
    {
        return dao.getInstance(appName, dc, id);
    }
}
