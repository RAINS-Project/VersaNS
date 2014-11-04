
package net.maxgigapop.versans.nps.api;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for serviceContract complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="serviceContract">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="providerSTP" type="{http://maxgigapop.net/versans/nps/api/}serviceTerminationPoint" minOccurs="0"/>
 *         &lt;element name="customerSTP" type="{http://maxgigapop.net/versans/nps/api/}serviceTerminationPoint" maxOccurs="unbounded"/>
 *         &lt;element name="policyData" type="{http://maxgigapop.net/versans/nps/api/}servicePolicy" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="type" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "serviceContract", propOrder = {
    "providerSTP",
    "customerSTP",
    "policyData"
})
public class ServiceContract {

    protected ServiceTerminationPoint providerSTP;
    @XmlElement(required = true)
    protected List<ServiceTerminationPoint> customerSTP;
    protected List<ServicePolicy> policyData;
    @XmlAttribute(name = "id", required = true)
    protected String id;
    @XmlAttribute(name = "type", required = true)
    protected String type;

    /**
     * Gets the value of the providerSTP property.
     * 
     * @return
     *     possible object is
     *     {@link ServiceTerminationPoint }
     *     
     */
    public ServiceTerminationPoint getProviderSTP() {
        return providerSTP;
    }

    /**
     * Sets the value of the providerSTP property.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceTerminationPoint }
     *     
     */
    public void setProviderSTP(ServiceTerminationPoint value) {
        this.providerSTP = value;
    }

    /**
     * Gets the value of the customerSTP property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the customerSTP property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCustomerSTP().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ServiceTerminationPoint }
     * 
     * 
     */
    public List<ServiceTerminationPoint> getCustomerSTP() {
        if (customerSTP == null) {
            customerSTP = new ArrayList<ServiceTerminationPoint>();
        }
        return this.customerSTP;
    }

    /**
     * Gets the value of the policyData property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the policyData property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPolicyData().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ServicePolicy }
     * 
     * 
     */
    public List<ServicePolicy> getPolicyData() {
        if (policyData == null) {
            policyData = new ArrayList<ServicePolicy>();
        }
        return this.policyData;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

}
