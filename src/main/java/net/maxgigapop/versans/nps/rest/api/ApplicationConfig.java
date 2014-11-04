/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.rest.api;

import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import net.maxgigapop.versans.nps.manager.NPSContractManager;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
import net.maxgigapop.versans.nps.manager.PolicyManager;
import net.maxgigapop.versans.nps.manager.TopologyManager;

/**
 *
 * @author xyang
 */

@ApplicationPath("/")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method.
     * It is automatically populated with
     * all resources defined in the project.
     * If required, comment out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        this.init();
        resources.add(net.maxgigapop.versans.nps.rest.api.ModelResource.class);
        resources.add(net.maxgigapop.versans.nps.rest.api.DeltaResource.class);
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
}
