/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.manager;

import net.maxgigapop.versans.nps.device.NetworkDeviceInstance;
import net.maxgigapop.versans.nps.api.ServiceTerminationPoint;
import net.maxgigapop.versans.nps.api.ServicePolicy;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import org.w3c.dom.*;
import java.io.StringWriter;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author xyang
 */
public class NPSContract implements java.io.Serializable {
    private String id = "";
    private String description = "";
    // support only P2P for now
    private ServiceTerminationPoint providerSTP = null;
    private List<ServiceTerminationPoint> customerSTPs = null;
    private List<ServicePolicy> servicePolicies = null;
    private List<NetworkDeviceInstance> deviceProvisionSequence = null;
    private String status = "";
    private String error = "";
    private Timestamp modifiedTime = null;
    private String contractXml = "";
    private boolean deleted = false;
    private List<String> deletingDeltaTags = new ArrayList();
    
    public NPSContract() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ServiceTerminationPoint getProviderSTP() {
        return providerSTP;
    }

    public void setProviderSTP(ServiceTerminationPoint providerSTP) {
        this.providerSTP = providerSTP;
    }

    public List<ServiceTerminationPoint> getCustomerSTPs() {
        return customerSTPs;
    }

    public void setCustomerSTPs(List<ServiceTerminationPoint> customerSTPs) {
        this.customerSTPs = customerSTPs;
    }

    public List<ServicePolicy> getServicePolicies() {
        return servicePolicies;
    }

    public void setServicePolicies(List<ServicePolicy> servicePolicies) {
        this.servicePolicies = servicePolicies;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<NetworkDeviceInstance> getDeviceProvisionSequence() {
        return deviceProvisionSequence;
    }

    public void setDeviceProvisionSequence(List<NetworkDeviceInstance> deviceProvisionSequence) {
        this.deviceProvisionSequence = deviceProvisionSequence;
    }
    
    // PREPARING, STARTING, ACTIVE, TERMINATING, TERMINATED, ROLLBACKING, ROLLBACKED, FAILED, UNKNOWN
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Timestamp getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Timestamp modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public String getContractXml() {
        return contractXml;
    }

    public void setContractXml(String contractXml) {
        this.contractXml = contractXml;
    }

    public String getDeletingDeltaTags() {
        String ret = "";
        for (String tag: this.deletingDeltaTags) {
            ret = ret + "," + tag;
        }
        if (!ret.isEmpty())
            ret = ret.substring(1);
        return ret;
    }

    public void setDeletingDeltaTags(String deletingDeltaTags) {
        this.deletingDeltaTags.clear();
        if (deletingDeltaTags == null)
            return;
        String[] sa = deletingDeltaTags.split(",");
        if (sa == null)
            return;
        for (String s: sa)
            this.deletingDeltaTags.add(s);
    }

    public List<String> getDeletingDeltaTagList() {
        return deletingDeltaTags;
    }

    public void setDeletingDeltaTagList(List<String> deletingDeltaTagList) {
        this.deletingDeltaTags = deletingDeltaTagList;
    }
}
