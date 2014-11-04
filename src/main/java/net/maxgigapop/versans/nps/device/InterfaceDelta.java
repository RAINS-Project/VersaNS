/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

/**
 *
 * @author xyang
 */
public class InterfaceDelta implements java.io.Serializable {
    int id = 0;
    int interfaceId = 0;
    int deviceDeltaId = 0;
    String cmdToApply = "";
    String cmdToDelete = "";
    String cmdToVerify = "";
    String xpathVerifyExpr = "";
    boolean deleted = false;
    DeviceDelta deviceDelta = null;

    public String getCmdToApply() {
        return cmdToApply;
    }

    public void setCmdToApply(String cmdToApply) {
        this.cmdToApply = cmdToApply;
    }

    public String getCmdToDelete() {
        return cmdToDelete;
    }

    public void setCmdToDelete(String cmdToDelete) {
        this.cmdToDelete = cmdToDelete;
    }

    public String getCmdToVerify() {
        return cmdToVerify;
    }

    public void setCmdToVerify(String cmdToVerify) {
        this.cmdToVerify = cmdToVerify;
    }

    public String getXpathVerifyExpr() {
        return xpathVerifyExpr;
    }

    public void setXpathVerifyExpr(String xpathVerifyExpr) {
        this.xpathVerifyExpr = xpathVerifyExpr;
    }


    public int getDeviceDeltaId() {
        return deviceDeltaId;
    }

    public void setDeviceDeltaId(int deviceDeltaId) {
        this.deviceDeltaId = deviceDeltaId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public DeviceDelta getDeviceDelta() {
        return deviceDelta;
    }

    public void setDeviceDelta(DeviceDelta deviceDelta) {
        this.deviceDelta = deviceDelta;
    }

    public int getInterfaceId() {
        return interfaceId;
    }

    public void setInterfaceId(int interfaceId) {
        this.interfaceId = interfaceId;
    }
    
}
