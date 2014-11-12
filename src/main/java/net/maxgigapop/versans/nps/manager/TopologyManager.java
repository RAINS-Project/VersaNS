/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.manager;

import net.maxgigapop.versans.nps.device.Device;
import net.maxgigapop.versans.nps.device.NetworkDeviceInstance;
import net.maxgigapop.versans.nps.device.NetworkDeviceFactory;
import net.maxgigapop.versans.nps.device.Interface;
import net.maxgigapop.versans.nps.config.NPSGlobalConfig;
import net.maxgigapop.versans.nps.config.ConfigException;
import net.maxgigapop.versans.nps.config.NPSConfigYaml;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.api.ServiceException;
import java.util.*;
import org.apache.log4j.*;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.StringWriter;
import net.maxgigapop.versans.nps.rest.model.*;

/**
 *
 * @author xyang
 */ 
public class TopologyManager extends Thread {
    private org.apache.log4j.Logger log;
    private Date lastestModelTime = new Date(0);
    private OntModel topologyOntBaseModel = null;
    private OntModel topologyOntModel = null;
    
    public TopologyManager() {
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }
    
    // parse a topology configure file (yaml or xml)
    // create device and interface in DB if not existent
    public void initNetworkTopology() throws ConfigException {
        NPSGlobalConfig config = NPSConfigYaml.getInstance().getNPSGlobalConfig();
        Map devices = config.getDevices();
        Iterator devIt = devices.keySet().iterator();
        while (devIt.hasNext()) {
            String dName = (String) devIt.next();
            Map deviceCfg = (Map) devices.get(dName);
            String urn = (String)deviceCfg.get("urn");
            String makeModel = (String)deviceCfg.get("make_model");
            String address = (String)deviceCfg.get("address");
            String location = (String)deviceCfg.get("location");
            String description = (String)deviceCfg.get("description");
            Map connector = (Map)deviceCfg.get("connector");
            Map interfaces = (Map)deviceCfg.get("interfaces");
            if (urn == null) {
                throw new ConfigException("device '"+dName+"' has no urn configured");
            }
            Device device = NPSGlobalState.getDeviceStore().getByUrn(urn);
            boolean isNewDevice = false;
            if (device == null) {
                device = new Device();
                device.setUrn(urn);
                isNewDevice = true;
            } 
            if (makeModel != null) {
                device.setMakeModel(makeModel);
            } else {
                throw new ConfigException("device '" + dName + "' has no make_model configured");
            }
            if (address != null) {
                device.setAddress(address);
            } else {
                throw new ConfigException("device '" + dName + "' has no address configured");
            }
            if (location != null) {
                device.setLocation(location);
            }
            if (description != null) {
                device.setDescription(description);
            }
            if (connector != null) {
                device.setConnectorConfig(connector);
            } else {
                throw new ConfigException("device '"+dName+"' has no connector configured");
            }
            if (isNewDevice) {
                NPSGlobalState.getDeviceStore().add(device);
            } else {
                NPSGlobalState.getDeviceStore().update(device);
            }
            if (interfaces == null) {
                log.warn("device '"+dName+"' has no interfaces configured");
                continue;
            }
            Iterator ifIt = interfaces.keySet().iterator();
            while (ifIt.hasNext()) {
                String iName = (String) ifIt.next();
                Map intfCfg = (Map) interfaces.get(iName);
                String ifUrn = (String)intfCfg.get("urn");
                if (ifUrn == null) {
                    throw new ConfigException("device '"+dName+"' / interface '"
                            +iName+"' has no urn configured");
                }
                Interface intf = NPSGlobalState.getInterfaceStore().getByUrn(ifUrn);
                boolean isNewIf = false;
                if (intf == null) {
                    intf = new Interface();
                    intf.setUrn(ifUrn);
                    isNewIf = true;
                } 
                String ifDescr = (String)intfCfg.get("description");
                if (ifDescr != null)
                    intf.setDescription(ifDescr);
                String aliasUrn = (String)intfCfg.get("alias_urn");
                if (aliasUrn != null)
                    intf.setAliasUrn(aliasUrn);
                intf.setDeviceId(device.getId());
                intf.setMakeModel("Generic");
                if (isNewIf) {
                    NPSGlobalState.getInterfaceStore().add(intf);
                } else {     
                    NPSGlobalState.getInterfaceStore().update(intf);
                } 
                device.getInterfaces().add(intf);
            }
        }
    }
    
    // compute full P2P path of STPs -- hack for now: single hop 
    public List<ServiceTerminationPoint> computeP2PPath(ServiceTerminationPoint srcSTP, 
            ServiceTerminationPoint dstSTP) throws ServiceException {
        List<ServiceTerminationPoint> path = new ArrayList<ServiceTerminationPoint>();
        path.add(srcSTP);
        path.add(dstSTP);
        return path;
    }

    
    // create explicit p2p or mp path by simply including all STPs 
    public List<ServiceTerminationPoint> createExplicitPath(List<ServiceTerminationPoint> customerSTPs)
            throws ServiceException {
        List<ServiceTerminationPoint> path = new ArrayList<ServiceTerminationPoint>();
        for (ServiceTerminationPoint stp: customerSTPs) {
            String ifUrn = stp.getId();
            if (NPSGlobalState.getInterfaceStore().getByUrn(ifUrn) == null) {
                throw new ServiceException("createExplicitPath - undefined urn "+ ifUrn);
            }
            path.add(stp);
        }
        return path;
    }


    // generate list of Device with associaed STPs 
    public List<NetworkDeviceInstance> createDeviceInstanceSequence(String contractId,
            List<ServiceTerminationPoint> path) throws ServiceException {
        List<NetworkDeviceInstance> devInsSequence = new ArrayList<NetworkDeviceInstance>();
        NetworkDeviceFactory devInsFactory = NetworkDeviceFactory.getFactory();
        for (ServiceTerminationPoint stp: path) {
            NetworkDeviceInstance devIns = devInsFactory.create(contractId, stp);
            boolean hasDevIns = false;
            for (NetworkDeviceInstance i : devInsSequence) {
                if (i == devIns) {
                    hasDevIns = true;
                    break;
                }
            }
            if (!hasDevIns) {
                devInsSequence.add(devIns);
            }
        }
        return devInsSequence;
    }
    
    public OntModel createTopologyBaseModel() {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF); 
        
        model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
        model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        model.setNsPrefix("nml", "http://schemas.ogf.org/nml/2013/03/base#");
        model.setNsPrefix("mrs", "http://schemas.ogf.org/mrs/2013/12/topology#");
        Property type = model.createProperty( "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        Resource NamedIndividual = model.createResource("http://www.w3.org/2002/07/owl#NamedIndividual");

        //List<Interface> interfaces = NPSGlobalState.getInterfaceStore().getAll();
        
        //$$ TODO: get the top level ontology resource URN from config file
        Resource resTopology = model.createResource("urn:ogf:network:sdn.maxgigapop.net:network");        
        model.add(model.createStatement(resTopology, type, NamedIndividual));
        model.add(model.createStatement(resTopology, type, Nml.Topology));
        
        List<Device> devices = NPSGlobalState.getDeviceStore().getAll();
        for (Device dev: devices) {
            Resource resNode = model.createResource(dev.getUrn());
            model.add(model.createStatement(resTopology, Nml.hasNode, resNode));
            model.add(model.createStatement(resNode, Nml.belongsTo, resTopology));
            model.add(model.createStatement(resNode, type, NamedIndividual));
            if (dev.getLocation() != null && !dev.getLocation().isEmpty()) {
                model.add(model.createStatement(resNode, Nml.address, dev.getLocation()));
            }
            if (dev.getDescription() != null && !dev.getDescription().isEmpty()) {
                model.add(model.createStatement(resNode, Nml.name, dev.getDescription()));
            }
            List<Interface> devIfs = NPSGlobalState.getInterfaceStore().getByDeviceId(dev.getId());
            if (devIfs == null || devIfs.isEmpty())
                continue;
            for (Interface intf: devIfs) {
                Resource resPort = model.createResource(intf.getUrn());
                model.add(model.createStatement(resNode, Nml.hasBidirectionalPort, resPort));
                model.add(model.createStatement(resPort, Nml.belongsTo, resNode));
                model.add(model.createStatement(resPort, type, NamedIndividual));
                if (intf.getDescription() != null && !intf.getDescription().isEmpty()) {
                   model.add(model.createStatement(resPort, Nml.name, intf.getDescription()));
                }
                if (intf.getAliasUrn() != null && !intf.getAliasUrn().isEmpty()) {
                    model.add(model.createStatement(resNode, Nml.isAlias, intf.getAliasUrn()));
                }
            }
        }
        
        return model;
    }
    
    @Override
    public void run() {
        // 1. create base ontology model from deviceStore and interfaceStore
        if (this.topologyOntBaseModel == null) {
            this.topologyOntBaseModel = this.createTopologyBaseModel();
            StringWriter out = new StringWriter();
            topologyOntBaseModel.write(out);
            log.info("created ontology for base topology: " + out.toString());
        }

        // $$ TODO add poll loop with configurable interval
        // 2. poll all contracts 
        boolean modelToBeUpdated = false;
        List<NPSContract> npsContracts = NPSGlobalState.getContractManager().getAll();
        synchronized (npsContracts) { 
            for (NPSContract contract: npsContracts) {
                if (contract.getModifiedTime().after(lastestModelTime)) {
                    //$$ TODO: convert contract into ontology information     
                    modelToBeUpdated = true;
                }
            }
        }
        if (modelToBeUpdated) {
            this.topologyOntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF); 
            this.topologyOntModel.add(this.topologyOntBaseModel);
            //$$ TODO: mash up with the contract ontology information
        }
    }
}
