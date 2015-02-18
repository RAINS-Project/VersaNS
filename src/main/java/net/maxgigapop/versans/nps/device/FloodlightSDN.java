/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.maxgigapop.versans.nps.api.ServicePolicy;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.device.floodlight.Flow;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
import net.maxgigapop.versans.nps.manager.NPSUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author xyang
 */
public class FloodlightSDN implements NetworkDeviceInstance {

    protected String contractId = "";
    protected Device deviceRef = null;
    protected String status = "";
    protected String lastStatus = "";
    protected List<ServiceTerminationPoint> localSTPs = new ArrayList<ServiceTerminationPoint>();
    protected List<ServicePolicy> localPolicies = new ArrayList<ServicePolicy>();
    protected DeviceDelta delta = null;
    private org.apache.log4j.Logger log;

    public FloodlightSDN() {
        this.log = org.apache.log4j.Logger.getLogger(this.getClass());
    }

    @Override
    public String getContractId() {
        return contractId;
    }

    @Override
    public void setContractId(String id) {
        this.contractId = id;
    }

    // reference to physical device
    @Override
    public Device getDeviceRef() {
        return deviceRef;
    }

    @Override
    public void setDeviceRef(Device deviceRef) {
        this.deviceRef = deviceRef;
    }

    // current status (IDLE, APPLYING, APPLIED, DELETING,  
    // DELTED, ACTIVE, TIMEOUT, FAILED, UNKNOWN)
    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    // last status
    @Override
    public String getLastStatus() {
        return lastStatus;
    }

    @Override
    public void setLastStatus(String status) {
        this.lastStatus = status;
    }

    // local interfaces config
    @Override
    public List<ServiceTerminationPoint> getLocalSTPs() {
        if (localSTPs == null) {
            localSTPs = new ArrayList<ServiceTerminationPoint>();
        }
        return localSTPs;
    }

    // policy config
    @Override
    public List<ServicePolicy> getLocalPolicies() {
        if (localPolicies == null) {
            localPolicies = new ArrayList<ServicePolicy>();
        }
        return localPolicies;
    }

    public String getDpidByName(String deviceName) {
        Map dpidMap = (Map)deviceRef.connectorConfig.get("dpid_map");
        if (dpidMap != null)
        {
            return (String)dpidMap.get(deviceName);
        }
        return null;
    }
    
    // provisioning actions
    @Override
    public void generateDelta() throws DeviceException {
        if (this.delta == null) {
            this.delta = NPSGlobalState.getDeviceDeltaStore().getByDeviceAndContractId(this.deviceRef.getId(), contractId);
        }
        if (this.delta == null) {
            // construct flows
            HashMap<String, HashMap<String, String>> switchPortVlanMap = new HashMap<String, HashMap<String, String>>();
            for (ServiceTerminationPoint stp : localSTPs) {
                String deviceName = NPSUtils.getDcnUrnField(stp.getId(), "node");
                String dpid = this.getDpidByName(deviceName);
                if (dpid == null)
                    continue;
                HashMap<String, String> portVlanMap = switchPortVlanMap.get(dpid);
                if (portVlanMap == null) {
                    portVlanMap = new HashMap<String, String>();
                    switchPortVlanMap.put(dpid, portVlanMap);
                }
                String portName = NPSUtils.getDcnUrnField(stp.getId(), "port");
                String vlan = stp.getLayer2Info().getOuterVlanTag().getValue();
                portVlanMap.put(portName, vlan);
            }
            String jsonApplyCmd = null;
            String jsonDeleteCmd = null;
            String flowNamePrefix = contractId + "-switch-port-";
            for (String dpid: switchPortVlanMap.keySet()) {
                HashMap<String, String> portVlanMap = switchPortVlanMap.get(dpid);
                for (String inPort : portVlanMap.keySet()) {
                    String inVlan = portVlanMap.get(inPort);
                    String flowName = flowNamePrefix + dpid+"-"+inPort;
                    try {
                        Flow flowObj = new Flow(dpid, flowName, inPort, inVlan);
                        for (String outPort : portVlanMap.keySet()) {
                            String outVlan = portVlanMap.get(outPort);
                            if (outPort.equals(inPort)) {
                                continue;
                            }
                            flowObj.addOutPortAndVlan(outPort, outVlan);
                        }
                        if (jsonApplyCmd == null) {
                            jsonApplyCmd = "";
                            jsonDeleteCmd = "";
                        } else {
                            jsonApplyCmd += ";";
                            jsonDeleteCmd += ";";
                        }
                        jsonApplyCmd += flowObj.toString();
                        jsonDeleteCmd += String.format("{\"switch\":\"%s\",\"name\":\"%s\"}", dpid, flowName);
                    } catch (JSONException e) {
                        throw new DeviceException("JSONException caught in generateDelta(): " + e.getMessage());
                    }
                }
            }
            this.delta = new DeviceDelta();
            this.delta.setCmdToApply(jsonApplyCmd);
            this.delta.setCmdToDelete(jsonDeleteCmd);
            this.delta.setCmdToVerify(flowNamePrefix);
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
            String[] jsonCmdFlows = delta.getCmdToApply().split(";");
            for (String flowJson : jsonCmdFlows) {
                try {
                    Flow flowObj = new Flow(flowJson);
                    String response = connector.pushStaticFlow(flowObj, true);
                    JSONObject jsonResponse = new JSONObject(response);
                    String jsonError = null;
                    if (jsonResponse.has("error")) {
                        jsonError = jsonResponse.getString("error");
                    }
                    if (jsonError != null && jsonError.equalsIgnoreCase("true")) {
                        transferStatus("FAILED");
                        return;
                    }
                    String jsonStatus = null;
                    if (jsonResponse.has("status")) {
                       jsonStatus = jsonResponse.getString("status");
                    }
                    if (jsonStatus == null  || !jsonStatus.contains("pushed")) {
                        transferStatus("UNKNOWN");
                        return;
                    }
                } catch (JSONException e) {
                    throw new DeviceException("JSONException caught in applyDelta: " + e.getMessage());
                }
            }
        }
        transferStatus("APPLIED");
    }

    @Override
    public void deleteDelta() throws DeviceException {
        if (delta == null || this.delta.getCmdToDelete().isEmpty()) {
            throw new DeviceException("No device delta or empty GRi in CmdToDelete");
        }
        transferStatus("DELETING");
        boolean deleteError = false;
        synchronized(RESTConnector.simplyLock) {
            RESTConnector connector = RESTConnector.getRESTConnector();
            connector.setConfig(deviceRef.getConnectorConfig());
            String[] jsonCmdFlows = delta.getCmdToDelete().split(";");
            for (String flowJson : jsonCmdFlows) {
                try {
                    Flow flowObj = new Flow(flowJson);
                    String response = connector.pushStaticFlow(flowObj, false);
                    JSONObject jsonResponse = new JSONObject(response);
                    String jsonError = null;
                    if (jsonResponse.has("error")) {
                        jsonError = jsonResponse.getString("error");
                    }
                    if (jsonError != null && jsonError.equalsIgnoreCase("true")) {
                        deleteError = true;
                    }
                    String jsonStatus = null;
                    if (jsonResponse.has("status")) {
                       jsonStatus = jsonResponse.getString("status");
                    }
                    if (jsonStatus != null  || !jsonStatus.contains("deleted")) {
                        deleteError = true;
                    }
                } catch (JSONException e) {
                    throw new DeviceException("JSONException caught in deleteDelta: " + e.getMessage());
                }
            }
        }
        if (deleteError)
            transferStatus("UNKNOWN");
        else
            transferStatus("DELETED");
    }

    @Override
    public String verifyDelta() throws DeviceException {
        if (delta == null || this.delta.getCmdToVerify().isEmpty()) {
            throw new DeviceException("No device delta or empty GRi in CmdToVerify");
        }
        synchronized(RESTConnector.simplyLock) {
            RESTConnector connector = RESTConnector.getRESTConnector();
            connector.setConfig(deviceRef.getConnectorConfig());
            String response = connector.queryStaticFlowsAll(delta.getCmdToVerify());
            String[] jsonCmdFlows = delta.getCmdToApply().split(";");
            for (String flowJson : jsonCmdFlows) {
                try {
                    Flow flowObj = new Flow(flowJson);
                    String flowName = flowObj.getString("name");
                    if (!response.contains(String.format("\"%s\"", flowName))){
                        transferStatus("FAILED");        
                        throw new DeviceException(String.format("verifyDelta cannot find flow '%s'", flowName));
                    }
                } catch (JSONException e) {
                    throw new DeviceException("JSONException caught in verifyDelta: " + e.getMessage());
                }
            }
        }
        transferStatus("ACTIVE");        
        return status;
    }

    private void transferStatus(String status) {
        this.lastStatus = this.status;
        this.status = status;
    }
}
