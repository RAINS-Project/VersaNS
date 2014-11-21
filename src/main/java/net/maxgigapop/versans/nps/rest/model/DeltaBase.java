/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.rest.model;

import java.util.*;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="delta")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeltaBase {
    @XmlElement(required=true) 
    protected Long id = 0L;
    @XmlElement(required=true) 
    protected Date creationTime;
    @XmlElement(required=true)
    protected String referenceVersion = "";
    @XmlElement(required=true) 
    protected String modelReduction = null;
    @XmlElement(required=true) 
    protected String modelAddition = null;
    @XmlElement(required=true) 
    protected String status = "";

    //$$ TODO: add client id to make unique delta for multi-clients
        // or change id long -> string / uuid

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

    public String getModelAddition() {
        return modelAddition;
    }

    public void setModelAddition(String modelAddition) {
        this.modelAddition = modelAddition;
    }

    public String getModelReduction() {
        return modelReduction;
    }

    public void setModelReduction(String modelReduction) {
        this.modelReduction = modelReduction;
    }

    public String getReferenceVersion() {
        return referenceVersion;
    }

    public void setReferenceVersion(String referenceVersion) {
        this.referenceVersion = referenceVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.versans.model.DeltaBase[ id=" + id + " ]";
    }
}
