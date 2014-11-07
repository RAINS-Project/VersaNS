/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author xyang
 */
public class Device implements java.io.Serializable  {
    int id = 0;
    String urn = "";
    String makeModel = "";
    String address = "";
    String location = "";
    String description = "";
    List<Interface> interfaces = new ArrayList<Interface>();
    Map connectorConfig = null;

    public Device() {}

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMakeModel() {
        return makeModel;
    }

    public void setMakeModel(String makeModel) {
        this.makeModel = makeModel;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public List<Interface> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<Interface> interfaces) {
        this.interfaces = interfaces;
    }

    public Map getConnectorConfig() {
        return connectorConfig;
    }

    public void setConnectorConfig(Map connectorConfig) {
        this.connectorConfig = connectorConfig;
    }
}
