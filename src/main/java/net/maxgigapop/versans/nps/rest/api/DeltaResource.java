/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.rest.api;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import net.maxgigapop.versans.nps.manager.NPSContractManager;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
import net.maxgigapop.versans.nps.manager.PolicyManager;
import net.maxgigapop.versans.nps.manager.TopologyManager;
import net.maxgigapop.versans.nps.rest.model.DeltaBase;

/**
 * REST Web Service
 *
 * @author xyang
 */
@Path("delta")
public class DeltaResource {

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of DeltaResource
     */
    public DeltaResource() {
    }

    @PostConstruct 
    public void init()  {
    	if (NPSGlobalState.Inited)
    		return;
        try {
            //init global status
            NPSGlobalState.init();
        } catch(Exception ex) {
            ex.printStackTrace();
            return;
        }
        //init contract manager
        NPSContractManager contractManager = new NPSContractManager();
        contractManager.start();
        NPSGlobalState.setContractManager(contractManager);
        //init topology manager 
        TopologyManager topologyManager = new TopologyManager();
        NPSGlobalState.setTopologyManager(topologyManager);
        try {
            topologyManager.initNetworkTopology();
        } catch(Exception ex) {
            ex.printStackTrace();
            return;
        }
        //init policy manager
        PolicyManager policyManager = new PolicyManager();
        NPSGlobalState.setPolicyManager(policyManager);
    }
    
    /**
     * Retrieves representation of an instance of net.maxgigapop.versans.nps.api.DeltaResource
     * @return an instance of net.maxgigapop.sdnx.services.nps.rest.model.DeltaBase
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public DeltaBase getXml() {
        DeltaBase delta = new DeltaBase();
        return delta;
    }

    /**
     * PUT method for updating or creating an instance of DeltaResource
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes({"application/xml", "application/json"})
    public void putXml(DeltaBase content) {
    }
}
