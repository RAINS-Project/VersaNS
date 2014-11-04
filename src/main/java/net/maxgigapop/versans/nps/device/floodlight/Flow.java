/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device.floodlight;

//import net.sf.json.JSONObject;
import java.util.HashMap;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author xyang
 */
public class Flow extends JSONObject{
    private HashMap<String, String> portVlanMap;
    private org.apache.log4j.Logger log;

    public Flow() { 
        this.log = org.apache.log4j.Logger.getLogger(this.getClass());
        portVlanMap = new HashMap<String, String>();
    }
    
    public Flow(String jsonStr) throws JSONException {
        super(jsonStr);
        this.log = org.apache.log4j.Logger.getLogger(this.getClass());
    }
    
    public Flow(String s, String n, String p,  String v) throws JSONException {
        this.log = org.apache.log4j.Logger.getLogger(this.getClass());
        this.put("switch", s);
        this.put("name", n);
        this.put("ingress-port", p);
        this.put("vlan-id", v);
        this.put("priority", "32768");
        this.put("active", "true");
        this.put("cookie", "0");
        portVlanMap = new HashMap<String, String>();
        portVlanMap.put(p, v);
    }
    
    public Flow(String s, String n, String p,  String o, String v) throws JSONException {
        this.log = org.apache.log4j.Logger.getLogger(this.getClass());
        this.put("switch", s);
        this.put("name", n);
        this.put("ingress-port", p);
        this.put("vlan-id", v);
        this.put("actions", String.format("set-vlan-id=%s,output=%s", v, o));
        this.put("priority", "32768");
        this.put("active", "true");
        this.put("cookie", "0");
        portVlanMap = new HashMap<String, String>();
        portVlanMap.put(p, v);
        portVlanMap.put(o, v);
    }
    
    public void addOutPortAndVlan(String port, String vlan) throws JSONException {
        if (vlan == null && portVlanMap.get("vlan-id") != null) {
            vlan = (String)portVlanMap.get("vlan-id");
        }
        if (vlan != null && portVlanMap.get(port) == null) {
            portVlanMap.put(port, vlan);
            String actions = "";
            if (this.has("actions")) {
                actions = (String) this.get("actions");
                actions += ",";
            }
            actions += String.format("set-vlan-id=%s,output=%s", vlan, port);
            this.put("actions", actions);
        }
    }
}
