
package net.maxgigapop.versans.nps.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for modifyContent complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="modifyContent">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="transactionId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="serviceContract" type="{http://maxgigapop.net/versans/nps/api/}serviceContract"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "modifyContent", propOrder = {
    "transactionId",
    "description",
    "serviceContract"
})
public class ModifyContent {

    @XmlElement(required = true)
    protected String transactionId;
    @XmlElement(required = true)
    protected String description;
    @XmlElement(required = true)
    protected ServiceContract serviceContract;

    /**
     * Gets the value of the transactionId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the value of the transactionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTransactionId(String value) {
        this.transactionId = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the serviceContract property.
     * 
     * @return
     *     possible object is
     *     {@link ServiceContract }
     *     
     */
    public ServiceContract getServiceContract() {
        return serviceContract;
    }

    /**
     * Sets the value of the serviceContract property.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceContract }
     *     
     */
    public void setServiceContract(ServiceContract value) {
        this.serviceContract = value;
    }

}
