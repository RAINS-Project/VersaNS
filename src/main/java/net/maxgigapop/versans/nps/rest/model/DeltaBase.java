/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.rest.model;

import java.sql.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="delta")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeltaBase {
    @XmlElement(required=true) 
    protected Long id = 0L;
    @XmlElement(required=true) 
    protected String referenceVersion = "";
    @XmlElement(required=true) 
    protected String targetVersion = "";
    @XmlElement(required=true) 
    protected ModelBase modelAddition = null;
    @XmlElement(required=true) 
    protected ModelBase modelReduction = null;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceVersion() {
        return referenceVersion;
    }

    public void setReferenceVersion(String referenceVersion) {
        this.referenceVersion = referenceVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public ModelBase getModelAddition() {
        return modelAddition;
    }

    public void setModelAddition(ModelBase modelAddition) {
        this.modelAddition = modelAddition;
    }

    public ModelBase getModelReduction() {
        return modelReduction;
    }

    public void setModelReduction(ModelBase modelReduction) {
        this.modelReduction = modelReduction;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.versans.model.DeltaBase[ id=" + id + " ]";
    }
}
