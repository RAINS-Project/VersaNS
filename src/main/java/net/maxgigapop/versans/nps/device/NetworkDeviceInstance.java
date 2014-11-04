/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import java.util.List;
import net.maxgigapop.versans.nps.api.ServicePolicy;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;

/**
 *
 * @author xyang
 */
public interface NetworkDeviceInstance {

    // contract Id
    public String getContractId();
    public void setContractId(String id);
    // reference to physical device
    public Device getDeviceRef();
    public void setDeviceRef(Device deviceRef);
    // current status (IDLE, APPLYING, APPLIED, DELETING,  
    // DELETED, ACTIVE, TIMEOUT, FAILED, UNKNOWN)
    public String getStatus();
    public void setStatus(String status);
    // last status
    public String getLastStatus();
    public void setLastStatus(String status);
    // local interfaces config
    public List<ServiceTerminationPoint> getLocalSTPs();
    // policy config
    public List<ServicePolicy> getLocalPolicies();
    // provisioning actions
    public void generateDelta() throws DeviceException;
    public DeviceDelta getDelta() throws DeviceException;
    public void setDelta(DeviceDelta delta) throws DeviceException;
    public void applyDelta() throws DeviceException;
    public void deleteDelta() throws DeviceException;
    public String verifyDelta() throws DeviceException;        
}
