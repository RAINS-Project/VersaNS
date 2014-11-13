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
import com.hp.hpl.jena.rdf.model.*;
import net.maxgigapop.versans.nps.api.*;
import net.maxgigapop.versans.nps.rest.model.*;
import java.io.StringWriter;

/**
 *
 * @author xyang
 */ 
public class TopologyManager extends Thread {
    private org.apache.log4j.Logger log;
    private Date lastestModelTime = new Date(0);
    private OntModel topologyOntBaseModel = null;
    private OntModel topologyOntModel = null;
    private long pollInterval = 60000L; // 60 seconds
    private final Integer topologyOntModelLock = new Integer(0);

    public TopologyManager() {
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }

    public Date getLastestModelTime() {
        return lastestModelTime;
    }

    public void setLastestModelTime(Date lastestModelTime) {
        this.lastestModelTime = lastestModelTime;
    }

    public long getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(long pollInterval) {
        this.pollInterval = pollInterval;
    }

    public OntModel getTopologyOntModel() {
        synchronized(this.topologyOntModelLock) {
            return topologyOntModel;
        }
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
        
        model.setNsPrefix("rdf", RdfOwl.getRdfURI());
        model.setNsPrefix("rdfs", RdfOwl.getRdfsURI());
        model.setNsPrefix("owl", RdfOwl.getOwlURI());
        model.setNsPrefix("xsd", RdfOwl.getXsdURI());
        model.setNsPrefix("nml", Nml.getURI());
        model.setNsPrefix("mrs", Mrs.getURI());

        //$$ TODO: get the top level ontology resource URN from config file
        Resource resTopology = model.createResource("urn:ogf:network:sdn.maxgigapop.net:network");        
        model.add(model.createStatement(resTopology, RdfOwl.type, RdfOwl.NamedIndividual));
        model.add(model.createStatement(resTopology, RdfOwl.type, Nml.Topology));
        
        List<Device> devices = NPSGlobalState.getDeviceStore().getAll();
        for (Device dev: devices) {
            Resource resNode = model.createResource(dev.getUrn());
            model.add(model.createStatement(resTopology, Nml.hasNode, resNode));
            model.add(model.createStatement(resNode, Nml.belongsTo, resTopology));
            model.add(model.createStatement(resNode, RdfOwl.type, RdfOwl.NamedIndividual));
            if (dev.getLocation() != null && !dev.getLocation().isEmpty()) {
                model.add(model.createStatement(resNode, Nml.address, dev.getLocation()));
            }
            if (dev.getDescription() != null && !dev.getDescription().isEmpty()) {
                model.add(model.createStatement(resNode, Nml.name, dev.getDescription()));
            }
            List<Interface> devIfs = NPSGlobalState.getInterfaceStore().getByDeviceId(dev.getId());
            if (devIfs == null || devIfs.isEmpty())
                continue;
            // create SwitchingService that connects all these interfaces
            Resource resSwSvc = model.createResource(dev.getUrn()+":l2switching");
            model.add(model.createStatement(resSwSvc, RdfOwl.type, RdfOwl.NamedIndividual));
            model.add(model.createStatement(resSwSvc, RdfOwl.type, Nml.SwitchingService));
            model.add(model.createStatement(resSwSvc, Nml.encoding, model.createResource("http://schemas.ogf.org/nml/2012/10/ethernet#vlan")));
            model.add(model.createStatement(resSwSvc, Nml.labelSwapping, "false"));
            model.add(model.createStatement(resNode, Nml.hasService, resSwSvc));
            for (Interface intf: devIfs) {
                Resource resPort = model.createResource(intf.getUrn());
                model.add(model.createStatement(resNode, Nml.hasBidirectionalPort, resPort));
                model.add(model.createStatement(resPort, Nml.belongsTo, resNode));
                model.add(model.createStatement(resSwSvc, Nml.hasBidirectionalPort, resPort));
                model.add(model.createStatement(resPort, Nml.belongsTo, resSwSvc));
                model.add(model.createStatement(resPort, RdfOwl.type, RdfOwl.NamedIndividual));
                if (intf.getDescription() != null && !intf.getDescription().isEmpty()) {
                   model.add(model.createStatement(resPort, Nml.name, intf.getDescription()));
                }
                if (intf.getAliasUrn() != null && !intf.getAliasUrn().isEmpty()) {
                    model.add(model.createStatement(resNode, Nml.isAlias, intf.getAliasUrn()));
                }
                //$$ TODO: add LabelGroup to under resPort
            }
        }
        return model;
    }
    
    public void addContractToOntModel(OntModel model, NPSContract contract) {
        Resource resSubnet = null;
        Resource resNode = null;
        for (ServiceTerminationPoint stp: contract.getCustomerSTPs()) {
            String portUrn = stp.getInterfaceRef();
            Layer2Info l2info = stp.getLayer2Info();
            if (l2info != null) {
                VlanTag outerVlan = l2info.getOuterVlanTag();
                //?? VlanTag innerVlan =  l2info.getInnerVlanTag();
                // create SwitchingSubnet (if not existing)
                if (resSubnet == null) {
                    Resource resPort = model.getResource(portUrn);
                    //$$ if (resPort == null) throw exception;
                    Resource resSwSvc = null;
                    StmtIterator stmts = model.listStatements(null, Nml.hasBidirectionalPort, resPort);
                    while (stmts.hasNext()) {
                        Statement stmt = stmts.next();
                        Resource stmtSubject = stmt.getSubject();
                        StmtIterator innerStmts = model.listStatements(stmtSubject, RdfOwl.type, Nml.SwitchingService);
                        if (innerStmts != null && innerStmts.hasNext()) {
                            resSwSvc = stmtSubject;
                        }
                        innerStmts = model.listStatements(stmtSubject, RdfOwl.type, Nml.Node);
                        if (innerStmts != null && innerStmts.hasNext()) {
                            resNode = stmtSubject;
                        }
                    }
                    //$$: if (resSwSvc == null || resNode == null) throw exception;                  
                    resSubnet = model.createResource(resSwSvc.getURI() + ":vlan-" + outerVlan.toString());
                    model.add(model.createStatement(resSubnet, RdfOwl.type, RdfOwl.NamedIndividual));
                    model.add(model.createStatement(resSubnet, RdfOwl.type, Mrs.SwitchingSubnet));
                    model.add(model.createStatement(resSubnet, Nml.encoding, model.createResource("http://schemas.ogf.org/nml/2012/10/ethernet#vlan")));
                    model.add(model.createStatement(resSubnet, Nml.labelSwapping, "false"));
                }
                //create sub-interface BidirectionalPort for outerVlan
                Resource resSubIf = model.createResource(portUrn + ":vlan-" + outerVlan.toString());
                model.add(model.createStatement(resSubIf, RdfOwl.type, RdfOwl.NamedIndividual));
                model.add(model.createStatement(resSubIf, RdfOwl.type, Nml.BidirectionalPort));
                //$$ TODO: add Nml.Label to under resSubIf

                //add sub-interface BidirectionalPort to the SwitchingSubnet
                model.add(model.createStatement(resSubnet, Nml.hasBidirectionalPort, resSubIf));
                model.add(model.createStatement(resSubIf, Nml.belongsTo, resSubnet));
            }
        }
        if (contract.getProviderSTP() != null) {
            ServiceTerminationPoint providerStp = contract.getProviderSTP();
            String providerPortUrn = providerStp.getInterfaceRef();
            Resource resProviderPort = model.getResource(providerPortUrn); // $$ null -> exception
            Layer2Info providerL2Info = providerStp.getLayer2Info();
            if (providerL2Info != null && resSubnet != null) {
                VlanTag outerVlan = providerL2Info.getOuterVlanTag();
                //VlanTag innerVlan =  l2info.getInnerVlanTag();
                Resource resSubIf = model.createResource(providerPortUrn + ":vlan-" + outerVlan.toString());
                model.add(model.createStatement(resSubIf, RdfOwl.type, RdfOwl.NamedIndividual));
                model.add(model.createStatement(resSubIf, RdfOwl.type, Nml.BidirectionalPort));
                //add sub-interface BidirectionalPort to the SwitchingSubnet
                model.add(model.createStatement(resSubnet, Nml.hasBidirectionalPort, resSubIf));
                model.add(model.createStatement(resSubIf, Nml.belongsTo, resSubnet));
            }
            Layer3Info providerL3Info = providerStp.getLayer3Info();
            if (providerL3Info != null) {
                ServiceTerminationPoint customerStp = contract.getCustomerSTPs().get(0);
                String customerPortUrn = customerStp.getInterfaceRef();
                Resource resCustomerPort = model.getResource(customerPortUrn); // $$ null -> exception
                Layer3Info customerL3Info = customerStp.getLayer3Info();
                // create RoutingService 
                Resource resRtSvc = RdfOwl.createResource(model, resNode.getURI() + ":l3routing", Mrs.RoutingService);
                model.add(model.createStatement(resNode, Nml.hasService, resRtSvc));
                //$$ add localAddress property ?
                //$$ add other RoutingService properties
                // add Route from provider to customer
                String rtToCustomerUri = resNode.getURI() + ":l3route:" + contract.getId() + ":p2c";
                Resource resRtToCustomer = RdfOwl.createResource(model, rtToCustomerUri, Mrs.Route);
                // to: customer bgp-asn
                Resource resNetAddrCustomerASN = RdfOwl.createResource(model, rtToCustomerUri + ":bgp-asn", Mrs.NetworkAddress);
                model.add(model.createStatement(resNetAddrCustomerASN, Mrs.value, customerL3Info.getBgpInfo().getPeerASN()));
                model.add(model.createStatement(resRtToCustomer, Mrs.routeTo, resNetAddrCustomerASN));
                // next: customer bgp-remote-ip/30 + resSubIf
                Resource resNetAddrCustomerRmtIfIp = RdfOwl.createResource(model, rtToCustomerUri + ":bgp-remote-ip", Mrs.NetworkAddress);
                model.add(model.createStatement(resNetAddrCustomerRmtIfIp, Mrs.value, customerL3Info.getBgpInfo().getLinkRemoteIpAndMask()));
                model.add(model.createStatement(resRtToCustomer, Mrs.nextHop, resNetAddrCustomerRmtIfIp));
                // add Route from customer to provider 
                String rtToProviderUri = resNode.getURI() + ":l3route:" + contract.getId() + ":c2p";
                if (customerL3Info != null && customerL3Info.getBgpInfo() != null) {
                    Resource resRtToProvider = RdfOwl.createResource(model, rtToProviderUri, Mrs.Route);
                    // to: provider bgp-asn
                    Resource resNetAddrProviderASN = RdfOwl.createResource(model, rtToProviderUri+":bgp-asn", Mrs.NetworkAddress);
                    model.add(model.createStatement(resNetAddrProviderASN, Mrs.value, providerL3Info.getBgpInfo().getPeerASN()));
                    model.add(model.createStatement(resRtToProvider, Mrs.routeTo, resNetAddrProviderASN));
                    // next: provider bgp-remote-ip/30 + resSubIf
                    Resource resNetAddrProviderRmtIfIp = RdfOwl.createResource(model, rtToProviderUri+":bgp-remote-ip", Mrs.NetworkAddress);
                    model.add(model.createStatement(resNetAddrProviderRmtIfIp, Mrs.value, providerL3Info.getBgpInfo().getLinkRemoteIpAndMask()));
                    model.add(model.createStatement(resRtToProvider, Mrs.nextHop, resNetAddrProviderRmtIfIp));
                    // from: customer bgp-asn + bgp-local-ip/30 + prefix-list + resSubIf
                    model.add(model.createStatement(resRtToProvider, Mrs.routeFrom, resNetAddrCustomerASN));
                    model.add(model.createStatement(resRtToProvider, Mrs.routeFrom, resNetAddrCustomerRmtIfIp));
                    model.add(model.createStatement(resRtToProvider, Mrs.routeFrom, resCustomerPort));
                    Resource resNetAddrCustomerPrefixList = RdfOwl.createResource(model, rtToCustomerUri+":bgp-prefix-list", Mrs.NetworkAddress);
                    model.add(model.createStatement(resNetAddrCustomerPrefixList, Mrs.value, NPSUtils.concatStringsWSep(customerL3Info.getBgpInfo().getPeerIpPrefix(), ",")));
                    model.add(model.createStatement(resRtToProvider, Mrs.routeFrom, resNetAddrCustomerPrefixList));
                    // add the other Route (resRtToCustomer) from: provider bgp-asn + bgp-local-ip/30 + prefix-list + resSubIf
                    model.add(model.createStatement(resRtToCustomer, Mrs.routeFrom, resNetAddrProviderASN));
                    model.add(model.createStatement(resRtToCustomer, Mrs.routeFrom, resNetAddrProviderRmtIfIp));
                    model.add(model.createStatement(resRtToCustomer, Mrs.routeFrom, resProviderPort));
                    Resource resNetAddrProviderPrefixList = RdfOwl.createResource(model, rtToProviderUri+":bgp-prefix-list", Mrs.NetworkAddress);
                    model.add(model.createStatement(resNetAddrProviderPrefixList, Mrs.value, NPSUtils.concatStringsWSep(providerL3Info.getBgpInfo().getPeerIpPrefix(), ",")));
                    model.add(model.createStatement(resRtToCustomer, Mrs.routeFrom, resNetAddrProviderPrefixList));
                }
            }
        }
    }

    @Override
    public void run() {
        // 1. create base ontology model from deviceStore and interfaceStore
        if (this.topologyOntBaseModel == null) {
            this.topologyOntBaseModel = this.createTopologyBaseModel();
            ModelBase baseModel = new ModelBase();
            StringWriter ttlWriter = new StringWriter();
            topologyOntBaseModel.write(ttlWriter, "TURTLE");
            baseModel.setTtlModel(ttlWriter.toString());
            baseModel.setVersion(UUID.randomUUID().toString());
            baseModel.setStatus("ACTIVE");
            NPSGlobalState.getModelStore().add(baseModel);
        }
        // 2. poll all contracts 
        while (true) {
            try {
                this.sleep(pollInterval);
            } catch (InterruptedException e) {
                log.warn("TopologyManager run sleep got interrupted!");
            }
            synchronized (this.topologyOntModelLock) { 
                this.topologyOntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF); 
                this.topologyOntModel.add(this.topologyOntBaseModel);
                List<NPSContract> npsContracts = NPSGlobalState.getContractManager().getAll();
                boolean hasNewModel = false;
                synchronized (npsContracts) {
                    for (NPSContract contract: npsContracts) {
                        // all contracts count (active or in-process) 
                        //?? Should failed count or be given special treatment ??
                        if (contract.getStatus().contains("ROLLBACKED"))
                            continue;
                        if (contract.getModifiedTime().after(lastestModelTime)) {
                            addContractToOntModel(this.topologyOntModel, contract);
                            hasNewModel = true;
                        }
                    }
                }
                if (hasNewModel) { // ?? store every model or not ??
                    ModelBase newModel = new ModelBase();
                    StringWriter ttlWriter = new StringWriter();
                    topologyOntBaseModel.write(ttlWriter, "TURTLE");
                    newModel.setTtlModel(ttlWriter.toString());
                    newModel.setVersion(UUID.randomUUID().toString());
                    newModel.setStatus("ACTIVE");
                    NPSGlobalState.getModelStore().add(newModel);
                }
            }
        }
    }
}
