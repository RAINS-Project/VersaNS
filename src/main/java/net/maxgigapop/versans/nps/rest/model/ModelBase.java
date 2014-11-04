/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.rest.model;

import java.util.*;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="model")
@XmlAccessorType(XmlAccessType.FIELD)
public class ModelBase {
    @XmlElement(required=true) 
    protected Long id = 0L;
    @XmlElement(required=true) 
    protected Date creationTime;
    @XmlElement(required=true) 
    protected Long cxtVersion = 0L;
    @XmlElement(required=true) 
    protected String cxtVersionTag = "";
    @XmlElement(required=true) 
    protected boolean committed = false;
    @XmlElement(required=true) 
    protected String ttlModel = "";

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getCxtVersionTag() {
        return cxtVersionTag;
    }
    
    public Long getCxtVersion() {
        return cxtVersion;
    }

    public void setCxtVersion(Long cxtVersion) {
        this.cxtVersion = cxtVersion;
    }

    public void setCxtVersionTag(String cxtVersionTag) {
        this.cxtVersionTag = cxtVersionTag;
    }

    public boolean isCommitted() {
        return committed;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    public String getTtlModel() {
        return ttlModel;
    }

    public void setTtlModel(String ttlModel) {
        this.ttlModel = ttlModel;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.versans.model.ModelBase[ id=" + id + " ]";
    }
}

