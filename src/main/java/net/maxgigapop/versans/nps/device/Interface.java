/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

/**
 *
 * @author xyang
 */
public class Interface implements java.io.Serializable  {
    int id = 0;
    String urn = "";
    String model = "";
    int deviceId = 0;
    String description = "";
    Device parentDevice = null;

    public Interface() {}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public Device getParentDevice() {
        return parentDevice;
    }

    public void setParentDevice(Device parentDevice) {
        this.parentDevice = parentDevice;
    }
}
