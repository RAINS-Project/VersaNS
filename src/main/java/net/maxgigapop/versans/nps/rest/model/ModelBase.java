/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.rest.model;

import java.util.*;
import javax.persistence.OneToOne;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="model")
@XmlAccessorType(XmlAccessType.FIELD)
public class ModelBase {
    @XmlElement(required=true) 
    protected Long id = 0L;
    @XmlElement(required=true) 
    protected Date creationTime;
    @XmlElement(required=true) 
    protected String version = "";
    @XmlElement(required=true) 
    protected String ttlModel = "";
    @XmlElement(required=true) 
    protected String status = "";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTtlModel() {
        return ttlModel;
    }

    public void setTtlModel(String ttlModel) {
        this.ttlModel = ttlModel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.versans.model.ModelBase[ id=" + id + " ]";
    }
}

