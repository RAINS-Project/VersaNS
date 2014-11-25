/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import net.maxgigapop.versans.nps.device.junos.JunoscriptConnector;
import net.maxgigapop.versans.nps.device.junos.JunoscriptGenerator;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.api.ServicePolicy;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
import org.apache.log4j.Logger;

import javax.xml.xpath.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import java.io.StringReader;
import net.maxgigapop.versans.nps.manager.NPSUtils;

import org.ho.yaml.Yaml;

/**
 *
 * @author xyang
 */
public class RouterJUNOS implements NetworkDeviceInstance{
    protected String contractId = "";
    protected Device deviceRef = null;
    protected String status = "";
    protected String lastStatus = "";
    protected List<ServiceTerminationPoint> localSTPs = new ArrayList<ServiceTerminationPoint>();
    protected List<ServicePolicy> localPolicies = new ArrayList<ServicePolicy>();
    protected DeviceDelta delta = null;
    private org.apache.log4j.Logger log;

    public RouterJUNOS() {
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

    // provisioning actions
    @Override
    public void generateDelta() throws DeviceException{
        if (this.delta == null) {
            this.delta = NPSGlobalState.getDeviceDeltaStore().getByDeviceAndContractId(this.deviceRef.getId(), contractId);
        }
        if (this.delta == null) {
            this.delta = JunoscriptGenerator.generateDelta(contractId.replaceAll(":", "_"), localSTPs, localPolicies);
        }
    }
    
    @Override
    public DeviceDelta getDelta() throws DeviceException{
        if (delta == null)
            generateDelta();
        return delta;
    }
    
    @Override
    public void setDelta(DeviceDelta delta) throws DeviceException{
        this.delta = delta;
    }

    @Override
    public void applyDelta() throws DeviceException{
        if (delta == null)
            generateDelta();
        JunoscriptConnector connector = new JunoscriptConnector();
        transferStatus("APPLYING"); 
        connector.setConfig(deviceRef.getConnectorConfig());
        connector.sendApplyCommand(this);
        transferStatus("APPLIED"); 
    }

    @Override
    public void deleteDelta() throws DeviceException{
        if (delta == null)
            generateDelta();
        JunoscriptConnector connector = new JunoscriptConnector();
        transferStatus("DELETING"); 
        connector.setConfig(deviceRef.getConnectorConfig());
        connector.sendDeleteCommand(this);
        transferStatus("DELETED"); 
    }

    @Override
    public String verifyDelta() throws DeviceException{
        if (delta == null)
            generateDelta();
        JunoscriptConnector connector = new JunoscriptConnector();
        connector.setConfig(deviceRef.getConnectorConfig());
        String verifyResponse = connector.sendVerifyCommand(this);      
        
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(false);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(verifyResponse)));
            Map<String, String> xpathExprMap = (Map<String, String>) Yaml.load(delta.getXpathVerifyExpr());
            // active criterion: "//rpc-reply/bgp-information/bgp-peer[peer-address[starts-with(.,'"+neighbor_addr+"+')] and peer-state='ESTABLISHED']";
            XPath xpath = XPathFactory.newInstance().newXPath();
            String xpathActive = xpathExprMap.get("ACTIVE");
            //String xpathExist = xpathExprMap.get("EXIST"); // not used
            //String xpathInsetup = xpathExprMap.get("INSETUP"); // not used
            //String xpathFailed = xpathExprMap.get("FAILED"); // not used
            XPathExpression expr = xpath.compile(xpathActive);
            NodeList nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if (nodeList != null && nodeList.getLength() != 0) {
                transferStatus("ACTIVE");        
            } 
        } catch (Exception e) {
            throw new DeviceException("verifyDelta failed to parse response; exception:" + e.getMessage());
        }

        return status;
    }     
    
    private void transferStatus(String status) {
        this.lastStatus = this.status;
        this.status = status;
    }
}
