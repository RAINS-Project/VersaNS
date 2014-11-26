/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.config;

import java.util.Iterator;
import java.util.Map;
import net.maxgigapop.versans.nps.device.Interface;
import net.maxgigapop.versans.nps.manager.NPSGlobalState;

/**
 *
 * @author xyang
 */
public class NPSGlobalConfig {
    private String dbUser = null;
    private String dbPass = null;
    private String templateDir = null;
    private String providerDefaultBgpGroup = null;
    private String customerDefaultBgpGroup = null;
    private int pollInterval = 0;
    private int extendedPollInterval = 0;
    private Map devices = null;

    public String getDbPass() {
        return dbPass;
    }

    public void setDbPass(String dbPass) {
        this.dbPass = dbPass;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getTemplateDir() {
        return templateDir;
    }

    public void setTemplateDir(String templateDir) {
        this.templateDir = templateDir;
    }

    public String getCustomerDefaultBgpGroup() {
        return customerDefaultBgpGroup;
    }

    public void setCustomerDefaultBgpGroup(String customerDefaultBgpGroup) {
        this.customerDefaultBgpGroup = customerDefaultBgpGroup;
    }

    public String getProviderDefaultBgpGroup() {
        return providerDefaultBgpGroup;
    }

    public void setProviderDefaultBgpGroup(String providerDefaultBgpGroup) {
        this.providerDefaultBgpGroup = providerDefaultBgpGroup;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getExtendedPollInterval() {
        return extendedPollInterval;
    }

    public void setExtendedPollInterval(int extendedPollInterval) {
        this.extendedPollInterval = extendedPollInterval;
    }


    public Map getDevices() {
        return devices;
    }

    public void setDevices(Map devices) {
        this.devices = devices;
    }
    
    synchronized public boolean isProviderFacingInterface(String anIfUrn) {
        Iterator devIt = this.devices.keySet().iterator();
        while (devIt.hasNext()) {
            String dName = (String) devIt.next();
            Map deviceCfg = (Map) devices.get(dName);
            Map interfaces = (Map)deviceCfg.get("interfaces");
            if (interfaces == null)
                continue;
            Iterator ifIt = interfaces.keySet().iterator();
            while (ifIt.hasNext()) {
                String iName = (String) ifIt.next();
                Map intfCfg = (Map) interfaces.get(iName);
                String ifUrn = (String)intfCfg.get("urn");
                if (ifUrn != null && ifUrn.equals(anIfUrn)) {
                    String isProviderFacing = (String)intfCfg.get("provider_facing");
                    if (isProviderFacing != null && isProviderFacing.toLowerCase().equals("true"))
                        return true;
                    break;
                }
            }
        }
        return false;
    }
}
