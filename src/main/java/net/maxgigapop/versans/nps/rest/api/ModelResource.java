/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.rest.api;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import net.maxgigapop.versans.nps.api.ServiceException;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
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
    
    /**
     * Retrieves last version of Model
     * @return an instance of net.maxgigapop.versans.nps.rest.model.ModelBase
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public ModelBase pull() {
        ModelBase model = NPSGlobalState.getModelStore().getHead();
        if (model == null)
            throw new NotFoundException("None!");     
        return model;
    }

    /**
     * Push a new version of Model
     * @param an model instance
     * @return an HTTP response with a status String.
     */
    @POST
    @Consumes({"application/xml", "application/json"})
    public String push(ModelBase model) {
        throw new UnsupportedOperationException();
    }

    /**
     * Commit the version of String being pushed
     * @param content representation for the resource
     * @return an HTTP response with a status String.
     */
    @PUT
    @Consumes({"application/xml", "application/json"})
    @Path("{version}")
    public String commit(@PathParam("version") String version) {
        throw new UnsupportedOperationException();
    }
}
