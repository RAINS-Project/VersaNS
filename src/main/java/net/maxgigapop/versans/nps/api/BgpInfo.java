
package net.maxgigapop.versans.nps.api;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for bgpInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="bgpInfo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="groupName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="linkLocalIpAndMask" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="linkRemoteIpAndMask" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="peerASN" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="peerPrefixListName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="peerIpPrefix" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "bgpInfo", propOrder = {
    "groupName",
    "linkLocalIpAndMask",
    "linkRemoteIpAndMask",
    "peerASN",
    "peerPrefixListName",
    "peerIpPrefix"
})
public class BgpInfo {

    protected String groupName;
    protected String linkLocalIpAndMask;
    protected String linkRemoteIpAndMask;
    protected String peerASN;
    protected String peerPrefixListName;
    protected List<String> peerIpPrefix;

    /**
     * Gets the value of the groupName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Sets the value of the groupName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGroupName(String value) {
        this.groupName = value;
    }

    /**
     * Gets the value of the linkLocalIpAndMask property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLinkLocalIpAndMask() {
        return linkLocalIpAndMask;
    }

    /**
     * Sets the value of the linkLocalIpAndMask property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLinkLocalIpAndMask(String value) {
        this.linkLocalIpAndMask = value;
    }

    /**
     * Gets the value of the linkRemoteIpAndMask property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLinkRemoteIpAndMask() {
        return linkRemoteIpAndMask;
    }

    /**
     * Sets the value of the linkRemoteIpAndMask property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLinkRemoteIpAndMask(String value) {
        this.linkRemoteIpAndMask = value;
    }

    /**
     * Gets the value of the peerASN property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPeerASN() {
        return peerASN;
    }

    /**
     * Sets the value of the peerASN property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPeerASN(String value) {
        this.peerASN = value;
    }

    /**
     * Gets the value of the peerPrefixListName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPeerPrefixListName() {
        return peerPrefixListName;
    }

    /**
     * Sets the value of the peerPrefixListName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPeerPrefixListName(String value) {
        this.peerPrefixListName = value;
    }

    /**
     * Gets the value of the peerIpPrefix property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the peerIpPrefix property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPeerIpPrefix().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getPeerIpPrefix() {
        if (peerIpPrefix == null) {
            peerIpPrefix = new ArrayList<String>();
        }
        return this.peerIpPrefix;
    }

}
