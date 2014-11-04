
package net.maxgigapop.versans.nps.api;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for servicePolicy complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="servicePolicy">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="subject" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="resourceRef" type="{http://maxgigapop.net/versans/nps/api/}resourceReference" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="action" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="constraintType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="constraintValue" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "servicePolicy", propOrder = {
    "subject",
    "resourceRef",
    "action",
    "constraintType",
    "constraintValue"
})
public class ServicePolicy {

    protected String subject;
    protected List<ResourceReference> resourceRef;
    @XmlElement(required = true)
    protected String action;
    @XmlElement(required = true)
    protected String constraintType;
    @XmlElement(required = true)
    protected String constraintValue;

    /**
     * Gets the value of the subject property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the value of the subject property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSubject(String value) {
        this.subject = value;
    }

    /**
     * Gets the value of the resourceRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the resourceRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getResourceRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ResourceReference }
     * 
     * 
     */
    public List<ResourceReference> getResourceRef() {
        if (resourceRef == null) {
            resourceRef = new ArrayList<ResourceReference>();
        }
        return this.resourceRef;
    }

    /**
     * Gets the value of the action property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the value of the action property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAction(String value) {
        this.action = value;
    }

    /**
     * Gets the value of the constraintType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConstraintType() {
        return constraintType;
    }

    /**
     * Sets the value of the constraintType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConstraintType(String value) {
        this.constraintType = value;
    }

    /**
     * Gets the value of the constraintValue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConstraintValue() {
        return constraintValue;
    }

    /**
     * Sets the value of the constraintValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConstraintValue(String value) {
        this.constraintValue = value;
    }

}
