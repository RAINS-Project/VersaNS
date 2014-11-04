/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.rest.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import net.maxgigapop.versans.nps.api.ServiceException;
import net.maxgigapop.versans.nps.manager.NPSContractManager;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
import net.maxgigapop.versans.nps.manager.PolicyManager;
import net.maxgigapop.versans.nps.manager.TopologyManager;
import net.maxgigapop.versans.nps.rest.model.ModelBase;

/**
 * REST Web Service
 *
 * @author xyang
 */

@Path("model")
public class ModelResource {

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of ModelResource
     */
    
    public ModelResource() {
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
     * Retrieves representation of an instance of net.maxgigapop.sdnx.services.nps.rest.api.ModelResource
     * @return an instance of net.maxgigapop.sdnx.services.nps.rest.model.ModelBase
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public ModelBase getXml() {
        //TODO return proper representation object
        ModelBase model = new ModelBase();
        try {
            NPSGlobalState.getContractManager().handleQuery("test-1");
        } catch (ServiceException ex) {
            Logger.getLogger(ModelResource.class.getName()).log(Level.SEVERE, null, ex);
            model.setTtlModel(ex.getMessage());
        }
        return model;
    }

    /**
     * PUT method for updating or creating an instance of ModelResource
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes({"application/xml", "application/json"})
    public void putXml(ModelBase model) {
    }
}
