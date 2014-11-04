
package net.maxgigapop.versans.nps.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for layer3Info complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="layer3Info">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="bgpInfo" type="{http://maxgigapop.net/versans/nps/api/}bgpInfo"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "layer3Info", propOrder = {
    "bgpInfo"
})
public class Layer3Info {

    @XmlElement(required = true)
    protected BgpInfo bgpInfo;

    /**
     * Gets the value of the bgpInfo property.
     * 
     * @return
     *     possible object is
     *     {@link BgpInfo }
     *     
     */
    public BgpInfo getBgpInfo() {
        return bgpInfo;
    }

    /**
     * Sets the value of the bgpInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link BgpInfo }
     *     
     */
    public void setBgpInfo(BgpInfo value) {
        this.bgpInfo = value;
    }

}
