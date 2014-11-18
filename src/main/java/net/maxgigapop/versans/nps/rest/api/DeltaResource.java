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
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
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
    
    /**
     * Retrieves representation of an instance of net.maxgigapop.versans.nps.api.DeltaResource
     * @return an instance of net.maxgigapop.sdnx.services.nps.rest.model.DeltaBase
     */
    @GET
    @Produces({"application/xml", "application/json"})
    @Path("{referenceVersion}/{id}")
    public String query(@PathParam("referenceVersion") String referenceVersion, @PathParam("id") long id) throws NotFoundException {
        DeltaBase delta = NPSGlobalState.getDeltaStore().getByIdWithReferenceVersion(referenceVersion, id);
        if (delta == null)
            throw new NotFoundException(String.format("Unknown Delta id=%d with referenceVersion='%s'", id, referenceVersion));
        return delta.getStatus();
    }

    /**
     * Push a new version of Model
     * @param an model instance
     * @return an HTTP response with a status String.
     */
    @POST
    @Consumes({"application/xml", "application/json"})
    public String push(DeltaBase delta) {
        NPSGlobalState.getDeltaStore().add(delta);
        
        //$$ TODO: execute push logic
        
       return delta.getStatus();
    }

    /**
     * Commit the version of String being pushed
     * @param content representation for the resource
     * @return an HTTP response with a status String.
     */
    @PUT
    @Consumes({"application/xml", "application/json"})
    @Path("{referenceVersion}/{id}")
    public String commit(@PathParam("referenceVersion") String referenceVersion, @PathParam("id") long id) throws NotFoundException {
        DeltaBase delta = NPSGlobalState.getDeltaStore().getByIdWithReferenceVersion(referenceVersion, id);
        if (delta == null)
            throw new NotFoundException(String.format("Unknown Delta id=%d with referenceVersion='%s'", id, referenceVersion));

        //$$ TODO: execute commit logic

        return delta.getStatus();
    }
}
