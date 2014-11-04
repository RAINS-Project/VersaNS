package net.maxgigapop.versans.nps.api.impl;


import javax.annotation.PostConstruct;
import javax.jws.WebService;

import java.util.List;

import net.maxgigapop.versans.nps.api.NPServicePortType;
import net.maxgigapop.versans.nps.api.ServiceException;
import net.maxgigapop.versans.nps.api.ServicePolicy;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.manager.NPSContractManager;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
import net.maxgigapop.versans.nps.manager.PolicyManager;
import net.maxgigapop.versans.nps.manager.TopologyManager;

@WebService(serviceName = "NPService", endpointInterface = "net.maxgigapop.versans.nps.api.NPServicePortType", targetNamespace = "http://maxgigapop.net/versans/nps/api/")
public class NPServicePortTypeImpl implements NPServicePortType {
	
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
    
    public void modify(
			javax.xml.ws.Holder<java.lang.String> transactionId,
			java.lang.String description,
			net.maxgigapop.versans.nps.api.ServiceContract serviceContract,
			javax.xml.ws.Holder<java.lang.String> status,
			javax.xml.ws.Holder<java.lang.String> message)
			throws ServiceException {
		throw new java.lang.UnsupportedOperationException("NPService "
                + this.getClass().getName() + "#modify is not supported yet.");
	}

	public void query(javax.xml.ws.Holder<java.lang.String> transactionId,
			java.lang.String contractId,
			javax.xml.ws.Holder<java.lang.String> status,
			javax.xml.ws.Holder<java.lang.String> message)
			throws ServiceException {
        try {
            status.value = NPSGlobalState.getContractManager().handleQuery(contractId);
            message.value = "";
        } catch (ServiceException se) {
            status.value = "FAILED";
            message.value = se.getMessage();
        }
        return;
	}

	public void setup(
			javax.xml.ws.Holder<java.lang.String> transactionId,
			java.lang.String description,
			net.maxgigapop.versans.nps.api.ServiceContract serviceContract,
			javax.xml.ws.Holder<java.lang.String> status,
			javax.xml.ws.Holder<java.lang.String> message)
			throws ServiceException {
        String contractId = serviceContract.getId();
        String contractType = serviceContract.getType();
        ServiceTerminationPoint providerSTP = serviceContract.getProviderSTP();
        List<ServiceTerminationPoint> customerSTPs = serviceContract.getCustomerSTP();
        List<ServicePolicy> policies = serviceContract.getPolicyData();
        try {
            status.value = NPSGlobalState.getContractManager().handleSetup(contractId, 
                contractType, description, providerSTP, customerSTPs, policies);
            message.value = "";
        } catch (ServiceException se) {
            status.value = "FAILED";
            message.value = se.getMessage();
        }
		return;
	}

	public void teardown(javax.xml.ws.Holder<java.lang.String> transactionId,
			java.lang.String contractId,
			javax.xml.ws.Holder<java.lang.String> status,
			javax.xml.ws.Holder<java.lang.String> message)
			throws ServiceException {
        try {
            status.value = NPSGlobalState.getContractManager().handleTeardown(contractId);
            message.value = "";
        } catch (ServiceException se) {
            status.value = "FAILED";
            message.value = se.getMessage();
        }
		return;
	}
}
