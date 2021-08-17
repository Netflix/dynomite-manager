/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.nfsidecar.tokensdb;

import java.util.List;
import java.util.Map;

import com.netflix.nfsidecar.identity.AppsInstance;

/**
 *  Interface for managing Dynomite instance data. Provides functionality
 *  to register, update, delete or list instances from the registry 
 */

public interface IAppsInstanceFactory
{
    /**
     * Return a list of all Dynomite server nodes registered.
     * @param appName the cluster name
     * @return a list of all nodes in {@code appName}
     */
    public List<AppsInstance> getAllIds(String appName);

    
    /**
     * Return a list of Local Dynomite server nodes registered.
     * @param appName the cluster name
     * @param region the the region of the node
     * @return a list of nodes in {@code appName} and same Racks
     */
	public List<AppsInstance> getLocalDCIds(String appName, String region);    
    
    /**
     * Return the Dynomite server node with the given {@code id}.
     * @param appName the cluster name
     * @param id the node id
     * @return the node with the given {@code id}, or {@code null} if none found
     */
    public AppsInstance getInstance(String appName, String dc, int id);

    /**
     * Create/Register an instance of the server with its info.
     * @param app
     * @param id
     * @param instanceID
     * @param hostname
     * @param ip
     * @param zone
     * @param volumes
     * @param token
     * @param rack
     * @return the new node
     */
    public AppsInstance create(String app, int id, String instanceID, String hostname, int dynomitePort, int dynomiteSecurePort, int dynomiteSecureStoragePort, int peerPort, String ip, String zone,
                               Map<String, Object> volumes, String token, String rack);

    /**
     * Delete the server node from the registry
     * @param inst the node to delete
     */
    public void delete(AppsInstance inst);

    /**
     * Update the details of the server node in registry
     * @param inst the node to update
     */
    public void update(AppsInstance inst);

    /**
     * Sort the list by instance ID
     * @param return_ the list of nodes to sort
     */
    public void sort(List<AppsInstance> return_);

    /**
     * Attach volumes if required
     * @param instance
     * @param mountPath
     * @param device
     */
    public void attachVolumes(AppsInstance instance, String mountPath, String device);



}