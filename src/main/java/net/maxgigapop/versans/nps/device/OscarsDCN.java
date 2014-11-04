/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import net.maxgigapop.versans.nps.device.oscars.OscarsConnector;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.api.ServicePolicy;
import net.maxgigapop.versans.nps.manager.JAXBHelper;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;
import org.apache.log4j.Logger;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

import javax.xml.xpath.*;
import javax.xml.namespace.QName;
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
public class OscarsDCN implements NetworkDeviceInstance{

    protected String contractId = "";
    protected Device deviceRef = null;
    protected String status = "";
    protected String lastStatus = "";
    protected List<ServiceTerminationPoint> localSTPs = new ArrayList<ServiceTerminationPoint>();
    protected List<ServicePolicy> localPolicies = new ArrayList<ServicePolicy>();
    protected DeviceDelta delta = null;
    private org.apache.log4j.Logger log;

    public OscarsDCN() {
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
            String resCreateYaml = OscarsConnector.generateCreateResevationContent(contractId, localSTPs, localPolicies);
            if (this.delta == null) {
                this.delta = new DeviceDelta();
                this.delta.setCmdToApply(resCreateYaml);
            }
            File verifyXpathsFile = new File(NPSGlobalState.getTemplateDir()+"/oscars6-verify-xpaths.txt");
            try {
                delta.setXpathVerifyExpr(FileUtils.readFileToString(verifyXpathsFile));
            } catch (IOException ex) {
                throw new DeviceException(ex.getMessage());
            }
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
        OscarsConnector connector = new OscarsConnector();
        changeStatus("APPLYING"); 
        connector.setConfig(deviceRef.getConnectorConfig());
        String resCreateYaml = OscarsConnector.generateCreateResevationContent(contractId, localSTPs, localPolicies);
        if (this.delta == null) {
            this.delta = new DeviceDelta();
            this.delta.setCmdToApply(resCreateYaml);
        }
        String dcnStatus = connector.requestCreateReservation(resCreateYaml);
        if (dcnStatus.equalsIgnoreCase("Ok")) {
            String gri = connector.getGlobalReservationId();
            this.delta.setCmdToDelete(gri);
            this.delta.setCmdToVerify(gri);
        } else {
            changeStatus("FAILED"); 
            throw new DeviceException("OSCARS rejected CreateReservation requesst");            
        }
        changeStatus("APPLIED"); 
    }

    @Override
    public void deleteDelta() throws DeviceException{
        if (delta == null || this.delta.getCmdToDelete().isEmpty()) {
            throw new DeviceException("No device delta or empty GRi in CmdToDelete");
        }
        OscarsConnector connector = new OscarsConnector();
        changeStatus("DELETING");
        connector.setConfig(deviceRef.getConnectorConfig());
        connector.requestCancelReservation(this.delta.getCmdToDelete());
        changeStatus("DELETED"); 
    }

    @Override
    public String verifyDelta() throws DeviceException{
        if (delta == null || this.delta.getCmdToVerify().isEmpty()) {
            throw new DeviceException("No device delta or empty GRi in CmdToVerify");
        }
        OscarsConnector connector = new OscarsConnector();
        connector.setConfig(deviceRef.getConnectorConfig());
        String queryResponse = connector.requestQueryReservation(this.delta.getCmdToVerify());
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(false);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(queryResponse)));
            Map<String, String> xpathExprMap = (Map<String, String>) Yaml.load(delta.getXpathVerifyExpr());
            XPath xpath = XPathFactory.newInstance().newXPath();
            String xpathActive = xpathExprMap.get("ACTIVE");
            String xpathExist = xpathExprMap.get("EXIST");
            String xpathInsetup = xpathExprMap.get("INSETUP");
            String xpathFailed = xpathExprMap.get("FAILED");
            XPathExpression exprActive = xpath.compile(xpathActive);
            XPathExpression exprInsetup = xpath.compile(xpathInsetup);
            XPathExpression exprExist = xpath.compile(xpathExist);
            XPathExpression exprFailed = xpath.compile(xpathFailed);
            NodeList nodeListActive = (NodeList) exprActive.evaluate(doc, XPathConstants.NODESET);
            NodeList nodeListExist = (NodeList) exprExist.evaluate(doc, XPathConstants.NODESET);
            NodeList nodeListInsetup = (NodeList) exprInsetup.evaluate(doc, XPathConstants.NODESET);
            NodeList nodeListFailed = (NodeList) exprFailed.evaluate(doc, XPathConstants.NODESET);
            if (nodeListActive != null && nodeListActive.getLength() != 0) {
                changeStatus("ACTIVE");        
            }
            else if (nodeListExist == null || nodeListExist.getLength() == 0) {
                changeStatus("FAILED");
                throw new DeviceException("verifyDelta found no reservation data");
            }
            else if (nodeListInsetup != null && nodeListInsetup.getLength() != 0) {
                changeStatus("APPLYING");        
            }
            else if (nodeListFailed != null && nodeListFailed.getLength() != 0) {
                changeStatus("FAILED");        
                throw new DeviceException("verifyDelta saw OSCARS status='FAILED'");
            }
        } catch (Exception e) {
            changeStatus("FAILED");        
            throw new DeviceException("verifyDelta failed to parse response; exception:" + e.getMessage());
        }
        return status;
    }     
    
    private void changeStatus(String status) {
        this.lastStatus = this.status;
        this.status = status;
    	log.debug(String.format("%s status transition from %s to %s", this.contractId, this.lastStatus, this.status));
    }
}
