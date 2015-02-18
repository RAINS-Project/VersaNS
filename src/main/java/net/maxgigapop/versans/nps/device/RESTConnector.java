/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import net.maxgigapop.versans.nps.device.DeviceException;
import net.maxgigapop.versans.nps.device.floodlight.Flow;
import org.apache.log4j.Logger;
import sun.misc.BASE64Encoder;

/**
 *
 * @author xyang
 */
public class RESTConnector {
    private Logger log = null;
    private String controllerUrl = null;
    private HashMap<String, String> dpidMap = null;
    private String httpUser = null;
    private String httpPass = null;
    private HttpURLConnection httpConn;
    private DataOutputStream httpOut = null;
    private BufferedReader httpIn = null;
    static protected RESTConnector instance = null;
    static public final String simplyLock = "RESTConnector Singleton Lock";
   
    protected RESTConnector() {
        this.log = org.apache.log4j.Logger.getLogger(this.getClass());
    }

    static public RESTConnector getRESTConnector() {
        if (instance == null) {
            instance = new RESTConnector();
        }
        return instance;
    }
    
    public void setConfig(Map connectorConfig) throws DeviceException {
        if (connectorConfig == null) {
            throw new DeviceException("no config set!");
        }
        this.controllerUrl = (String) connectorConfig.get("controller_url");
        this.dpidMap = (HashMap) connectorConfig.get("dpid_map");
        this.httpUser = (String) connectorConfig.get("http_user");
        this.httpPass = (String) connectorConfig.get("password");
        if (this.controllerUrl == null || this.controllerUrl.isEmpty()) {
            throw new DeviceException("controller_url not configured");
        }
        if (this.dpidMap == null || this.dpidMap.isEmpty()) {
            throw new DeviceException("dpid_map not configured");
        }
    }

    // TODO: SSL and HTTP authentication support
    protected void connect(String url, String method) throws DeviceException {
        try {
            URL address = new URL(url);
            httpConn = (HttpURLConnection)address.openConnection();
            if (this.httpUser != null) {
                String authString = this.httpUser + ":" + this.httpPass;
                System.out.println("Auth string: " + authString);
    
                String authStringEnc = new BASE64Encoder().encode(authString.getBytes());
                System.out.println("Base64 encoded auth string: " + authStringEnc);
 
                httpConn.setRequestProperty("Authorization", "Basic " + authStringEnc);
            }
            if (method.equalsIgnoreCase("DELETE")) {
                httpConn.setRequestMethod("POST");
                httpConn.setRequestProperty("X-HTTP-Method-Override", "DELETE");
            } else {
                httpConn.setRequestMethod(method);
            }
            httpConn.setRequestProperty("Content-Type", "application/json");
            if (method.equalsIgnoreCase("GET")) {
                httpConn.setDoOutput(false);
            } else {
                httpConn.setDoOutput(true);                
            }
            httpConn.setReadTimeout(5000);
            if (method.equalsIgnoreCase("GET")) {
                httpConn.connect();
            } else {
                httpOut = new DataOutputStream(httpConn.getOutputStream());
            }
            this.log.info("connected to REST controller at " + url);
        } catch (Exception e) {
            httpConn = null;
            httpOut = null;
            this.log.error(String.format("exception when connecting to '%s': %s", url, e.getCause()));
            throw new DeviceException(String.format("exception when connecting to '%s': %s", url, e.getCause()));
        }
    }
    
    protected void disconnect() {
        String event = "JSONConnector.disconnect";
        try {
            if (httpOut != null) {
                httpOut.close();
                httpOut = null;
            }
            if (httpIn != null) {
                httpIn.close();
                httpIn = null;
            }
            if (httpConn != null) {
                httpConn.disconnect();
                httpConn = null;
            }
            this.log.info("Disconnected from controller");
        } catch (Exception e) {
            httpConn = null;
            httpOut = null;
            httpIn = null;
            this.log.error("Exception when discconnecting from controller : " + e.getMessage());
        }
    }

    public String sendCommand(String command) throws DeviceException {                
        String responseString = "";
        try {
            if (httpConn == null) {
                throw new DeviceException("HTTP socket connection not ready!");
            }
            if (command != null && !command.isEmpty()) {
                httpOut.writeBytes(command);
                httpOut.flush();
            }
            this.log.info("command sent to REST controller: " + command);
            httpIn = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String line;
            while ((line = httpIn.readLine()) != null) {
                responseString += line;
            }
            this.log.info("response from REST controller: " + responseString);
        } catch (IOException e) {
            log.error("IOException when sending command to Controller, msg=" + e.getMessage());
            this.disconnect();
            throw new DeviceException("IOException when sending command to Controller, msg=" + e.getMessage());
        } finally {
            this.disconnect();
        }
        return responseString;
    }

    public String pushStaticFlow(Flow flowObj, boolean doAdd) throws DeviceException {
        String url = controllerUrl+"/wm/staticflowentrypusher/json";
        String response;
        log.debug(String.format("Sending %s command: %s", doAdd?"add-flow":"del-flow", flowObj.toString()));
        if (doAdd) {
            this.connect(url, "POST");
        } else {
            this.connect(url, "DELETE");
        }
        response = this.sendCommand(flowObj.toString());
        log.debug(String.format("Received response: %s", response));
        return response;
    }


    public String queryStaticFlows(String flowNamePrefix, String switchDpid) throws DeviceException {
        String url = controllerUrl+String.format("/wm/staticflowentrypusher/list/%s/json",switchDpid);
        this.connect(url, "GET");
        String response = this.sendCommand("");
        log.debug(String.format("Received query-flows response: %s", response));
        return response;
    }
    
    public String queryStaticFlowsAll(String flowNamePrefix) throws DeviceException {
        String response = "";
        for (String dpid: this.dpidMap.values()) {
            if (!response.isEmpty()) {
                response += ";";
            }
            response += queryStaticFlows(flowNamePrefix, dpid);
        }
        return response;
    }
}
