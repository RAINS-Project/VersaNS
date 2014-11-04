/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.config;

import java.util.Map;

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
}
