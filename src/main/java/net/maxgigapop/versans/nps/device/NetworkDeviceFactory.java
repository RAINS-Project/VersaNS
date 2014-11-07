/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import net.maxgigapop.versans.nps.manager.NPSUtils;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.api.ServiceException;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.*;
        
/**
 *
 * @author xyang
 */
public class NetworkDeviceFactory {
    private static NetworkDeviceFactory singleton = null;
    private org.apache.log4j.Logger log;
    private List<NetworkDeviceInstance> deviceInstances = null;
    
    private NetworkDeviceFactory() {
        log = org.apache.log4j.Logger.getLogger(this.getClass());
        deviceInstances = new ArrayList<NetworkDeviceInstance>();
    }
    
    public static NetworkDeviceFactory getFactory() {
        if (NetworkDeviceFactory.singleton == null)
            NetworkDeviceFactory.singleton = new NetworkDeviceFactory();
        return NetworkDeviceFactory.singleton;
    }

    public List<NetworkDeviceInstance> getAll() {
        return deviceInstances;
    }

    public NetworkDeviceInstance create(String contractId, Device deviceRef) 
                throws ServiceException {
        NetworkDeviceInstance ndi = null;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> aClass = cl.loadClass(deviceRef.getMakeModel());
            ndi = (NetworkDeviceInstance)aClass.newInstance();
        } catch (Exception ex) {
            throw new ServiceException("NetworkDeviceFactory.create cannot instantiate device makeModel="+deviceRef.getMakeModel());
        }
        ndi.setContractId(contractId);
        ndi.setDeviceRef(deviceRef); 
        ndi.setStatus("IDLE");
        ndi.setLastStatus("IDLE");
        synchronized (deviceInstances) {
            deviceInstances.add(ndi);
            return ndi;
        }        
    }
            
    public NetworkDeviceInstance create(String contractId, ServiceTerminationPoint stp) 
            throws ServiceException {
        NetworkDeviceInstance ndi = this.lookup(contractId, stp);
        if (ndi != null) {
            boolean isNew = true;
            for (ServiceTerminationPoint si: ndi.getLocalSTPs()) {
                if (si.getId().equalsIgnoreCase(stp.getId())) {
                    isNew = false;
                    break;
                }
            }
            if (isNew)
                ndi.getLocalSTPs().add(stp);
            return ndi;
        }
        String ifUrn = NPSUtils.extractInterfaceUrn(stp.getId());
        Interface stpIf = NPSGlobalState.getInterfaceStore().getByUrn(ifUrn);
        if (stpIf == null) {
            throw new ServiceException("NetworkDeviceFactory.create malformed STP: "+ifUrn);
        }
        Device deviceRef = NPSGlobalState.getDeviceStore().getById(stpIf.getDeviceId());
        if (deviceRef == null) {
            throw new ServiceException("NetworkDeviceFactory.create cannot find device by interface: "+ifUrn);
        }
        ndi = create(contractId, deviceRef);
        ndi.getLocalSTPs().add(stp);
        return ndi;
    }
    
    public void delete(String contractId) {
        synchronized (deviceInstances) {
            for (NetworkDeviceInstance ndi: deviceInstances) {
                if (ndi.getContractId().equalsIgnoreCase(contractId)) {
                    deviceInstances.remove(ndi);
                }
            }
        }
    }

    public NetworkDeviceInstance lookup(String contractId, ServiceTerminationPoint stp) {
        synchronized (deviceInstances) {
            String ifUrn = NPSUtils.extractInterfaceUrn(stp.getId());
            Interface stpIf = NPSGlobalState.getInterfaceStore().getByUrn(ifUrn);
            if (stpIf == null) {
                return null;
            }
            for (NetworkDeviceInstance ndi: deviceInstances) {
                if (ndi.getContractId().equalsIgnoreCase(contractId) 
                        && stp != null && ndi.getDeviceRef() != null
                        && stpIf.getDeviceId() == ndi.getDeviceRef().getId()) {
                    return ndi;
                }
            }
        }
        return null;
    }

    public List<NetworkDeviceInstance> lookupByContract(String contractId) {
        List<NetworkDeviceInstance> ndiList = new ArrayList<NetworkDeviceInstance>();
        synchronized (deviceInstances) {
            for (NetworkDeviceInstance ndi: deviceInstances) {
                if (ndi.getContractId().equalsIgnoreCase(contractId)) {
                    ndiList.add(ndi);
                }
            }
        }
        if (ndiList.isEmpty())
            return null;
        return ndiList;
    }

    public List<NetworkDeviceInstance> lookupBySTP(ServiceTerminationPoint stp) {
        List<NetworkDeviceInstance> ndiList = new ArrayList<NetworkDeviceInstance>();
        synchronized (deviceInstances) {
            for (NetworkDeviceInstance ndi: deviceInstances) {
                String ifUrn = NPSUtils.extractInterfaceUrn(stp.getId());
                Interface stpIf = NPSGlobalState.getInterfaceStore().getByUrn(ifUrn);
                if (stpIf == null) {
                    return null;
                }
                if (stp != null && ndi.getDeviceRef() != null
                        && stpIf.getDeviceId() == ndi.getDeviceRef().getId()) {
                    ndiList.add(ndi);
                }
            }
        }
        if (ndiList.isEmpty())
            return null;
        return ndiList;
    }
}
