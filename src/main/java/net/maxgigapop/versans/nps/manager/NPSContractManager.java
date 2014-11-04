/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.manager;

import net.maxgigapop.versans.nps.device.DeviceDelta;
import net.maxgigapop.versans.nps.device.Device;
import net.maxgigapop.versans.nps.device.DeviceException;
import net.maxgigapop.versans.nps.device.NetworkDeviceInstance;
import net.maxgigapop.versans.nps.device.NetworkDeviceFactory;
import net.maxgigapop.versans.nps.device.Interface;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.api.ServicePolicy;
import net.maxgigapop.versans.nps.api.ServiceException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 *
 * @author xyang
 */
public class NPSContractManager extends Thread {

    private volatile boolean goRun = false;
    private List<NPSContract> npsContracts;
    private List<NPSContractRunner> npsRunnerThreads;
    private org.apache.log4j.Logger log;
    private static Session session;
    private static org.hibernate.Transaction tx;

    public NPSContractManager(){
        npsContracts = new ArrayList<NPSContract>();
        npsRunnerThreads = new ArrayList<NPSContractRunner>();
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }
    
    public boolean isGoRun() {
        return goRun;
    }

    public void setGoRun(boolean goRun) {
        this.goRun = goRun;
    }

    @Override
    public void run() {
        reloadFromDB();
        goRun = true;
        while (goRun) {
            //polling rspecThreads and rspecRspecs for status change
            synchronized(npsRunnerThreads) {
                for (NPSContractRunner runnerThread: npsRunnerThreads) {
                    NPSContract contract = runnerThread.getContract();
                    if (contract.getStatus().equalsIgnoreCase("ACTIVE")) {
                        //Contrcat provisioned in ACTIVE state, polling interval increased
                        if (runnerThread.getPollInterval() < NPSGlobalState.getExtendedPollInterval())
                            runnerThread.setPollInterval(NPSGlobalState.getExtendedPollInterval());
                    } else if (contract.getStatus().equalsIgnoreCase("TERMINATED")) {
                        // remove both thread and contract
                        npsRunnerThreads.remove(runnerThread);
                        npsContracts.remove(contract);
                        break;  // go loop again!
                    } else if (contract.getStatus().equalsIgnoreCase("ROLLBACKED")) {
                        // only remove thread. keep contract.
                        // TODO: retry - resurrect thread for rollbacked contract
                        //npsRunnerThreads.remove(runnerThread);
                        break;  // go loop again!
                    }
                    //give other instructions e.g., terminate on expires / scheduling
                }
            }
        }
    }
    
    public String handleSetup(String contractId, String contractType,
            String description, ServiceTerminationPoint providerSTP,
            List<ServiceTerminationPoint> customerSTPs,
            List<ServicePolicy> policies) throws ServiceException {
        log.info("NPSContractManager.handleSetup - start");
        
        NPSContract contract = this.getContractById(contractId);
        if (contract != null) {
            throw new ServiceException("Contract ID:" + contractId + " has already existed!");
        }

        if (providerSTP == null && customerSTPs.size() <2) {
            throw new ServiceException("Contract ID:" + contractId + " needs at least two STPs");
        }

        // constract and update NPSContract object 
        contract = new NPSContract();
        contract.setId(contractId);
        contract.setDescription(description);
        contract.setProviderSTP(providerSTP);       
        contract.setCustomerSTPs(customerSTPs);

        
        // call topologyManager to compute / expand P2P path
        TopologyManager topoManager = NPSGlobalState.getTopologyManager();
        List<ServiceTerminationPoint> path;
        if (providerSTP != null) {
            path = topoManager.computeP2PPath(providerSTP, customerSTPs.get(0));
        } else  {
            path = topoManager.computeP2PPath(customerSTPs.get(0), customerSTPs.get(1));
        }
        
        List<NetworkDeviceInstance> devInsSequence = null;
        if (contractType.equalsIgnoreCase("dcn-layer2")) {
            devInsSequence = new ArrayList<NetworkDeviceInstance>();
            String srcIfUrn = customerSTPs.get(0).getId();
            Interface srcIf = NPSGlobalState.getInterfaceStore().getByUrn(srcIfUrn);
            if (srcIf == null) {
                throw new ServiceException("Contract ID:" + contractId + " - undefined DCN source urn "+ srcIfUrn);
            }
            Device deviceRef = NPSGlobalState.getDeviceStore().getById(srcIf.getDeviceId());
            if (deviceRef == null) {
                throw new ServiceException("Contract ID:" + contractId + " - cannot find DCN device for urn="+srcIfUrn);
            }
            NetworkDeviceInstance ndi = NetworkDeviceFactory.getFactory().create(contractId, deviceRef);
            ndi.getLocalSTPs().addAll(contract.getCustomerSTPs());
            devInsSequence.add(ndi);
        } else if (contractType.equalsIgnoreCase("sdn-openflow")) {
            // Assume customer provides explit path in service request.
            // TODO: compute p2p and mp multi-hop paths using PCE. 
            path = topoManager.createExplicitPath(customerSTPs);
            devInsSequence = topoManager.createDeviceInstanceSequence(contractId, path);
        } else if (contractType.equalsIgnoreCase("aws-layer2") || contractType.equalsIgnoreCase("aws-layer3")) {
            devInsSequence = topoManager.createDeviceInstanceSequence(contractId, path);
        } else {
            throw new ServiceException("Contract ID:" + contractId + " - unsupoorted contract type "+ contractType);
        }
        
        contract.setDeviceProvisionSequence(devInsSequence);
        
        // call policyManager to refine policy statements
        PolicyManager policyManager = NPSGlobalState.getPolicyManager();
        List<ServicePolicy> refinedPolicies = policyManager.refineDeviceInstancePolicies(contractId, policies, devInsSequence);
        contract.setServicePolicies(refinedPolicies);

        contract.setStatus("PREPARING");
        
        // store contract object to DB
        this.addContract(contract);
        
        // construct and set off NPSContractRunner
        NPSContractRunner contractRunner = new NPSContractRunner(this, contract);
        synchronized(npsRunnerThreads) {
            npsRunnerThreads.add(contractRunner);
        }
        
        contractRunner.setPollInterval(NPSGlobalState.getPollInterval());
        contract.setStatus("STARTING");
        contractRunner.start();

        log.info("NPSContractManager.handleSetup - end");

        updateContract(contract);
        return contract.getStatus();
    }

    public String handleTeardown(String contractId) throws ServiceException {
        log.info("NPSContractManager.handleTeardown - start");
        NPSContract contract = this.getContractById(contractId);
        if (contract == null) {
            throw new ServiceException("Unknown contractId="+contractId);
        }
        synchronized(npsRunnerThreads) {
            for (NPSContractRunner runnerThread: npsRunnerThreads) {
                if (runnerThread.getContract() != null 
                        && runnerThread.getContract().getId().equalsIgnoreCase(contractId)
                        && !runnerThread.getContract().getStatus().equalsIgnoreCase("TERMINATED")
                        && !runnerThread.getContract().getStatus().equalsIgnoreCase("TERMINATING")) {
                    runnerThread.setGoRun(false);
                    runnerThread.interrupt();
                    log.info("NPSContractManager.handleTeardown - end");
                    contract.setStatus("TERMINATING");
                    return contract.getStatus();
                }
            }
        }
        throw new ServiceException("handleTeardown: No runner thread found for contract "+contractId);
    }

    public String handleQuery(String contractId) throws ServiceException {
        log.info("NPSContractManager.handleQuery - start");
        NPSContract contract = this.getContractById(contractId);
        if (contract == null) {
            throw new ServiceException("Unknown contractId="+contractId);
        }
        log.info("NPSContractManager.handleQuery - end");
        if (contract.getStatus().equalsIgnoreCase("FAILED")) {
            throw new ServiceException(contract.getError());
        }
        return contract.getStatus();
    }

    public void addContract(NPSContract contract) throws ServiceException {
        synchronized(npsContracts) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                session.save(contract);
                session.flush();
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                contract.setStatus("FAILED");
                throw new ServiceException("NPSContractManager.addContract (" 
                        + contract.getId() + ") failed for DB error: "
                        + e.getMessage());
            } finally {
                if (session.isOpen()) {
                    session.close();
                }
            }
            npsContracts.add(contract);
        }
    }

    public void updateContract(NPSContract contract) throws ServiceException {
        synchronized(npsContracts) {
            if (contract!= null || !contract.getId().isEmpty()) {
                try {
                    session = HibernateUtil.getSessionFactory().openSession();
                    tx = session.beginTransaction();
                    session.update(contract);
                    session.flush();
                    tx.commit();
                } catch (Exception e) {
                    tx.rollback();
                    log.error("NPSContractManager.updateContract (" 
                        + contract.getId() + ") status= " 
                            + contract.getStatus() +" exception=" + e.getMessage());
                } finally {
                    if (session.isOpen()) session.close();
                }
            } else {
                throw new ServiceException("NPSContractManager.updateContract: unknown contract");
            }
        }
    }

    public NPSContract getContractById(String cid) {
        synchronized (npsContracts) {
            for (NPSContract ct: npsContracts) {
                if (ct.getId().equalsIgnoreCase(cid)) {
                    return ct;
                }
            }
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from NPSContract as contract where" 
                        + " contract.id='" + cid + "'");
                if (q.list().size() > 0) {
                    npsContracts.add((NPSContract)q.list().get(0));//cached
                    return (NPSContract)q.list().get(0);
                }
            } catch (Exception e) {
                tx.rollback();
                e.printStackTrace();
            } finally {
                if (session.isOpen()) session.close();
            }
        }
        return null;
    }

    // contracts in PREPARING status are set to FAILED
    // resurrect threads for contracts in ACTIVE, STARTING and INSETUP status in goRun=true mode
    // contracts in TERMINATING status have thread with goRun=false 
    // contracts in ROLLBACKED status are reloaded in idle mode (thread not started)
    public void reloadFromDB() {
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            Query q = session.createQuery("from NPSContract as contract where" 
                    + " contract.deleted<>1");
            if (q.list().size() == 0) {
                return;
            }
            npsContracts = (List<NPSContract>)q.list();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            if (session.isOpen()) session.close();
        }

        Iterator<NPSContract> itCt = npsContracts.iterator();
        while(itCt.hasNext()) {
            NPSContract contract = itCt.next();
            try {
                restoreContract(contract);
            } catch (Exception ex) {
                contract.setError("reloadFromDB failed to resurrect contract from '"
                        + contract.getStatus() +"' status");
                contract.setStatus("FAILED");
                contract.setDeleted(true);
                try {
                    updateContract(contract);
                } catch (Exception e) {
                    ;
                }
                itCt.remove();
            } 
            if (contract.getStatus().equalsIgnoreCase("ACTIVE")
                    || contract.getStatus().equalsIgnoreCase("STARTING")
                    || contract.getStatus().equalsIgnoreCase("ROLLBACKING")
                    || contract.getStatus().equalsIgnoreCase("ROLLBACKED")
                    || contract.getStatus().equalsIgnoreCase("TERMINATING")) {
                NPSContractRunner contractRunner = new NPSContractRunner(this, contract);
                synchronized (npsRunnerThreads) {
                    npsRunnerThreads.add(contractRunner);
                }
                if (contract.getStatus().equalsIgnoreCase("TERMINATING"))
                    contractRunner.setGoRun(false);
                else if (contract.getStatus().equalsIgnoreCase("ROLLBACKED"))
                    contractRunner.setGoPoll(false);
                contractRunner.setPollInterval(NPSGlobalState.getPollInterval());
                contractRunner.setReloaded(true);
                contractRunner.start();
            }
            else if (contract.getStatus().equalsIgnoreCase("PREPARING")) {
                contract.setStatus("FAILED");
                contract.setError("reloadFromDB force 'FAILED' from status 'PREPARING'");
                try {
                    updateContract(contract);
                } catch (Exception ex) {
                    ;
                }
                itCt.remove();
            }
        }
    }
    
    public void restoreContract(NPSContract contract) throws DeviceException, ServiceException{
        List<DeviceDelta> deltaList = NPSGlobalState.getDeviceDeltaStore().getByContractId(contract.getId());
        for (DeviceDelta delta: deltaList) {
            Device device = NPSGlobalState.getDeviceStore().getById(delta.getDeviceId());
            NetworkDeviceInstance ndi = NetworkDeviceFactory.getFactory().create(contract.getId(), device);
            ndi.setStatus("UNKNOWN");
            ndi.setDelta(delta);
            if (contract.getDeviceProvisionSequence() == null) {
                contract.setDeviceProvisionSequence(new ArrayList<NetworkDeviceInstance>());
            }
            contract.getDeviceProvisionSequence().add(ndi);
        }
    }

}
