
package net.maxgigapop.versans.nps.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for serviceTerminationPoint complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="serviceTerminationPoint">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="interfaceRef" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="layer2Info" type="{http://maxgigapop.net/versans/nps/api/}layer2Info" minOccurs="0"/>
 *         &lt;element name="layer3Info" type="{http://maxgigapop.net/versans/nps/api/}layer3Info" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "serviceTerminationPoint", propOrder = {
    "interfaceRef",
    "layer2Info",
    "layer3Info"
})
public class ServiceTerminationPoint {

    @XmlElement(required = true)
    protected String interfaceRef;
    protected Layer2Info layer2Info;
    protected Layer3Info layer3Info;
    @XmlAttribute(name = "id", required = true)
    protected String id;

    /**
     * Gets the value of the interfaceRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInterfaceRef() {
        return interfaceRef;
    }

    /**
     * Sets the value of the interfaceRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInterfaceRef(String value) {
        this.interfaceRef = value;
    }

    /**
     * Gets the value of the layer2Info property.
     * 
     * @return
     *     possible object is
     *     {@link Layer2Info }
     *     
     */
    public Layer2Info getLayer2Info() {
        return layer2Info;
    }

    /**
     * Sets the value of the layer2Info property.
     * 
     * @param value
     *     allowed object is
     *     {@link Layer2Info }
     *     
     */
    public void setLayer2Info(Layer2Info value) {
        this.layer2Info = value;
    }

    /**
     * Gets the value of the layer3Info property.
     * 
     * @return
     *     possible object is
     *     {@link Layer3Info }
     *     
     */
    public Layer3Info getLayer3Info() {
        return layer3Info;
    }

    /**
     * Sets the value of the layer3Info property.
     * 
     * @param value
     *     allowed object is
     *     {@link Layer3Info }
     *     
     */
    public void setLayer3Info(Layer3Info value) {
        this.layer3Info = value;
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

}
