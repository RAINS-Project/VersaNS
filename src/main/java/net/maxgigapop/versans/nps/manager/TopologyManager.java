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
import java.io.StringReader;
import net.maxgigapop.versans.nps.api.*;
import net.maxgigapop.versans.nps.rest.model.*;
import java.io.StringWriter;
import net.maxgigapop.versans.nps.device.DeviceException;
import net.maxgigapop.versans.nps.device.RESTConnector;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author xyang
 */ 
public class TopologyManager extends Thread {
    private static final Logger logger = Logger.getLogger(TopologyManager.class.getName());
    private Date lastestModelTime = new Date(0);
    private OntModel topologyOntBaseModel = null;
    private OntModel topologyOntHeadModel = null;
    private String topologyOntHeadModelVersion = "";
    private OntModel topologyOntModel = null;
    private long pollInterval = 30000L; // 30 seconds
    private final Integer topologyOntModelLock = new Integer(0);

    public TopologyManager() {
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

    public OntModel getTopologyOntHeadModel() {
        return topologyOntHeadModel;
    }

    public String getTopologyOntHeadModelVersion() {
        return topologyOntHeadModelVersion;
    }
    
    // parse a topology configure file (yaml or xml)
    // create device and interface in DB if not existent
    public void initNetworkTopology() throws ConfigException, DeviceException, URIException {
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
                logger.warn("device '"+dName+"' has no interfaces configured");
                // Find all available interfaces on the device (Only support OESS right now)
                if (dName.equals("OESS")) {
                    // Clean up database
                    device.setInterfaces(new ArrayList<Interface>());
                    List<Interface> intfs = NPSGlobalState.getInterfaceStore().getByDeviceUrn(urn);
                    if (intfs != null) {
                        for (Interface intf : intfs) {
                            NPSGlobalState.getInterfaceStore().delete(intf);
                        } 
                    }
                    synchronized(RESTConnector.simplyLock) {
                        RESTConnector OESSconnector = RESTConnector.getRESTConnector();
                        OESSconnector.setConfig(device.getConnectorConfig());
                        String queryCmd = "services/data.cgi?action=get_all_resources_for_workgroup&workgroup_id=1";
                        try {
                            String response = OESSconnector.addNewCircuit(queryCmd);
                            JSONObject jsonResponse = new JSONObject(response);
                            JSONArray jsonResult = jsonResponse.getJSONArray("results");
                            for (int i = 0; i < jsonResult.length(); i++) {
                                JSONObject intfJSON = jsonResult.getJSONObject(i);
                                if (intfJSON.getString("operational_state").equals("up")) {
                                    String nodeName = intfJSON.getString("node_name");
                                    String intName = URIUtil.encodeQuery(intfJSON.getString("interface_name"));
                                    String desc = intfJSON.getString("description");
                                    String ifUrn = "urn:ogf:network:domain=openflow.maxgigapop.net:node=" + nodeName + ":port=" + intName + ":link=*";
                                    Interface intf = new Interface();
                                    intf.setUrn(ifUrn);
                                    intf.setDescription(desc);
                                    //intf.setAliasUrn(aliasUrn);
                                    intf.setDeviceId(device.getId());
                                    intf.setMakeModel("Generic");
                                    NPSGlobalState.getInterfaceStore().add(intf);
                                    device.getInterfaces().add(intf);
                                }
                            }
                        } catch (JSONException e) {
                            throw new DeviceException("JSONException caught when calculating shortest path: " + e.getMessage());
                        }
                    }
                }
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

        //$$ TODO: get the top level topology URN from config file
        Resource resTopology = RdfOwl.createResource(model, "urn:ogf:network:sdn.maxgigapop.net:network", Nml.Topology); 
        
        List<Device> devices = NPSGlobalState.getDeviceStore().getAll();
        for (Device dev: devices) {
            Resource resNode =  RdfOwl.createResource(model, dev.getUrn(), Nml.Node);
            model.add(model.createStatement(resTopology, Nml.hasNode, resNode));
            model.add(model.createStatement(resNode, Nml.belongsTo, resTopology));
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
            Resource resSwSvc =  RdfOwl.createResource(model, dev.getUrn()+":l2switching", Nml.SwitchingService);
            model.add(model.createStatement(resSwSvc, Nml.encoding, model.createResource("http://schemas.ogf.org/nml/2012/10/ethernet#vlan")));
            model.add(model.createStatement(resSwSvc, Nml.labelSwapping, "false"));
            model.add(model.createStatement(resNode, Nml.hasService, resSwSvc));
            if (dev.getMakeModel().contains("Router")) {
                Resource resRtSvc = RdfOwl.createResource(model, resNode.getURI() + ":l3routing", Mrs.RoutingService);
                model.add(model.createStatement(resNode, Nml.hasService, resRtSvc));
                //$$ add localAddress or hasNetworkAddress property?
                //$$ add other RoutingService properties?
            }
            for (Interface intf: devIfs) {
                Resource resPort = RdfOwl.createResource(model, intf.getUrn(), Nml.BidirectionalPort);
                model.add(model.createStatement(resNode, Nml.hasBidirectionalPort, resPort));
                model.add(model.createStatement(resPort, Nml.belongsTo, resNode));
                model.add(model.createStatement(resSwSvc, Nml.hasBidirectionalPort, resPort));
                model.add(model.createStatement(resPort, Nml.belongsTo, resSwSvc));
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

    public void addContractToOntModel(OntModel model, NPSContract contract) {
        Resource resSubnet = null;
        Resource resNode = null;
        Resource resCustomerSubIf = null;
        if (contract.getCustomerSTPs() == null) {
            logger.warn(String.format("%s has no customerSTPs", contract));
            return;
        }
        for (ServiceTerminationPoint stp: contract.getCustomerSTPs()) {
            String portUrn = stp.getId();
            Layer2Info l2info = stp.getLayer2Info();
            if (l2info != null) {
                VlanTag outerVlan = l2info.getOuterVlanTag();
                //?? VlanTag innerVlan =  l2info.getInnerVlanTag();
                // create SwitchingSubnet (if not existing)
                if (resSubnet == null) {
                    Resource resPort = model.getResource(portUrn);
                    //$$ if (resPort == null) throw exception;
                    // look for SwitchingService and Node that contains this port
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
                    // do not create subnet for l3routing contract
                    if (contract.getProviderSTP() == null) {
                        String subnetUri = contract.getId()+":vlan";
                        if (!subnetUri.contains("urn:") && !subnetUri.contains("uri:")) {
                            subnetUri = resSwSvc.getURI() + ":" + subnetUri;
                        }
                        resSubnet = RdfOwl.createResource(model, subnetUri, Mrs.SwitchingSubnet);
                        model.add(model.createStatement(resSwSvc, Mrs.providesSubnet, resSubnet));
                        model.add(model.createStatement(resSubnet, Nml.encoding, model.createResource("http://schemas.ogf.org/nml/2012/10/ethernet#vlan")));
                        model.add(model.createStatement(resSubnet, Nml.labelSwapping, "false"));
                    }
                }
                //create sub-interface BidirectionalPort for outerVlan
                resCustomerSubIf = RdfOwl.createResource(model, portUrn + ":vlan-" + outerVlan.getValue(), Nml.BidirectionalPort);
                model.add(model.createStatement(resNode, Nml.hasBidirectionalPort, resCustomerSubIf));
                //add Nml.Label for resSubIf
                Resource resSubIfLabel = RdfOwl.createResource(model, portUrn + ":vlan-" + outerVlan.getValue()+":label", Nml.Label);
                model.add(model.createStatement(resCustomerSubIf, Nml.hasLabel, resSubIfLabel));
                model.add(model.createStatement(resSubIfLabel, Nml.labeltype, model.createResource("http://schemas.ogf.org/nml/2012/10/ethernet#vlan")));
                model.add(model.createStatement(resSubIfLabel, Nml.value, outerVlan.getValue()));
                //add sub-interface BidirectionalPort to the SwitchingSubnet
                if (contract.getProviderSTP() == null) {
                    model.add(model.createStatement(resSubnet, Nml.hasBidirectionalPort, resCustomerSubIf));
                    model.add(model.createStatement(resCustomerSubIf, Nml.belongsTo, resSubnet));
                }
            }
        }
        if (contract.getProviderSTP() != null) {
            ServiceTerminationPoint providerStp = contract.getProviderSTP();
            String providerPortUrn = providerStp.getId();
            Resource resProviderPort = model.getResource(providerPortUrn); // $$ null -> exception
            Layer2Info providerL2Info = providerStp.getLayer2Info();
            Resource resProviderSubIf = null;
            if (providerL2Info != null) {
                VlanTag outerVlan = providerL2Info.getOuterVlanTag();
                //?? VlanTag innerVlan =  l2info.getInnerVlanTag();
                resProviderSubIf = RdfOwl.createResource(model, providerPortUrn + ":vlan-" + outerVlan.getValue(), Nml.BidirectionalPort);
                 //add Nml.Label for resSubIf
                Resource resSubIfLabel = RdfOwl.createResource(model, providerPortUrn + ":vlan-" + outerVlan.getValue()+":label", Nml.Label);
                model.add(model.createStatement(resProviderSubIf, Nml.hasLabel, resSubIfLabel));
                model.add(model.createStatement(resSubIfLabel, Nml.labeltype, model.createResource("http://schemas.ogf.org/nml/2012/10/ethernet#vlan")));
                model.add(model.createStatement(resSubIfLabel, Nml.value, outerVlan.getValue()));
               // add sub-interface BidirectionalPort to the Node (and ?SwitchingSubnet?)
                model.add(model.createStatement(resNode, Nml.hasBidirectionalPort, resProviderSubIf));
                //$$ we should not create subnet for a l3routing cross-connect contract
                //model.add(model.createStatement(resSubnet, Nml.hasBidirectionalPort, resSubIf));
                //model.add(model.createStatement(resSubIf, Nml.belongsTo, resSubnet));
            }
            Layer3Info providerL3Info = providerStp.getLayer3Info();
            if (providerL3Info != null) {
                ServiceTerminationPoint customerStp = contract.getCustomerSTPs().get(0);
                String customerPortUrn = customerStp.getId();
                Resource resCustomerPort = model.getResource(customerPortUrn); // $$ null -> exception
                Layer3Info customerL3Info = customerStp.getLayer3Info();
                // get RoutingService 
                Resource resRtSvc = model.getResource(resNode.getURI() + ":l3routing");
                //$$ if (resRtSvc == null) throw exception
                // add Route from provider to customer
                String rtToCustomerUri = contract.getId() + ":p2c";
                if (!rtToCustomerUri.contains("urn:") && !rtToCustomerUri.contains("uri:")) {
                    rtToCustomerUri = resRtSvc.getURI() + ":" + rtToCustomerUri;
                }
                Resource resRtToCustomer = RdfOwl.createResource(model, rtToCustomerUri, Mrs.Route);
                model.add(model.createStatement(resRtSvc, Mrs.providesRoute, resRtToCustomer));
                int netAddrNo = 1;
                // to: customer bgp-asn
                Resource resNetAddrCustomerASN = RdfOwl.createResource(model, String.format("%s:aid-%d", rtToCustomerUri, netAddrNo++), Mrs.NetworkAddress);
                model.add(model.createStatement(resNetAddrCustomerASN, Mrs.type, "bgp-asn"));
                model.add(model.createStatement(resNetAddrCustomerASN, Mrs.value, customerL3Info.getBgpInfo().getPeerASN()));
                model.add(model.createStatement(resRtToCustomer, Mrs.routeTo, resNetAddrCustomerASN));
                // next: customer bgp-remote-ip/30 + resSubIf
                Resource resNetAddrCustomerRmtIfIp = RdfOwl.createResource(model, String.format("%s:aid-%d", rtToCustomerUri, netAddrNo++), Mrs.NetworkAddress);
                model.add(model.createStatement(resNetAddrCustomerRmtIfIp, Mrs.type, "bgp-remote-ip"));
                model.add(model.createStatement(resNetAddrCustomerRmtIfIp, Mrs.value, customerL3Info.getBgpInfo().getLinkRemoteIpAndMask()));
                model.add(model.createStatement(resRtToCustomer, Mrs.nextHop, resNetAddrCustomerRmtIfIp));
                // add Route from customer to provider 
                String rtToProviderUri = contract.getId() + ":c2p";
                if (!rtToProviderUri.contains("urn:") && !rtToProviderUri.contains("uri:")) {
                    rtToProviderUri = resRtSvc.getURI() + ":" + rtToProviderUri;
                }
                if (customerL3Info != null && customerL3Info.getBgpInfo() != null) {
                    Resource resRtToProvider = RdfOwl.createResource(model, rtToProviderUri, Mrs.Route);
                    model.add(model.createStatement(resRtSvc, Mrs.providesRoute, resRtToProvider));
                    // to: provider bgp-asn
                    Resource resNetAddrProviderASN = RdfOwl.createResource(model, String.format("%s:aid-%d", rtToProviderUri, netAddrNo++), Mrs.NetworkAddress);
                    model.add(model.createStatement(resNetAddrProviderASN, Mrs.type, "bgp-asn"));
                    model.add(model.createStatement(resNetAddrProviderASN, Mrs.value, providerL3Info.getBgpInfo().getPeerASN()));
                    model.add(model.createStatement(resRtToProvider, Mrs.routeTo, resNetAddrProviderASN));
                    // next: provider bgp-remote-ip/30 + resSubIf
                    Resource resNetAddrProviderRmtIfIp = RdfOwl.createResource(model, String.format("%s:aid-%d", rtToProviderUri, netAddrNo++), Mrs.NetworkAddress);
                    model.add(model.createStatement(resNetAddrProviderRmtIfIp, Mrs.type, "bgp-remote-ip"));
                    model.add(model.createStatement(resNetAddrProviderRmtIfIp, Mrs.value, providerL3Info.getBgpInfo().getLinkRemoteIpAndMask()));
                    model.add(model.createStatement(resRtToProvider, Mrs.nextHop, resNetAddrProviderRmtIfIp));
                    // from: customer bgp-asn + bgp-local-ip/30 + prefix-list + resSubIf
                    model.add(model.createStatement(resRtToProvider, Mrs.routeFrom, resNetAddrCustomerASN));
                    model.add(model.createStatement(resRtToProvider, Mrs.routeFrom, resNetAddrCustomerRmtIfIp));
                    model.add(model.createStatement(resRtToProvider, Mrs.routeFrom, resCustomerSubIf));
                    Resource resNetAddrCustomerPrefixList = RdfOwl.createResource(model, String.format("%s:aid-%d", rtToCustomerUri, netAddrNo++), Mrs.NetworkAddress);
                    model.add(model.createStatement(resNetAddrCustomerPrefixList, Mrs.type, "bgp-prefix-list"));
                    model.add(model.createStatement(resNetAddrCustomerPrefixList, Mrs.value, NPSUtils.concatStringsWSep(customerL3Info.getBgpInfo().getPeerIpPrefix(), ",")));
                    model.add(model.createStatement(resRtToProvider, Mrs.routeFrom, resNetAddrCustomerPrefixList));
                    // add the other Route (resRtToCustomer) from: provider bgp-asn + bgp-local-ip/30 + prefix-list + resSubIf
                    model.add(model.createStatement(resRtToCustomer, Mrs.routeFrom, resNetAddrProviderASN));
                    model.add(model.createStatement(resRtToCustomer, Mrs.routeFrom, resNetAddrProviderRmtIfIp));
                    model.add(model.createStatement(resRtToCustomer, Mrs.routeFrom, resProviderSubIf));
                    Resource resNetAddrProviderPrefixList = RdfOwl.createResource(model, String.format("%s:aid-%d", rtToProviderUri, netAddrNo++), Mrs.NetworkAddress);
                    model.add(model.createStatement(resNetAddrProviderPrefixList, Mrs.type, "bgp-prefix-list"));
                    model.add(model.createStatement(resNetAddrProviderPrefixList, Mrs.value, NPSUtils.concatStringsWSep(providerL3Info.getBgpInfo().getPeerIpPrefix(), ",")));
                    model.add(model.createStatement(resRtToCustomer, Mrs.routeFrom, resNetAddrProviderPrefixList));
                }
            }
        }
    }

    @Override
    public void run() {
        // reload this.topologyOntHeadModel
        ModelBase modelBaseHead = NPSGlobalState.getModelStore().getHead();
        if (modelBaseHead != null) {
            this.topologyOntHeadModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            StringReader ttlReader = new StringReader(modelBaseHead.getTtlModel());
            this.topologyOntHeadModel.read(ttlReader, null, "TURTLE");
            this.topologyOntHeadModelVersion = modelBaseHead.getVersion();
        }
        // 1. create base ontology model from deviceStore and interfaceStore
        this.topologyOntBaseModel = this.createTopologyBaseModel();
        // store base model if nothing stored before.
        if (this.topologyOntHeadModel == null) {
            ModelBase baseModel = new ModelBase();
            StringWriter ttlWriter = new StringWriter();
            topologyOntBaseModel.write(ttlWriter, "TURTLE");
            baseModel.setTtlModel(ttlWriter.toString());
            baseModel.setVersion(UUID.randomUUID().toString());
            baseModel.setStatus("ACTIVE");
            NPSGlobalState.getModelStore().add(baseModel);
            this.topologyOntHeadModel = topologyOntBaseModel;
            this.topologyOntHeadModelVersion = baseModel.getVersion();
            lastestModelTime = new Date();
        }
        // 2. poll all contracts 
        while (true) {
            try {
                this.sleep(pollInterval);
            } catch (InterruptedException e) {
                logger.warn("TopologyManager run sleep got interrupted!");
            }
            synchronized (this.topologyOntModelLock) { 
                this.topologyOntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF); 
                this.topologyOntModel.setNsPrefix("rdf", RdfOwl.getRdfURI());
                this.topologyOntModel.setNsPrefix("rdfs", RdfOwl.getRdfsURI());
                this.topologyOntModel.setNsPrefix("owl", RdfOwl.getOwlURI());
                this.topologyOntModel.setNsPrefix("xsd", RdfOwl.getXsdURI());
                this.topologyOntModel.setNsPrefix("nml", Nml.getURI());
                this.topologyOntModel.setNsPrefix("mrs", Mrs.getURI());
                // add base model (w/o inferenced / trivial ontologies)
                this.topologyOntModel.add(this.topologyOntBaseModel.getBaseModel());
                List<NPSContract> npsContracts = NPSGlobalState.getContractManager().getAll();
                synchronized (npsContracts) {
                    Iterator<NPSContract> itc = npsContracts.iterator();
                    while (itc.hasNext()) {
                        NPSContract contract = itc.next();
                        if (contract.getStatus().contains("ROLLBACKED") 
                                || contract.getStatus().contains("FAILED")
                                || contract.getStatus().contains("TERMINATED"))
                            continue;
                        addContractToOntModel(this.topologyOntModel, contract);
                    }
                }                
                if (this.topologyOntHeadModel == null
                    || (this.topologyOntHeadModel != null && this.topologyOntModel != this.topologyOntHeadModel
                        && !this.topologyOntModel.isIsomorphicWith(this.topologyOntHeadModel))) {
                    ModelBase newModel = new ModelBase();
                    StringWriter ttlWriter = new StringWriter();
                    this.topologyOntModel.write(ttlWriter, "TURTLE");
                    newModel.setTtlModel(ttlWriter.toString());
                    newModel.setVersion(UUID.randomUUID().toString());
                    newModel.setStatus("ACTIVE");
                    NPSGlobalState.getModelStore().add(newModel);
                    this.topologyOntHeadModel = this.topologyOntModel;
                    this.topologyOntHeadModelVersion = newModel.getVersion();
                    lastestModelTime = new Date();
                }
             }
        }
    }
}
