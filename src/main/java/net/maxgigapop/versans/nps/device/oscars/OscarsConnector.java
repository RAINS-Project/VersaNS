/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device.oscars;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.maxgigapop.versans.nps.api.ServicePolicy;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.device.DeviceException;
import net.maxgigapop.versans.nps.manager.NPSUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author xyang
 */
public class OscarsConnector {
    private String idcUrl = "";
    private String idcClientCommand = "";
    private String clientKeystore = "";
    private String clientKeystorePass = "";
    private String clientKeystoreUser = "";
    private String trustKeystore = "";
    private String trustKeystorePass = "";
    private String gri = null;
    private Logger log = Logger.getLogger(OscarsConnector.class);
    static public int defaultBandwidth = 100; // 100 Mbps
    static public String defaultDuration = "5:00:00"; // 5 days
    public OscarsConnector() { }

    // place holder
    static private String yamlTemplate = "---\n" 
    		+ "command:   <_command_>\n"
    		+ "url:        '<_idc_url_>'\n"
    		+ "keystore:   <_client_keystore_>\n"
    		+ "keystore-password: <_client_keystore_pass_>\n"
    		+ "key-alias: <_client_keystore_user_>\n"
    		+ "truststore: <_ssl_keystore_>\n"
    		+ "truststore-password: <_ssl_keystore_pass_>\n"
    		+ "output-format: rawResponse\n"
    		+ "version:    0.6\n"
    		;

    public void setConfig(Map connectorConfig) throws DeviceException {
        if (connectorConfig == null) {
            throw new DeviceException("no config set!");
        } 
        
        this.idcUrl    = (String) connectorConfig.get("idc_url");
        this.idcClientCommand     = (String) connectorConfig.get("client_command");
        this.clientKeystore     = (String) connectorConfig.get("client_keystore");
        this.clientKeystorePass     = (String) connectorConfig.get("client_keystore_password");
        this.clientKeystoreUser     = (String) connectorConfig.get("client_keystore_user");
        this.trustKeystore     = (String) connectorConfig.get("trust_keystore");
        this.trustKeystorePass     = (String) connectorConfig.get("trust_keystore_password");
        if (this.idcUrl == null || this.idcUrl.isEmpty()) {
            throw new DeviceException("idc_url null or empty");
        }
        if (this.idcClientCommand == null || this.idcClientCommand.isEmpty()) {
            throw new DeviceException("client_command null or empty");
        }
        if (this.clientKeystore == null || this.clientKeystore.isEmpty()) {
            throw new DeviceException("client_keystore null or empty");
        }
        if (this.clientKeystorePass == null || this.clientKeystorePass.isEmpty()) {
            throw new DeviceException("client_keystore_pass null or empty");
        }
        if (this.clientKeystoreUser == null || this.clientKeystoreUser.isEmpty()) {
            throw new DeviceException("client_keystore_user null or empty");
        }
        if (this.trustKeystore == null || this.trustKeystore.isEmpty()) {
            throw new DeviceException("trust_keystore null or empty");
        }
        if (this.trustKeystorePass == null || this.trustKeystorePass.isEmpty()) {
            throw new DeviceException("trust_keystore_pass null or empty");
        }
    }

    private String executeShellCommandWithInput(String cmd, String yaml) throws DeviceException {
    	Process p = null;
    	ReadStream rsIn = null;
    	ReadStream rsErr = null;
    	try {
	    	p = Runtime.getRuntime().exec(cmd);
	    	OutputStream os = p.getOutputStream();
	    	OutputStreamWriter out = new OutputStreamWriter (os);
	    	out.write(yaml);
	    	out.flush();
	    	out.close();
	    	rsIn = new ReadStream("stdin", p.getInputStream ());
	    	rsErr = new ReadStream("stderr", p.getErrorStream ());
	    	rsIn.start ();
	    	rsErr.start ();
    		p.waitFor();
    	} catch (Exception e) {  
    		throw new DeviceException("executeShellCommandWithInput Exception: "+e.getMessage());
    	} finally {
    		if (p != null) {
    			p.destroy();
    			try {
	    			rsIn.interrupt();
    			} catch (Exception e) {
    				;
    			}
    			try {
	    			rsErr.interrupt();
    			} catch (Exception e) {
    				;
    			}
    		}
    	}
    	if (rsErr.getString().contains("Exception") || rsIn.getString().isEmpty())
    		return rsErr.getString();
    	return rsIn.getString();
    }
    
    private String extractXmlValueByTag(String xml, String tag) {
        int pos1 = xml.indexOf(String.format("<%s", tag));
        if (pos1 == -1) {
            return "";
        }
        int taglen = xml.indexOf(String.format(">", tag), pos1) - pos1+1;
        int pos2 = xml.indexOf(String.format("</%s>", tag), pos1);
        if (pos2 == -1) {
            return "";
        }
        return xml.substring(pos1 + taglen, pos2);
    }
    
    /**
     * createReservation
     */
    public String requestCreateReservation(String requestYaml) 
        throws DeviceException {
    	String createYaml = fillSecurityInfo(yamlTemplate);
    	createYaml = createYaml.replaceAll("<_command_>", "createReservation");
    	createYaml += requestYaml;
    	String responseXml = executeShellCommandWithInput(this.idcClientCommand, createYaml);
    	if (responseXml.isEmpty() || responseXml.contains("Exception"))
    		return "FAILED";
    	String status = extractXmlValueByTag(responseXml, "ns3:status");
    	if (status.isEmpty())
    		return "UNKNOWN";
        // Extract GRI
        this.gri = extractXmlValueByTag(responseXml, "ns3:globalReservationId");
        return status; 
    }
    
    public String getGlobalReservationId() {
        return gri;
    }

    /**
     *  cancelReservation
     *
     */
    public String requestCancelReservation(String aGri)
        throws DeviceException {
        
        if (aGri == null || aGri.isEmpty()) {            
            throw new DeviceException("request to cancel with null / empty GRI");
        }
        this.gri = aGri;
    	String cancelYaml = fillSecurityInfo(yamlTemplate);
    	cancelYaml = cancelYaml.replaceAll("<_command_>", "cancelReservation");
    	cancelYaml += String.format("gri:  '%s'\n", aGri);
    	return executeShellCommandWithInput(this.idcClientCommand, cancelYaml);
    }

    /**
     * queryReservation
     * 
     */
    public String requestQueryReservation(String aGri)
        throws DeviceException {
        /* Send Request */
        if (aGri == null || aGri.isEmpty()) {            
            throw new DeviceException("request to query with null / empty GRI");
        }
        gri = aGri;

    	String queryYaml = fillSecurityInfo(yamlTemplate);
    	queryYaml = queryYaml.replaceAll("<_command_>", "queryReservation");
    	queryYaml += String.format("gri:  '%s'\n", aGri);
    	return executeShellCommandWithInput(this.idcClientCommand, queryYaml);
    }
    
    public String fillSecurityInfo(String yamlTemplate)
            throws DeviceException {
        	String yamlStr = yamlTemplate.replaceAll("<_idc_url_>", idcUrl);
        	yamlStr = yamlStr.replaceAll("<_client_keystore_>", clientKeystore);
        	yamlStr = yamlStr.replaceAll("<_client_keystore_pass_>", clientKeystorePass);
        	yamlStr = yamlStr.replaceAll("<_client_keystore_user_>", clientKeystoreUser);
        	yamlStr = yamlStr.replaceAll("<_ssl_keystore_>", trustKeystore);
        	yamlStr = yamlStr.replaceAll("<_ssl_keystore_pass_>", trustKeystorePass);
        	return yamlStr;
    }

    /**
     * generateCreateResevationContent
     * 
     */
    static public String generateCreateResevationContent(String contractId, 
            List<ServiceTerminationPoint> stpList, List<ServicePolicy> policyList) 
        throws DeviceException {
        if (stpList.size() != 2) {
            throw new DeviceException("DCN circuit requires exactly two STPs");
        }
        ServiceTerminationPoint stpSrc = stpList.get(0);
        ServiceTerminationPoint stpDst = stpList.get(1);
        String srcUrn = stpSrc.getId();
        String dstUrn = stpDst.getId();
        String srcVlan = "any";
        String dstVlan = "any";
        String descr = "MSX/NPS L2DCN Contract ID:"+contractId;
        if (stpSrc.getLayer2Info() != null && stpSrc.getLayer2Info().getOuterVlanTag() != null) {
            srcVlan = stpSrc.getLayer2Info().getOuterVlanTag().getValue();
            if (srcVlan.equalsIgnoreCase("untagged"))
                srcVlan = "0";
        }
        if (stpDst.getLayer2Info() != null && stpDst.getLayer2Info().getOuterVlanTag() != null) {
            dstVlan = stpDst.getLayer2Info().getOuterVlanTag().getValue();
            if (dstVlan.equalsIgnoreCase("untagged"))
                dstVlan = "0";
        }
        int bw = defaultBandwidth; // 10 mbps by default
        if (policyList != null) {
            Map mapInput = new HashMap();
            for (ServicePolicy policy: policyList) {
                if (policy.getSubject().equalsIgnoreCase("provider")
                        && policy.getAction().equalsIgnoreCase("limit")
                        && policy.getConstraintType().equalsIgnoreCase("bandwidth")) {
                    bw = (int)(NPSUtils.bandwdithToBps(policy.getConstraintValue())/1000000);
                }
            }
        }

        String createYaml = "login: 'client'\n";
    	createYaml += "layer: 2\n";
    	createYaml += String.format("bandwidth: %d\n", bw);
    	createYaml += String.format("src: '%s'\n", srcUrn);
    	createYaml += String.format("dst: '%s'\n", dstUrn);
    	createYaml += String.format("description: \"%s\"\n", descr);
    	createYaml += String.format("srcvlan: '%s'\n", srcVlan);
    	createYaml += String.format("dstvlan: '%s'\n", dstVlan);
    	createYaml += String.format("start-time: 'now'\n");
    	createYaml += String.format("end-time: '+%s'\n", defaultDuration);
    	createYaml += "path-setup-mode: 'timer-automatic'\n";
    	createYaml += "path-type: 'strict'\n";
    	return createYaml;
    }
}
