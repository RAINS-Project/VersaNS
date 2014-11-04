/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import java.util.List;

/**
 *
 * @author xyang
 */
public class DeviceDelta implements java.io.Serializable {
    int id = 0;
    int deviceId = 0;
    String contractId = "";
    String cmdToApply = "";
    String cmdToDelete = "";
    String cmdToVerify = "";
    String xpathVerifyExpr = "";
    List<InterfaceDelta> interfaceDeltas = null;   
    boolean deleted = false;

    public DeviceDelta() {}

    
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

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<InterfaceDelta> getInterfaceDeltas() {
        return interfaceDeltas;
    }
    
}
