
package net.maxgigapop.versans.nps.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for layer2Info complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="layer2Info">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="outerVlanTag" type="{http://maxgigapop.net/versans/nps/api/}vlanTag" minOccurs="0"/>
 *         &lt;element name="innerVlanTag" type="{http://maxgigapop.net/versans/nps/api/}vlanTag" minOccurs="0"/>
 *         &lt;element name="mtu" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "layer2Info", propOrder = {
    "outerVlanTag",
    "innerVlanTag",
    "mtu"
})
public class Layer2Info {

    protected VlanTag outerVlanTag;
    protected VlanTag innerVlanTag;
    protected String mtu;

    /**
     * Gets the value of the outerVlanTag property.
     * 
     * @return
     *     possible object is
     *     {@link VlanTag }
     *     
     */
    public VlanTag getOuterVlanTag() {
        return outerVlanTag;
    }

    /**
     * Sets the value of the outerVlanTag property.
     * 
     * @param value
     *     allowed object is
     *     {@link VlanTag }
     *     
     */
    public void setOuterVlanTag(VlanTag value) {
        this.outerVlanTag = value;
    }

    /**
     * Gets the value of the innerVlanTag property.
     * 
     * @return
     *     possible object is
     *     {@link VlanTag }
     *     
     */
    public VlanTag getInnerVlanTag() {
        return innerVlanTag;
    }

    /**
     * Sets the value of the innerVlanTag property.
     * 
     * @param value
     *     allowed object is
     *     {@link VlanTag }
     *     
     */
    public void setInnerVlanTag(VlanTag value) {
        this.innerVlanTag = value;
    }

    /**
     * Gets the value of the mtu property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMtu() {
        return mtu;
    }

    /**
     * Sets the value of the mtu property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMtu(String value) {
        this.mtu = value;
    }

}
