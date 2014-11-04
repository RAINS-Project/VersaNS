/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.manager;

import net.maxgigapop.versans.nps.device.DeviceDelta;
import net.maxgigapop.versans.nps.device.DeviceException;
import net.maxgigapop.versans.nps.device.NetworkDeviceInstance;
import net.maxgigapop.versans.nps.api.ServiceException;

import java.util.*;
import org.apache.log4j.*;

/**
 *
 * @author xyang
 */
public class NPSContractRunner extends Thread {
    private volatile boolean goRun = true;
    private volatile boolean goPoll = true;
    private volatile int pollInterval = 30000; //30 secs by default
    // TODO: timeout configs
    private volatile boolean reloaded = false; //set true when reloaded from DB
    private volatile boolean goRetry = false;
    private NPSContract contract;
    private NPSContractManager manager;
    private org.apache.log4j.Logger log;


    private NPSContractRunner() {}

    public NPSContractRunner(NPSContractManager manager, NPSContract contract) {
        super();
        this.manager = manager;
        this.contract = contract;
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }

    public NPSContract getContract() {
        return contract;
    }

    public void setContract(NPSContract contract) {
        this.contract = contract;
    }

    public NPSContractManager getManager() {
        return manager;
    }

    public void setManager(NPSContractManager manager) {
        this.manager = manager;
    }

    public synchronized boolean isGoPoll() {
        return goPoll;
    }

    public synchronized void setGoPoll(boolean goPoll) {
        this.goPoll = goPoll;
    }

    public synchronized boolean isGoRun() {
        return goRun;
    }

    public synchronized void setGoRun(boolean goRun) {
        this.goRun = goRun;
    }

    public synchronized NPSContract getRspec() {
        return contract;
    }

    public synchronized void setRspec(NPSContract contract) {
        this.contract = contract;
    }

    public synchronized int getPollInterval() {
        return pollInterval;
    }

    public synchronized void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public boolean isReloaded() {
        return reloaded;
    }

    public void setReloaded(boolean reloaded) {
        this.reloaded = reloaded;
    }

    public boolean isGoRetry() {
        return goRetry;
    }

    public void setGoRetry(boolean goRetry) {
        this.goRetry = goRetry;
    }
    
    @Override
    public void run() {
        if (this.contract.getDeviceProvisionSequence().isEmpty()) {
            contract.setStatus("FAILED");
            contract.setError("NPSContractRunner fail to map service (contractId="
                    + contract.getId() + ") onto devices.");
            contract.setDeleted(true);
            return;
        }
        
        if (!reloaded) {
            provision();
        } else {
            if (contract.getStatus().equalsIgnoreCase("ROLLBACKING")) {
                this.rollback();
            }
        }
                
        while (goRun) {
            try {
                this.sleep(pollInterval); //30 secs
            } catch (InterruptedException e) {
                if (!goRun) {
                    break;
                }
            }
            if (goRetry) {
                provision();
                goRetry = false;
            }
            if (goRun && goPoll) {
                try {
                    // poll deivces
                    String status = "ACTIVE";
                    for (NetworkDeviceInstance ndi: contract.getDeviceProvisionSequence()) {
                        String ndiStatus = ndi.verifyDelta();
                        if (!ndiStatus.equalsIgnoreCase("ACTIVE")) {
                            status = "UNKNOWN";
                            // continue; // keep polling
                        }
                    }
                    
                    // update db
                    if (status.equalsIgnoreCase("ACTIVE")) {
                        contract.setStatus(status);
                        try {
                            manager.updateContract(contract);
                        } catch (ServiceException ex) {
                            contract.setStatus("FAILED");
                            contract.setError("fail to update database with ACTIVE status");
                            goPoll = false; // no more polling
                        }
                    }
                } catch (DeviceException e) {
                    e.printStackTrace();
                    if (contract.getStatus().equalsIgnoreCase("ACTIVE")) {
                        // verifyDelta failed on an already 'ACTIVE' contract
                        log.error("NPSContractRunner  (contractId=" + contract.getId()
                                + ") fell back from 'ACTIVE' due to network abnormlity");
                        rollback();
                    } else{
                        log.error("NPSContractRunner (contractId=" + contract.getId()
                                + ") Exception:" + e.getMessage());
                        contract.setStatus("FAILED");
                        contract.setError("fail to setup (contractId=" + contract.getId()
                                + ") Exception:" + e.getMessage());
                        try {
                            manager.updateContract(contract);
                        } catch (ServiceException ex) {
                            ;
                        }
                        goRun = false;
                    }
                } 
            }
        }

        terminate();
    }

    public void provision() {
        log.debug("NPSContractRunner.provision - setup start (contractId=" + contract.getId() + ")");
        goPoll = true;
        for (NetworkDeviceInstance ndi : this.contract.getDeviceProvisionSequence()) {
            try {
                ndi.applyDelta();
                ndi.getDelta().setDeleted(false);
                ndi.getDelta().setContractId(this.contract.getId());
                ndi.getDelta().setDeviceId(ndi.getDeviceRef().getId());
                ndi.getDelta().setDeleted(false);
                //!! for now, applying only occurs once 
                NPSGlobalState.getDeviceDeltaStore().add(ndi.getDelta());
            } catch (DeviceException e) {
                e.printStackTrace();
                contract.setStatus("ROLLBACKING");
                try {
                    manager.updateContract(contract);
                } catch (ServiceException ex) {
                    ;
                }
                this.rollback();
                goPoll = false;
            }
        }
        log.debug("NPSContractRunner.provision - setup end");
    }

    public void rollback() {
        log.debug("NPSContractRunner.rollback - start (contractId=" + contract.getId() + ")");
        for (NetworkDeviceInstance ndi : this.contract.getDeviceProvisionSequence()) {
            if (!ndi.getStatus().equalsIgnoreCase("IDLE")
                    && !ndi.getStatus().equalsIgnoreCase("DELETING")) {
                try {
                    DeviceDelta delta = ndi.getDelta();
                    if (delta != null) {
                        ndi.deleteDelta();
                        delta.setDeleted(true);
                        NPSGlobalState.getDeviceDeltaStore().update(ndi.getDelta());
                    }
                } catch (DeviceException e) {
                    e.printStackTrace();
                }
            }
        }
        contract.setStatus("ROLLBACKED");
        try {
            manager.updateContract(contract);
        } catch (ServiceException ex) {
            contract.setStatus("FAILED");
            contract.setError("fail to update database with ROLLBACKED status");
        }
        log.debug("NPSContractRunner.rollback - end");
    }

    public void terminate() {
        log.debug("NPSContractRunner.terminate - start (contractId="+ contract.getId()+")");
        for (NetworkDeviceInstance ndi : this.contract.getDeviceProvisionSequence()) {
            if (!ndi.getStatus().equalsIgnoreCase("IDLE")
                    && !ndi.getStatus().equalsIgnoreCase("DELETING")) {
                try {
                    DeviceDelta delta = ndi.getDelta();
                    if (delta != null) {
                        ndi.deleteDelta();
                        delta.setDeleted(true);
                        NPSGlobalState.getDeviceDeltaStore().update(delta);
                    }
                } catch (DeviceException e) {
                    e.printStackTrace();
                }
            }
        }
        contract.setStatus("TERMINATED");
        contract.setDeleted(true);
        try {
            manager.updateContract(contract);
        } catch (ServiceException ex) {
            contract.setStatus("FAILED");
            contract.setError("fail to update database with TERMINATED status");
        }
        goRun = false;
        goPoll = false;
        log.debug("NPSContractRunner.terminate - end");
    }
    
    // TODO: verify terminated (delta has been removed from device)!
}
