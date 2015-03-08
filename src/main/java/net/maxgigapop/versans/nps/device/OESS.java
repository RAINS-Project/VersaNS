/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.maxgigapop.versans.nps.api.ServicePolicy;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.device.floodlight.Flow;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
import net.maxgigapop.versans.nps.manager.NPSUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author Chen Chen
 */
public class OESS implements NetworkDeviceInstance {
    protected String contractId = "";
    protected Device deviceRef = null;
    protected String status = "";
    protected String lastStatus = "";
    protected List<ServiceTerminationPoint> localSTPs = new ArrayList<ServiceTerminationPoint>();
    protected List<ServicePolicy> localPolicies = new ArrayList<ServicePolicy>();
    protected DeviceDelta delta = null;
    private org.apache.log4j.Logger log;
    
    public OESS () {
        this.log = org.apache.log4j.Logger.getLogger(this.getClass());
    }
    @Override
    public String getContractId(){
        return contractId;
    }
    
    @Override
    public void setContractId(String id){
        this.contractId = id;
    }
    
    // reference to physical device
    @Override
    public Device getDeviceRef(){
        return deviceRef;
    }

    @Override
    public void setDeviceRef(Device deviceRef){
        this.deviceRef = deviceRef;
    }
    
    // current status (IDLE, APPLYING, APPLIED, DELETING,  
    // DELTED, ACTIVE, TIMEOUT, FAILED, UNKNOWN)
    @Override
    public String getStatus(){
        return status;
    }

    @Override
    public void setStatus(String status){
        this.status = status;
    }
    
    // last status
    @Override
    public String getLastStatus(){
        return lastStatus;
    }
    
    @Override
    public void setLastStatus(String status){
        this.lastStatus = status;
    }
    
    // local interfaces config
    @Override
    public List<ServiceTerminationPoint> getLocalSTPs(){
        if (localSTPs == null)
            localSTPs = new ArrayList<ServiceTerminationPoint>();
        return localSTPs;
    }

    // policy config
    @Override
    public List<ServicePolicy> getLocalPolicies(){
        if (localPolicies == null)
            localPolicies = new ArrayList<ServicePolicy>();
        return localPolicies;
    }
    
    @Override
    public void generateDelta() throws DeviceException {
        // May need to add verifycmd sometime in the future
        if (this.delta == null) {
            this.delta = NPSGlobalState.getDeviceDeltaStore().getByDeviceAndContractId(this.deviceRef.getId(), contractId);
        }
        if (this.delta == null) {
            // construct flows
            String applyCmd = "provisioning.cgi?action=provision_circuit&circuit_id=-1";
            applyCmd += "&description=circuit-for-contract-" + contractId + "&bandwidth=0&provision_time=-1&remove_time=-1&workgroup_id=1";
            for (ServiceTerminationPoint stp : localSTPs) {
                String deviceName = NPSUtils.getDcnUrnField(stp.getId(), "node");
                applyCmd += "&node=" + deviceName;
                String portName = NPSUtils.getDcnUrnField(stp.getId(), "port");
                applyCmd += "&interface=" + portName;
                String vlan = stp.getLayer2Info().getOuterVlanTag().getValue();
                applyCmd += "&tag=" + vlan;
            }
            this.delta = new DeviceDelta();
            this.delta.setCmdToApply(applyCmd);
        }
    }

    @Override
    public DeviceDelta getDelta() throws DeviceException {
        if (delta == null) {
            generateDelta();
        }
        return delta;
    }

    @Override
    public void setDelta(DeviceDelta delta) throws DeviceException {
        this.delta = delta;
    }

    @Override
    public void applyDelta() throws DeviceException {
        if (delta == null) {
            generateDelta();
        }
        transferStatus("APPLYING");
        synchronized(RESTConnector.simplyLock) {
            RESTConnector connector = RESTConnector.getRESTConnector();
            connector.setConfig(deviceRef.getConnectorConfig());
            try {
                String response = connector.addNewCircuit(this.delta.getCmdToApply());
                JSONObject jsonResponse = new JSONObject(response);
                JSONObject jsonResult = jsonResponse.getJSONObject("result");
                String deleteCmd = "provisioning.cgi?action=remove_circuit";
                if (jsonResult.getInt("success") != 1) {
                    transferStatus("FAILED");
                    return;
                }
                deleteCmd +="&circuit=" + jsonResult.getString("circuit_id") + "&remove_time=-1&workgroup_id=1";
                this.delta.setCmdToDelete(deleteCmd);
                
            } catch (JSONException e) {
                throw new DeviceException("JSONException caught in applyDelta: " + e.getMessage());
            }
        }
        transferStatus("APPLIED");
    }

    @Override
    public void deleteDelta() throws DeviceException {
        
    }

    @Override
    public String verifyDelta() throws DeviceException {
        return "";
    }
    
    private void transferStatus(String status) {
        this.lastStatus = this.status;
        this.status = status;
    }
}
