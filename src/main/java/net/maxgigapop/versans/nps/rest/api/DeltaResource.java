/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.rest.api;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import net.maxgigapop.versans.nps.api.*;
import net.maxgigapop.versans.nps.config.NPSConfigYaml;
import net.maxgigapop.versans.nps.manager.*;
import net.maxgigapop.versans.nps.rest.model.*;

/**
 * REST Web Service
 *
 * @author xyang
 */
@Path("delta")
public class DeltaResource {

    @Context
    private UriInfo context;

    private final org.apache.log4j.Logger log;

    /**
     * Creates a new instance of DeltaResource
     */
    public DeltaResource() {
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }
    
    /**
     * Retrieves representation of an instance of net.maxgigapop.versans.nps.api.DeltaResource
     * @return an instance of net.maxgigapop.sdnx.services.nps.rest.model.DeltaBase
     */
    @GET
    @Produces({"application/xml", "application/json"})
    @Path("/{referenceVersion}/{id}")
    public String query(@PathParam("referenceVersion") String referenceVersion, @PathParam("id") String id) throws NotFoundException {
        DeltaBase delta = NPSGlobalState.getDeltaStore().getByIdWithReferenceVersion(referenceVersion, id);
        if (delta == null)
            throw new NotFoundException(String.format("Unknown Delta id='%s' with referenceVersion='%s'", id, referenceVersion));
        
        // find all contracts with id.contains(delta.referenceVersion+"-"+delta.id) as well as contractRunners
        List<NPSContract> contractList = NPSGlobalState.getContractManager().getContractByDescriptionContains(String.format("%s-%s", delta.getReferenceVersion(), delta.getId()));
        
        boolean allActive = true;
        boolean inSetup = false;
        for (NPSContract contract : contractList) {
            if (contract.getStatus().contains("FAILED") || contract.getStatus().contains("ROLLBACKED") || contract.getStatus().contains("TERMINATED")) {
                delta.setStatus("COMMIT_FAILED");
                break;
            }
            if (!contract.getStatus().equals("ACTIVE")) {
                allActive = false;
                if (!contract.getStatus().equals("PREPARING")) {
                    inSetup = true;
                }
            }
        }
        if (allActive)
            delta.setStatus("ACTIVE");
        if (inSetup)
            delta.setStatus("INSETUP");
        NPSGlobalState.getDeltaStore().update(delta);
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
        if (delta.getId() == null || delta.getId().isEmpty()) {
            throw new BadRequestException(String.format("Failed to push delta - invalid ID"));
        }
        if (delta.getReferenceVersion() == null || delta.getReferenceVersion().isEmpty()) {
            throw new BadRequestException(String.format("Failed to push delta - invalid referenceVersion"));
        }        
        DeltaBase existDelta = NPSGlobalState.getDeltaStore().getByIdWithReferenceVersion(delta.getReferenceVersion(), delta.getId());
        if (existDelta != null) {
            return existDelta.getStatus();
        }
        // verify, map contracts (+/-) and add delta to db
        OntModel modelReduction = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        try {
            StringReader ttlReader = new StringReader(delta.getModelReduction());
            modelReduction.read(ttlReader, null, "TURTLE");
        } catch (Exception ex) {
            throw new BadRequestException(String.format("Failed to push delta - cannot parse modelReduction component, due to %s", ex.getMessage()));
        }
        OntModel modelAddition = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        try {
            StringReader ttlReader = new StringReader(delta.getModelAddition());
            modelAddition.read(ttlReader, null, "TURTLE");
        } catch (Exception ex) {
            throw new BadRequestException(String.format("Failed to push delta - cannot parse modelAddition component, due to %s", ex.getMessage()));
        }

        ModelBase refModelBase = NPSGlobalState.getModelStore().getByVersion(delta.getReferenceVersion());
        if (refModelBase == null) {
            throw new BadRequestException(String.format("Failed to push delta - cannot find reference model (version=%s)", delta.getReferenceVersion()));
        }
        
        // if (referenceVersion not head) calcuate the realtime modelAddition and modelReduction
        OntModel ontHeadModel = NPSGlobalState.getTopologyManager().getTopologyOntHeadModel();
        if (ontHeadModel != null && !delta.getReferenceVersion().equals(NPSGlobalState.getTopologyManager().getTopologyOntHeadModelVersion())) {
            OntModel ontNewModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
            try {
                StringReader ttlReader = new StringReader(refModelBase.getTtlModel());
                ontNewModel.read(ttlReader, null, "TURTLE");
            } catch (Exception ex) {
                throw new InternalServerErrorException(String.format("Failed to parse reference model (version=%s), due to %s", 
                        refModelBase.getVersion(), ex.getMessage()));
            }
            try {
                ontNewModel.remove(modelReduction);
                ontNewModel.add(modelAddition);
            } catch (Exception ex) {
                throw new InternalServerErrorException(String.format("Failed to apply delta (id=%s) to reference model (version=%s), due to %s",
                        delta.getId(), refModelBase.getVersion(), ex.getMessage()));
            }
            
            try {
                modelReduction.removeAll();
                modelReduction.add(ontHeadModel.difference(ontNewModel));
                modelAddition.removeAll();
                modelAddition.add(ontNewModel.difference(ontHeadModel));
            } catch (Exception ex) {
                throw new InternalServerErrorException(String.format("Failed to create realtime delta based on head model (version=%s), due to: %s", 
                        NPSGlobalState.getTopologyManager().getTopologyOntHeadModelVersion(), ex.getMessage()));
            }
        }
                    StringWriter ttlWriter1 = new StringWriter();
                    modelReduction.write(ttlWriter1, "TURTLE");
                    System.out.print(ttlWriter1);
                    StringWriter ttlWriter2 = new StringWriter();
                    modelAddition.write(ttlWriter2, "TURTLE");
                    System.out.print(ttlWriter2);

        //$$ map realtime modelReduction to contracts to tear down
        // throw exception if any SwitchingSubnet or Route is mal formatted
        List<NPSContract> deleteContractList = this.lookupL2L3ContractsFromOntModel(modelReduction);
        
        //$$ map realtime modelAddition to contracts to set up
        // throw exception if any SwitchingSubnet or Route is mal formatted
        List<ServiceContract> serviceContractList = this.createL2L3ContractsFromOntModel(modelAddition, String.format(":delta=%s-%s", delta.getReferenceVersion(), delta.getId()));

        // mark contract to teardown by later commit
        for (NPSContract deleteContract: deleteContractList) {
            if (deleteContract.getDeletingDeltaTagList() == null)
                deleteContract.setDeletingDeltaTagList(new ArrayList<String>());
            deleteContract.getDeletingDeltaTagList().add(String.format("%s-%s", delta.getReferenceVersion(), delta.getId()));
            try {
                NPSGlobalState.getContractManager().updateContract(deleteContract);
            } catch (ServiceException ex) {
                log.error(String.format("Failed to updateContract(%s) after adding a deletingDeltaTag, due to %s", deleteContract.getId(), ex.getMessage()));
            }
        }

        List<NPSContract> rollbackContractList = new ArrayList<NPSContract>();
        boolean hasSetupFailure = false;
        for (ServiceContract serviceContract: serviceContractList) {
            try {
                NPSGlobalState.getContractManager().handleSetup(serviceContract, String.format("Serving delta: %s-%s", delta.getReferenceVersion(), delta.getId()), true);
            } catch (ServiceException ex) {
                // collect the added contracts to be deleted by rollback
                List<NPSContract> npsContracts = NPSGlobalState.getContractManager().getAll();
                synchronized (npsContracts) {
                    Iterator<NPSContract> itc = npsContracts.iterator();
                    while (itc.hasNext()) {
                        NPSContract contract = itc.next();
                        if (contract.getDescription().equals(String.format("Serving delta: %s-%s", delta.getReferenceVersion(), delta.getId()))) {
                            rollbackContractList.add(contract);
                        }
                    }
                }
                hasSetupFailure = true;
            }
        }
        // rollback: delete all contracts in rollbackContractList
        if (!rollbackContractList.isEmpty()) {
            for (NPSContract contract : rollbackContractList) {
                try {
                    NPSGlobalState.getContractManager().deleteContract(contract);
                } catch (ServiceException ex) {
                    log.error(String.format("Failed to deleteContract(%s) when rolling back the delta setup process, due to %s", contract.getId(), ex.getMessage()));
                }
            }
        }        
        if (hasSetupFailure) {
            throw new InternalServerErrorException(String.format("Failed to setup delta (id=%s) with referenceVersion=%s)", delta.getId(), delta.getReferenceVersion()));
        }
        
        delta.setStatus("CONFIRMED");
        NPSGlobalState.getDeltaStore().add(delta);
        return delta.getStatus();
    }

    /**
     * Commit the version of String being pushed
     * @param content representation for the resource
     * @return an HTTP response with a status String.
     */
    @PUT
    @Consumes({"application/xml", "application/json"})
    @Path("/{referenceVersion}/{id}/{action}")
    public String commit(@PathParam("referenceVersion") String referenceVersion, @PathParam("id") String id, @PathParam("action") String action) throws NotFoundException {
        if (!action.toLowerCase().equals("commit")) {
            throw new BadRequestException("Invalid action: "+action);
        }
        // doCommit
        DeltaBase delta = NPSGlobalState.getDeltaStore().getByIdWithReferenceVersion(referenceVersion, id);
        if (delta == null)
            throw new NotFoundException(String.format("Unknown Delta id=%s with referenceVersion='%s'", id, referenceVersion));
        if (!delta.getStatus().equals("CONFIRMED")) {
            return delta.getStatus();
        }
        // commitTeardown
        // use getAllFresh() to fix thread safety hazard
        List<NPSContract> npsContracts = NPSGlobalState.getContractManager().getAllFresh();
        Iterator<NPSContract> contractIter = npsContracts.iterator();
        while (contractIter.hasNext()) {
            NPSContract contract = contractIter.next();
            // search deletingDeltaTags
            if (contract.getDeletingDeltaTagList() != null && contract.getDeletingDeltaTagList().contains(String.format("%s-%s", delta.getReferenceVersion(), delta.getId()))) {
                try {
                    if (contract.getStatus().equals("PREPARING")) {
                        // if used NPSGlobalState.getContractManager().getAll(), the below will be removing on the same list
                        // and cause ConcurrentModificationException. Only contractIter.remove() can avoid that. 
                        // We can put db delete code here (without deleteContract) and then use contractIter.remove().
                        NPSGlobalState.getContractManager().deleteContract(contract);
                    } else {
                        NPSGlobalState.getContractManager().handleTeardown(contract.getId());
                    }
                } catch (ServiceException ex) {
                    throw new InternalServerErrorException(String.format("Failed to commit delta when deleting sub-level contract '%s'", contract.getId()));
                }
            }
        }
        // commitSetup
        // find all contracts with id.contains(delta.referenceVersion+"-"+delta.id) as well as contractRunners
        List<NPSContract> contractList = NPSGlobalState.getContractManager().getContractByDescriptionContains(String.format("%s-%s", delta.getReferenceVersion(), delta.getId()));        
        // commitSetup
        if (contractList != null) {
            for (NPSContract newContract : contractList) {
                try {
                    if (newContract.getStatus().equals("PREPARING")) {
                        if (newContract.getDeviceProvisionSequence() == null || newContract.getCustomerSTPs() == null) {
                            try {
                                NPSGlobalState.getContractManager().restoreContract(newContract);
                            } catch (Exception ex) {
                                throw new InternalServerErrorException(String.format("Contract %s cannot be restored", newContract.getId()));
                            }
                        }
                        NPSGlobalState.getContractManager().commitSetup(newContract);
                    }
                } catch (ServiceException ex) {
                    delta.setStatus("COMMIT_FAILED");
                    NPSGlobalState.getDeltaStore().update(delta);
                    throw new InternalServerErrorException(String.format("Failed to commit delta when provisoning sub-level contract '%s'", newContract.getId()));
                }
            }
        }
        delta.setStatus("COMMITTED");
        NPSGlobalState.getDeltaStore().update(delta);
        return delta.getStatus();
    }
    
    List<NPSContract> lookupL2L3ContractsFromOntModel(OntModel model) {
        List<NPSContract> allContracts = NPSGlobalState.getContractManager().getAll();
        List<NPSContract> contractList = new ArrayList<NPSContract>();
        // find all SwitchingSubnet elements
        StmtIterator stmts = model.listStatements(null, RdfOwl.type, Mrs.SwitchingSubnet);
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            Resource stmtSubject = stmt.getSubject();
            String l2SubnetUri = stmtSubject.getURI();
            if (l2SubnetUri.endsWith(":vlan")) {
                l2SubnetUri = l2SubnetUri.substring(0, l2SubnetUri.length()-5);
            }
            NPSContract aContract = null;
            synchronized (allContracts) {
                Iterator<NPSContract> itc = allContracts.iterator();
                while (itc.hasNext()) {
                    NPSContract contract = itc.next();
                    if (contract.getId().equals(l2SubnetUri)) {
                        aContract = contract;
                        break;
                    }
                    String[] uriFields = l2SubnetUri.split(":");
                    if (uriFields.length > 1 && contract.getId().endsWith(":" + uriFields[uriFields.length - 1])) {
                        aContract = contract;
                        break;
                    }
                }
            }
            if (aContract != null) {
                if (aContract.getDeviceProvisionSequence() == null || aContract.getCustomerSTPs() == null) {
                    try {
                        NPSGlobalState.getContractManager().restoreContract(aContract);
                    } catch (Exception ex) {
                        throw new InternalServerErrorException(String.format("Contract %s cannot be restored", aContract.getId()));
                    }
                }
                contractList.add(aContract); 
            }
        }
        // find all Route elements
        stmts = model.listStatements(null, RdfOwl.type, Mrs.Route);
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            Resource stmtSubject = stmt.getSubject();
            String l3RouteUri = stmtSubject.getURI();
            NPSContract aContract = null;
            synchronized (allContracts) {
                Iterator<NPSContract> itc = allContracts.iterator();
                while (itc.hasNext()) {
                    NPSContract contract = itc.next();
                    if (l3RouteUri.startsWith(contract.getId())) {
                        aContract = contract;
                        break;
                    }
                    String[] uriFields = l3RouteUri.split(":");
                    if (uriFields.length > 2 && (uriFields[uriFields.length-1].equalsIgnoreCase("p2c") || uriFields[uriFields.length-1].equalsIgnoreCase("c2p")) 
                            && contract.getId().endsWith(":"+uriFields[uriFields.length-2])) {
                        aContract = contract;
                        break;
                    }
                }
            }
            if (aContract != null && !contractList.contains(aContract)) {
                if (aContract.getDeviceProvisionSequence() == null || aContract.getCustomerSTPs() == null) {
                    try {
                        NPSGlobalState.getContractManager().restoreContract(aContract);
                    } catch (Exception ex) {
                        throw new InternalServerErrorException(String.format("Contract %s cannot be restored", aContract.getId()));
                    }
                }
                contractList.add(aContract);
            }
        }
        return contractList;
    }

    List<ServiceContract> createL2L3ContractsFromOntModel(OntModel model, String contractIdSuffix) {
        List<ServiceContract> serviceContractList= new ArrayList<ServiceContract>();
        // find all SwitchingSubnet elements
        StmtIterator stmts = model.listStatements(null, RdfOwl.type, Mrs.SwitchingSubnet);
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            Resource resSubnet = stmt.getSubject();
            String l2SubnetUri = resSubnet.getURI();
            List<ServiceTerminationPoint> stpList = new ArrayList();
            StmtIterator subIfStmts = model.listStatements(resSubnet, Nml.hasBidirectionalPort, (RDFNode)null);
            while (subIfStmts.hasNext()) {
                Statement subIfStmt = subIfStmts.next();
                Resource resSubIf = subIfStmt.getObject().asResource();
                ServiceTerminationPoint stp = new ServiceTerminationPoint();
                String subIfUrn = NPSUtils.extractSubInterfaceUrn(resSubIf.getURI());
                stp.setId(subIfUrn);    
                stp.setInterfaceRef(resSubIf.getURI());
                StmtIterator subIfLabelStmts = model.listStatements(resSubIf, Nml.hasLabel, (RDFNode)null);
                Resource subIfLabel = null;
                while (subIfLabelStmts.hasNext()) {
                    subIfLabel = subIfLabelStmts.next().getObject().asResource();
                    Resource labelType = subIfLabel.getPropertyResourceValue(Nml.labeltype);
                    if (labelType != null && labelType.getURI().equals("http://schemas.ogf.org/nml/2012/10/ethernet#vlan")) {
                        break;
                    }
                    subIfLabel = null;
                }
                if (subIfLabel == null) {
                    throw new BadRequestException(String.format("Subnet interface %s has no Ethernet VLAN label", resSubIf.getURI()));
                }
                
                StmtIterator subIfLabelValueStmts = model.listStatements(subIfLabel, Nml.value, (String)null);
                if (!subIfLabelValueStmts.hasNext()){
                    throw new BadRequestException(String.format("Subnet interface %s has Ethernet VLAN label without value", resSubIf.getURI()));
                }
                String labelValue = subIfLabelValueStmts.next().getObject().asLiteral().getString();
                Layer2Info l2info = new Layer2Info();
                VlanTag vlanTag = new VlanTag();
                vlanTag.setTagged(labelValue.equalsIgnoreCase("untagged") ||  labelValue.equalsIgnoreCase("0") || labelValue.equalsIgnoreCase("-1") ? false: true);
                vlanTag.setValue(labelValue);
                l2info.setOuterVlanTag(vlanTag);
                stp.setLayer2Info(l2info);
                stpList.add(stp);
            }
            if (stpList.size() < 2) {
                throw new BadRequestException(String.format("Subnet %s contains fewer than 2 interfaces", resSubnet.getURI()));
            }
            ServiceContract serviceContract = new ServiceContract();
            serviceContract.setId(l2SubnetUri+contractIdSuffix);
            serviceContract.getCustomerSTP().addAll(stpList);
            serviceContract.setType("dcn-layer2"); //$$ for now
            serviceContractList.add(serviceContract);
        }
        // find all Route elements
        List<HashMap<String, String>> routeList = new ArrayList();
        stmts = model.listStatements(null, RdfOwl.type, Mrs.Route);
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            Resource resRoute = stmt.getSubject();
            StmtIterator routeItemStmts = model.listStatements(resRoute, null, (RDFNode) null);
            HashMap<String, String> routeMap = new HashMap();
            routeMap.put("uri", resRoute.getURI());
            routeList.add(routeMap);
            while (routeItemStmts.hasNext()) {
                Statement routeItemStmt = routeItemStmts.next();
                Resource routeItemS = routeItemStmt.getSubject();
                Property routeItemP = routeItemStmt.getPredicate();
                Resource routeItemO = routeItemStmt.getObject().asResource();
                if (model.listStatements(routeItemO, RdfOwl.type, Mrs.NetworkAddress).hasNext()) {
                    StmtIterator netAddrStmts = model.listStatements(routeItemO, Mrs.type, (String) null);
                    if (netAddrStmts.hasNext()) {
                        String netAddrType = routeItemP + ":" + netAddrStmts.next().getObject().asLiteral().getString();
                        netAddrStmts = model.listStatements(routeItemO, Mrs.value, (String) null);
                        if (netAddrStmts.hasNext()) {
                            String netAddrValue = netAddrStmts.next().getObject().asLiteral().getString();
                            routeMap.put(netAddrType, netAddrValue);
                        }
                    }
                } else if (model.listStatements(routeItemO, RdfOwl.type, Nml.BidirectionalPort).hasNext()) {
                    routeMap.put(routeItemP + ":port", routeItemO.getURI());
                }
            }
        }
        // pair up routes
        Iterator<HashMap<String, String>> iterRoute = routeList.iterator();
        while (iterRoute.hasNext()) {
            HashMap<String, String> routeMap = iterRoute.next();
            if (routeMap.containsKey("paired")) {
                continue;
            }
            Iterator<HashMap<String, String>> iterRoute2 = routeList.iterator();
            while (iterRoute2.hasNext()) {
                HashMap<String, String> routeMap2 = iterRoute2.next();
                if (routeMap2 == routeMap)
                    continue;
                if (routeMap2.containsKey("paired"))
                    continue;
                // check paring critera
                if (routeMap.containsKey(Mrs.routeFrom+":bgp-remote-ip") && routeMap2.containsKey(Mrs.nextHop+":bgp-remote-ip") 
                    && routeMap.get(Mrs.routeFrom+":bgp-remote-ip").equals(routeMap2.get(Mrs.nextHop+":bgp-remote-ip"))
                    && routeMap.containsKey(Mrs.nextHop+":bgp-remote-ip") && routeMap2.containsKey(Mrs.routeFrom+":bgp-remote-ip") 
                    && routeMap.get(Mrs.nextHop+":bgp-remote-ip").equals(routeMap2.get(Mrs.routeFrom+":bgp-remote-ip"))) {
                    // collect l2info
                    if (!routeMap.containsKey(Mrs.routeFrom+":port") || !routeMap2.containsKey(Mrs.routeFrom+":port"))
                        continue;
                    Resource resIf1 = model.getResource(routeMap.get(Mrs.routeFrom+":port"));
                    // $$ TODO: what if the customer sub-interface (If2) has existed
                    // $$ add refOntModel as another parameter of this method. Then If1 refers to AWS provider side!
                    Resource resIf2 = model.getResource(routeMap2.get(Mrs.routeFrom+":port"));
                    if (resIf1 == null || resIf2 == null)
                        continue;
                    Resource resIf1Label = resIf1.getPropertyResourceValue(Nml.hasLabel);
                    Resource resIf2Label = resIf2.getPropertyResourceValue(Nml.hasLabel);
                    if (resIf1Label == null || resIf2Label == null)
                        continue;
                    Resource resIf1LabelType = resIf1Label.getPropertyResourceValue(Nml.labeltype);
                    Statement resIf1LabelValue = resIf1Label.getProperty(Nml.value);
                    Resource resIf2LabelType = resIf2Label.getPropertyResourceValue(Nml.labeltype);
                    Statement resIf2LabelValue = resIf2Label.getProperty(Nml.value);
                    if (resIf1LabelType == null || resIf1LabelValue == null || resIf2LabelType == null || resIf2LabelValue == null)
                        continue;
                    VlanTag vlanTag1 = new VlanTag();
                    if (resIf1LabelType.getURI().equals("http://schemas.ogf.org/nml/2012/10/ethernet#vlan")) {
                        String labelValue = resIf1LabelValue.getObject().asLiteral().getString();
                        vlanTag1.setTagged(labelValue.equalsIgnoreCase("untagged") ||  labelValue.equalsIgnoreCase("0") || labelValue.equalsIgnoreCase("-1") ? false: true);
                        vlanTag1.setValue(labelValue);
                    }
                    Layer2Info l2info1 = new Layer2Info();
                    l2info1.setOuterVlanTag(vlanTag1);
                    VlanTag vlanTag2 = new VlanTag();
                    if (resIf2LabelType.getURI().equals("http://schemas.ogf.org/nml/2012/10/ethernet#vlan")) {
                        String labelValue = resIf2LabelValue.getObject().asLiteral().getString();
                        vlanTag2.setTagged(labelValue.equalsIgnoreCase("untagged") ||  labelValue.equalsIgnoreCase("0") || labelValue.equalsIgnoreCase("-1") ? false: true);
                        vlanTag2.setValue(labelValue);
                    }
                    Layer2Info l2info2 = new Layer2Info();
                    l2info2.setOuterVlanTag(vlanTag2);
                    // create L3 ServiceContract
                    ServiceContract serviceContract = new ServiceContract();
                    serviceContract.setId(routeMap.get("uri")+contractIdSuffix);
                    serviceContract.setType("aws-layer3");
                    HashMap<String, String> routeP2C = routeMap;
                    HashMap<String, String> routeC2P = routeMap2;
                    Layer2Info l2infoP = l2info1;
                    Layer2Info l2infoC = l2info2;
                    // identify provider facing interface
                    Statement if2NameStmt = resIf2.getProperty(Nml.name);
                    if (NPSConfigYaml.getInstance().getNPSGlobalConfig().isProviderFacingInterface(NPSUtils.extractInterfaceUrn(resIf2.getURI()))
                        || (if2NameStmt != null && if2NameStmt.getObject().asLiteral().getString().toLowerCase().contains("aws"))) {
                        routeP2C = routeMap2;
                        routeC2P = routeMap;
                        l2infoP = l2info2;
                        l2infoC = l2info1;
                    }
                    // create provider STP
                    ServiceTerminationPoint stpProvider = new ServiceTerminationPoint();
                    String ifUrnP = NPSUtils.extractInterfaceUrn(routeP2C.get(Mrs.routeFrom+":port"));
                    stpProvider.setId(ifUrnP);
                    stpProvider.setInterfaceRef(routeP2C.get(Mrs.routeFrom+":port"));
                    stpProvider.setLayer2Info(l2infoP);
                    Layer3Info l3infoP = new Layer3Info();
                    BgpInfo bgpInfoP = new BgpInfo();
                    bgpInfoP.setGroupName(null);
                    String remoteIpPrefix = routeP2C.get(Mrs.routeFrom+":bgp-remote-ip");
                    bgpInfoP.setLinkRemoteIpAndMask(remoteIpPrefix);
                    String localIpPrefix = NPSUtils.getSlash30Peer(remoteIpPrefix);
                    bgpInfoP.setLinkLocalIpAndMask(localIpPrefix);
                    bgpInfoP.setPeerASN(routeC2P.get(Mrs.routeTo+":bgp-asn"));
                    String[] bgpPrefixeArray = routeP2C.get(Mrs.routeFrom+":bgp-prefix-list").split(",");
                    bgpInfoP.getPeerIpPrefix().addAll(Arrays.asList(bgpPrefixeArray));
                    l3infoP.setBgpInfo(bgpInfoP);
                    stpProvider.setLayer3Info(l3infoP);
                    // create customer STP
                    ServiceTerminationPoint stpCustomer = new ServiceTerminationPoint();
                    String ifUrnC = NPSUtils.extractInterfaceUrn(routeC2P.get(Mrs.routeFrom+":port"));
                    stpCustomer.setId(ifUrnC);
                    stpCustomer.setInterfaceRef(routeC2P.get(Mrs.routeFrom+":port"));
                    stpCustomer.setLayer2Info(l2infoC);
                    Layer3Info l3infoC = new Layer3Info();
                    BgpInfo bgpInfoC = new BgpInfo();
                    bgpInfoC.setGroupName(null);
                    remoteIpPrefix = routeC2P.get(Mrs.routeFrom+":bgp-remote-ip");
                    bgpInfoC.setLinkRemoteIpAndMask(remoteIpPrefix);
                    localIpPrefix = NPSUtils.getSlash30Peer(remoteIpPrefix);
                    bgpInfoC.setLinkLocalIpAndMask(localIpPrefix);
                    bgpInfoC.setPeerASN(routeP2C.get(Mrs.routeTo+":bgp-asn"));
                    bgpPrefixeArray = routeC2P.get(Mrs.routeFrom+":bgp-prefix-list").split(",");
                    bgpInfoC.getPeerIpPrefix().addAll(Arrays.asList(bgpPrefixeArray));
                    l3infoC.setBgpInfo(bgpInfoC);
                    stpCustomer.setLayer3Info(l3infoC);
                    serviceContract.setProviderSTP(stpProvider);
                    serviceContract.getCustomerSTP().add(stpCustomer);
                    // remove both routes from list
                    routeMap.put("paired", "");
                    routeMap2.put("paired", "");
                    serviceContractList.add(serviceContract);
                    break;
                }
            }
        }
        iterRoute = routeList.iterator();
        while (iterRoute.hasNext()) {
            HashMap<String, String> routeMap = iterRoute.next();
            if (!routeMap.containsKey("paired")) {
                throw new BadRequestException(String.format("Route '%s' cannot be paired up and translated into L3 contract", routeMap.get("uri")));
            }
        }
        return serviceContractList;
    }

}
